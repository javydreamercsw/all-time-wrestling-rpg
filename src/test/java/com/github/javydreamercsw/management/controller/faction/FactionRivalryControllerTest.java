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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRivalry;
import com.github.javydreamercsw.management.service.faction.FactionRivalryService;
import com.github.javydreamercsw.management.service.resolution.ResolutionResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WithMockUser(authorities = {"ADMIN", "ROLE_ADMIN", "ROLE_BOOKER"})
class FactionRivalryControllerTest extends AbstractControllerTest {

  @MockitoBean private FactionRivalryService factionRivalryService;

  private FactionRivalry testRivalry;

  @BeforeEach
  void setUp() {
    Faction faction1 = new Faction();
    faction1.setId(1L);
    faction1.setName("The Shield");

    Faction faction2 = new Faction();
    faction2.setId(2L);
    faction2.setName("The Authority");

    testRivalry = new FactionRivalry();
    testRivalry.setId(1L);
    testRivalry.setFaction1(faction1);
    testRivalry.setFaction2(faction2);
    testRivalry.setHeat(50);
    testRivalry.setIsActive(true);
  }

  @Test
  void getAllFactionRivalries_returnsPage() throws Exception {
    when(factionRivalryService.getAllFactionRivalries(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(testRivalry)));

    mockMvc
        .perform(get("/api/faction-rivalries").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(1));
  }

  @Test
  void getFactionRivalryById_found() throws Exception {
    when(factionRivalryService.getFactionRivalryById(1L)).thenReturn(Optional.of(testRivalry));

    mockMvc
        .perform(get("/api/faction-rivalries/1").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void getFactionRivalryById_notFound() throws Exception {
    when(factionRivalryService.getFactionRivalryById(anyLong())).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/faction-rivalries/99").with(csrf())).andExpect(status().isNotFound());
  }

  @Test
  void getActiveFactionRivalries_returnsList() throws Exception {
    when(factionRivalryService.getActiveFactionRivalries()).thenReturn(List.of(testRivalry));

    mockMvc
        .perform(get("/api/faction-rivalries/active").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1));
  }

  @Test
  void getRivalriesForFaction_returnsList() throws Exception {
    when(factionRivalryService.getActiveRivalriesForFaction(1L)).thenReturn(List.of(testRivalry));

    mockMvc
        .perform(get("/api/faction-rivalries/faction/1").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1));
  }

