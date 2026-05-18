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
package com.github.javydreamercsw.management.controller.faction;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractRestControllerIT;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.faction.FactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for FactionController. Tests the complete REST API for faction management. */
@DisplayName("FactionController Integration Tests")
@Transactional
class FactionControllerIT extends AbstractRestControllerIT {

  @Autowired private FactionService factionService;

  @BeforeEach
  public void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new FactionController(factionService))
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
  }

  private Faction createTestFaction(final String name, final Long leaderId) {
    return factionService
        .createFaction(name, "Test faction", leaderId, defaultUniverse.getId())
        .orElseThrow();
  }

  @Test
  @DisplayName("Should return empty page when no factions exist")
  void shouldReturnEmptyPageWhenNoFactionsExist() throws Exception {
    mockMvc
        .perform(get("/api/factions").param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  @DisplayName("Should return all factions with pagination")
  void shouldReturnAllFactionsWithPagination() throws Exception {
    Wrestler leader1 = createTestWrestler("Leader One", 10_000L);
    Wrestler leader2 = createTestWrestler("Leader Two", 10_000L);
    createTestFaction("Faction Alpha", leader1.getId());
    createTestFaction("Faction Beta", leader2.getId());

    mockMvc
        .perform(get("/api/factions").param("page", "0").param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  @DisplayName("Should return faction by ID when found")
  void shouldReturnFactionByIdWhenFound() throws Exception {
    Wrestler leader = createTestWrestler("The Leader", 5_000L);
    Faction faction = createTestFaction("The Stable", leader.getId());

    mockMvc
        .perform(get("/api/factions/{id}", faction.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("The Stable"));
  }

  @Test
  @DisplayName("Should return 404 when faction ID not found")
  void shouldReturn404WhenFactionIdNotFound() throws Exception {
    mockMvc.perform(get("/api/factions/{id}", 999_999L)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return faction by name when found")
  void shouldReturnFactionByNameWhenFound() throws Exception {
    Wrestler leader = createTestWrestler("Named Leader", 5_000L);
    createTestFaction("Unique Faction Name", leader.getId());

    mockMvc
        .perform(get("/api/factions/name/{name}", "Unique Faction Name"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Unique Faction Name"));
  }

  @Test
  @DisplayName("Should return 404 when faction name not found")
  void shouldReturn404WhenFactionNameNotFound() throws Exception {
    mockMvc
        .perform(get("/api/factions/name/{name}", "NonExistentFaction"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return active factions list")
  void shouldReturnActiveFactionsList() throws Exception {
    mockMvc
        .perform(get("/api/factions/active"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  @DisplayName("Should return factions by type")
  void shouldReturnFactionsByType() throws Exception {
    mockMvc
        .perform(get("/api/factions/type/{type}", "singles"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  @DisplayName("Should return largest factions")
  void shouldReturnLargestFactions() throws Exception {
    mockMvc
        .perform(get("/api/factions/largest").param("limit", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  @DisplayName("Should return factions with rivalries")
  void shouldReturnFactionsWithRivalries() throws Exception {
    mockMvc
        .perform(get("/api/factions/with-rivalries"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  @DisplayName("Should return faction for wrestler when found")
  void shouldReturnFactionForWrestlerWhenFound() throws Exception {
    Wrestler leader = createTestWrestler("Faction Leader", 20_000L);
    createTestFaction("Leader Faction", leader.getId());

    mockMvc
        .perform(get("/api/factions/wrestler/{wrestlerId}", leader.getId()))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Should return 404 for wrestler not in any faction")
  void shouldReturn404ForWrestlerNotInAnyFaction() throws Exception {
    Wrestler loner = createTestWrestler("Lone Wolf", 1_000L);

    mockMvc
        .perform(get("/api/factions/wrestler/{wrestlerId}", loner.getId()))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should create faction successfully")
  void shouldCreateFactionSuccessfully() throws Exception {
    Wrestler leader = createTestWrestler("New Leader", 15_000L);
    FactionController.CreateFactionRequest request =
        new FactionController.CreateFactionRequest(
            "New Faction", "A brand new faction", leader.getId(), defaultUniverse.getId());

    mockMvc
        .perform(
            post("/api/factions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("New Faction"));
  }

  @Test
  @DisplayName("Should return 400 when faction name already exists")
  void shouldReturn400WhenFactionNameAlreadyExists() throws Exception {
    Wrestler leader1 = createTestWrestler("First Leader", 10_000L);
    Wrestler leader2 = createTestWrestler("Second Leader", 10_000L);
    createTestFaction("Duplicate Name", leader1.getId());

    FactionController.CreateFactionRequest request =
        new FactionController.CreateFactionRequest(
            "Duplicate Name", "Another faction", leader2.getId(), defaultUniverse.getId());

    mockMvc
        .perform(
            post("/api/factions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should disband faction successfully when found")
  void shouldDisbandFactionSuccessfullyWhenFound() throws Exception {
    Wrestler leader = createTestWrestler("Disbandable Leader", 5_000L);
    Faction faction = createTestFaction("Doomed Faction", leader.getId());

    mockMvc
        .perform(delete("/api/factions/{id}", faction.getId()).param("reason", "Story complete"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Should return 400 when disbanding non-existent faction")
  void shouldReturn400WhenDisbandingNonExistentFaction() throws Exception {
    mockMvc
        .perform(delete("/api/factions/{id}", 999_999L).param("reason", "Gone"))
        .andExpect(status().isBadRequest());
  }
}
