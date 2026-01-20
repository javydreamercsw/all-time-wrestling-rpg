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
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CampaignChapterServiceTest {

  private CampaignChapterService chapterService;

  @BeforeEach
  void setUp() {
    chapterService = new CampaignChapterService(new ObjectMapper());
    chapterService.init(); // Loads from campaign_chapters.json
  }

  @Test
  void testGetAllChapters() {
    List<CampaignChapterDTO> chapters = chapterService.getAllChapters();
    assertThat(chapters).isNotEmpty();
    assertThat(chapters.size()).isGreaterThanOrEqualTo(2);
  }

  @Test
  void testGetSpecificChapter() {
    Optional<CampaignChapterDTO> ch1 = chapterService.getChapter("ch1_beginning");
    assertThat(ch1).isPresent();
    assertThat(ch1.get().getTitle()).isEqualTo("The Beginning");

    Optional<CampaignChapterDTO> ch2 = chapterService.getChapter("ch2_tournament");
    assertThat(ch2).isPresent();
    assertThat(ch2.get().getTitle()).isEqualTo("The Tournament");
  }

  @Test
  void testChapterRules() {
    CampaignChapterDTO ch2 = chapterService.getChapter("ch2_tournament").get();
    assertThat(ch2.getRules().getQualifyingMatches()).isEqualTo(4);
    assertThat(ch2.getRules().getMinWinsToQualify()).isEqualTo(3);
  }

  @Test
  void testFindAvailableChapters() {
    com.github.javydreamercsw.management.domain.campaign.CampaignState state =
        new com.github.javydreamercsw.management.domain.campaign.CampaignState();

    // Initially ch1 should be available (no criteria)
    List<CampaignChapterDTO> available = chapterService.findAvailableChapters(state);
    assertThat(available).extracting(CampaignChapterDTO::getId).contains("ch1_beginning");

    // After completing ch1, ch2 should be available
    state.getCompletedChapterIds().add("ch1_beginning");
    available = chapterService.findAvailableChapters(state);
    assertThat(available).extracting(CampaignChapterDTO::getId).contains("ch2_tournament");
  }

  @Test
  void testIsChapterComplete() {
    com.github.javydreamercsw.management.domain.campaign.CampaignState state =
        new com.github.javydreamercsw.management.domain.campaign.CampaignState();
    state.setCurrentChapterId("ch1_beginning");
    state.setMatchesPlayed(0);
    state.setVictoryPoints(0);

    // Not complete yet
    assertThat(chapterService.isChapterComplete(state)).isFalse();

    // Meet criteria (1 match, 5 VP)
    state.setMatchesPlayed(1);
    state.setVictoryPoints(5);
    assertThat(chapterService.isChapterComplete(state)).isTrue();
  }
}