  @Test
  void getRivalriesRequiringSegments_returnsList() throws Exception {
    when(factionRivalryService.getRivalriesRequiringMatches()).thenReturn(List.of(testRivalry));

    mockMvc
        .perform(get("/api/faction-rivalries/requiring-matches").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1));
  }

  @Test
  void getRivalriesEligibleForResolution_returnsList() throws Exception {
    when(factionRivalryService.getRivalriesEligibleForResolution())
        .thenReturn(List.of(testRivalry));

    mockMvc
        .perform(get("/api/faction-rivalries/eligible-for-resolution").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1));
  }

  @Test
  void getRivalriesRequiringRule_returnsList() throws Exception {
    when(factionRivalryService.getRivalriesRequiringStipulationMatches())
        .thenReturn(List.of(testRivalry));

    mockMvc
        .perform(get("/api/faction-rivalries/requiring-rule").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1));
  }

  @Test
  void getHottestRivalries_returnsList() throws Exception {
    when(factionRivalryService.getHottestRivalries(anyInt())).thenReturn(List.of(testRivalry));

    mockMvc
        .perform(get("/api/faction-rivalries/hottest").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1));
  }

  @Test
  void getTagTeamRivalries_returnsList() throws Exception {
    when(factionRivalryService.getTagTeamRivalries()).thenReturn(List.of(testRivalry));

    mockMvc
        .perform(get("/api/faction-rivalries/tag-team").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1));
  }

  @Test
  void getRivalriesInvolvingStables_returnsList() throws Exception {
    when(factionRivalryService.getRivalriesInvolvingStables()).thenReturn(List.of(testRivalry));

    mockMvc
        .perform(get("/api/faction-rivalries/involving-stables").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1));
  }

  @Test
  void createFactionRivalry_success() throws Exception {
    when(factionRivalryService.createFactionRivalry(anyLong(), anyLong(), any()))
        .thenReturn(Optional.of(testRivalry));

    FactionRivalryController.CreateFactionRivalryRequest request =
        new FactionRivalryController.CreateFactionRivalryRequest(1L, 2L, "Epic feud");

    mockMvc
        .perform(
            post("/api/faction-rivalries")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void createFactionRivalry_failure() throws Exception {
    when(factionRivalryService.createFactionRivalry(anyLong(), anyLong(), any()))
        .thenReturn(Optional.empty());

    FactionRivalryController.CreateFactionRivalryRequest request =
        new FactionRivalryController.CreateFactionRivalryRequest(99L, 100L, null);

    mockMvc
        .perform(
            post("/api/faction-rivalries")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void addHeat_success() throws Exception {
    when(factionRivalryService.addHeat(anyLong(), anyInt(), anyString()))
        .thenReturn(Optional.of(testRivalry));

    FactionRivalryController.AddHeatRequest request =
        new FactionRivalryController.AddHeatRequest(10, "Post-match brawl");

    mockMvc
        .perform(
            post("/api/faction-rivalries/1/heat")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void addHeat_failure() throws Exception {
    when(factionRivalryService.addHeat(anyLong(), anyInt(), anyString()))
        .thenReturn(Optional.empty());

    FactionRivalryController.AddHeatRequest request =
        new FactionRivalryController.AddHeatRequest(10, "Reason");

    mockMvc
        .perform(
            post("/api/faction-rivalries/99/heat")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void addHeatBetweenFactions_success() throws Exception {
    when(factionRivalryService.addHeatBetweenFactions(anyLong(), anyLong(), anyInt(), anyString()))
        .thenReturn(Optional.of(testRivalry));

    FactionRivalryController.AddHeatBetweenFactionsRequest request =
        new FactionRivalryController.AddHeatBetweenFactionsRequest(1L, 2L, 15, "Segment");

    mockMvc
        .perform(
            post("/api/faction-rivalries/heat")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void addHeatBetweenFactions_failure() throws Exception {
    when(factionRivalryService.addHeatBetweenFactions(anyLong(), anyLong(), anyInt(), anyString()))
        .thenReturn(Optional.empty());

    FactionRivalryController.AddHeatBetweenFactionsRequest request =
        new FactionRivalryController.AddHeatBetweenFactionsRequest(99L, 100L, 5, "Reason");

    mockMvc
        .perform(
            post("/api/faction-rivalries/heat")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void attemptResolution_returnsResult() throws Exception {
    ResolutionResult<FactionRivalry> result =
        new ResolutionResult<>(true, "Rivalry resolved", testRivalry, 5, 3, 8);

    when(factionRivalryService.attemptResolution(anyLong(), any(), any())).thenReturn(result);

    FactionRivalryController.AttemptResolutionRequest request =
        new FactionRivalryController.AttemptResolutionRequest(5, 3);

    mockMvc
        .perform(
            post("/api/faction-rivalries/1/resolve")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resolved").value(true));
  }

  @Test
  void endFactionRivalry_success() throws Exception {
    when(factionRivalryService.endFactionRivalry(anyLong(), anyString()))
        .thenReturn(Optional.of(testRivalry));

    mockMvc
        .perform(delete("/api/faction-rivalries/1").with(csrf()).param("reason", "Story concluded"))
        .andExpect(status().isOk());
  }

  @Test
  void endFactionRivalry_failure() throws Exception {
    when(factionRivalryService.endFactionRivalry(anyLong(), anyString()))
        .thenReturn(Optional.empty());

    mockMvc
        .perform(delete("/api/faction-rivalries/99").with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getRivalryStatistics_returnsStats() throws Exception {
    when(factionRivalryService.getActiveFactionRivalries()).thenReturn(List.of(testRivalry));
    when(factionRivalryService.getTotalWrestlersInRivalries()).thenReturn(8L);
    when(factionRivalryService.getRivalriesRequiringMatches()).thenReturn(List.of(testRivalry));
    when(factionRivalryService.getRivalriesEligibleForResolution()).thenReturn(List.of());
    when(factionRivalryService.getRivalriesRequiringStipulationMatches()).thenReturn(List.of());

    mockMvc
        .perform(get("/api/faction-rivalries/statistics").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalActiveRivalries").value(1))
        .andExpect(jsonPath("$.totalWrestlersInvolved").value(8));
  }
}
