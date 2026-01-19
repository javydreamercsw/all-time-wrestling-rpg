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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

  @Mock private CampaignRepository campaignRepository;
  @Mock private CampaignStateRepository campaignStateRepository;
  @InjectMocks private CampaignService campaignService;

  @Test
  void testStartCampaign() {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);

    when(campaignRepository.save(any(Campaign.class))).thenAnswer(i -> i.getArguments()[0]);

    Campaign campaign = campaignService.startCampaign(wrestler);

    assertThat(campaign).isNotNull();
    assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ACTIVE);
    assertThat(campaign.getState()).isNotNull();
    assertThat(campaign.getState().getCurrentChapter()).isEqualTo(1);

    verify(campaignRepository).save(any(Campaign.class)); // Called twice actually
    verify(campaignStateRepository).save(any(CampaignState.class));
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
