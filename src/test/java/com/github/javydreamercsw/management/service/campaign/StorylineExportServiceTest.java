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
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StorylineExportServiceTest {

  private StorylineExportService exportService;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
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
