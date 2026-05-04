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
package com.github.javydreamercsw.management.service.segment;

import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.WrestlerContext;
import com.github.javydreamercsw.base.service.segment.SegmentOutcomeProvider;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.utils.DiceBag;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service responsible for determining segment outcomes. */
@Service
@Slf4j
public class SegmentOutcomeService implements SegmentOutcomeProvider {

  private final WrestlerRepository wrestlerRepository;
  private final WrestlerService wrestlerService;
  private final InjuryService injuryService;
  private final Random random;

  @Autowired
  public SegmentOutcomeService(
      WrestlerRepository wrestlerRepository,
      WrestlerService wrestlerService,
      InjuryService injuryService) {
    this(wrestlerRepository, wrestlerService, injuryService, new Random());
  }

  public SegmentOutcomeService(
      WrestlerRepository wrestlerRepository,
      WrestlerService wrestlerService,
      InjuryService injuryService,
      Random random) {
    this.wrestlerRepository = wrestlerRepository;
    this.wrestlerService = wrestlerService;
    this.injuryService = injuryService;
    this.random = random;
  }

  /**
   * Determines the outcome of a segment if it's not already determined.
   *
   * @param context the narration context
   * @return the updated context
   */
  public com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext
      determineOutcomeIfNeeded(
          com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext
              context) {
    if (context.getDeterminedOutcome() == null || context.getDeterminedOutcome().isBlank()) {
      String outcome = determineOutcome(null, context.getWrestlers(), context.getVenue(), 1L);
      context.setDeterminedOutcome(outcome);
    }
    return context;
  }

  public String determineOutcome(
      Segment segment,
      @NonNull List<WrestlerContext> wrestlers,
      SegmentNarrationService.VenueContext venue,
      @NonNull Long universeId) {
    if (wrestlers.isEmpty()) {
      return "The segment ends without a clear outcome.";
    }

    if (wrestlers.size() == 1) {
      return determineSingleWrestlerOutcome(wrestlers.get(0));
    } else if (wrestlers.size() == 2) {
      return determineTwoWrestlerOutcome(wrestlers, venue, universeId);
    } else {
      return determineMultiWrestlerOutcome(wrestlers, venue, universeId);
    }
  }

  /** Determines outcome for a single wrestler (exhibition or promo). */
  private String determineSingleWrestlerOutcome(@NonNull WrestlerContext wrestler) {
    return wrestler.getName()
        + " delivers an impressive performance, showcasing their skills to the crowd";
  }

  private String determineTwoWrestlerOutcome(
      @NonNull List<WrestlerContext> wrestlers,
      SegmentNarrationService.VenueContext venue,
      Long universeId) {
    WrestlerContext wrestler1 = wrestlers.get(0);
    WrestlerContext wrestler2 = wrestlers.get(1);

    Optional<Wrestler> dbWrestler1 = findWrestlerByName(wrestler1.getName());
    Optional<Wrestler> dbWrestler2 = findWrestlerByName(wrestler2.getName());

    // Calculate weights
    int weight1 = calculateWrestlerWeight(dbWrestler1.orElse(null), wrestler1, venue, universeId);
    int weight2 = calculateWrestlerWeight(dbWrestler2.orElse(null), wrestler2, venue, universeId);

    // Determine winner using weighted random selection
    int totalWeight = weight1 + weight2;
    double randomValue = new DiceBag(random, new int[] {totalWeight}).roll();

    WrestlerContext winnerContext;
    WrestlerContext loserContext;
    Optional<Wrestler> winner;
    if (randomValue < weight1) {
      winnerContext = wrestler1;
      loserContext = wrestler2;
      winner = dbWrestler1;
    } else {
      winnerContext = wrestler2;
      loserContext = wrestler1;
      winner = dbWrestler2;
    }

    // Generate outcome description
    String finishingMove = getRandomFinishingMove(winner.orElse(null));
    return String.format(
        "%s defeats %s with %s", winnerContext.getName(), loserContext.getName(), finishingMove);
  }

