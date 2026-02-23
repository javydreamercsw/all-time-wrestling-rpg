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
package com.github.javydreamercsw.management.service.campaign;

import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCard;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AlignmentService {

  private final WrestlerAlignmentRepository wrestlerAlignmentRepository;
  private final CampaignStateRepository campaignStateRepository;

  public void shiftAlignment(@NonNull Campaign campaign, int amount) {
    if (amount == 0) {
      return;
    }

    WrestlerAlignment alignment =
        wrestlerAlignmentRepository
            .findByWrestler(campaign.getWrestler())
            .orElseThrow(() -> new IllegalStateException("Alignment not found"));

    int oldLevel = alignment.getLevel();
    AlignmentType oldType = alignment.getAlignmentType();

    // Logic for bidirectional shift
    if (oldType == AlignmentType.NEUTRAL) {
      if (amount > 0) {
        alignment.setAlignmentType(AlignmentType.FACE);
        alignment.setLevel(amount);
      } else {
        alignment.setAlignmentType(AlignmentType.HEEL);
        alignment.setLevel(Math.abs(amount));
      }
    } else if (oldType == AlignmentType.FACE) {
      int newLevel = oldLevel + amount;
      if (newLevel <= 0) {
        alignment.setAlignmentType(AlignmentType.NEUTRAL);
        alignment.setLevel(0);
      } else {
        alignment.setLevel(Math.min(5, newLevel));
      }
    } else { // HEEL
      // amount > 0 moves toward Neutral (reduces level)
      // amount < 0 moves further toward Heel (increases level)
      int newLevel = oldLevel - amount;
      if (newLevel <= 0) {
        alignment.setAlignmentType(AlignmentType.NEUTRAL);
        alignment.setLevel(0);
      } else {
        alignment.setLevel(Math.min(5, newLevel));
      }
    }

    wrestlerAlignmentRepository.save(alignment);
    handleLevelChange(campaign, oldLevel, alignment.getLevel());

    // If type changed (e.g. turned), update ability cards
    if (alignment.getAlignmentType() != oldType) {
      updateAbilityCards(campaign);
    }
  }

  public void updateAbilityCards(@NonNull Campaign campaign) {
    Wrestler wrestler = campaign.getWrestler();
    Optional<WrestlerAlignment> alignmentOpt = wrestlerAlignmentRepository.findByWrestler(wrestler);

    if (alignmentOpt.isEmpty()) {
      return; // No alignment tracking yet
    }

    WrestlerAlignment alignment = alignmentOpt.get();
    CampaignState state = campaign.getState();
    List<CampaignAbilityCard> currentCards = state.getActiveCards();

    // Check for alignment mismatch (Turn happened)
    boolean alignmentChanged =
        currentCards.stream().anyMatch(c -> c.getAlignmentType() != alignment.getAlignmentType());

    if (alignmentChanged) {
      // Discard all cards of wrong alignment
      currentCards.clear();
      recalculatePendingPicks(state, alignment);
      log.info(
          "Wrestler {} turned. Card inventory cleared and picks recalculated.", wrestler.getName());
    }

    state.setActiveCards(currentCards);
    campaignStateRepository.save(state);
  }

  public void handleLevelChange(@NonNull Campaign campaign, int oldLevel, int newLevel) {
    Wrestler wrestler = campaign.getWrestler();
    WrestlerAlignment alignment =
        wrestlerAlignmentRepository
            .findByWrestler(wrestler)
            .orElseThrow(() -> new IllegalStateException("Alignment not found"));

    AlignmentType type = alignment.getAlignmentType();
    CampaignState state = campaign.getState();
    List<CampaignAbilityCard> cards = state.getActiveCards();

    // Grant first pick when reaching Level 1 from 0 (Neutral)
    if (oldLevel == 0 && newLevel >= 1 && type != AlignmentType.NEUTRAL) {
      log.info("Reached Level 1 {}: Eligible for first Level 1 card.", type);
      state.setPendingL1Picks(state.getPendingL1Picks() + 1);
    }

    // Rules logic
    if (type == AlignmentType.FACE) {
      // Face Level 4: Gain a level 2 card
      if (oldLevel < 4 && newLevel >= 4) {
        log.info("Face reached Level 4: Eligible for Level 2 card.");
        state.setPendingL2Picks(state.getPendingL2Picks() + 1);
      }
      // Face Level 5: Gain a level 3 card, lose a level 1 card
      if (oldLevel < 5 && newLevel >= 5) {
        log.info("Face reached Level 5: Gain Level 3 card, Lose Level 1 card.");
        removeOneCardOfLevel(cards, 1);
        state.setPendingL3Picks(state.getPendingL3Picks() + 1);
      }
    } else {
      // Heel Level 4: Gain a level 2 card, lose a level 1 card
      if (oldLevel < 4 && newLevel >= 4) {
        log.info("Heel reached Level 4: Gain Level 2 card, Lose Level 1 card.");
        removeOneCardOfLevel(cards, 1);
        state.setPendingL2Picks(state.getPendingL2Picks() + 1);
        if (state.getPendingL1Picks() > 0) {
          state.setPendingL1Picks(state.getPendingL1Picks() - 1);
        }
      }
      // Heel Level 5: Gain another level 1 card
      if (oldLevel < 5 && newLevel >= 5) {
        log.info("Heel reached Level 5: Eligible for another Level 1 card.");
        state.setPendingL1Picks(state.getPendingL1Picks() + 1);
      }
    }

    campaignStateRepository.save(state);
  }

  private void removeOneCardOfLevel(@NonNull List<CampaignAbilityCard> cards, int level) {
    cards.stream()
        .filter(c -> c.getLevel() == level)
        .findFirst()
        .ifPresent(
            card -> {
              cards.remove(card);
              log.info("Removed Level {} card: {}", level, card.getName());
            });
  }

  private void recalculatePendingPicks(
      @NonNull CampaignState state, @NonNull WrestlerAlignment alignment) {
    int level = alignment.getLevel();
    AlignmentType type = alignment.getAlignmentType();

    state.setPendingL1Picks(0);
    state.setPendingL2Picks(0);
    state.setPendingL3Picks(0);

    if (type == AlignmentType.FACE) {
      if (level >= 1 && level < 5) state.setPendingL1Picks(1);
      if (level >= 4) state.setPendingL2Picks(1);
      if (level >= 5) state.setPendingL3Picks(1);
    } else {
      // HEEL
      if (level >= 1 && level < 4) state.setPendingL1Picks(1);
      if (level >= 4) state.setPendingL2Picks(1);
      if (level >= 5) state.setPendingL1Picks(1); // Regain L1 slot
    }
  }
}
