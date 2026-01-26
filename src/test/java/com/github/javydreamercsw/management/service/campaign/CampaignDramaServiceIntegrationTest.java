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
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.drama.DramaEvent;
import com.github.javydreamercsw.management.domain.drama.DramaEventType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@WithMockUser(roles = "BOOKER")
class CampaignDramaServiceIntegrationTest extends AbstractIntegrationTest {

  @Autowired private CampaignDramaService campaignDramaService;
  @MockitoBean private Random random;

  @Test
  @Transactional
  void testTriggerRivalEvent() {
    Wrestler player = createTestWrestler("Player One");
    player = wrestlerRepository.save(player);

    Wrestler rival = createTestWrestler("Rival One");
    rival = wrestlerRepository.save(rival);

    // Mock random to just return 0 (first available opponent)
    when(random.nextInt(org.mockito.ArgumentMatchers.anyInt())).thenReturn(0);
    when(random.nextDouble()).thenReturn(0.5);

    Campaign campaign = Campaign.builder().wrestler(player).build();

    Optional<DramaEvent> event = campaignDramaService.triggerRivalEvent(campaign);

    assertThat(event).isPresent();
    assertThat(event.get().getEventType()).isEqualTo(DramaEventType.CAMPAIGN_RIVAL);
    assertThat(event.get().getPrimaryWrestler()).isEqualTo(player);
    assertThat(event.get().getSecondaryWrestler()).isNotNull();
    assertThat(event.get().getSecondaryWrestler()).isNotEqualTo(player);
  }

  @Test
  @Transactional
  void testCheckForStoryEvents_Chapter2() {
    Wrestler player = createTestWrestler("Chapter Two Player");
    player = wrestlerRepository.save(player);

    Wrestler rival = createTestWrestler("Rival Two");
    wrestlerRepository.save(rival);

    Campaign campaign = Campaign.builder().wrestler(player).build();
    CampaignState state =
        CampaignState.builder().campaign(campaign).currentChapterId("tournament").build();
    campaign.setState(state);

    when(random.nextInt(org.mockito.ArgumentMatchers.anyInt())).thenReturn(0);

    Optional<DramaEvent> event = campaignDramaService.checkForStoryEvents(campaign);

    assertThat(event).isPresent();
    assertThat(event.get().getEventType()).isEqualTo(DramaEventType.CAMPAIGN_RIVAL);
  }
}
