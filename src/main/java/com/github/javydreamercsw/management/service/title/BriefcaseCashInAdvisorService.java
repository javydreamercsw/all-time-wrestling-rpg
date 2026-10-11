/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.title;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.title.*;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.GameSettingService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Deterministic advisor for NPC briefcase cash-ins (ATW-2b9u): evaluates HELD opportunities held by
 * NPCs against the titles currently in circulation and scores when/where/whom to cash in.
 *
 * <p>All hard gates (setting, NPC-only, active holder, HELD status, expiry, active title, reigning
 * champion, gender/division, universe, cooldown) run before scoring; only candidates that pass
 * every gate and clear the threshold become proposals. Mode (PLANNED vs AMBUSH) is decided later,
 * at row-build/approval time, by whether the reigning champion has a non-promo segment on the card.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BriefcaseCashInAdvisorService {

  /** Game-days a booker decline suppresses re-proposals, unless urgency is in the top band. */
  static final int DECLINE_COOLDOWN_DAYS = 30;

  /** Score threshold a candidate must clear to become a proposal. */
  static final int PROPOSAL_THRESHOLD = 50;

  /** Maximum score: 35 urgency + 25 heat + 20 prestige + 15 staleness + 5 PLE. */
  private static final int MAX_SCORE = 100;

  private final TitleOpportunityRepository opportunityRepository;
  private final TitleReignRepository titleReignRepository;
  private final SegmentRepository segmentRepository;
  private final TitleRepository titleRepository;
  private final RivalryRepository rivalryRepository;
  private final WrestlerRepository wrestlerRepository;
  private final GameSettingService gameSettingService;
  private final Clock clock;

  /**
   * One proposed cash-in: which briefcase, against which title (and its reigning champions), in
   * which mode, with the deterministic score that produced it.
   */
  public record BriefcaseProposal(
      TitleOpportunity opportunity,
      Title title,
      List<Wrestler> champions,
      BriefcaseCashInMode mode,
      int score) {}

  /** Scoring inputs, separated from I/O so {@link #scoreOf} stays pure and unit-testable. */
  public record BriefcaseInputs(
      int daysUntilExpiry,
      int expiryWindow,
      int rivalryHeat,
      WrestlerTier titleTier,
      int daysSinceLastDefense,
      DefenseFrequencyType defenseFrequency,
      boolean premiumLiveEvent) {}

  /**
   * Evaluates every NPC-held, HELD briefcase against this show and returns the proposals that clear
   * the threshold, ordered by score descending. Deterministic: same state, same proposals.
   */
  public List<BriefcaseProposal> proposalsFor(Show show) {
    if (!gameSettingService.isNpcBriefcaseCashInEnabled()) {
      return List.of();
    }
    LocalDate gameDate = gameSettingService.getCurrentGameDate();
    List<Title> activeTitles = titleRepository.findByIsActiveTrue();

    return opportunityRepository.findByStatus(TitleOpportunityStatus.HELD).stream()
        .filter(opp -> passesGates(opp, gameDate))
        .flatMap(opp -> scoreAgainstTitles(opp, show, gameDate, activeTitles).stream())
        .filter(p -> p.score() >= PROPOSAL_THRESHOLD)
        .sorted(Comparator.comparingInt(BriefcaseProposal::score).reversed())
        .toList();
  }

  /**
   * Pure scoring function (0-100): urgency 35, heat 25, prestige 20, staleness 15, PLE 5.
   * Deterministic and side-effect-free so tests can pin each component.
   */
  public int scoreOf(BriefcaseInputs in) {
    int score = 0;
    score += urgencyPoints(in.daysUntilExpiry(), in.expiryWindow());
    score += Math.round(25f * Math.min(in.rivalryHeat(), 100) / 100f);
    score += prestigePoints(in.titleTier());
    score += stalenessPoints(in.daysSinceLastDefense(), in.defenseFrequency());
    if (in.premiumLiveEvent()) {
      score += 5;
    }
    return Math.min(score, MAX_SCORE);
  }

  /** u² urgency curve: flat early, steep near expiry. No expiry date → no urgency. */
  private int urgencyPoints(int daysUntilExpiry, int expiryWindow) {
    if (expiryWindow <= 0) {
      return 0;
    }
    double u = 1.0 - ((double) daysUntilExpiry / expiryWindow);
    u = Math.max(0.0, Math.min(1.0, u));
    return (int) Math.round(35 * u * u);
  }

  private int prestigePoints(WrestlerTier tier) {
    if (tier == null) {
      return 0;
    }
    return switch (tier) {
      case ICON -> 20;
      case MAIN_EVENTER -> 16;
      case MIDCARDER -> 12;
      case CONTENDER -> 8;
      case RISER -> 4;
      case ROOKIE -> 2;
    };
  }

  private int stalenessPoints(int daysSinceLastDefense, DefenseFrequencyType frequency) {
    int expected = frequency != null ? frequency.getDays() : 7;
    if (expected <= 0 || daysSinceLastDefense <= 0) {
      return 0;
    }
    double ratio = Math.min(1.0, (double) daysSinceLastDefense / expected);
    return (int) Math.round(15 * ratio);
  }

  private boolean passesGates(TitleOpportunity opportunity, LocalDate gameDate) {
    if (!opportunity.isHeld() || opportunity.isExpired(gameDate)) {
      return false;
    }
    Wrestler holder = opportunity.getWrestler();
    if (holder == null
        || Boolean.TRUE.equals(holder.getIsPlayer())
        || holder.getId() == null
        || !Boolean.TRUE.equals(holder.getActive())) {
      return false;
    }
    // Cooldown: a recent booker decline suppresses re-proposals, except when urgency is in the
    // top band (last quarter of the case's window) — the case is about to lapse and the booker
    // must see it again regardless of earlier rejections.
    if (opportunity.getLastDeclinedAt() != null) {
      long sinceDecline = ChronoUnit.DAYS.between(opportunity.getLastDeclinedAt(), gameDate);
      boolean inCooldown = sinceDecline < DECLINE_COOLDOWN_DAYS;
      boolean topBand = isTopUrgencyBand(opportunity, gameDate);
      if (inCooldown && !topBand) {
        return false;
      }
    }
    return true;
  }

  private boolean isTopUrgencyBand(TitleOpportunity opportunity, LocalDate gameDate) {
    if (opportunity.getExpiryDate() == null) {
      return false;
    }
    long window = ChronoUnit.DAYS.between(opportunity.getEarnedAt(), opportunity.getExpiryDate());
    if (window <= 0) {
      return false;
    }
    long daysUntilExpiry = ChronoUnit.DAYS.between(gameDate, opportunity.getExpiryDate());
    return daysUntilExpiry <= window / 4;
  }

  private List<BriefcaseProposal> scoreAgainstTitles(
      TitleOpportunity opportunity, Show show, LocalDate gameDate, List<Title> activeTitles) {
    Wrestler holder = opportunity.getWrestler();
    return activeTitles.stream()
        .flatMap(title -> evaluateTitle(opportunity, holder, title, show, gameDate).stream())
        .toList();
  }

  private Optional<BriefcaseProposal> evaluateTitle(
      TitleOpportunity opportunity, Wrestler holder, Title title, Show show, LocalDate gameDate) {
    // Title gates mirroring validateCashIn's order.
    if (!Boolean.TRUE.equals(title.getIsActive())) {
      return Optional.empty();
    }
    if (title.getUniverse() != null
        && opportunity.getUniverse() != null
        && !title.getUniverse().getId().equals(opportunity.getUniverse().getId())) {
      return Optional.empty();
    }
    if (title.getGender() != null
        && holder.getGender() != null
        && title.getGender() != holder.getGender()) {
      return Optional.empty();
    }
    // A gendered briefcase is a division credential: it challenges its own division's titles
    // (or an ungendered title), never the other division's (ATW-hq8d).
    if (opportunity.getGender() != null
        && title.getGender() != null
        && opportunity.getGender() != title.getGender()) {
      return Optional.empty();
    }
    List<Wrestler> champions =
        titleReignRepository.findByTitleIdAndEndDateIsNull(title.getId()).stream()
            .flatMap(reign -> reign.getChampions().stream())
            .distinct()
            .toList();
    if (champions.isEmpty()) {
      return Optional.empty();
    }

    int heat =
        champions.stream()
            .mapToInt(
                champion ->
                    rivalryRepository
                        .findActiveRivalryBetween(holder, champion)
                        .map(Rivalry::getHeat)
                        .orElse(0))
            .max()
            .orElse(0);

    int daysSinceLastDefense = daysSinceLastDefense(title, gameDate);
    int daysUntilExpiry =
        opportunity.getExpiryDate() != null
            ? (int) ChronoUnit.DAYS.between(gameDate, opportunity.getExpiryDate())
            : 0;
    int expiryWindow =
        opportunity.getExpiryDate() != null
            ? (int) ChronoUnit.DAYS.between(opportunity.getEarnedAt(), opportunity.getExpiryDate())
            : 0;

    int score =
        scoreOf(
            new BriefcaseInputs(
                daysUntilExpiry,
                expiryWindow,
                heat,
                title.getTier(),
                daysSinceLastDefense,
                title.getEffectiveDefenseFrequencyType(),
                show != null && show.isPremiumLiveEvent()));

    return Optional.of(
        new BriefcaseProposal(opportunity, title, champions, BriefcaseCashInMode.PLANNED, score));
  }

  /**
   * Days since the title was last defended (reign start as floor, latest title segment as override)
   * — the same recipe ShowPlanningService uses for its staleness display.
   */
  private int daysSinceLastDefense(Title title, LocalDate gameDate) {
    Instant lastDefense =
        titleReignRepository.findByTitleAndEndDateIsNull(title).stream()
            .map(TitleReign::getStartDate)
            .findFirst()
            .orElse(null);
    Optional<Instant> lastMatch =
        segmentRepository.findByTitle(title).stream()
            .map(Segment::getSegmentDate)
            .max(Comparator.naturalOrder());
    if (lastMatch.isPresent() && (lastDefense == null || lastMatch.get().isAfter(lastDefense))) {
      lastDefense = lastMatch.get();
    }
    if (lastDefense == null) {
      return 0;
    }
    return (int)
        ChronoUnit.DAYS.between(lastDefense.atZone(clock.getZone()).toLocalDate(), gameDate);
  }
}
