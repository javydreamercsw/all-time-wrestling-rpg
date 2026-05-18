/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.controller.ranking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.controller.AbstractRestControllerIT;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.ranking.RankingService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for RankingController REST endpoints. */
@DisplayName("RankingController Integration Tests")
@Transactional
class RankingControllerIT extends AbstractRestControllerIT {

  @Autowired private RankingService rankingService;

  private Title savedTitle;

  @BeforeEach
  public void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new RankingController(rankingService)).build();
  }

  private Title createTitle(final String name) {
    Title title = new Title();
    title.setName(name);
    title.setTier(WrestlerTier.MIDCARDER);
    title.setChampionshipType(ChampionshipType.SINGLE);
    title.setIncludeInRankings(true);
    title.setUniverse(defaultUniverse);
    return titleRepository.saveAndFlush(title);
  }

  @Test
  @DisplayName("GET /api/rankings/championships returns 200 and empty array when no titles exist")
  void shouldReturnEmptyListWhenNoChampionshipsExist() throws Exception {
    titleRepository.deleteAll();

    mockMvc
        .perform(get("/api/rankings/championships"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @DisplayName("GET /api/rankings/championships returns 200 and list when titles exist")
  void shouldReturnChampionshipsWhenTitlesExist() throws Exception {
    titleRepository.deleteAll();
    createTitle("World Heavyweight Championship");
    createTitle("Intercontinental Championship");

    mockMvc
        .perform(get("/api/rankings/championships"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].name").isString())
        .andExpect(jsonPath("$[0].id").isNumber());
  }

  @Test
  @DisplayName("GET /api/rankings/championships returns championship fields")
  void shouldReturnCorrectChampionshipFields() throws Exception {
    titleRepository.deleteAll();
    createTitle("Singles Title IT");

    mockMvc
        .perform(get("/api/rankings/championships"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Singles Title IT"))
        .andExpect(jsonPath("$[0].tier").isString());
  }

  @Test
  @DisplayName("GET /api/rankings/championships/{id}/contenders returns 200 with contenders list")
  void shouldReturnContendersForChampionship() throws Exception {
    titleRepository.deleteAll();
    savedTitle = createTitle("Contenders Title IT");

    Wrestler wrestler = createTestWrestler("Test Contender", 50_000L);

    mockMvc
        .perform(get("/api/rankings/championships/{id}/contenders", savedTitle.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  @DisplayName("GET /api/rankings/championships/{id}/contenders returns empty list for unknown id")
  void shouldReturnEmptyContendersForUnknownChampionship() throws Exception {
    mockMvc
        .perform(get("/api/rankings/championships/{id}/contenders", 999_999L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @DisplayName("GET /api/rankings/championships/{id}/champion returns 404 when no current champion")
  void shouldReturn404WhenNoCurrentChampion() throws Exception {
    titleRepository.deleteAll();
    savedTitle = createTitle("Vacant Title IT");

    mockMvc
        .perform(get("/api/rankings/championships/{id}/champion", savedTitle.getId()))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET /api/rankings/championships/{id}/champion returns 200 with champion data")
  void shouldReturnCurrentChampionWhenTitleIsHeld() throws Exception {
    titleRepository.deleteAll();
    savedTitle = createTitle("Held Title IT");

    Wrestler champion = createTestWrestler("The Champion", 100_000L);
    savedTitle.awardTitleTo(List.of(champion), Instant.now());
    titleRepository.saveAndFlush(savedTitle);

    mockMvc
        .perform(get("/api/rankings/championships/{id}/champion", savedTitle.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("The Champion"));
  }

  @Test
  @DisplayName("GET /api/rankings/championships/{id}/champion returns 404 for unknown championship")
  void shouldReturn404ForUnknownChampionshipWhenCheckingChampion() throws Exception {
    mockMvc
        .perform(get("/api/rankings/championships/{id}/champion", 999_999L))
        .andExpect(status().isNotFound());
  }
}