  private String determineMultiWrestlerOutcome(
      @NonNull List<WrestlerContext> wrestlers,
      SegmentNarrationService.VenueContext venue,
      Long universeId) {
    List<WrestlerWeight> wrestlerWeights =
        wrestlers.stream()
            .map(
                wrestler -> {
                  Optional<Wrestler> dbWrestler = findWrestlerByName(wrestler.getName());
                  int weight =
                      calculateWrestlerWeight(dbWrestler.orElse(null), wrestler, venue, universeId);
                  return new WrestlerWeight(wrestler, weight);
                })
            .toList();

    // Determine winner using weighted random selection
    int totalWeight = wrestlerWeights.stream().mapToInt(WrestlerWeight::weight).sum();
    double randomValue = new DiceBag(random, new int[] {totalWeight}).roll();

    double cumulativeWeight = 0;
    WrestlerContext winnerContext = wrestlers.get(0); // fallback
    for (WrestlerWeight wrestlerWeight : wrestlerWeights) {
      cumulativeWeight += wrestlerWeight.weight();
      if (randomValue <= cumulativeWeight) {
        winnerContext = wrestlerWeight.wrestler();
        break;
      }
    }
    Optional<Wrestler> dbWinner = findWrestlerByName(winnerContext.getName());

    // Generate outcome description
    String finishingMove = getRandomFinishingMove(dbWinner.orElse(null));
    return String.format(
        "%s emerges victorious from the %d-way match with %s",
        winnerContext.getName(), wrestlers.size(), finishingMove);
  }

  private int calculateWrestlerWeight(
      Wrestler dbWrestler,
      WrestlerContext contextWrestler,
      SegmentNarrationService.VenueContext venue,
      Long universeId) {
    if (dbWrestler == null) {
      log.debug(
          "Wrestler {} not found in database, using default weight", contextWrestler.getName());
      return 50;
    }

    com.github.javydreamercsw.management.domain.wrestler.WrestlerState state =
        wrestlerService.getOrCreateState(dbWrestler.getId(), universeId);

    // Base weight from fan weight
    int fanWeight = Math.toIntExact(state.getFans() / 5);

    // Tier bonus
    int tierBonus = getTierBonus(state);

    // Health penalty from bumps and active injuries
    int healthPenalty = state.getBumps(); // Each bump = -1 penalty

    // Add injury penalties (active injuries significantly reduce effectiveness)
    healthPenalty += injuryService.getTotalHealthPenaltyForWrestler(dbWrestler.getId(), universeId);

    int totalWeight = Math.max(1, fanWeight + tierBonus - healthPenalty);

    // Home territory bonus (+10%) when venue location matches wrestler heritage
    if (venue != null
        && venue.getLocation() != null
        && contextWrestler.getHailingFrom() != null
        && !contextWrestler.getHailingFrom().isBlank()) {
      String venueLoc = venue.getLocation().toLowerCase();
      for (String tag : contextWrestler.getHailingFrom().toLowerCase().split(",")) {
        String trimmed = tag.trim();
        if (!trimmed.isEmpty() && venueLoc.contains(trimmed)) {
          totalWeight += (int) (totalWeight * 0.10);
          log.debug(
              "Home field bonus (+10%) applied to {} in {}", dbWrestler.getName(), venue.getName());
          break;
        }
      }
    }

    log.debug(
        "Weight for {}: {} (Fans: {}, TierBonus: {}, HealthPenalty: {})",
        dbWrestler.getName(),
        totalWeight,
        fanWeight,
        tierBonus,
        healthPenalty);

    return totalWeight;
  }

  private int getTierBonus(WrestlerState state) {
    return switch (state.getTier()) {
      case ICON -> 50;
      case MAIN_EVENTER -> 30;
      case MIDCARDER -> 15;
      case CONTENDER -> 5;
      default -> 0;
    };
  }

  private String getRandomFinishingMove(Wrestler winner) {
    if (winner != null && winner.getDecks() != null) {
      List<String> finishers =
          winner.getDecks().stream()
              .flatMap(deck -> deck.getCards().stream())
              .map(DeckCard::getCard)
              .filter(Card::getFinisher)
              .map(Card::getName)
              .distinct()
              .toList();
      if (!finishers.isEmpty()) {
        return finishers.get(random.nextInt(finishers.size()));
      }
    }
    List<String> genericFinishers =
        List.of(
            "a devastating powerbomb",
            "a sudden roll-up",
            "a high-flying splash",
            "a precise superkick",
            "a brutal submission hold");
    return genericFinishers.get(random.nextInt(genericFinishers.size()));
  }

  private Optional<Wrestler> findWrestlerByName(String name) {
    return wrestlerRepository.findByName(name);
  }

  private record WrestlerWeight(WrestlerContext wrestler, int weight) {}
}
