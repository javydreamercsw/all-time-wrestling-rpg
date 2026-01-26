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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdvancedCampaignChaptersTest {

  private CampaignChapterService chapterService;

  @BeforeEach
  void setUp() {
    chapterService = new CampaignChapterService(new ObjectMapper());
    chapterService.init(); // Loads from campaign_chapters.json
  }

  @Test
  void testFightingChampionChapterExists() {
    Optional<CampaignChapterDTO> chapter = chapterService.getChapter("fighting_champion");
    assertThat(chapter).isPresent();
    assertThat(chapter.get().getTitle()).isEqualTo("The Fighting Champion");
    // Validate triggers
    assertThat(chapter.get().getEntryPoints())
        .anyMatch(ep -> ep.getName().equals("Champion Status"));
  }

  @Test
  void testGangWarfareChapterExists() {
    Optional<CampaignChapterDTO> chapter = chapterService.getChapter("gang_warfare");
    assertThat(chapter).isPresent();
    assertThat(chapter.get().getTitle()).isEqualTo("Gang Warfare");
    assertThat(chapter.get().getEntryPoints()).anyMatch(ep -> ep.getName().equals("Faction Heat"));
  }

  @Test
  void testCorporatePowerTripChapterExists() {
    Optional<CampaignChapterDTO> chapter = chapterService.getChapter("corporate_power_trip");
    assertThat(chapter).isPresent();
    assertThat(chapter.get().getTitle()).isEqualTo("Corporate Power Trip");
    assertThat(chapter.get().getEntryPoints())
        .anyMatch(ep -> ep.getName().equals("Authority Heat"));
  }
}
