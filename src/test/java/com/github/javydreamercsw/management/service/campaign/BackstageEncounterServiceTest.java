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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignEncounter;
import com.github.javydreamercsw.management.domain.campaign.CampaignEncounterRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.CampaignEncounterResponseDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BackstageEncounterServiceTest {

  @Mock private SegmentNarrationServiceFactory aiFactory;
  @Mock private CampaignEncounterRepository encounterRepository;
  @Mock private CampaignStateRepository stateRepository;
  @Mock private CampaignService campaignService;
  @Mock private WrestlerRepository wrestlerRepository;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks private BackstageEncounterService backstageEncounterService;

  private Campaign campaign;
  private CampaignState state;

  @BeforeEach
  void setUp() {
    backstageEncounterService =
        new BackstageEncounterService(
            aiFactory,
            encounterRepository,
            stateRepository,
            campaignService,
            wrestlerRepository,
            objectMapper);

    state = new CampaignState();
    state.setActionsTaken(0);
    state.setCurrentGameDate(LocalDate.of(2026, 2, 24));

    Wrestler wrestler = new Wrestler();
    wrestler.setName("Test Wrestler");

    campaign = new Campaign();
    campaign.setState(state);
    campaign.setWrestler(wrestler);
  }

  @Test
  void testShouldTriggerEncounter_FalseWhenActionsTaken() {
    state.setActionsTaken(1);
    assertThat(backstageEncounterService.shouldTriggerEncounter(campaign)).isFalse();
  }

  @Test
  void testShouldTriggerEncounter_FalseWhenAlreadyTriggeredToday() throws Exception {
    state.setActionsTaken(0);
    state.setFeatureData("{\"lastBackstageEncounterDate\":\"2026-02-24\"}");
    assertThat(backstageEncounterService.shouldTriggerEncounter(campaign)).isFalse();
  }

  @Test
  void testShouldTriggerEncounter_TrueWhenNotTriggeredAndRandomSucceeds() {
    state.setActionsTaken(0);
    state.setFeatureData(null);

    Random mockRandom = mock(Random.class);
    when(mockRandom.nextInt(100)).thenReturn(10); // 10 < 20
    ReflectionTestUtils.setField(backstageEncounterService, "random", mockRandom);

    assertThat(backstageEncounterService.shouldTriggerEncounter(campaign)).isTrue();
    verify(stateRepository).save(state);
    assertThat(state.getFeatureData()).contains("2026-02-24");
  }

  @Test
  void testRecordBackstageChoice() {
    CampaignEncounter encounter = new CampaignEncounter();
    when(encounterRepository.findByCampaignOrderByEncounterDateAsc(campaign))
        .thenReturn(List.of(encounter));

    CampaignEncounterResponseDTO.Choice choice =
        CampaignEncounterResponseDTO.Choice.builder()
            .text("Option A")
            .alignmentShift(2)
            .momentumBonus(3)
            .build();

    backstageEncounterService.recordBackstageChoice(campaign, choice);

    assertThat(encounter.getPlayerChoice()).isEqualTo("Option A");
    assertThat(encounter.getAlignmentShift()).isEqualTo(2);
    assertThat(state.getMomentumBonus()).isEqualTo(3);
    assertThat(state.getActionsTaken()).isEqualTo(1);

    verify(campaignService).shiftAlignment(campaign, 2);
    verify(stateRepository, atLeastOnce()).save(state);
  }
}
