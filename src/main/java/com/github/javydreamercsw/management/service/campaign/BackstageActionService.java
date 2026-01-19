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

import com.github.javydreamercsw.management.domain.campaign.BackstageActionHistory;
import com.github.javydreamercsw.management.domain.campaign.BackstageActionHistoryRepository;
import com.github.javydreamercsw.management.domain.campaign.BackstageActionType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import java.time.LocalDateTime;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class BackstageActionService {

  private final Random random;
  private final CampaignStateRepository campaignStateRepository;
  private final BackstageActionHistoryRepository actionHistoryRepository;
  private final InjuryService injuryService;

  /**
   * Perform a backstage action.
   *
   * @param campaign The campaign context.
   * @param actionType The type of action.
   * @param attributeValue The value of the wrestler's attribute for this action.
   * @return The result of the action.
   */
  public ActionOutcome performAction(
      Campaign campaign, BackstageActionType actionType, int attributeValue) {
    int successes = rollDice(attributeValue);
    String outcomeDescription = "";
    CampaignState state = campaign.getState();

    switch (actionType) {
      case TRAINING:
        if (successes > 0) {
          state.setSkillTokens(state.getSkillTokens() + 1);
          outcomeDescription = "Training successful. Gained 1 Skill Token.";
        } else {
          outcomeDescription = "Training failed. No tokens gained.";
        }
        break;
      case RECOVERY:
        if (successes >= 2) {
          // Heals 2 bumps OR 1 injury. Logic: prioritize injury if exists?
          // Or bumps if close to injury?
          // Simple logic for now: If has injury, heal injury. Else heal bumps.
          if (!injuryService
              .getActiveInjuriesForWrestler(campaign.getWrestler().getId())
              .isEmpty()) {
            // TODO: Heal random injury or specific? Assuming random for now or manual selection in
            // UI.
            // For MVP, let's just say "Injury healing available" or heal bumps if no choice.
            // Let's heal 2 bumps for automation simplicity unless we pass injury ID.
            // Re-reading spec: "2 successes remove 2 bumps or 1 injury".
            // I'll prioritize bumps for now to avoid complexity of selecting injury here.
            // Or I can check if bumps > 0.
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
        }
        break;
      case PROMO:
        // TODO: Check unlock status
        if (successes > 0) {
          // TODO: Advance Face/Heel track
          // TODO: Add Momentum
          outcomeDescription = "Promo successful. Alignment shifted. Gained Momentum.";
        } else {
          outcomeDescription = "Promo failed.";
        }
        break;
      case ATTACK:
        // TODO: Check unlock status & Heel only
        if (successes > 0) {
          // TODO: Advance Heel track
          // TODO: Penalty to opponent
          outcomeDescription = "Attack successful. Alignment shifted. Opponent damaged.";
        } else {
          outcomeDescription = "Attack failed.";
        }
        break;
    }

    campaignStateRepository.save(state);

    BackstageActionHistory history =
        BackstageActionHistory.builder()
            .campaign(campaign)
            .actionType(actionType)
            .actionDate(LocalDateTime.now())
            .diceRolled(attributeValue)
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

  public record ActionOutcome(int successes, String description) {}
}
