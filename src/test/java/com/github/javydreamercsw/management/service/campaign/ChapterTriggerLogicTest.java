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
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChapterTriggerLogicTest {

  private CampaignChapterService chapterService;

  @BeforeEach
  public void setUp() {
    ObjectMapper objectMapper = new ObjectMapper();
    FeatureDataService featureDataService =
        new FeatureDataService(objectMapper, mock(CampaignStateRepository.class));
    chapterService =
        new CampaignChapterService(
            objectMapper,
            featureDataService,
            org.mockito.Mockito.mock(
                com.github.javydreamercsw.management.service.expansion.ExpansionService.class));
    chapterService.init();
  }

  @Test
  void testFactionTrigger() {
    // 1. Setup State
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
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

    // 3. Setup Faction Membership via state
    Faction faction = new Faction();
    faction.setId(10L);

    Universe universe = new Universe();
    universe.setId(1L);

    WrestlerState wrestlerStateObj = new WrestlerState();
    wrestlerStateObj.setWrestler(wrestler);
    wrestlerStateObj.setUniverse(universe);
    wrestlerStateObj.setFaction(faction);
    wrestler.getWrestlerStates().add(wrestlerStateObj);

    // 4. Check again - should FIND Gang Warfare
    available = chapterService.findAvailableChapters(state);
    assertThat(available).extracting(CampaignChapterDTO::getId).contains("gang_warfare");
  }

  @Test
  void testChampionTrigger() {
    // 1. Setup State
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    Campaign campaign = mock(Campaign.class);
    when(campaign.getWrestler()).thenReturn(wrestler);

    CampaignState state = new CampaignState();
    state.setCampaign(campaign);
    state.setCompletedChapterIds(new ArrayList<>());

    // 2. Initial check - should NOT find Fighting Champion
    List<CampaignChapterDTO> available = chapterService.findAvailableChapters(state);
    assertThat(available).extracting(CampaignChapterDTO::getId).doesNotContain("fighting_champion");

    // 3. Setup Champion Status
    TitleReign reign = new TitleReign();
    reign.setEndDate(null); // Mark as active
    wrestler.getReigns().add(reign);

    // 4. Check again - should FIND Fighting Champion
    available = chapterService.findAvailableChapters(state);
    assertThat(available).extracting(CampaignChapterDTO::getId).contains("fighting_champion");
  }

  @Test
  void testAuthorityTrigger() {
    // 1. Setup State
    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    Campaign campaign = mock(Campaign.class);
    when(campaign.getWrestler()).thenReturn(wrestler);

    CampaignState state = new CampaignState();
    state.setCampaign(campaign);
    state.setCompletedChapterIds(new ArrayList<>());
    state.setVictoryPoints(10); // Not enough for trigger (15)

    // 2. Initial check - should NOT find Corporate Power Trip
    List<CampaignChapterDTO> available = chapterService.findAvailableChapters(state);
    assertThat(available)
        .extracting(CampaignChapterDTO::getId)
        .doesNotContain("corporate_power_trip");

    // 3. Increase VP
    state.setVictoryPoints(15);

    // 4. Check again - should FIND Corporate Power Trip
    available = chapterService.findAvailableChapters(state);
    assertThat(available).extracting(CampaignChapterDTO::getId).contains("corporate_power_trip");
  }
}
