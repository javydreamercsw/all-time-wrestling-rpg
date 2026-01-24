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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChapterTriggerLogicTest {

  private CampaignChapterService chapterService;

  @BeforeEach
  void setUp() {
    chapterService = new CampaignChapterService(new ObjectMapper());
    chapterService.init();
  }

  @Test
  void testFactionTrigger() {
    // 1. Setup Mock State
    Wrestler wrestler = mock(Wrestler.class);
    Campaign campaign = mock(Campaign.class);
    when(campaign.getWrestler()).thenReturn(wrestler);

    CampaignState state = new CampaignState();
    state.setCampaign(campaign);
    state.setVictoryPoints(0);
    state.setMatchesPlayed(0);
    state.setCompletedChapterIds(new ArrayList<>());

    // 2. Initial check - should NOT find Gang Warfare
    List<CampaignChapterDTO> available = chapterService.findAvailableChapters(state);
    assertThat(available).extracting(CampaignChapterDTO::getId).doesNotContain("gang_warfare");

    // 3. Mock Faction Membership
    Faction faction = new Faction();
    when(wrestler.getFaction()).thenReturn(faction);

    // 4. Check again - should FIND Gang Warfare
    available = chapterService.findAvailableChapters(state);
    assertThat(available).extracting(CampaignChapterDTO::getId).contains("gang_warfare");
  }
}
