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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCard;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignmentRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AlignmentServiceTest {

  @Mock private WrestlerAlignmentRepository wrestlerAlignmentRepository;

  @Mock private CampaignStateRepository campaignStateRepository;

  @InjectMocks private AlignmentService alignmentService;

  private Wrestler wrestler;
  private Campaign campaign;
  private CampaignState state;
  private WrestlerAlignment alignment;

  @BeforeEach
  void setUp() {
    wrestler = new Wrestler();
    wrestler.setName("Test Wrestler");

    state = CampaignState.builder().activeCards(new ArrayList<>()).build();

    campaign = new Campaign();
    campaign.setWrestler(wrestler);
    campaign.setState(state);

    alignment = new WrestlerAlignment();
    alignment.setWrestler(wrestler);

    when(wrestlerAlignmentRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
    when(campaignStateRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
  }

  // ==================== shiftAlignment ====================

  @Test
  void shiftAlignment_zeroAmount_doesNothing() {
    alignmentService.shiftAlignment(campaign, 0);

    verify(wrestlerAlignmentRepository, never()).findByWrestler(any());
    verify(campaignStateRepository, never()).save(any());
  }

  @Test
  void shiftAlignment_neutralPositive_becomesFace() {
    alignment.setAlignmentType(AlignmentType.NEUTRAL);
    alignment.setLevel(0);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    alignmentService.shiftAlignment(campaign, 2);

    assertEquals(AlignmentType.FACE, alignment.getAlignmentType());
    assertEquals(2, alignment.getLevel());
    verify(wrestlerAlignmentRepository).save(alignment);
    verify(campaignStateRepository, times(2)).save(state);
  }

  @Test
  void shiftAlignment_neutralNegative_becomesHeel() {
    alignment.setAlignmentType(AlignmentType.NEUTRAL);
    alignment.setLevel(0);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    alignmentService.shiftAlignment(campaign, -3);

    assertEquals(AlignmentType.HEEL, alignment.getAlignmentType());
    assertEquals(3, alignment.getLevel());
    verify(wrestlerAlignmentRepository).save(alignment);
    verify(campaignStateRepository, times(2)).save(state);
  }

  @Test
  void shiftAlignment_facePositive_levelIncreases() {
    alignment.setAlignmentType(AlignmentType.FACE);
    alignment.setLevel(2);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    alignmentService.shiftAlignment(campaign, 1);

    assertEquals(AlignmentType.FACE, alignment.getAlignmentType());
    assertEquals(3, alignment.getLevel());
    verify(wrestlerAlignmentRepository).save(alignment);
    verify(campaignStateRepository).save(state);
  }

  @Test
  void shiftAlignment_faceLevel_cappedAt5() {
    alignment.setAlignmentType(AlignmentType.FACE);
    alignment.setLevel(4);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    alignmentService.shiftAlignment(campaign, 10);

    assertEquals(AlignmentType.FACE, alignment.getAlignmentType());
    assertEquals(5, alignment.getLevel());
  }

  @Test
  void shiftAlignment_faceGoesNegative_becomesNeutral() {
    alignment.setAlignmentType(AlignmentType.FACE);
    alignment.setLevel(1);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    alignmentService.shiftAlignment(campaign, -2);

    assertEquals(AlignmentType.NEUTRAL, alignment.getAlignmentType());
    assertEquals(0, alignment.getLevel());
    verify(wrestlerAlignmentRepository).save(alignment);
    // campaignStateRepository.save is called from handleLevelChange AND updateAbilityCards
    verify(campaignStateRepository, times(2)).save(state);
  }

  @Test
  void shiftAlignment_faceGoesToZero_becomesNeutral() {
    alignment.setAlignmentType(AlignmentType.FACE);
    alignment.setLevel(2);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    alignmentService.shiftAlignment(campaign, -2);

    assertEquals(AlignmentType.NEUTRAL, alignment.getAlignmentType());
    assertEquals(0, alignment.getLevel());
  }

  @Test
  void shiftAlignment_heelPositiveAmount_movesTowardNeutral() {
    alignment.setAlignmentType(AlignmentType.HEEL);
    alignment.setLevel(3);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    alignmentService.shiftAlignment(campaign, 1);

    assertEquals(AlignmentType.HEEL, alignment.getAlignmentType());
    assertEquals(2, alignment.getLevel());
    verify(wrestlerAlignmentRepository).save(alignment);
    verify(campaignStateRepository).save(state);
  }

  @Test
  void shiftAlignment_heelLevelDropsToZero_becomesNeutral() {
    alignment.setAlignmentType(AlignmentType.HEEL);
    alignment.setLevel(2);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    alignmentService.shiftAlignment(campaign, 2);

    assertEquals(AlignmentType.NEUTRAL, alignment.getAlignmentType());
    assertEquals(0, alignment.getLevel());
    verify(wrestlerAlignmentRepository).save(alignment);
    // type changed → updateAbilityCards also saves
    verify(campaignStateRepository, times(2)).save(state);
  }

  @Test
  void shiftAlignment_heelNegativeAmount_deepensHeel() {
    alignment.setAlignmentType(AlignmentType.HEEL);
    alignment.setLevel(2);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    alignmentService.shiftAlignment(campaign, -1);

    assertEquals(AlignmentType.HEEL, alignment.getAlignmentType());
    assertEquals(3, alignment.getLevel());
  }

  @Test
  void shiftAlignment_heelLevel_cappedAt5() {
    alignment.setAlignmentType(AlignmentType.HEEL);
    alignment.setLevel(4);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    alignmentService.shiftAlignment(campaign, -10);

    assertEquals(AlignmentType.HEEL, alignment.getAlignmentType());
    assertEquals(5, alignment.getLevel());
  }

  @Test
  void shiftAlignment_alignmentNotFound_throwsIllegalStateException() {
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.empty());

    assertThrows(IllegalStateException.class, () -> alignmentService.shiftAlignment(campaign, 1));
  }

  // ==================== handleLevelChange ====================

  @Test
  void handleLevelChange_neutralToLevel1_grantsPendingL1Pick() {
    alignment.setAlignmentType(AlignmentType.FACE);
    alignment.setLevel(1);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));
    state.setPendingL1Picks(0);

    alignmentService.handleLevelChange(campaign, 0, 1);

    assertEquals(1, state.getPendingL1Picks());
    verify(campaignStateRepository).save(state);
  }

  @Test
  void handleLevelChange_faceReachesLevel4_grantsPendingL2Pick() {
    alignment.setAlignmentType(AlignmentType.FACE);
    alignment.setLevel(4);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));
    state.setPendingL2Picks(0);

    alignmentService.handleLevelChange(campaign, 3, 4);

    assertEquals(1, state.getPendingL2Picks());
    verify(campaignStateRepository).save(state);
  }

  @Test
  void handleLevelChange_faceReachesLevel5_grantsPendingL3PickAndRemovesL1Card() {
    alignment.setAlignmentType(AlignmentType.FACE);
    alignment.setLevel(5);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    CampaignAbilityCard l1Card =
        CampaignAbilityCard.builder()
            .name("L1 Face Card")
            .alignmentType(AlignmentType.FACE)
            .level(1)
            .build();
    state.getActiveCards().add(l1Card);
    state.setPendingL3Picks(0);

    alignmentService.handleLevelChange(campaign, 4, 5);

    assertEquals(1, state.getPendingL3Picks());
    assertEquals(0, state.getActiveCards().size());
    verify(campaignStateRepository).save(state);
  }

  @Test
  void handleLevelChange_heelReachesLevel4_grantsPendingL2PickAndRemovesL1Card() {
    alignment.setAlignmentType(AlignmentType.HEEL);
    alignment.setLevel(4);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    CampaignAbilityCard l1Card =
        CampaignAbilityCard.builder()
            .name("L1 Heel Card")
            .alignmentType(AlignmentType.HEEL)
            .level(1)
            .build();
    state.getActiveCards().add(l1Card);
    state.setPendingL2Picks(0);
    state.setPendingL1Picks(1);

    alignmentService.handleLevelChange(campaign, 3, 4);

    assertEquals(1, state.getPendingL2Picks());
    assertEquals(0, state.getActiveCards().size());
    assertEquals(0, state.getPendingL1Picks());
    verify(campaignStateRepository).save(state);
  }

  @Test
  void handleLevelChange_heelReachesLevel5_grantsPendingL1Pick() {
    alignment.setAlignmentType(AlignmentType.HEEL);
    alignment.setLevel(5);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));
    state.setPendingL1Picks(0);

    alignmentService.handleLevelChange(campaign, 4, 5);

    assertEquals(1, state.getPendingL1Picks());
    verify(campaignStateRepository).save(state);
  }

  @Test
  void handleLevelChange_alignmentNotFound_throwsIllegalStateException() {
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.empty());

    assertThrows(
        IllegalStateException.class, () -> alignmentService.handleLevelChange(campaign, 0, 1));
  }

  // ==================== updateAbilityCards ====================

  @Test
  void updateAbilityCards_noAlignmentFound_doesNothing() {
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.empty());

    alignmentService.updateAbilityCards(campaign);

    verify(campaignStateRepository, never()).save(any());
  }

  @Test
  void updateAbilityCards_noMismatch_doesNotClearCards() {
    alignment.setAlignmentType(AlignmentType.FACE);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    CampaignAbilityCard faceCard =
        CampaignAbilityCard.builder()
            .name("Face Card")
            .alignmentType(AlignmentType.FACE)
            .level(1)
            .build();
    state.getActiveCards().add(faceCard);

    alignmentService.updateAbilityCards(campaign);

    assertEquals(1, state.getActiveCards().size());
    verify(campaignStateRepository).save(state);
  }

  @Test
  void updateAbilityCards_alignmentMismatch_clearsCardsAndRecalculates() {
    alignment.setAlignmentType(AlignmentType.HEEL);
    alignment.setLevel(2);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    CampaignAbilityCard faceCard =
        CampaignAbilityCard.builder()
            .name("Old Face Card")
            .alignmentType(AlignmentType.FACE)
            .level(1)
            .build();
    state.getActiveCards().add(faceCard);
    state.setPendingL1Picks(0);

    alignmentService.updateAbilityCards(campaign);

    assertEquals(0, state.getActiveCards().size());
    // HEEL level 2 → pendingL1Picks should be 1
    assertEquals(1, state.getPendingL1Picks());
    verify(campaignStateRepository).save(state);
  }

  // ==================== recalculatePendingPicks (via updateAbilityCards) ====================

  @Test
  void recalculatePendingPicks_faceLevel1_setsL1PickOnly() {
    alignment.setAlignmentType(AlignmentType.FACE);
    alignment.setLevel(1);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    // Force a mismatch to trigger recalculation
    CampaignAbilityCard heelCard =
        CampaignAbilityCard.builder()
            .name("Heel Card")
            .alignmentType(AlignmentType.HEEL)
            .level(1)
            .build();
    state.getActiveCards().add(heelCard);

    alignmentService.updateAbilityCards(campaign);

    assertEquals(1, state.getPendingL1Picks());
    assertEquals(0, state.getPendingL2Picks());
    assertEquals(0, state.getPendingL3Picks());
  }

  @Test
  void recalculatePendingPicks_faceLevel4_setsL1AndL2Picks() {
    alignment.setAlignmentType(AlignmentType.FACE);
    alignment.setLevel(4);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    CampaignAbilityCard heelCard =
        CampaignAbilityCard.builder()
            .name("Heel Card")
            .alignmentType(AlignmentType.HEEL)
            .level(1)
            .build();
    state.getActiveCards().add(heelCard);

    alignmentService.updateAbilityCards(campaign);

    assertEquals(1, state.getPendingL1Picks());
    assertEquals(1, state.getPendingL2Picks());
    assertEquals(0, state.getPendingL3Picks());
  }

  @Test
  void recalculatePendingPicks_faceLevel5_setsAllPickLevels() {
    alignment.setAlignmentType(AlignmentType.FACE);
    alignment.setLevel(5);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    CampaignAbilityCard heelCard =
        CampaignAbilityCard.builder()
            .name("Heel Card")
            .alignmentType(AlignmentType.HEEL)
            .level(1)
            .build();
    state.getActiveCards().add(heelCard);

    alignmentService.updateAbilityCards(campaign);

    // Face level 5: L1 is NOT set (level >= 5 not in 1..4), L2 is set, L3 is set
    assertEquals(0, state.getPendingL1Picks());
    assertEquals(1, state.getPendingL2Picks());
    assertEquals(1, state.getPendingL3Picks());
  }

  @Test
  void recalculatePendingPicks_heelLevel1_setsL1PickOnly() {
    alignment.setAlignmentType(AlignmentType.HEEL);
    alignment.setLevel(1);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    CampaignAbilityCard faceCard =
        CampaignAbilityCard.builder()
            .name("Face Card")
            .alignmentType(AlignmentType.FACE)
            .level(1)
            .build();
    state.getActiveCards().add(faceCard);

    alignmentService.updateAbilityCards(campaign);

    assertEquals(1, state.getPendingL1Picks());
    assertEquals(0, state.getPendingL2Picks());
    assertEquals(0, state.getPendingL3Picks());
  }

  @Test
  void recalculatePendingPicks_heelLevel4_setsL2PickOnly() {
    alignment.setAlignmentType(AlignmentType.HEEL);
    alignment.setLevel(4);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    CampaignAbilityCard faceCard =
        CampaignAbilityCard.builder()
            .name("Face Card")
            .alignmentType(AlignmentType.FACE)
            .level(1)
            .build();
    state.getActiveCards().add(faceCard);

    alignmentService.updateAbilityCards(campaign);

    // HEEL level 4: L1 range is 1..3 so not set; L2 is set
    assertEquals(0, state.getPendingL1Picks());
    assertEquals(1, state.getPendingL2Picks());
    assertEquals(0, state.getPendingL3Picks());
  }

  @Test
  void recalculatePendingPicks_heelLevel5_setsL1AndL2Picks() {
    alignment.setAlignmentType(AlignmentType.HEEL);
    alignment.setLevel(5);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    CampaignAbilityCard faceCard =
        CampaignAbilityCard.builder()
            .name("Face Card")
            .alignmentType(AlignmentType.FACE)
            .level(1)
            .build();
    state.getActiveCards().add(faceCard);

    alignmentService.updateAbilityCards(campaign);

    // HEEL level 5: regains L1 slot, has L2
    assertEquals(1, state.getPendingL1Picks());
    assertEquals(1, state.getPendingL2Picks());
    assertEquals(0, state.getPendingL3Picks());
  }

  @Test
  void recalculatePendingPicks_neutralAlignment_resetsAllPicks() {
    alignment.setAlignmentType(AlignmentType.NEUTRAL);
    alignment.setLevel(0);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    // Build a card list with a HEEL card to trigger mismatch — but alignment is NEUTRAL
    // NEUTRAL has no cards to mismatch against; inject a FACE card to force the code path
    CampaignAbilityCard heelCard =
        CampaignAbilityCard.builder()
            .name("Heel Card")
            .alignmentType(AlignmentType.HEEL)
            .level(1)
            .build();
    state.getActiveCards().add(heelCard);
    state.setPendingL1Picks(3);
    state.setPendingL2Picks(2);
    state.setPendingL3Picks(1);

    alignmentService.updateAbilityCards(campaign);

    // After recalculate: NEUTRAL type → neither FACE nor HEEL block runs → all stay 0
    assertEquals(0, state.getPendingL1Picks());
    assertEquals(0, state.getPendingL2Picks());
    assertEquals(0, state.getPendingL3Picks());
  }

  // ==================== shiftAlignment level milestones ====================

  @Test
  void shiftAlignment_faceReachesLevel4_pendingL2PickGranted() {
    alignment.setAlignmentType(AlignmentType.FACE);
    alignment.setLevel(3);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));
    state.setPendingL2Picks(0);

    alignmentService.shiftAlignment(campaign, 1);

    assertEquals(AlignmentType.FACE, alignment.getAlignmentType());
    assertEquals(4, alignment.getLevel());
    assertEquals(1, state.getPendingL2Picks());
  }

  @Test
  void shiftAlignment_heelReachesLevel5_pendingL1PickGranted() {
    alignment.setAlignmentType(AlignmentType.HEEL);
    alignment.setLevel(4);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));
    state.setPendingL1Picks(0);
    state
        .getActiveCards()
        .add(
            CampaignAbilityCard.builder()
                .name("L1 Card")
                .alignmentType(AlignmentType.HEEL)
                .level(1)
                .build());

    alignmentService.shiftAlignment(campaign, -1);

    assertEquals(AlignmentType.HEEL, alignment.getAlignmentType());
    assertEquals(5, alignment.getLevel());
    assertEquals(1, state.getPendingL1Picks());
  }

  @Test
  void shiftAlignment_neutralToFace_savedAndAbilityCardsUpdated() {
    alignment.setAlignmentType(AlignmentType.NEUTRAL);
    alignment.setLevel(0);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    alignmentService.shiftAlignment(campaign, 1);

    assertEquals(AlignmentType.FACE, alignment.getAlignmentType());
    assertEquals(1, alignment.getLevel());
    // save called by handleLevelChange and by updateAbilityCards (type changed)
    verify(campaignStateRepository, times(2)).save(state);
  }

  @Test
  void shiftAlignment_neutralToHeel_savedAndAbilityCardsUpdated() {
    alignment.setAlignmentType(AlignmentType.NEUTRAL);
    alignment.setLevel(0);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    alignmentService.shiftAlignment(campaign, -1);

    assertEquals(AlignmentType.HEEL, alignment.getAlignmentType());
    assertEquals(1, alignment.getLevel());
    verify(campaignStateRepository, times(2)).save(state);
  }

  @Test
  void shiftAlignment_sameAlignmentNoTypeChange_updateAbilityCardsNotTriggered() {
    alignment.setAlignmentType(AlignmentType.FACE);
    alignment.setLevel(2);
    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    List<CampaignAbilityCard> cards = state.getActiveCards();
    int originalSize = cards.size();

    alignmentService.shiftAlignment(campaign, 1);

    // Only handleLevelChange saves, not updateAbilityCards
    verify(campaignStateRepository, times(1)).save(state);
    assertEquals(originalSize, state.getActiveCards().size());
  }

  // ==================== getOrCreateUniverseAlignment ====================

  @Test
  void getOrCreateUniverseAlignment_createsNeutralWhenNotFound() {
    Universe universe = Universe.builder().name("Test Universe").build();
    when(wrestlerAlignmentRepository.findByWrestlerAndUniverse(wrestler, universe))
        .thenReturn(Optional.empty());

    WrestlerAlignment result = alignmentService.getOrCreateUniverseAlignment(wrestler, universe);

    assertNotNull(result);
    assertEquals(AlignmentType.NEUTRAL, result.getAlignmentType());
    assertEquals(0, result.getLevel());
    verify(wrestlerAlignmentRepository).save(any(WrestlerAlignment.class));
  }

  @Test
  void getOrCreateUniverseAlignment_returnsExistingRecord() {
    Universe universe = Universe.builder().name("Test Universe").build();
    WrestlerAlignment existing =
        WrestlerAlignment.builder()
            .wrestler(wrestler)
            .universe(universe)
            .alignmentType(AlignmentType.FACE)
            .level(3)
            .build();
    when(wrestlerAlignmentRepository.findByWrestlerAndUniverse(wrestler, universe))
        .thenReturn(Optional.of(existing));

    WrestlerAlignment result = alignmentService.getOrCreateUniverseAlignment(wrestler, universe);

    assertSame(existing, result);
    verify(wrestlerAlignmentRepository, never()).save(any());
  }

  // ==================== setUniverseAlignment ====================

  @Test
  void setUniverseAlignment_persistsTypeAndLevel() {
    Universe universe = Universe.builder().name("Test Universe").build();
    when(wrestlerAlignmentRepository.findByWrestlerAndUniverse(wrestler, universe))
        .thenReturn(Optional.empty());

    WrestlerAlignment result =
        alignmentService.setUniverseAlignment(wrestler, universe, AlignmentType.HEEL, 4);

    assertEquals(AlignmentType.HEEL, result.getAlignmentType());
    assertEquals(4, result.getLevel());
  }

  @Test
  void setUniverseAlignment_clampsLevelToMaxFive() {
    Universe universe = Universe.builder().name("Test Universe").build();
    when(wrestlerAlignmentRepository.findByWrestlerAndUniverse(wrestler, universe))
        .thenReturn(Optional.empty());

    WrestlerAlignment result =
        alignmentService.setUniverseAlignment(wrestler, universe, AlignmentType.FACE, 99);

    assertEquals(5, result.getLevel());
  }

  @Test
  void setUniverseAlignment_clampsLevelToMinZero() {
    Universe universe = Universe.builder().name("Test Universe").build();
    when(wrestlerAlignmentRepository.findByWrestlerAndUniverse(wrestler, universe))
        .thenReturn(Optional.empty());

    WrestlerAlignment result =
        alignmentService.setUniverseAlignment(wrestler, universe, AlignmentType.HEEL, -3);

    assertEquals(0, result.getLevel());
  }
}
