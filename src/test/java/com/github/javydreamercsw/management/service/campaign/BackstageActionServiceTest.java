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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.BackstageActionHistoryRepository;
import com.github.javydreamercsw.management.domain.campaign.BackstageActionType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignPhase;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BackstageActionServiceTest {

  @Mock private CampaignStateRepository campaignStateRepository;
  @Mock private BackstageActionHistoryRepository actionHistoryRepository;
  @Mock private InjuryService injuryService;
  @Mock private CampaignService campaignService;
  @Mock private SegmentRuleRepository segmentRuleRepository;
  @Mock private WrestlerService wrestlerService;
  @Mock private BackstageEncounterService backstageEncounterService;
  @Mock private FeatureDataService featureDataService;

  @Spy @InjectMocks private BackstageActionService backstageActionService;

  private Campaign campaign;
  private CampaignState state;
  private Wrestler wrestler;
  private WrestlerAlignment heelAlignment;

  @BeforeEach
  void setUp() {
    wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName("Test Wrestler");

    heelAlignment = WrestlerAlignment.builder().alignmentType(AlignmentType.HEEL).level(2).build();
    wrestler.setAlignment(heelAlignment);

    state =
        CampaignState.builder()
            .currentPhase(CampaignPhase.BACKSTAGE)
            .actionsTaken(0)
            .promoUnlocked(true)
            .attackUnlocked(true)
            .lastActionType(null)
            .lastActionSuccess(false)
            .skillTokens(0)
            .momentumBonus(0)
            .opponentHealthPenalty(0)
            .healthPenalty(0)
            .build();

    campaign = Campaign.builder().wrestler(wrestler).state(state).build();

    when(campaignStateRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
    when(actionHistoryRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
    when(injuryService.getActiveInjuriesForWrestler(anyLong(), anyLong())).thenReturn(List.of());
  }

  // ==================== Phase check ====================

  @Test
  void performAction_wrongPhase_returnsErrorWithZeroSuccesses() {
    state.setCurrentPhase(CampaignPhase.MATCH);

    var outcome = backstageActionService.performAction(campaign, BackstageActionType.TRAINING, 3);

    assertThat(outcome.successes()).isZero();
    assertThat(outcome.description())
        .isEqualTo("Actions can only be taken during Backstage phase.");
    verify(campaignStateRepository, never()).save(any());
  }

  // ==================== Action limit check ====================

  @Test
  void performAction_actionLimitReached_returnsErrorWithZeroSuccesses() {
    state.setActionsTaken(2);

    var outcome = backstageActionService.performAction(campaign, BackstageActionType.TRAINING, 3);

    assertThat(outcome.successes()).isZero();
    assertThat(outcome.description()).isEqualTo("Action limit reached for this phase.");
    verify(campaignStateRepository, never()).save(any());
  }

  // ==================== Consecutive action check ====================

  @Test
  void performAction_consecutiveSameActionAfterSuccess_returnsError() {
    state.setLastActionType(BackstageActionType.TRAINING);
    state.setLastActionSuccess(true);

    var outcome = backstageActionService.performAction(campaign, BackstageActionType.TRAINING, 3);

    assertThat(outcome.successes()).isZero();
    assertThat(outcome.description())
        .isEqualTo("Cannot perform the same action twice in a row unless it failed.");
    verify(campaignStateRepository, never()).save(any());
  }

  @Test
  void performAction_sameActionAfterFailure_isAllowed() {
    state.setLastActionType(BackstageActionType.TRAINING);
    state.setLastActionSuccess(false);
    doReturn(List.of(4, 5, 6)).when(backstageActionService).rollDice(anyInt());

    var outcome = backstageActionService.performAction(campaign, BackstageActionType.TRAINING, 3);

    assertThat(outcome.successes()).isGreaterThan(0);
  }

  // ==================== Unlock checks ====================

  @Test
  void performAction_promoLocked_returnsError() {
    state.setPromoUnlocked(false);

    var outcome = backstageActionService.performAction(campaign, BackstageActionType.PROMO, 3);

    assertThat(outcome.successes()).isZero();
    assertThat(outcome.description()).isEqualTo("Promo action is locked.");
    verify(campaignStateRepository, never()).save(any());
  }

  @Test
  void performAction_attackLocked_returnsError() {
    state.setAttackUnlocked(false);

    var outcome = backstageActionService.performAction(campaign, BackstageActionType.ATTACK, 3);

    assertThat(outcome.successes()).isZero();
    assertThat(outcome.description()).isEqualTo("Attack action is locked.");
    verify(campaignStateRepository, never()).save(any());
  }

  // ==================== Alignment check ====================

  @Test
  void performAction_attackWhenNotHeel_returnsError() {
    heelAlignment.setAlignmentType(AlignmentType.FACE);

    var outcome = backstageActionService.performAction(campaign, BackstageActionType.ATTACK, 3);

    assertThat(outcome.successes()).isZero();
    assertThat(outcome.description()).isEqualTo("Attack action is restricted to Heels.");
    verify(campaignStateRepository, never()).save(any());
  }

  @Test
  void performAction_attackWhenNeutral_returnsError() {
    heelAlignment.setAlignmentType(AlignmentType.NEUTRAL);

    var outcome = backstageActionService.performAction(campaign, BackstageActionType.ATTACK, 3);

    assertThat(outcome.successes()).isZero();
    assertThat(outcome.description()).isEqualTo("Attack action is restricted to Heels.");
  }

  @Test
  void performAction_attackWhenAlignmentFace_returnsError() {
    // Create fresh wrestler with FACE alignment — setAlignment(null) is a no-op on Wrestler
    Wrestler faceWrestler = new Wrestler();
    faceWrestler.setId(2L);
    faceWrestler.setName("Face Wrestler");
    WrestlerAlignment faceAlignment =
        WrestlerAlignment.builder().alignmentType(AlignmentType.FACE).level(1).build();
    faceWrestler.setAlignment(faceAlignment);
    campaign.setWrestler(faceWrestler);

    var outcome = backstageActionService.performAction(campaign, BackstageActionType.ATTACK, 3);

    assertThat(outcome.successes()).isZero();
    assertThat(outcome.description()).isEqualTo("Attack action is restricted to Heels.");
  }

  @Test
  void performAction_attackWhenNoAlignmentSet_returnsError() {
    // Wrestler with no alignment added at all (alignments set is empty)
    Wrestler noAlignmentWrestler = new Wrestler();
    noAlignmentWrestler.setId(3L);
    noAlignmentWrestler.setName("No Alignment Wrestler");
    // Note: setAlignment(null) is a no-op; by not calling setAlignment, alignments is empty
    // getAlignment() returns null when alignments is empty
    campaign.setWrestler(noAlignmentWrestler);

    var outcome = backstageActionService.performAction(campaign, BackstageActionType.ATTACK, 3);

    assertThat(outcome.successes()).isZero();
    assertThat(outcome.description()).isEqualTo("Attack action is restricted to Heels.");
  }

  // ==================== TRAINING ====================

  @Test
  void performAction_trainingSuccess_addsSkillTokens() {
    doReturn(List.of(4, 5, 6)).when(backstageActionService).rollDice(anyInt());
    state.setSkillTokens(1);

    var outcome = backstageActionService.performAction(campaign, BackstageActionType.TRAINING, 3);

    assertThat(outcome.successes()).isEqualTo(3);
    assertThat(outcome.description()).contains("Gained 3 Skill Token(s)");
    assertThat(state.getSkillTokens()).isEqualTo(4);
    verify(campaignStateRepository).save(state);
  }

  @Test
  void performAction_trainingFailure_noTokensGained() {
    doReturn(List.of(1, 2, 3)).when(backstageActionService).rollDice(anyInt());

    var outcome = backstageActionService.performAction(campaign, BackstageActionType.TRAINING, 3);

    assertThat(outcome.successes()).isZero();
    assertThat(outcome.description()).isEqualTo("Training failed. No tokens gained.");
    assertThat(state.getSkillTokens()).isZero();
    verify(campaignStateRepository).save(state);
  }

  // ==================== RECOVERY ====================

  @Test
  void performAction_recoveryTwoSuccessesWithInjury_healsInjury() {
    var injury = new Injury();
    injury.setId(42L);
    var severity = com.github.javydreamercsw.management.domain.injury.InjurySeverity.MINOR;
    injury.setSeverity(severity);
    when(injuryService.getActiveInjuriesForWrestler(anyLong(), anyLong()))
        .thenReturn(List.of(injury));
    // wrestler has no WrestlerState (bumps = 0) by default
    doReturn(List.of(4, 5)).when(backstageActionService).rollDice(anyInt());

    var outcome = backstageActionService.performAction(campaign, BackstageActionType.RECOVERY, 2);

    assertThat(outcome.successes()).isEqualTo(2);
    assertThat(outcome.description()).contains("Healed injury");
    verify(injuryService).healInjuryFree(42L);
  }

  @Test
  void performAction_recoveryTwoSuccessesNoBumpsOrInjuries_fullyHealthy() {
    when(injuryService.getActiveInjuriesForWrestler(anyLong(), anyLong())).thenReturn(List.of());
    doReturn(List.of(4, 5)).when(backstageActionService).rollDice(anyInt());

    var outcome = backstageActionService.performAction(campaign, BackstageActionType.RECOVERY, 2);

    assertThat(outcome.successes()).isEqualTo(2);
    assertThat(outcome.description()).contains("fully healthy");
    verify(wrestlerService, never()).healBump(anyLong(), anyLong());
  }

  @Test
  void performAction_recoveryTwoSuccessesWithBumpsNoInjuries_removesTwoBumps() {
    when(injuryService.getActiveInjuriesForWrestler(anyLong(), anyLong())).thenReturn(List.of());
    WrestlerState ws = new WrestlerState();
    ws.setBumps(3);
    // Use Spy pattern on Wrestler to return the WrestlerState
    Wrestler spyWrestler = org.mockito.Mockito.spy(new Wrestler());
    spyWrestler.setId(10L);
    spyWrestler.setName("Spy Wrestler");
    spyWrestler.setAlignment(heelAlignment);
    doReturn(Optional.of(ws)).when(spyWrestler).getDefaultState();
    campaign.setWrestler(spyWrestler);
    doReturn(List.of(4, 5)).when(backstageActionService).rollDice(anyInt());

    var outcome = backstageActionService.performAction(campaign, BackstageActionType.RECOVERY, 2);

    assertThat(outcome.successes()).isEqualTo(2);
    assertThat(outcome.description()).contains("Removed 2 bumps");
    verify(wrestlerService, org.mockito.Mockito.times(2)).healBump(anyLong(), anyLong());
  }

  @Test
  void performAction_recoveryOneSuccessWithBumps_removesOneBump() {
    when(injuryService.getActiveInjuriesForWrestler(anyLong(), anyLong())).thenReturn(List.of());
    WrestlerState ws = new WrestlerState();
    ws.setBumps(2);
    Wrestler spyWrestler = org.mockito.Mockito.spy(new Wrestler());
    spyWrestler.setId(11L);
    spyWrestler.setName("Spy Wrestler");
    spyWrestler.setAlignment(heelAlignment);
    doReturn(Optional.of(ws)).when(spyWrestler).getDefaultState();
    campaign.setWrestler(spyWrestler);
    doReturn(List.of(4, 2, 3)).when(backstageActionService).rollDice(anyInt());

    var outcome = backstageActionService.performAction(campaign, BackstageActionType.RECOVERY, 3);

    assertThat(outcome.successes()).isEqualTo(1);
    assertThat(outcome.description()).contains("Removed 1 bump");
    verify(wrestlerService, org.mockito.Mockito.times(1)).healBump(anyLong(), anyLong());
  }

  @Test
  void performAction_recoveryZeroSuccesses_recoveryFailed() {
    doReturn(List.of(1, 2, 3)).when(backstageActionService).rollDice(anyInt());

    var outcome = backstageActionService.performAction(campaign, BackstageActionType.RECOVERY, 3);

    assertThat(outcome.successes()).isZero();
    assertThat(outcome.description()).contains("Recovery failed");
    verify(wrestlerService, never()).healBump(anyLong(), anyLong());
    verify(injuryService, never()).healInjuryFree(anyLong());
  }

  // ==================== PROMO ====================

  @Test
  void performAction_promoSuccess_shiftsAlignmentAndAddsMomentum() {
    doReturn(List.of(4, 5, 6)).when(backstageActionService).rollDice(anyInt());
    state.setMomentumBonus(0);

    var outcome = backstageActionService.performAction(campaign, BackstageActionType.PROMO, 3);

    assertThat(outcome.successes()).isEqualTo(3);
    assertThat(outcome.description()).contains("Promo successful");
    assertThat(outcome.description()).contains("+3 momentum");
    assertThat(state.getMomentumBonus()).isEqualTo(3);
    verify(campaignService).shiftAlignment(campaign, 1);
  }

  @Test
  void performAction_promoFailure_returnsFailureMessage() {
    doReturn(List.of(1, 2, 3)).when(backstageActionService).rollDice(anyInt());

    var outcome = backstageActionService.performAction(campaign, BackstageActionType.PROMO, 3);

    assertThat(outcome.successes()).isZero();
    assertThat(outcome.description()).contains("Promo failed");
    verify(campaignService, never()).shiftAlignment(any(), anyInt());
  }

  // ==================== ATTACK ====================

  @Test
  void performAction_attackSuccess_shiftsAlignmentAndSetsOpponentPenalty() {
    doReturn(List.of(4, 5, 6)).when(backstageActionService).rollDice(anyInt());
    state.setOpponentHealthPenalty(0);

    var outcome = backstageActionService.performAction(campaign, BackstageActionType.ATTACK, 3);

    assertThat(outcome.successes()).isEqualTo(3);
    assertThat(outcome.description()).contains("Attack successful");
    assertThat(outcome.description()).contains("-3 health");
    assertThat(state.getOpponentHealthPenalty()).isEqualTo(3);
    verify(campaignService).shiftAlignment(campaign, -1);
  }

  @Test
  void performAction_attackRollContainsOne_selfPenaltyApplied() {
    // Rolls contain a 1 → self penalty; also has a success (4) to pass the attack
    doReturn(List.of(1, 4, 2)).when(backstageActionService).rollDice(anyInt());
    state.setHealthPenalty(0);

    var outcome = backstageActionService.performAction(campaign, BackstageActionType.ATTACK, 3);

    assertThat(outcome.description()).contains("Rolled a 1");
    assertThat(outcome.description()).contains("-1 starting health");
    assertThat(state.getHealthPenalty()).isEqualTo(1);
  }

  @Test
  void performAction_attackFailedWithRollOf1_selfPenaltyAppliedWithoutOpponentPenalty() {
    // All rolls fail (1, 2, 3) but contain a 1 → self penalty still applies
    doReturn(List.of(1, 2, 3)).when(backstageActionService).rollDice(anyInt());
    state.setHealthPenalty(0);
    state.setOpponentHealthPenalty(0);

    var outcome = backstageActionService.performAction(campaign, BackstageActionType.ATTACK, 3);

    assertThat(outcome.successes()).isZero();
    assertThat(outcome.description()).contains("Rolled a 1");
    assertThat(state.getHealthPenalty()).isEqualTo(1);
    assertThat(state.getOpponentHealthPenalty()).isZero();
  }

  // ==================== State persistence ====================

  @Test
  void performAction_success_incrementsActionsTakenAndSetsLastAction() {
    doReturn(List.of(4, 5)).when(backstageActionService).rollDice(anyInt());
    state.setActionsTaken(0);

    backstageActionService.performAction(campaign, BackstageActionType.TRAINING, 2);

    assertThat(state.getActionsTaken()).isEqualTo(1);
    assertThat(state.getLastActionType()).isEqualTo(BackstageActionType.TRAINING);
    assertThat(state.getLastActionSuccess()).isTrue();
    verify(campaignStateRepository).save(state);
    verify(actionHistoryRepository).save(any());
  }

  @Test
  void performAction_failure_setsLastActionSuccessFalse() {
    doReturn(List.of(1, 2, 3)).when(backstageActionService).rollDice(anyInt());

    backstageActionService.performAction(campaign, BackstageActionType.TRAINING, 3);

    assertThat(state.getLastActionSuccess()).isFalse();
    assertThat(state.getLastActionType()).isEqualTo(BackstageActionType.TRAINING);
    verify(campaignStateRepository).save(state);
  }

  // ==================== rollDice ====================

  @Test
  void rollDice_zeroOrNegative_returnsEmptyList() {
    assertThat(backstageActionService.rollDice(0)).isEmpty();
    assertThat(backstageActionService.rollDice(-1)).isEmpty();
  }

  @Test
  void rollDice_three_returnsThreeResults() {
    List<Integer> rolls = backstageActionService.rollDice(3);

    assertThat(rolls).hasSize(3);
    assertThat(rolls).allMatch(r -> r >= 1 && r <= 6);
  }

  @Test
  void rollDice_one_returnsSingleResult() {
    List<Integer> rolls = backstageActionService.rollDice(1);

    assertThat(rolls).hasSize(1);
    assertThat(rolls.get(0)).isBetween(1, 6);
  }
}
