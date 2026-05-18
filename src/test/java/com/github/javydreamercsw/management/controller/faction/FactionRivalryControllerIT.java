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
import com.github.javydreamercsw.management.domain.faction.FactionRivalry;
import com.github.javydreamercsw.management.service.faction.FactionRivalryService;
import com.github.javydreamercsw.management.service.faction.FactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for FactionRivalryController REST endpoints. */
@DisplayName("FactionRivalryController Integration Tests")
@Transactional
class FactionRivalryControllerIT extends AbstractRestControllerIT {

  @Autowired private FactionRivalryService factionRivalryService;
  @Autowired private FactionService factionService;

  @BeforeEach
  public void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new FactionRivalryController(factionRivalryService))
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
  }

  private static int factionCounter = 0;

  private Faction createFaction(final String name) {
    // Each faction needs a unique wrestler leader (service requires non-null leaderId)
    var leader =
        createTestWrestler("IT Rivalry Leader " + (++factionCounter), 5_000L + factionCounter);
    return factionService
        .createFaction(name, "Description for " + name, leader.getId(), defaultUniverse.getId())
        .orElseThrow(() -> new IllegalStateException("Could not create faction: " + name));
  }

  private FactionRivalry createRivalry(final Faction f1, final Faction f2) {
    return factionRivalryService
        .createFactionRivalry(f1.getId(), f2.getId(), "Test storyline")
        .orElseThrow(() -> new IllegalStateException("Could not create faction rivalry"));
  }

  @Test
  @DisplayName("GET /api/faction-rivalries returns 200 with paginated results")
  void shouldGetAllFactionRivalriesPaginated() throws Exception {
    Faction f1 = createFaction("IT Faction Alpha");
    Faction f2 = createFaction("IT Faction Beta");
    createRivalry(f1, f2);

    mockMvc
        .perform(get("/api/faction-rivalries").param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  @DisplayName("GET /api/faction-rivalries/{id} returns 200 for existing rivalry")
  void shouldGetFactionRivalryById() throws Exception {
    Faction f1 = createFaction("IT Faction Gamma");
    Faction f2 = createFaction("IT Faction Delta");
    FactionRivalry rivalry = createRivalry(f1, f2);

    mockMvc
        .perform(get("/api/faction-rivalries/{id}", rivalry.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(rivalry.getId()))
        .andExpect(jsonPath("$.heat").value(0));
  }

  @Test
  @DisplayName("GET /api/faction-rivalries/{id} returns 404 for non-existent rivalry")
  void shouldReturn404ForNonExistentRivalry() throws Exception {
    mockMvc.perform(get("/api/faction-rivalries/{id}", -999L)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET /api/faction-rivalries/active returns 200 with active rivalries")
  void shouldGetActiveRivalries() throws Exception {
    Faction f1 = createFaction("IT Faction Epsilon");
    Faction f2 = createFaction("IT Faction Zeta");
    createRivalry(f1, f2);

    mockMvc
        .perform(get("/api/faction-rivalries/active"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  @DisplayName("GET /api/faction-rivalries/faction/{id} returns 200 with rivalries for faction")
  void shouldGetRivalriesForFaction() throws Exception {
    Faction f1 = createFaction("IT Faction Eta");
    Faction f2 = createFaction("IT Faction Theta");
    createRivalry(f1, f2);

    mockMvc
        .perform(get("/api/faction-rivalries/faction/{factionId}", f1.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  @DisplayName("GET /api/faction-rivalries/statistics returns 200 with totalActiveRivalries")
  void shouldGetRivalryStatistics() throws Exception {
    Faction f1 = createFaction("IT Faction Iota");
    Faction f2 = createFaction("IT Faction Kappa");
    createRivalry(f1, f2);

    mockMvc
        .perform(get("/api/faction-rivalries/statistics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalActiveRivalries").value(1));
  }

  @Test
  @DisplayName("POST /api/faction-rivalries returns 200 when rivalry created successfully")
  void shouldCreateFactionRivalry() throws Exception {
    Faction f1 = createFaction("IT Faction Lambda");
    Faction f2 = createFaction("IT Faction Mu");

    FactionRivalryController.CreateFactionRivalryRequest request =
        new FactionRivalryController.CreateFactionRivalryRequest(
            f1.getId(), f2.getId(), "Epic feud storyline");

    mockMvc
        .perform(
            post("/api/faction-rivalries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.heat").value(0));
  }

  @Test
  @DisplayName("POST /api/faction-rivalries returns 400 when factions not found")
  void shouldReturn400WhenFactionsNotFound() throws Exception {
    FactionRivalryController.CreateFactionRivalryRequest request =
        new FactionRivalryController.CreateFactionRivalryRequest(-999L, -998L, "Bad request");

    mockMvc
        .perform(
            post("/api/faction-rivalries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /api/faction-rivalries/{id}/heat returns 200 and increases heat")
  void shouldAddHeatToRivalry() throws Exception {
    Faction f1 = createFaction("IT Faction Nu");
    Faction f2 = createFaction("IT Faction Xi");
    FactionRivalry rivalry = createRivalry(f1, f2);

    FactionRivalryController.AddHeatRequest request =
        new FactionRivalryController.AddHeatRequest(5, "Backstage brawl");

    mockMvc
        .perform(
            post("/api/faction-rivalries/{id}/heat", rivalry.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.heat").value(5));
  }

  @Test
  @DisplayName("POST /api/faction-rivalries/heat creates or updates rivalry between two factions")
  void shouldAddHeatBetweenFactions() throws Exception {
    Faction f1 = createFaction("IT Faction Omicron");
    Faction f2 = createFaction("IT Faction Pi");

    FactionRivalryController.AddHeatBetweenFactionsRequest request =
        new FactionRivalryController.AddHeatBetweenFactionsRequest(
            f1.getId(), f2.getId(), 8, "Title confrontation");

    mockMvc
        .perform(
            post("/api/faction-rivalries/heat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.heat").value(8));
  }

  @Test
  @DisplayName("DELETE /api/faction-rivalries/{id} returns 200 and ends rivalry")
  void shouldEndFactionRivalry() throws Exception {
    Faction f1 = createFaction("IT Faction Rho");
    Faction f2 = createFaction("IT Faction Sigma");
    FactionRivalry rivalry = createRivalry(f1, f2);

    mockMvc
        .perform(delete("/api/faction-rivalries/{id}", rivalry.getId()))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /api/faction-rivalries/requiring-matches returns 200")
  void shouldGetRivalriesRequiringMatches() throws Exception {
    mockMvc
        .perform(get("/api/faction-rivalries/requiring-matches"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  @DisplayName("GET /api/faction-rivalries/hottest returns 200 with list")
  void shouldGetHottestRivalries() throws Exception {
    mockMvc
        .perform(get("/api/faction-rivalries/hottest"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }
}
