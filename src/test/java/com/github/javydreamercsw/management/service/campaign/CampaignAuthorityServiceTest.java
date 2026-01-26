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
import static org.mockito.Mockito.verify;

import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampaignAuthorityServiceTest {

  @Mock private NpcRepository npcRepository;
  @Mock private CampaignStateRepository stateRepository;
  @InjectMocks private CampaignAuthorityService authorityService;

  @Test
  void testApplyUnfairMatchModifiers() {
    Wrestler player = new Wrestler();
    Campaign campaign = new Campaign();
    campaign.setWrestler(player);
    CampaignState state = new CampaignState();
    state.setHealthPenalty(0);
    state.setOpponentHealthPenalty(0);
    campaign.setState(state);

    authorityService.applyUnfairMatchModifiers(campaign, 2, -2);

    assertThat(state.getHealthPenalty()).isEqualTo(2);
    assertThat(state.getOpponentHealthPenalty()).isEqualTo(-2);
    verify(stateRepository).save(state);
  }
}
