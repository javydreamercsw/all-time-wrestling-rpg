package com.github.javydreamercsw.management.service.show;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
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
  @Autowired private WrestlerRepository wrestlerRepository;
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
        String promoType = selectRivalryPromoType(rivalry.getHeat());
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
              promoType,
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
        String promoType = selectSoloPromoType();
        Optional<Segment> promo = bookPromoSegment(show, List.of(wrestler), promoType);

        if (promo.isPresent()) {
          promos.add(promo.get());
          log.info("Booked solo promo: {} ({})", wrestler.getName(), promoType);
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

        String promoType = selectGroupPromoType();
        Optional<Segment> promo = bookPromoSegment(show, groupWrestlers, promoType);

        if (promo.isPresent()) {
          promos.add(promo.get());
          String wrestlerNames =
              groupWrestlers.stream()
                  .map(Wrestler::getName)
                  .reduce((a, b) -> a + ", " + b)
                  .orElse("Unknown");
          log.info("Booked group promo: {} ({})", wrestlerNames, promoType);
        }
      }
    }

    return promos;
  }

  /** Book a promo segment as a segment with promo rules. */
  private Optional<Segment> bookPromoSegment(
      @NonNull Show show, @NonNull List<Wrestler> wrestlers, @NonNull String promoType) {
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
      segmentRuleService.findByName(promoType).ifPresent(promo::addSegmentRule);

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
  private String generatePromoNarration(@NonNull List<Wrestler> wrestlers, @NonNull String promoType) {
    String participantNames =
        wrestlers.stream().map(Wrestler::getName).collect(Collectors.joining(" and "));
    return String.format("%s cuts a %s promo.", participantNames, promoType.toLowerCase());
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
  private String selectRivalryPromoType(int heat) {
    List<String> availablePromos = new ArrayList<>();

    if (heat >= 25) {
      // High heat - intense confrontations
      availablePromos.addAll(
          getPromoTypesByNames(
              List.of("Confrontation Promo", "Contract Signing", "Challenge Issued")));
    } else if (heat >= 15) {
      // Medium heat - building tension
      availablePromos.addAll(
          getPromoTypesByNames(
              List.of("Confrontation Promo", "Interview Segment", "Challenge Issued")));
    } else {
      // Low heat - establishing rivalry
      availablePromos.addAll(
          getPromoTypesByNames(
              List.of("Interview Segment", "Backstage Segment", "Challenge Issued")));
    }

    // Fallback to any available promo rule if none found
    if (availablePromos.isEmpty()) {
      availablePromos.add("Interview Segment"); // Safe fallback
    }

    return availablePromos.get(random.nextInt(availablePromos.size()));
  }

  /** Select solo promo type using database-driven selection. */
  private String selectSoloPromoType() {
    List<String> availablePromos =
        new ArrayList<>(
            getPromoTypesByNames(
                List.of(
                    "Solo Promo",
                    "Interview Segment",
                    "Championship Presentation",
                    "Retirement Speech",
                    "Challenge Issued")));

    // Fallback if none found
    if (availablePromos.isEmpty()) {
      availablePromos.add("Solo Promo"); // Safe fallback
    }

    return availablePromos.get(random.nextInt(availablePromos.size()));
  }

  /** Select group promo type using database-driven selection. */
  private String selectGroupPromoType() {
    List<String> availablePromos =
        new ArrayList<>(
            getPromoTypesByNames(
                List.of(
                    "Group Promo",
                    "Alliance Announcement",
                    "Backstage Segment",
                    "Contract Signing")));

    // Fallback if none found
    if (availablePromos.isEmpty()) {
      availablePromos.add("Group Promo"); // Safe fallback
    }

    return availablePromos.get(random.nextInt(availablePromos.size()));
  }

  /** Get promo type names from database, filtering out non-existent ones. */
  private List<String> getPromoTypesByNames(List<String> requestedNames) {
    return requestedNames.stream().filter(segmentRuleService::existsByName).toList();
  }

  /** Check if a segment is a promo segment. */
  public boolean isPromoSegment(@NonNull Segment segment) {
    return segment.getSegmentType() != null && "Promo".equals(segment.getSegmentType().getName());
  }

  /** Get all promo segments for a show. */
  public List<Segment> getPromosForShow(@NonNull Show show) {
    return segmentRepository.findByShow(show).stream().filter(this::isPromoSegment).toList();
  }
}
