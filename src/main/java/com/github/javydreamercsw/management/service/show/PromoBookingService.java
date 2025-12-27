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
package com.github.javydreamercsw.management.service.show;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.PromoType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for booking promo segments in wrestling shows. Promos are treated as segments with
 * special promo rules but no winners/losers, focusing on storyline development and character work.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PromoBookingService {
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private RivalryService rivalryService;
  @Autowired private SegmentRuleService segmentRuleService;
  @Autowired private Clock clock;
  @Autowired private Random random = new Random();

  /**
   * Book promo segments for a show based on storylines and wrestler availability.
   *
   * @param show The show to book promos for
   * @param availableWrestlers List of wrestlers available for promos
   * @param maxPromos Maximum number of promos to book
   * @return List of booked promo segments
   */
  @Transactional
  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_BOOKER')")
  public List<Segment> bookPromosForShow(
      @NonNull Show show, @NonNull List<Wrestler> availableWrestlers, int maxPromos) {
    List<Segment> promos = new ArrayList<>();

    if (availableWrestlers.isEmpty() || maxPromos <= 0) {
      return promos;
    }

    // Create a copy to avoid modifying the original list
    List<Wrestler> wrestlers = new ArrayList<>(availableWrestlers);
    Collections.shuffle(wrestlers, random);

    // 1. Book rivalry-based confrontation promos (highest priority)
    promos.addAll(bookRivalryPromos(show, wrestlers, Math.min(2, maxPromos)));

    // 2. Book character development promos
    int remainingPromos = maxPromos - promos.size();
    if (remainingPromos > 0) {
      promos.addAll(bookCharacterPromos(show, wrestlers, remainingPromos));
    }

    log.info("Booked {} promo segments for show '{}'", promos.size(), show.getName());
    return promos;
  }

  /** Book rivalry-based confrontation promos. */
  private List<Segment> bookRivalryPromos(
      @NonNull Show show, @NonNull List<Wrestler> availableWrestlers, int maxPromos) {
    List<Segment> promos = new ArrayList<>();
    List<Rivalry> activeRivalries = rivalryService.getActiveRivalries();

    int bookedPromos = 0;
    for (Rivalry rivalry : activeRivalries) {
      if (bookedPromos >= maxPromos) break;

      Wrestler wrestler1 = rivalry.getWrestler1();
      Wrestler wrestler2 = rivalry.getWrestler2();

      // Check if both wrestlers are available
      if (availableWrestlers.contains(wrestler1) && availableWrestlers.contains(wrestler2)) {
        PromoType promoType = selectRivalryPromoType(rivalry.getHeat());
        Optional<Segment> promo = bookPromoSegment(show, List.of(wrestler1, wrestler2), promoType);

        if (promo.isPresent()) {
          promos.add(promo.get());
          availableWrestlers.remove(wrestler1);
          availableWrestlers.remove(wrestler2);
          bookedPromos++;

          log.info(
              "Booked rivalry promo: {} vs {} ({}, Heat: {})",
              wrestler1.getName(),
              wrestler2.getName(),
              promoType.getDisplayName(),
              rivalry.getHeat());
        }
      }
    }

    return promos;
  }

  /** Book character development and storyline promos. */
  private List<Segment> bookCharacterPromos(
      Show show, List<Wrestler> availableWrestlers, int maxPromos) {
    List<Segment> promos = new ArrayList<>();

    for (int i = 0; i < maxPromos && !availableWrestlers.isEmpty(); i++) {
      // Decide between solo promo or group promo
      boolean soloPromo = random.nextDouble() < 0.7; // 70% chance of solo promo

      if (soloPromo || availableWrestlers.size() == 1) {
        // Book solo promo
        Wrestler wrestler = availableWrestlers.remove(0);
        PromoType promoType = selectSoloPromoType();
        Optional<Segment> promo = bookPromoSegment(show, List.of(wrestler), promoType);

        if (promo.isPresent()) {
          promos.add(promo.get());
          log.info("Booked solo promo: {} ({})", wrestler.getName(), promoType.getDisplayName());
        }
      } else {
        // Book group promo (2-3 wrestlers)
        int groupSize =
            Math.min(
                3, Math.min(availableWrestlers.size(), 2 + random.nextInt(2))); // 2-3 wrestlers
        List<Wrestler> groupWrestlers = new ArrayList<>();

        for (int j = 0; j < groupSize; j++) {
          groupWrestlers.add(availableWrestlers.remove(0));
        }

        PromoType promoType = selectGroupPromoType();
        Optional<Segment> promo = bookPromoSegment(show, groupWrestlers, promoType);

        if (promo.isPresent()) {
          promos.add(promo.get());
          String wrestlerNames =
              groupWrestlers.stream()
                  .map(Wrestler::getName)
                  .reduce((a, b) -> a + ", " + b)
                  .orElse("Unknown");
          log.info("Booked group promo: {} ({})", wrestlerNames, promoType.getDisplayName());
        }
      }
    }

    return promos;
  }

  /** Book a promo segment as a segment with promo rules. */
  private Optional<Segment> bookPromoSegment(
      @NonNull Show show, @NonNull List<Wrestler> wrestlers, @NonNull PromoType promoType) {
    try {
      // Get promo segment type (create if doesn't exist)
      SegmentType promoSegmentType = getOrCreatePromoSegmentType();

      // Create the promo "segment"
      Segment promo = new Segment();
      promo.setShow(show);
      promo.setSegmentType(promoSegmentType);
      promo.setSegmentDate(clock.instant());
      promo.setIsNpcGenerated(true);

      // Add wrestlers as participants
      for (Wrestler wrestler : wrestlers) {
        promo.addParticipant(wrestler);
      }

      // Apply promo rule
      segmentRuleService.findByName(promoType.getDisplayName()).ifPresent(promo::addSegmentRule);

      // Set narration for the promo segment
      promo.setNarration(generatePromoNarration(wrestlers, promoType));

      Segment savedPromo = segmentRepository.save(promo);
      return Optional.of(savedPromo);

    } catch (Exception e) {
      log.error("Error booking promo segment: {}", e.getMessage(), e);
      return Optional.empty();
    }
  }

  /** Generates a descriptive narration for a promo segment. */
  private String generatePromoNarration(
      @NonNull List<Wrestler> wrestlers, @NonNull PromoType promoType) {
    String participantNames =
        wrestlers.stream().map(Wrestler::getName).collect(Collectors.joining(" and "));
    return switch (promoType) {
      case CONFRONTATION_PROMO ->
          String.format(
              "%s and %s engage in a heated confrontation promo, exchanging insults and threats.",
              wrestlers.get(0).getName(), wrestlers.get(1).getName());
      case CONTRACT_SIGNING ->
          String.format(
              "%s and %s are in the ring for a contract signing, but tensions quickly escalate.",
              participantNames, promoType.getDisplayName().toLowerCase());
      case CHALLENGE_ISSUED ->
          String.format(
              "%s steps out to issue a challenge, calling out %s for a future match.",
              wrestlers.get(0).getName(),
              wrestlers.size() > 1 ? wrestlers.get(1).getName() : "anyone in the back");
      case INTERVIEW_SEGMENT ->
          String.format(
              "%s gives a passionate interview backstage, discussing their career and future"
                  + " aspirations.",
              participantNames);
      case BACKSTAGE_SEGMENT ->
          String.format(
              "A chaotic backstage segment unfolds with %s, leading to unexpected developments.",
              participantNames);
      case SOLO_PROMO ->
          String.format(
              "%s delivers a powerful solo promo in the center of the ring, addressing the fans and"
                  + " their rivals.",
              participantNames);
      case GROUP_PROMO ->
          String.format(
              "The group %s cuts a promo, asserting their dominance and laying down a challenge to"
                  + " the locker room.",
              participantNames);
      case CHAMPIONSHIP_PRESENTATION ->
          String.format(
              "A prestigious championship presentation is held for %s, celebrating their recent"
                  + " victory.",
              participantNames);
      case RETIREMENT_SPEECH ->
          String.format(
              "%s delivers an emotional retirement speech, thanking the fans for their support.",
              participantNames);
      case ALLIANCE_ANNOUNCEMENT ->
          String.format(
              "%s make a shocking alliance announcement, promising to dominate the competition"
                  + " together.",
              participantNames);
      default ->
          String.format(
              "%s cuts a %s promo, stirring up excitement among the fans.",
              participantNames, promoType.getDisplayName().toLowerCase());
    };
  }

  /** Get the promo segment type from database. */
  private SegmentType getOrCreatePromoSegmentType() {
    Optional<SegmentType> promoTypeOpt = segmentTypeRepository.findByName("Promo");

    if (promoTypeOpt.isPresent()) {
      return promoTypeOpt.get();
    }

    // Fallback: create promo segment type if not loaded from JSON
    log.warn("Promo segment type not found in database, creating fallback");
    SegmentType promoType = new SegmentType();
    promoType.setName("Promo");
    promoType.setDescription("Non-wrestling promo segment for storyline development");
    return segmentTypeRepository.save(promoType);
  }

  /** Select appropriate promo type based on rivalry heat using database-driven selection. */
  private PromoType selectRivalryPromoType(int heat) {
    List<PromoType> availablePromos = new ArrayList<>();

    if (heat >= 25) {
      // High heat - intense confrontations
      availablePromos.addAll(
          getPromoTypesByNames(
              List.of(
                  PromoType.CONFRONTATION_PROMO,
                  PromoType.CONTRACT_SIGNING,
                  PromoType.CHALLENGE_ISSUED)));
    } else if (heat >= 15) {
      // Medium heat - building tension
      availablePromos.addAll(
          getPromoTypesByNames(
              List.of(
                  PromoType.CONFRONTATION_PROMO,
                  PromoType.INTERVIEW_SEGMENT,
                  PromoType.CHALLENGE_ISSUED)));
    } else {
      // Low heat - establishing rivalry
      availablePromos.addAll(
          getPromoTypesByNames(
              List.of(
                  PromoType.INTERVIEW_SEGMENT,
                  PromoType.BACKSTAGE_SEGMENT,
                  PromoType.CHALLENGE_ISSUED)));
    }

    // Fallback to any available promo rule if none found
    if (availablePromos.isEmpty()) {
      availablePromos.add(PromoType.INTERVIEW_SEGMENT); // Safe fallback
    }

    return availablePromos.get(random.nextInt(availablePromos.size()));
  }

  /** Select solo promo type using database-driven selection. */
  private PromoType selectSoloPromoType() {
    List<PromoType> availablePromos =
        new ArrayList<>(
            getPromoTypesByNames(
                List.of(
                    PromoType.SOLO_PROMO,
                    PromoType.INTERVIEW_SEGMENT,
                    PromoType.CHAMPIONSHIP_PRESENTATION,
                    PromoType.RETIREMENT_SPEECH,
                    PromoType.CHALLENGE_ISSUED)));

    // Fallback if none found
    if (availablePromos.isEmpty()) {
      availablePromos.add(PromoType.SOLO_PROMO); // Safe fallback
    }

    return availablePromos.get(random.nextInt(availablePromos.size()));
  }

  /** Select group promo type using database-driven selection. */
  private PromoType selectGroupPromoType() {
    List<PromoType> availablePromos =
        new ArrayList<>(
            getPromoTypesByNames(
                List.of(
                    PromoType.GROUP_PROMO,
                    PromoType.ALLIANCE_ANNOUNCEMENT,
                    PromoType.BACKSTAGE_SEGMENT,
                    PromoType.CONTRACT_SIGNING)));

    // Fallback if none found
    if (availablePromos.isEmpty()) {
      availablePromos.add(PromoType.GROUP_PROMO); // Safe fallback
    }

    return availablePromos.get(random.nextInt(availablePromos.size()));
  }

  /** Get promo type names from database, filtering out non-existent ones. */
  private List<PromoType> getPromoTypesByNames(List<PromoType> requestedNames) {
    return requestedNames.stream()
        .filter(promoType -> segmentRuleService.existsByName(promoType.getDisplayName()))
        .toList();
  }

  /** Check if a segment is a promo segment. */
  @PreAuthorize("isAuthenticated()")
  public boolean isPromoSegment(@NonNull Segment segment) {
    return segment.getSegmentType() != null && "Promo".equals(segment.getSegmentType().getName());
  }

  /** Get all promo segments for a show. */
  @PreAuthorize("isAuthenticated()")
  public List<Segment> getPromosForShow(@NonNull Show show) {
    return segmentRepository.findByShow(show).stream().filter(this::isPromoSegment).toList();
  }
}
