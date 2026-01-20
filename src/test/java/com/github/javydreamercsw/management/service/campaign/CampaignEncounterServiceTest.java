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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignEncounter;
import com.github.javydreamercsw.management.domain.campaign.CampaignEncounterRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.dto.campaign.CampaignEncounterResponseDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampaignEncounterServiceTest {

  @Mock private SegmentNarrationServiceFactory aiFactory;
  @Mock private CampaignEncounterRepository encounterRepository;
  @Mock private CampaignStateRepository stateRepository;
  @Mock private CampaignChapterService chapterService;
  @Mock private CampaignService campaignService;
  @Mock private WrestlerRepository wrestlerRepository;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks private CampaignEncounterService encounterService;

  private Campaign campaign;
  private CampaignChapterDTO chapter;

  @BeforeEach
  void setUp() {
    encounterService =
        new CampaignEncounterService(
            aiFactory,
            encounterRepository,
            stateRepository,
            chapterService,
            campaignService,
            wrestlerRepository,
            objectMapper);

    Wrestler wrestler = new Wrestler();
    wrestler.setName("Test Wrestler");
    WrestlerAlignment alignment = new WrestlerAlignment();
    alignment.setAlignmentType(AlignmentType.NEUTRAL);
    alignment.setLevel(0);
    wrestler.setAlignment(alignment);

    CampaignState state = new CampaignState();
    state.setCurrentChapter(1);

    campaign = new Campaign();
    campaign.setWrestler(wrestler);
    campaign.setState(state);

    chapter =
        CampaignChapterDTO.builder()
            .chapterNumber(1)
            .title("Chapter 1")
            .aiSystemPrompt("Test Prompt")
            .build();
  }

  @Test
  void testGenerateEncounter() throws Exception {
    when(chapterService.getChapter(1)).thenReturn(Optional.of(chapter));
    when(encounterRepository.findByCampaignOrderByEncounterDateAsc(campaign))
        .thenReturn(new ArrayList<>());

    String aiJsonResponse =
        "{\"narrative\": \"Story\", \"choices\": [{\"text\": \"Choice 1\", \"label\": \"BTN\","
            + " \"alignmentShift\": 1, \"vpReward\": 0, \"nextPhase\": \"MATCH\", \"matchType\":"
            + " \"One on One\"}]}";
    when(aiFactory.generateText(anyString())).thenReturn(aiJsonResponse);

    CampaignEncounterResponseDTO response = encounterService.generateEncounter(campaign);

    assertThat(response.getNarrative()).isEqualTo("Story");
    assertThat(response.getChoices()).hasSize(1);
    assertThat(response.getChoices().get(0).getLabel()).isEqualTo("BTN");

    verify(encounterRepository).save(org.mockito.ArgumentMatchers.any(CampaignEncounter.class));
  }

  @Test
  void testRecordEncounterChoice() {
    CampaignEncounter encounter = new CampaignEncounter();
    encounter.setNarrativeText("Story");
    when(encounterRepository.findByCampaignOrderByEncounterDateAsc(campaign))
        .thenReturn(List.of(encounter));

    CampaignEncounterResponseDTO.Choice choice = new CampaignEncounterResponseDTO.Choice();
    choice.setText("Choice 1");
    choice.setAlignmentShift(-1);
    choice.setVpReward(5);

    encounterService.recordEncounterChoice(campaign, choice);

    assertThat(encounter.getPlayerChoice()).isEqualTo("Choice 1");
    assertThat(encounter.getAlignmentShift()).isEqualTo(-1);
    assertThat(encounter.getVpReward()).isEqualTo(5);

    verify(campaignService).shiftAlignment(campaign, -1);
    verify(stateRepository).save(campaign.getState());
  }
}
