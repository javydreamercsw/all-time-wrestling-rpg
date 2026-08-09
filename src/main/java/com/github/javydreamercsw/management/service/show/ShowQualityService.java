/*
* Copyright (C) 2026 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.service.show;

import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentParticipant;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes a 1–5 star quality score for a show at finalization time.
 *
 * <p><b>Per-segment scoring (0–100, stored in {@link Segment#segmentRating}):</b>
 *
 * <ul>
 *   <li>All segments: rivalry heat (0–20) + tier spread (0–10) + stipulation variety (0–10) + title
 *       on the line (+10) + main event (+5). NPC max = 55.
 *   <li>Player segments (any participant with {@code wrestler.isPlayer=true}): adds grueling factor
 *       for stamina burned (0–10) + HP burned (0–10) + signatures hit (0–10) + finisher used (+10)
 *       + winner momentum (0–5). Player max = 100.
 * </ul>
 *
 * <p><b>Grueling factor</b> takes the minimum of both participants' burn percentages so squash
 * matches (one wrestler untouched) don't score high on this dimension.
 *
 * <p><b>Show quality</b> = average of non-promo segment ratings / 20 → 1.0–5.0 stars.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ShowQualityService {

  private static final int HEAT_CAP = 50;
  private static final int MAX_MOMENTUM = 100;
  private static final double STAR_DIVISOR = 20.0;
  private static final double MIN_STARS = 1.0;
  private static final double MAX_STARS = 5.0;
  private static final double FAN_BONUS_THRESHOLD = 4.0;
  private static final double FAN_BONUS_PERCENT = 0.05;

  private final SegmentRepository segmentRepository;
  private final WrestlerStateRepository wrestlerStateRepository;
  private final WrestlerService wrestlerService;
  private final RivalryService rivalryService;
  private final PromoBookingService promoBookingService;

  /**
   * Computes and persists per-segment ratings and the show-level quality score. Called from {@link
   * ShowService#finalizeShow} — do NOT call this on read paths.
   *
   * @return the computed 1.0–5.0 star rating
   */
  public double computeAndPersist(final Show show, final List<Segment> segments) {
    List<Segment> matchSegments =
        segments.stream().filter(s -> !promoBookingService.isPromoSegment(s)).toList();

    if (matchSegments.isEmpty()) {
      show.setQualityScore(MIN_STARS);
      return MIN_STARS;
    }

    Long universeId = show.getUniverse() != null ? show.getUniverse().getId() : null;

    for (Segment segment : matchSegments) {
      int rating = scoreSegment(segment, universeId);
      segment.setSegmentRating(rating);
      segmentRepository.save(segment);
    }

    double avg =
        matchSegments.stream()
            .mapToInt(s -> s.getSegmentRating() != null ? s.getSegmentRating() : 0)
            .average()
            .orElse(0.0);

    double stars = Math.max(MIN_STARS, Math.min(MAX_STARS, avg / STAR_DIVISOR));
    // Round to nearest 0.5
    stars = Math.round(stars * 2.0) / 2.0;

    show.setQualityScore(stars);
    log.info(
        "Show '{}' quality computed: avg segment score={}, stars={}",
        show.getName(),
        String.format("%.1f", avg),
        String.format("%.1f", stars));
    return stars;
  }

  /**
   * Awards a +5% fan bonus to every unique wrestler on non-promo segments when the show quality
   * meets the threshold. No-ops when the show has no universe (universe-less shows have no state).
   */
  public void awardFanBonusesIfEligible(
      final Show show, final List<Segment> segments, final double qualityScore) {
    if (!qualifiesForFanBonus(qualityScore)) {
      return;
    }
    Long universeId = show.getUniverse() != null ? show.getUniverse().getId() : null;
    if (universeId == null) {
      return;
    }

    Set<Long> seen = new HashSet<>();
    segments.stream()
        .filter(s -> !promoBookingService.isPromoSegment(s))
        .flatMap(s -> s.getWrestlers().stream())
        .filter(w -> w.getId() != null && seen.add(w.getId()))
        .forEach(
            w -> {
              long currentFans =
                  wrestlerStateRepository
                      .findByWrestlerIdAndUniverseId(w.getId(), universeId)
                      .map(state -> state.getFans() != null ? state.getFans() : 0L)
                      .orElse(0L);
              long bonus = computeFanBonus(currentFans);
              wrestlerService.awardFans(w.getId(), universeId, bonus);
              log.debug(
                  "Fan bonus awarded to '{}': +{} fans (show quality {}★)",
                  w.getName(),
                  bonus,
                  String.format("%.1f", qualityScore));
            });
  }

  /** Returns true when a show's quality warrants the fan bonus. */
  public boolean qualifiesForFanBonus(final double qualityScore) {
    return qualityScore >= FAN_BONUS_THRESHOLD;
  }

  /**
   * Computes the absolute fan bonus for a wrestler with the given current fan count. +5% of current
   * fans, minimum 1.
   */
  public long computeFanBonus(final long currentFans) {
    return Math.max(1L, Math.round(currentFans * FAN_BONUS_PERCENT));
  }

  // ==================== INTERNAL SCORING ====================

  private int scoreSegment(final Segment segment, final Long universeId) {
    int score = 0;

    // --- Heat (0–20) ---
    if (segment.getRivalryId() != null) {
      int heat =
          rivalryService.getRivalryById(segment.getRivalryId()).map(r -> r.getHeat()).orElse(0);
      score += (int) (Math.min(heat, HEAT_CAP) / (double) HEAT_CAP * 20);
    }

    // --- Tier spread (0–10) ---
    if (universeId != null) {
      List<Wrestler> wrestlers = segment.getWrestlers();
      if (wrestlers.size() >= 2) {
        int maxTier = wrestlers.stream().mapToInt(w -> tierOrdinal(w, universeId)).max().orElse(0);
        int minTier = wrestlers.stream().mapToInt(w -> tierOrdinal(w, universeId)).min().orElse(0);
        score += Math.min(10, (maxTier - minTier) * 2);
      }
    }

    // --- Stipulation variety (0–10) ---
    score += Math.min(10, segment.getSegmentRules().size() * 5);

    // --- Title on the line (+10) ---
    if (Boolean.TRUE.equals(segment.getIsTitleSegment())) {
      score += 10;
    }

    // --- Main event (+5) ---
    if (segment.isMainEvent()) {
      score += 5;
    }

    // --- Player-match bonus factors ---
    boolean hasPlayer =
        segment.getWrestlers().stream().anyMatch(w -> Boolean.TRUE.equals(w.getIsPlayer()));
    if (hasPlayer) {
      score += scorePlayerFactors(segment);
    }

    return Math.min(100, score);
  }

  private int scorePlayerFactors(final Segment segment) {
    int bonus = 0;
    List<SegmentParticipant> participants = List.copyOf(segment.getParticipants());

    // --- Grueling: stamina burned (0–10) ---
    bonus += gruelingFactor(participants, false);

    // --- Grueling: HP burned (0–10) ---
    bonus += gruelingFactor(participants, true);

    // --- Signatures hit (0–10) ---
    int totalSigs = 0;
    for (SegmentParticipant p : participants) {
      Set<String> sigNames = signatureCardNames(p.getWrestler(), false);
      totalSigs +=
          p.getCardsPlayed().entrySet().stream()
              .filter(e -> sigNames.contains(e.getKey()))
              .mapToInt(Map.Entry::getValue)
              .sum();
    }
    bonus += Math.min(10, totalSigs * 2);

    // --- Finisher used (+10) ---
    boolean finisherUsed =
        participants.stream()
            .anyMatch(
                p ->
                    p.getWinningCardName() != null
                        && signatureCardNames(p.getWrestler(), true)
                            .contains(p.getWinningCardName()));
    if (finisherUsed) {
      bonus += 10;
    }

    // --- Winner's final momentum (0–5) ---
    for (SegmentParticipant p : participants) {
      if (Boolean.TRUE.equals(p.getIsWinner()) && p.getFinalMomentum() != null) {
        bonus += (int) (Math.min(p.getFinalMomentum(), MAX_MOMENTUM) / (double) MAX_MOMENTUM * 5);
        break;
      }
    }

    return bonus;
  }

  /**
   * Returns 0–10 based on how much the least-worn participant burned through stamina or health.
   * Using the minimum prevents squash matches from scoring high here.
   *
   * @param useHealth true = compare finalHealth vs startingHealth; false = stamina
   */
  private int gruelingFactor(final List<SegmentParticipant> participants, final boolean useHealth) {
    double minBurnPct = Double.MAX_VALUE;
    boolean found = false;
    for (SegmentParticipant p : participants) {
      Wrestler w = p.getWrestler();
      int starting =
          useHealth
              ? (w.getStartingHealth() != null ? w.getStartingHealth() : 1)
              : (w.getStartingStamina() != null ? w.getStartingStamina() : 1);
      Integer finalVal = useHealth ? p.getFinalHealth() : p.getFinalStamina();
      if (finalVal == null || starting <= 0) {
        continue;
      }
      double burned = Math.max(0.0, (starting - finalVal) / (double) starting);
      minBurnPct = Math.min(minBurnPct, burned);
      found = true;
    }
    if (!found || minBurnPct == Double.MAX_VALUE) {
      return 0;
    }
    return (int) (minBurnPct * 10);
  }

  /** Returns card names in the wrestler's deck(s) that are signatures (or finishers if true). */
  private Set<String> signatureCardNames(final Wrestler wrestler, final boolean finisherOnly) {
    return wrestler.getDecks().stream()
        .flatMap(deck -> deck.getCards().stream())
        .map(DeckCard::getCard)
        .filter(
            card ->
                finisherOnly
                    ? Boolean.TRUE.equals(card.getFinisher())
                    : Boolean.TRUE.equals(card.getSignature()))
        .map(card -> card.getName())
        .collect(Collectors.toSet());
  }

  private int tierOrdinal(final Wrestler wrestler, final Long universeId) {
    return wrestlerStateRepository
        .findByWrestlerIdAndUniverseId(wrestler.getId(), universeId)
        .map(state -> state.getTier().ordinal())
        .orElse(0);
  }
}
