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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.CampaignStoryline;
import com.github.javydreamercsw.management.domain.campaign.StorylineMilestone;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StorylineExportServiceTest {

  private StorylineExportService exportService;
  private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    objectMapper = new ObjectMapper();
    exportService = new StorylineExportService(objectMapper);
  }

  @Test
  void testToChapterDTO() {
    CampaignStoryline storyline =
        CampaignStoryline.builder()
            .title("The Forbidden Gate")
            .description("A mysterious portal has opened.")
            .build();

    CampaignChapterDTO result = exportService.toChapterDTO(storyline);

    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo("The Forbidden Gate");
    assertThat(result.getId()).isEqualTo("the_forbidden_gate");
    assertThat(result.getShortDescription()).isEqualTo("A mysterious portal has opened.");
    assertThat(result.getAiSystemPrompt()).contains("The Forbidden Gate");
  }

  @Test
  void tagTeamFlagSetFromTitle() {
    CampaignStoryline storyline =
        CampaignStoryline.builder()
            .title("Tag Team Glory: Road to the Championships")
            .description("A journey to become the greatest tag team.")
            .build();

    CampaignChapterDTO result = exportService.toChapterDTO(storyline);

    assertThat(result.isTagTeam()).isTrue();
    assertThat(result.isTournament()).isFalse();
  }

  @Test
  void tournamentFlagSetFromDescription() {
    CampaignStoryline storyline =
        CampaignStoryline.builder()
            .title("Battle for Supremacy")
            .description("Enter the tournament and prove you are the best.")
            .build();

    CampaignChapterDTO result = exportService.toChapterDTO(storyline);

    assertThat(result.isTournament()).isTrue();
    assertThat(result.isTagTeam()).isFalse();
  }

  @Test
  void tagTeamFlagSetFromMilestoneNarrativeGoal() {
    StorylineMilestone milestone =
        StorylineMilestone.builder()
            .title("Find a Partner")
            .narrativeGoal("Recruit a tag team partner to challenge for the titles.")
            .order(0)
            .status(StorylineMilestone.MilestoneStatus.PENDING)
            .build();

    CampaignStoryline storyline =
        CampaignStoryline.builder()
            .title("Unlikely Alliance")
            .description("Two rivals must work together.")
            .milestones(List.of(milestone))
            .build();

    CampaignChapterDTO result = exportService.toChapterDTO(storyline);

    assertThat(result.isTagTeam()).isTrue();
  }

  @Test
  void noFlagsForStandardSinglesStoryline() {
    CampaignStoryline storyline =
        CampaignStoryline.builder()
            .title("Rise to Power")
            .description("Climb the ladder and become champion.")
            .build();

    CampaignChapterDTO result = exportService.toChapterDTO(storyline);

    assertThat(result.isTagTeam()).isFalse();
    assertThat(result.isTournament()).isFalse();
  }

  @Test
  void testExportStorylineAsChapter() throws Exception {
    CampaignStoryline storyline =
        CampaignStoryline.builder().title("Chronos War").description("Battle across time.").build();

    String json = exportService.exportStorylineAsChapter(storyline);

    assertThat(json).contains("\"title\" : \"Chronos War\"");
    assertThat(json).contains("\"id\" : \"chronos_war\"");

    // Verify valid JSON
    CampaignChapterDTO deserialized = objectMapper.readValue(json, CampaignChapterDTO.class);
    assertThat(deserialized.getTitle()).isEqualTo("Chronos War");
  }
}
