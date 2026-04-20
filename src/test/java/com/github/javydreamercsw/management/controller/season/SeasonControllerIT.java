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
package com.github.javydreamercsw.management.controller.season;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.github.javydreamercsw.management.config.CacheConfig;
import com.github.javydreamercsw.management.controller.AbstractRestControllerIT;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.service.season.SeasonService;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for SeasonController. Tests the complete REST API functionality for season
 * management.
 */
@DisplayName("SeasonController Integration Tests")
@Transactional
class SeasonControllerIT extends AbstractRestControllerIT {

  @Autowired private SeasonService seasonService;
  @Autowired private SeasonRepository seasonRepository;
  @Autowired private CacheManager cacheManager;

  @BeforeEach
  public void setUp() {
    // Manually build MockMvc to bypass Vaadin servlet issues
    mockMvc = MockMvcBuilders.standaloneSetup(new SeasonController(seasonService)).build();

    seasonRepository.deleteAll();
    Objects.requireNonNull(cacheManager.getCache(CacheConfig.SEASONS_CACHE)).clear();

    // Ensure active universe is set for this test
    com.github.javydreamercsw.management.domain.universe.Universe universe =
        universeRepository.findAll().stream()
            .findFirst()
            .orElseGet(
                () ->
                    universeRepository.saveAndFlush(
                        com.github.javydreamercsw.management.domain.universe.Universe.builder()
                            .name("Default Universe")
                            .type(
                                com.github.javydreamercsw.management.domain.universe.Universe
                                    .UniverseType.GLOBAL)
                            .build()));
  }

  @Test
  @DisplayName("Should create new season successfully")
  void shouldCreateNewSeasonSuccessfully() throws Exception {
    SeasonController.CreateSeasonRequest request =
        new SeasonController.CreateSeasonRequest("Season 2024", "Main 2024 season", 5);

    mockMvc
        .perform(
            post("/api/seasons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Season 2024"))
        .andExpect(jsonPath("$.description").value("Main 2024 season"))
        .andExpect(jsonPath("$.showsPerPpv").value(5))
        .andExpect(jsonPath("$.isActive").value(true));
  }

  @Test
  @DisplayName("Should return 400 when name is missing")
  void shouldReturn400WhenNameIsMissing() throws Exception {
    SeasonController.CreateSeasonRequest request =
        new SeasonController.CreateSeasonRequest(null, "Description", 5);

    mockMvc
        .perform(
            post("/api/seasons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should return 400 when showsPerPpv is invalid")
  void shouldReturn400WhenShowsPerPpvIsInvalid() throws Exception {
    SeasonController.CreateSeasonRequest request =
        new SeasonController.CreateSeasonRequest("Season", "Description", 0);

    mockMvc
        .perform(
            post("/api/seasons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should get all seasons")
  void shouldGetAllSeasons() throws Exception {
    saveSeason("Season 1", true);
    saveSeason("Season 2", false);

    mockMvc
        .perform(get("/api/seasons"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(2));
  }

  @Test
  @DisplayName("Should get active seasons")
  void shouldGetActiveSeasons() throws Exception {
    saveSeason("Active Season", true);

    mockMvc
        .perform(get("/api/seasons/active"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Active Season"));
  }

  @Test
  @DisplayName("Should deactivate current active seasons when a new one is created")
  void shouldDeactivateCurrentActiveSeasonsWhenNewOneIsCreated() throws Exception {
    Season oldActive = saveSeason("Old Active", true);

    SeasonController.CreateSeasonRequest request =
        new SeasonController.CreateSeasonRequest("New Active", "New season description", 5);

    mockMvc
        .perform(
            post("/api/seasons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    assertThat(seasonRepository.findById(oldActive.getId()).get().getIsActive()).isFalse();
    assertThat(seasonRepository.findActiveSeason()).isPresent();
    assertThat(seasonRepository.findActiveSeason().get().getName()).isEqualTo("New Active");
  }

  private Season saveSeason(String name, boolean active) {
    Season season = new Season();
    season.setName(name);
    season.setDescription("Test description");
    season.setShowsPerPpv(5);
    season.setIsActive(active);
    return seasonRepository.save(season);
  }
}
