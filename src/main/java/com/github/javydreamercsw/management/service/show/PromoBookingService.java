package com.github.javydreamercsw.management.service.show;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.match.Match;
import com.github.javydreamercsw.management.domain.show.match.MatchRepository;
import com.github.javydreamercsw.management.domain.show.match.type.MatchType;
import com.github.javydreamercsw.management.domain.show.match.type.MatchTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.match.MatchRuleService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for booking promo segments in wrestling shows. Promos are treated as matches with special
 * promo rules but no winners/losers, focusing on storyline development and character work.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PromoBookingService {

  private final MatchRepository matchRepository;
  private final MatchTypeRepository matchTypeRepository;
  private final WrestlerRepository wrestlerRepository;
  private final RivalryService rivalryService;
  private final MatchRuleService matchRuleService;
  private final Clock clock;
  private final Random random = new Random();

  /**
   * Book promo segments for a show based on storylines and wrestler availability.
   *
   * @param show The show to book promos for
   * @param availableWrestlers List of wrestlers available for promos
   * @param maxPromos Maximum number of promos to book
   * @return List of booked promo segments
   */
  @Transactional
  public List<Match> bookPromosForShow(
      @NonNull Show show, @NonNull List<Wrestler> availableWrestlers, int maxPromos) {
    List<Match> promos = new ArrayList<>();

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
  private List<Match> bookRivalryPromos(
      @NonNull Show show, @NonNull List<Wrestler> availableWrestlers, int maxPromos) {
    List<Match> promos = new ArrayList<>();
    List<Rivalry> activeRivalries = rivalryService.getActiveRivalries();

    int bookedPromos = 0;
    for (Rivalry rivalry : activeRivalries) {
      if (bookedPromos >= maxPromos) break;

      Wrestler wrestler1 = rivalry.getWrestler1();
      Wrestler wrestler2 = rivalry.getWrestler2();

      // Check if both wrestlers are available
      if (availableWrestlers.contains(wrestler1) && availableWrestlers.contains(wrestler2)) {
        String promoType = selectRivalryPromoType(rivalry.getHeat());
        Optional<Match> promo = bookPromoSegment(show, List.of(wrestler1, wrestler2), promoType);

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
  private List<Match> bookCharacterPromos(
      Show show, List<Wrestler> availableWrestlers, int maxPromos) {
    List<Match> promos = new ArrayList<>();

    for (int i = 0; i < maxPromos && !availableWrestlers.isEmpty(); i++) {
      // Decide between solo promo or group promo
      boolean soloPromo = random.nextDouble() < 0.7; // 70% chance of solo promo

      if (soloPromo || availableWrestlers.size() == 1) {
        // Book solo promo
        Wrestler wrestler = availableWrestlers.remove(0);
        String promoType = selectSoloPromoType();
        Optional<Match> promo = bookPromoSegment(show, List.of(wrestler), promoType);

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
        Optional<Match> promo = bookPromoSegment(show, groupWrestlers, promoType);

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

  /** Book a promo segment as a match with promo rules. */
  private Optional<Match> bookPromoSegment(
      @NonNull Show show, @NonNull List<Wrestler> wrestlers, @NonNull String promoType) {
    try {
      // Get promo match type (create if doesn't exist)
      MatchType promoMatchType = getOrCreatePromoMatchType();

      // Create the promo "match"
      Match promo = new Match();
      promo.setShow(show);
      promo.setMatchType(promoMatchType);
      promo.setMatchDate(clock.instant());
      promo.setIsNpcGenerated(true);

      // Add wrestlers as participants
      for (Wrestler wrestler : wrestlers) {
        promo.addParticipant(wrestler);
      }

      // Apply promo rule
      matchRuleService.findByName(promoType).ifPresent(promo::addMatchRule);

      // Set promo rating (based on wrestler charisma/fan count)
      int promoRating = calculatePromoRating(wrestlers);
      promo.setMatchRating(promoRating);

      Match savedPromo = matchRepository.save(promo);
      return Optional.of(savedPromo);

    } catch (Exception e) {
      log.error("Error booking promo segment: {}", e.getMessage(), e);
      return Optional.empty();
    }
  }

  /** Get the promo match type from database. */
  private MatchType getOrCreatePromoMatchType() {
    Optional<MatchType> promoTypeOpt = matchTypeRepository.findByName("Promo");

    if (promoTypeOpt.isPresent()) {
      return promoTypeOpt.get();
    }

    // Fallback: create promo match type if not loaded from JSON
    log.warn("Promo match type not found in database, creating fallback");
    MatchType promoType = new MatchType();
    promoType.setName("Promo");
    promoType.setDescription("Non-wrestling promo segment for storyline development");
    return matchTypeRepository.save(promoType);
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
    return requestedNames.stream().filter(name -> matchRuleService.existsByName(name)).toList();
  }

  /** Calculate promo rating based on wrestler popularity and charisma. */
  private int calculatePromoRating(@NonNull List<Wrestler> wrestlers) {
    // Base rating
    int baseRating = 2 + random.nextInt(2); // 2-3 base rating

    // Bonus based on wrestler fan count (popularity)
    long totalFans = wrestlers.stream().mapToLong(Wrestler::getFans).sum();
    long averageFans = totalFans / wrestlers.size();

    int popularityBonus = 0;
    if (averageFans > 50000) {
      popularityBonus = 2; // Very popular wrestlers
    } else if (averageFans > 20000) {
      popularityBonus = 1; // Popular wrestlers
    }

    // Multi-wrestler bonus (group dynamics)
    int groupBonus = wrestlers.size() > 1 ? 1 : 0;

    return Math.min(5, baseRating + popularityBonus + groupBonus);
  }

  /** Check if a match is a promo segment. */
  public boolean isPromoSegment(@NonNull Match match) {
    return match.getMatchType() != null && "Promo".equals(match.getMatchType().getName());
  }

  /** Get all promo segments for a show. */
  public List<Match> getPromosForShow(@NonNull Show show) {
    return matchRepository.findByShow(show).stream().filter(this::isPromoSegment).toList();
  }
}
