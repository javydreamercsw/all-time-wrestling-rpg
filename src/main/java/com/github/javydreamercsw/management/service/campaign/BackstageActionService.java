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
import com.github.javydreamercsw.management.domain.campaign.BackstageActionHistory;
import com.github.javydreamercsw.management.domain.campaign.BackstageActionHistoryRepository;
import com.github.javydreamercsw.management.domain.campaign.BackstageActionType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignPhase;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import java.time.LocalDateTime;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class BackstageActionService {

  private final Random random;
  private final CampaignStateRepository campaignStateRepository;
  private final BackstageActionHistoryRepository actionHistoryRepository;
  private final InjuryService injuryService;
  private final CampaignService campaignService;
  private final SegmentRuleRepository segmentRuleRepository;

  public BackstageActionService(
      Random random,
      CampaignStateRepository campaignStateRepository,
      BackstageActionHistoryRepository actionHistoryRepository,
      InjuryService injuryService,
      @Lazy CampaignService campaignService,
      SegmentRuleRepository segmentRuleRepository) {
    this.random = random;
    this.campaignStateRepository = campaignStateRepository;
    this.actionHistoryRepository = actionHistoryRepository;
    this.injuryService = injuryService;
    this.campaignService = campaignService;
    this.segmentRuleRepository = segmentRuleRepository;
  }

  /**
   * Perform a backstage action.
   *
   * @param campaign The campaign context.
   * @param actionType The type of action.
   * @param diceSides The value of the wrestler's attribute for this action.
   * @return The result of the action.
   */
  public ActionOutcome performAction(
      Campaign campaign, BackstageActionType actionType, int diceSides) {

    CampaignState state = campaign.getState();

    // 1. Phase Check
    if (state.getCurrentPhase() != CampaignPhase.BACKSTAGE) {
      return new ActionOutcome(0, "Actions can only be taken during Backstage phase.");
    }

    // 2. Action Limit Check (Default 2)
    if (state.getActionsTaken() >= 2) {
      return new ActionOutcome(0, "Action limit reached for this phase.");
    }

    // 3. Consecutive Action Check
    if (state.getLastActionType() == actionType
        && Boolean.TRUE.equals(state.getLastActionSuccess())) {
      return new ActionOutcome(
          0, "Cannot perform the same action twice in a row unless it failed.");
    }

    // Training specific consecutive rule: "cannot perform Training twice in a row unless the first
    // attempt resulted in zero successes"
    // Our check above already handles this (isSuccess = successes > 0).

    // 4. Unlock Check
    if (actionType == BackstageActionType.PROMO && !state.isPromoUnlocked()) {
      return new ActionOutcome(0, "Promo action is locked.");
    }
    if (actionType == BackstageActionType.ATTACK && !state.isAttackUnlocked()) {
      return new ActionOutcome(0, "Attack action is locked.");
    }

    // 5. Alignment Check (Attack = Heel Only)
    if (actionType == BackstageActionType.ATTACK) {
      WrestlerAlignment alignment = campaign.getWrestler().getAlignment();
      if (alignment == null || alignment.getAlignmentType() != AlignmentType.HEEL) {
        return new ActionOutcome(0, "Attack action is restricted to Heels.");
      }
    }

    int successes = rollDice(diceSides);
    String outcomeDescription = "";
    boolean isSuccess = successes > 0;

    switch (actionType) {
      case TRAINING:
        if (successes > 0) {
          state.setSkillTokens(state.getSkillTokens() + successes);
          outcomeDescription = "Training successful. Gained " + successes + " Skill Token(s).";
        } else {
          outcomeDescription = "Training failed. No tokens gained.";
        }
        break;
      case RECOVERY:
        if (successes >= 2) {
          if (!injuryService
              .getActiveInjuriesForWrestler(campaign.getWrestler().getId())
              .isEmpty()) {
            if (state.getBumps() > 0) {
              int bumpsRemoved = Math.min(state.getBumps(), 2);
              state.setBumps(state.getBumps() - bumpsRemoved);
              outcomeDescription = "Recovery successful. Removed " + bumpsRemoved + " bumps.";
            } else {
              outcomeDescription = "Recovery successful (Injury healing not auto-applied yet).";
            }
          } else if (state.getBumps() > 0) {
            int bumpsRemoved = Math.min(state.getBumps(), 2);
            state.setBumps(state.getBumps() - bumpsRemoved);
            outcomeDescription = "Recovery successful. Removed " + bumpsRemoved + " bumps.";
          } else {
            outcomeDescription = "Recovery successful. Wrestler is fully healthy.";
          }
        } else if (successes == 1) {
          if (state.getBumps() > 0) {
            state.setBumps(state.getBumps() - 1);
            outcomeDescription = "Recovery successful. Removed 1 bump.";
          } else {
            outcomeDescription = "Recovery successful. Wrestler is fully healthy.";
          }
        } else {
          outcomeDescription = "Recovery failed.";
          isSuccess =
              false; // Explicitly fail if 0 successes (already default, but good for clarity logic
          // if 1 was fail)
        }
        break;
      case PROMO:
        if (successes > 0) {
          // Advance one space on chosen Face or Heel track
          campaignService.shiftAlignment(campaign, 1);

          // Gain +1 momentum per success for start of next match
          state.setMomentumBonus(state.getMomentumBonus() + successes);

          outcomeDescription =
              "Promo successful! Gained +1 on alignment track and +"
                  + successes
                  + " momentum for your next match.";
        } else {
          outcomeDescription = "Promo failed. The crowd wasn't feeling it.";
        }

        // Create a Promo segment
        createPromoSegment(campaign, isSuccess, outcomeDescription);
        break;
      case ATTACK:
        if (successes > 0) {
          outcomeDescription = "Attack successful. Alignment shifted. Opponent damaged.";
        } else {
          outcomeDescription = "Attack failed.";
        }
        break;
    }

    state.setActionsTaken(state.getActionsTaken() + 1);
    state.setLastActionType(actionType);
    state.setLastActionSuccess(isSuccess);

    campaignStateRepository.save(state);

    BackstageActionHistory history =
        BackstageActionHistory.builder()
            .campaign(campaign)
            .actionType(actionType)
            .actionDate(LocalDateTime.now())
            .diceRolled(diceSides)
            .successes(successes)
            .outcomeDescription(outcomeDescription)
            .build();
    actionHistoryRepository.save(history);

    return new ActionOutcome(successes, outcomeDescription);
  }

  /**
   * Roll dice and return the number of successes (4+).
   *
   * @param numberOfDice Number of dice to roll (based on attribute).
   * @return Number of successes.
   */
  public int rollDice(int numberOfDice) {
    int successes = 0;
    for (int i = 0; i < numberOfDice; i++) {
      int roll = random.nextInt(6) + 1;
      if (roll >= 4) {
        successes++;
      }
    }
    return successes;
  }

  private void createPromoSegment(Campaign campaign, boolean success, String description) {
    try {
      // Use existing service to create a segment but forced to Promo
      var show = campaignService.getOrCreateCampaignShow(campaign);
      var promoType = campaignService.getPromoSegmentType();

      var segment = new com.github.javydreamercsw.management.domain.show.segment.Segment();
      segment.setShow(show);
      segment.setSegmentType(promoType);
      segment.setSegmentDate(java.time.Instant.now());
      segment.setNarration("Backstage Promo: " + description);
      segment.setStatus(
          com.github.javydreamercsw.management.domain.show.segment.SegmentStatus.COMPLETED);
      segment.setAdjudicationStatus(
          com.github.javydreamercsw.management.domain.AdjudicationStatus.ADJUDICATED);

      // Add participants
      segment.addParticipant(campaign.getWrestler());
      if (success) {
        segment.setWinners(java.util.List.of(campaign.getWrestler()));
      }

      // Add "Promo" rule
      segmentRuleRepository.findByName("Promo").ifPresent(segment::addSegmentRule);

      campaignService.saveSegment(segment);
      log.info("Created promo segment for campaign {}", campaign.getId());
    } catch (Exception e) {
      log.error("Failed to create promo segment", e);
    }
  }

  public record ActionOutcome(int successes, String description) {}
}
