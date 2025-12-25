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
import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.base.service.segment.SegmentOutcomeProvider;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.utils.DiceBag;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for determining match outcomes when none is provided. Uses the same logic as
 * NPCMatchResolutionService but adapted for match narration contexts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SegmentOutcomeService implements SegmentOutcomeProvider {

  private final WrestlerRepository wrestlerRepository;
  private final Random random;

  /**
   * Determines the match outcome if none is provided in the context. Uses wrestler stats, tier
   * bonuses, and weighted random selection.
   */
  @Transactional
  public SegmentNarrationService.SegmentNarrationContext determineOutcomeIfNeeded(
      @NonNull SegmentNarrationService.SegmentNarrationContext context) {
    // If outcome is already determined, return as-is
    if (context.getDeterminedOutcome() != null
        && !context.getDeterminedOutcome().trim().isEmpty()) {
      log.debug("Segment outcome already determined: {}", context.getDeterminedOutcome());
      return context;
    }

    // If it's a promo with no wrestlers, don't determine an outcome automatically.
    // The narration will be based on the summary.
    if ("Promo".equalsIgnoreCase(context.getSegmentType().getSegmentType())
        && (context.getWrestlers() == null || context.getWrestlers().isEmpty())) {
      log.debug("Segment is a promo with no wrestlers, skipping outcome determination.");
      return context;
    }

    // If no wrestlers provided, can't determine outcome
    if (context.getWrestlers() == null || context.getWrestlers().isEmpty()) {
      log.warn("Cannot determine match outcome - no wrestlers provided");
      context.setDeterminedOutcome("Segment ends in a no contest due to insufficient participants");
      return context;
    }

    // Determine outcome based on number of wrestlers
    String outcome =
        switch (context.getWrestlers().size()) {
          case 1 -> determineSingleWrestlerOutcome(context.getWrestlers().get(0));
          case 2 -> determineTwoWrestlerOutcome(context.getWrestlers());
          default -> determineMultiWrestlerOutcome(context.getWrestlers());
        };

    context.setDeterminedOutcome(outcome);
    log.info("Automatically determined match outcome: {}", outcome);
    return context;
  }

  /** Determines outcome for a single wrestler (exhibition or promo). */
  private String determineSingleWrestlerOutcome(@NonNull WrestlerContext wrestler) {
    return wrestler.getName()
        + " delivers an impressive performance, showcasing their skills to the crowd";
  }

  /** Determines outcome for a two-wrestler match using weighted probability. */
  private String determineTwoWrestlerOutcome(@NonNull List<WrestlerContext> wrestlers) {
    WrestlerContext wrestler1 = wrestlers.get(0);
    WrestlerContext wrestler2 = wrestlers.get(1);

    // Get wrestler data from database for accurate stats
    Optional<Wrestler> dbWrestler1 = findWrestlerByName(wrestler1.getName());
    Optional<Wrestler> dbWrestler2 = findWrestlerByName(wrestler2.getName());

    // Calculate weights
    int weight1 = calculateWrestlerWeight(dbWrestler1.orElse(null), wrestler1);
    int weight2 = calculateWrestlerWeight(dbWrestler2.orElse(null), wrestler2);

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

  /** Determines outcome for a multi-wrestler match. */
  private String determineMultiWrestlerOutcome(@NonNull List<WrestlerContext> wrestlers) {
    // Calculate weights for all wrestlers
    List<WrestlerWeight> wrestlerWeights =
        wrestlers.stream()
            .map(
                wrestler -> {
                  Optional<Wrestler> dbWrestler = findWrestlerByName(wrestler.getName());
                  int weight = calculateWrestlerWeight(dbWrestler.orElse(null), wrestler);
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

  /** Calculates wrestler weight for match outcome determination. */
  private int calculateWrestlerWeight(Wrestler dbWrestler, WrestlerContext contextWrestler) {
    if (dbWrestler == null) {
      // Use default weight if wrestler not found in database
      log.debug(
          "Wrestler {} not found in database, using default weight", contextWrestler.getName());
      return 50; // Default weight
    }

    // Base weight from fan weight
    int fanWeight = dbWrestler.getFanWeight();

    // Tier bonus
    int tierBonus = getTierBonus(dbWrestler);

    // Health penalty from bumps and active injuries
    int healthPenalty = dbWrestler.getBumps(); // Each bump = -1 penalty

    // Add injury penalties (active injuries significantly reduce effectiveness)
    long activeInjuries =
        dbWrestler.getInjuries().stream().filter(Injury::isCurrentlyActive).count();
    healthPenalty += (int) activeInjuries * 3; // Each active injury = -3 penalty

    // Calculate total weight (minimum 1)
    int totalWeight = Math.max(1, fanWeight + tierBonus - healthPenalty);

    log.debug(
        "Calculated weight for {}: {} (fan: {}, tier: {}, health: {})",
        dbWrestler.getName(),
        totalWeight,
        fanWeight,
        tierBonus,
        healthPenalty);

    return totalWeight;
  }

  /** Gets tier bonus for wrestler weight calculation. */
  private int getTierBonus(Wrestler wrestler) {
    if (wrestler.getTier() == null) {
      return 0;
    }

    return switch (wrestler.getTier()) {
      case ROOKIE -> 0;
      case RISER -> 2;
      case CONTENDER -> 4;
      case MIDCARDER -> 6;
      case MAIN_EVENTER -> 8;
      case ICON -> 10;
    };
  }

  /** Finds wrestler by name in the database. */
  private Optional<Wrestler> findWrestlerByName(String name) {
    if (name == null || name.trim().isEmpty()) {
      return Optional.empty();
    }
    return wrestlerRepository.findByName(name.trim());
  }

  /** Gets a random finishing move for the winner. */
  private String getRandomFinishingMove(Wrestler wrestler) {
    if (wrestler != null) {
      List<Card> finishers = new ArrayList<>();
      List<Card> signatures = new ArrayList<>();

      for (Deck deck : wrestler.getDecks()) {
        for (DeckCard deckCard : deck.getCards()) {
          Card card = deckCard.getCard();
          if (card.getFinisher()) {
            finishers.add(card);
          }
          if (card.getSignature()) {
            signatures.add(card);
          }
        }
      }

      if (!finishers.isEmpty()) {
        return finishers.get(random.nextInt(finishers.size())).getName();
      }

      if (!signatures.isEmpty()) {
        return signatures.get(random.nextInt(signatures.size())).getName();
      }
    }
    // Use generic finishing moves
    String[] genericFinishers = {
      "a devastating finishing move",
      "their signature maneuver",
      "a powerful slam",
      "a high-impact finisher",
      "their trademark move",
      "a spectacular finishing sequence",
      "a crushing blow",
      "their ultimate technique"
    };
    return genericFinishers[random.nextInt(genericFinishers.length)];
  }

  /** Record for wrestler weight calculations. */
  private record WrestlerWeight(WrestlerContext wrestler, int weight) {}
}
