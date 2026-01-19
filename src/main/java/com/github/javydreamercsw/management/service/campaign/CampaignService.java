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
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCardRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignStatus;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CampaignService {

  private final CampaignRepository campaignRepository;
  private final CampaignStateRepository campaignStateRepository;
  private final CampaignAbilityCardRepository campaignAbilityCardRepository;
  private final WrestlerAlignmentRepository wrestlerAlignmentRepository;
  private final CampaignScriptService campaignScriptService;

  public Campaign startCampaign(Wrestler wrestler) {
    // Check if wrestler already has active campaign?
    // For now, allow new campaign.

    Campaign campaign =
        Campaign.builder()
            .wrestler(wrestler)
            .status(CampaignStatus.ACTIVE)
            .startedAt(LocalDateTime.now())
            .build();

    campaign = campaignRepository.save(campaign);

    CampaignState state =
        CampaignState.builder()
            .campaign(campaign)
            .currentChapter(1)
            .victoryPoints(0)
            .skillTokens(0)
            .bumps(0)
            .healthPenalty(0)
            .handSizePenalty(0)
            .staminaPenalty(0)
            .lastSync(LocalDateTime.now())
            .build();

    campaignStateRepository.save(state);
    campaign.setState(state);

    updateAbilityCards(campaign);

    return campaignRepository.save(campaign);
  }

  public void processMatchResult(Campaign campaign, boolean won) {
    CampaignState state = campaign.getState();
    int chapter = state.getCurrentChapter();
    int vpChange = 0;

    switch (chapter) {
      case 1:
        vpChange = won ? 2 : -1;
        break;
      case 2:
        // TODO: Implement Chapter 2 specific logic (Medium difficulty default?)
        vpChange = won ? 3 : -1; // Assuming slightly higher reward? Spec doesn't specify.
        // Spec: Chapter 2 ... Matches default to Medium difficulty.
        // Spec: Chapter 3 ... Victories earn 4 VP, losses lose 2 VP.
        // Spec for Chapter 1: Victories earn 2 VP, losses lose 1 VP.
        // I will interpolate for Chapter 2 or check if spec missed it.
        // "Chapter 2: The tournament quest. Matches default to Medium difficulty. Features a Rival
        // system..."
        // It doesn't specify VP for Chapter 2. I'll stick to Chapter 1 values or interpolate.
        // Let's assume Chapter 2 is 3 VP / -1 VP for now or same as Ch 1.
        // Given it's a tournament, maybe 3 VP.
        vpChange = won ? 3 : -1;
        break;
      case 3:
        vpChange = won ? 4 : -2;
        break;
    }

    state.setVictoryPoints(state.getVictoryPoints() + vpChange);
    campaignStateRepository.save(state);
  }

  public void advanceChapter(Campaign campaign) {
    CampaignState state = campaign.getState();
    if (state.getCurrentChapter() < 3) {
      state.setCurrentChapter(state.getCurrentChapter() + 1);
      campaignStateRepository.save(state);
    } else {
      completeCampaign(campaign);
    }
  }

  public void completeCampaign(Campaign campaign) {
    campaign.setStatus(CampaignStatus.COMPLETED);
    campaign.setEndedAt(LocalDateTime.now());
    campaignRepository.save(campaign);
  }

  /**
   * Updates the wrestler's available ability cards based on their alignment and level. If the
   * wrestler has turned (changed alignment), old cards are removed and new ones are added.
   *
   * @param campaign The campaign to update.
   */
  public void updateAbilityCards(Campaign campaign) {
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
      currentCards.removeIf(c -> c.getAlignmentType() != alignment.getAlignmentType());
    }

    // Determine unlocked levels based on track position (Space)
    int trackSpace = alignment.getLevel();
    List<Integer> unlockedLevels = new ArrayList<>();
    if (trackSpace >= 1) unlockedLevels.add(1);
    if (trackSpace >= 5) {
      unlockedLevels.add(2);
      unlockedLevels.add(3);
    }

    // Fetch potential cards
    // This simple logic adds ALL available cards for the level.
    // In a real game, the player might "choose" them.
    // For MVP, we grant access to all unlocked cards.
    for (Integer level : unlockedLevels) {
      List<CampaignAbilityCard> availableCards =
          campaignAbilityCardRepository.findByAlignmentTypeAndLevel(
              alignment.getAlignmentType(), level);

      for (CampaignAbilityCard card : availableCards) {
        if (!currentCards.contains(card)) {
          currentCards.add(card);
        }
      }
    }

    state.setActiveCards(currentCards);
    campaignStateRepository.save(state);
  }

  /**
   * Uses a one-time ability card, removing it from the active inventory.
   *
   * @param campaign The campaign.
   * @param cardId The ID of the card to use.
   */
  public void useAbilityCard(Campaign campaign, Long cardId) {
    CampaignState state = campaign.getState();
    CampaignAbilityCard card =
        campaignAbilityCardRepository
            .findById(cardId)
            .orElseThrow(() -> new IllegalArgumentException("Card not found"));

    if (state.getActiveCards().contains(card)) {
      boolean anyEffectTriggered = false;

      if (card.isOneTimeUse()) {
        campaignScriptService.executeEffect(card.getEffectScript(), campaign);
        anyEffectTriggered = true;
      }

      if (card.isSecondaryOneTimeUse()) {
        campaignScriptService.executeEffect(card.getSecondaryEffectScript(), campaign);
        anyEffectTriggered = true;
      }

      if (anyEffectTriggered) {
        // TODO: For cards with both passive and one-time effects, we might want to keep the card
        // in activeCards but mark the one-time effect as spent.
        // For now, we remove the card entirely when a one-time effect is used.
        state.getActiveCards().remove(card);
        campaignStateRepository.save(state);
        log.info(
            "Used ability card: {} for wrestler: {}",
            card.getName(),
            campaign.getWrestler().getName());
      }
    }
  }

  /**
   * Switches the wrestler's alignment (Turn).
   *
   * @param campaign The campaign.
   */
  public void turnWrestler(Campaign campaign) {
    Wrestler wrestler = campaign.getWrestler();
    WrestlerAlignment alignment =
        wrestlerAlignmentRepository
            .findByWrestler(wrestler)
            .orElseThrow(() -> new IllegalStateException("Wrestler has no alignment set"));

    AlignmentType newType =
        alignment.getAlignmentType() == AlignmentType.FACE
            ? AlignmentType.HEEL
            : AlignmentType.FACE;
    alignment.setAlignmentType(newType);
    // Keep level? Rules say "based on their current position on the track".
    // Usually a turn might reset or keep progress. Assuming keep level for now based on "based on
    // their current position".
    wrestlerAlignmentRepository.save(alignment);

    updateAbilityCards(campaign);
    log.info("Wrestler {} turned to {}", wrestler.getName(), newType);
  }
}
