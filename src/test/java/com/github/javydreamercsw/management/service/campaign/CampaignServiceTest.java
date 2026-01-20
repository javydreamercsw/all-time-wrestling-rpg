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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

  @Mock private CampaignRepository campaignRepository;
  @Mock private CampaignStateRepository campaignStateRepository;
  @Mock private CampaignAbilityCardRepository campaignAbilityCardRepository;
  @Mock private WrestlerAlignmentRepository wrestlerAlignmentRepository;
  @Mock private CampaignScriptService campaignScriptService;
  @InjectMocks private CampaignService campaignService;

  @Test
  void testStartCampaign() {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);

    when(campaignRepository.save(any(Campaign.class))).thenAnswer(i -> i.getArguments()[0]);
    lenient()
        .when(campaignRepository.findActiveByWrestler(any()))
        .thenReturn(java.util.Optional.empty());
    lenient().when(wrestlerAlignmentRepository.findByWrestler(any())).thenReturn(Optional.empty());
    lenient().when(wrestlerAlignmentRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    Campaign campaign = campaignService.startCampaign(wrestler);

    assertThat(campaign).isNotNull();
    assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
    assertThat(campaign.getState()).isNotNull();
    assertThat(campaign.getState().getCurrentChapter()).isEqualTo(1);
    assertThat(campaign.getState().getPendingL1Picks()).isZero(); // Neutral start

    verify(campaignRepository, org.mockito.Mockito.atLeastOnce()).save(any(Campaign.class));
    verify(campaignStateRepository).save(any(CampaignState.class));
  }

  @Test
  void testHandleLevelChange_NeutralToFace() {
    Wrestler wrestler = new Wrestler();
    Campaign campaign = new Campaign();
    campaign.setWrestler(wrestler);
    CampaignState state = new CampaignState();
    state.setActiveCards(new ArrayList<>());
    campaign.setState(state);

    WrestlerAlignment alignment = new WrestlerAlignment();
    alignment.setAlignmentType(AlignmentType.FACE);
    alignment.setLevel(1);

    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    // Transition from 0 to 1
    campaignService.handleLevelChange(campaign, 0, 1);

    // Should gain a Level 1 pick
    assertThat(state.getPendingL1Picks()).isEqualTo(1);
    verify(campaignStateRepository).save(state);
  }

  @Test
  void testHandleLevelChange_Face() {
    Wrestler wrestler = new Wrestler();
    Campaign campaign = new Campaign();
    campaign.setWrestler(wrestler);
    CampaignState state = new CampaignState();
    state.setActiveCards(new ArrayList<>());
    campaign.setState(state);

    WrestlerAlignment alignment = new WrestlerAlignment();
    alignment.setAlignmentType(AlignmentType.FACE);
    alignment.setLevel(5);

    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    // Add a Level 1 card to be lost
    CampaignAbilityCard l1Card = CampaignAbilityCard.builder().id(1L).level(1).build();
    state.getActiveCards().add(l1Card);

    // Change level from 4 to 5
    campaignService.handleLevelChange(campaign, 4, 5);

    // Should lose the Level 1 card and gain a Level 3 pick
    assertThat(state.getActiveCards()).isEmpty();
    assertThat(state.getPendingL3Picks()).isEqualTo(1);
    verify(campaignStateRepository).save(state);
  }

  @Test
  void testHandleLevelChange_Heel() {
    Wrestler wrestler = new Wrestler();
    Campaign campaign = new Campaign();
    campaign.setWrestler(wrestler);
    CampaignState state = new CampaignState();
    state.setActiveCards(new ArrayList<>());
    campaign.setState(state);

    WrestlerAlignment alignment = new WrestlerAlignment();
    alignment.setAlignmentType(AlignmentType.HEEL);
    alignment.setLevel(4);

    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    // Add a Level 1 card to be lost
    CampaignAbilityCard l1Card = CampaignAbilityCard.builder().id(1L).level(1).build();
    state.getActiveCards().add(l1Card);

    // Change level from 3 to 4
    campaignService.handleLevelChange(campaign, 3, 4);

    // Should lose the Level 1 card and gain a Level 2 pick
    assertThat(state.getActiveCards()).isEmpty();
    assertThat(state.getPendingL2Picks()).isEqualTo(1);
    verify(campaignStateRepository).save(state);
  }

  @Test
  void testGetPickableCards_Heel_Level5() {
    Wrestler wrestler = new Wrestler();
    Campaign campaign = new Campaign();
    campaign.setWrestler(wrestler);
    CampaignState state = new CampaignState();
    state.setActiveCards(new ArrayList<>());
    state.setPendingL1Picks(1); // Set pick count
    campaign.setState(state);

    WrestlerAlignment alignment = new WrestlerAlignment();
    alignment.setAlignmentType(AlignmentType.HEEL);
    alignment.setLevel(5);

    when(wrestlerAlignmentRepository.findByWrestler(wrestler)).thenReturn(Optional.of(alignment));

    CampaignAbilityCard l1Card = CampaignAbilityCard.builder().id(1L).name("L1").level(1).build();
    lenient()
        .when(campaignAbilityCardRepository.findByAlignmentTypeAndLevel(AlignmentType.HEEL, 1))
        .thenReturn(List.of(l1Card));

    List<CampaignAbilityCard> pickable = campaignService.getPickableCards(campaign);

    assertThat(pickable).contains(l1Card);
  }

  @Test
  void testPickAbilityCard() {
    Wrestler wrestler = new Wrestler();
    wrestler.setName("Test Wrestler");
    Campaign campaign = new Campaign();
    campaign.setWrestler(wrestler);
    CampaignState state = new CampaignState();
    state.setActiveCards(new ArrayList<>());
    state.setPendingL1Picks(1);
    campaign.setState(state);

    CampaignAbilityCard card =
        CampaignAbilityCard.builder().id(100L).name("Test Card").level(1).build();
    when(campaignAbilityCardRepository.findById(100L)).thenReturn(Optional.of(card));

    campaignService.pickAbilityCard(campaign, 100L);

    assertThat(state.getActiveCards()).contains(card);
    assertThat(state.getPendingL1Picks()).isZero();
    verify(campaignStateRepository).save(state);
  }

  @Test
  void testProcessMatchResult_Chapter1() {
    Campaign campaign = new Campaign();
    CampaignState state = new CampaignState();
    state.setCurrentChapter(1);
    state.setVictoryPoints(0);
    campaign.setState(state);

    campaignService.processMatchResult(campaign, true);

    assertThat(state.getVictoryPoints()).isEqualTo(2);
    verify(campaignStateRepository).save(state);

    campaignService.processMatchResult(campaign, false); // Loss

    assertThat(state.getVictoryPoints()).isEqualTo(1); // 2 - 1 = 1
  }

  @Test
  void testProcessMatchResult_Chapter3() {
    Campaign campaign = new Campaign();
    CampaignState state = new CampaignState();
    state.setCurrentChapter(3);
    state.setVictoryPoints(10);
    campaign.setState(state);

    campaignService.processMatchResult(campaign, true);

    assertThat(state.getVictoryPoints()).isEqualTo(14); // 10 + 4
    verify(campaignStateRepository).save(state);

    campaignService.processMatchResult(campaign, false); // Loss

    assertThat(state.getVictoryPoints()).isEqualTo(12); // 14 - 2
  }

  @Test
  void testAdvanceChapter() {
    Campaign campaign = new Campaign();
    CampaignState state = new CampaignState();
    state.setCurrentChapter(1);
    campaign.setState(state);

    campaignService.advanceChapter(campaign);

    assertThat(state.getCurrentChapter()).isEqualTo(2);
    verify(campaignStateRepository).save(state);
  }

  @Test
  void testCompleteCampaign() {
    Campaign campaign = new Campaign();
    CampaignState state = new CampaignState();
    state.setCurrentChapter(3);
    campaign.setState(state);

    campaignService.advanceChapter(campaign);

    assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.COMPLETED);
    assertThat(campaign.getEndedAt()).isNotNull();
    verify(campaignRepository).save(campaign);
  }
}
