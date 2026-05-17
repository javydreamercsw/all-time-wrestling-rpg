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
package com.github.javydreamercsw.management.controller.rivalry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryIntensity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerDTO;
import com.github.javydreamercsw.management.dto.rivalry.RivalryDTO;
import com.github.javydreamercsw.management.mapper.RivalryMapper;
import com.github.javydreamercsw.management.service.resolution.ResolutionResult;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class RivalryControllerTest extends AbstractControllerTest {

  @MockitoBean private RivalryService rivalryService;
  @MockitoBean private RivalryMapper rivalryMapper;

  private Rivalry rivalry;
  private RivalryDTO rivalryDTO;
  private WrestlerDTO wrestlerDTO1;
  private WrestlerDTO wrestlerDTO2;

  @BeforeEach
  void setUpRivalry() {
    Wrestler wrestler1 = Wrestler.builder().build();
    wrestler1.setId(1L);
    wrestler1.setName("Test Wrestler 1");

    Wrestler wrestler2 = Wrestler.builder().build();
    wrestler2.setId(2L);
    wrestler2.setName("Test Wrestler 2");

    rivalry = new Rivalry();
    rivalry.setId(1L);
    rivalry.setWrestler1(wrestler1);
    rivalry.setWrestler2(wrestler2);
    rivalry.setHeat(0);
    rivalry.setStartedDate(Instant.now());

    wrestlerDTO1 = new WrestlerDTO();
    wrestlerDTO1.setId(1L);
    wrestlerDTO1.setName("Test Wrestler 1");

    wrestlerDTO2 = new WrestlerDTO();
    wrestlerDTO2.setId(2L);
    wrestlerDTO2.setName("Test Wrestler 2");

    rivalryDTO = new RivalryDTO();
    rivalryDTO.setId(1L);
    rivalryDTO.setWrestler1(wrestlerDTO1);
    rivalryDTO.setWrestler2(wrestlerDTO2);
    rivalryDTO.setHeat(0);
    rivalryDTO.setIsActive(true);
  }

  // ==================== createRivalry ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void createRivalry() throws Exception {
    when(rivalryService.createRivalry(any(), any(), any())).thenReturn(Optional.of(rivalry));
    when(rivalryMapper.toRivalryDTO(any(Rivalry.class))).thenReturn(rivalryDTO);

    RivalryController.CreateRivalryRequest request =
        new RivalryController.CreateRivalryRequest(1L, 2L, "Some notes");

    mockMvc
        .perform(
            post("/api/rivalries")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.wrestler1.name").value("Test Wrestler 1"));
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void createRivalry_existingRivalry_returnsOk() throws Exception {
    rivalry.setHeat(15);
    when(rivalryService.createRivalry(any(), any(), any())).thenReturn(Optional.of(rivalry));
    when(rivalryMapper.toRivalryDTO(any(Rivalry.class))).thenReturn(rivalryDTO);

    RivalryController.CreateRivalryRequest request =
        new RivalryController.CreateRivalryRequest(1L, 2L, "Some notes");

    mockMvc
        .perform(
            post("/api/rivalries")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void createRivalry_wrestlersNotFound_returnsBadRequest() throws Exception {
    when(rivalryService.createRivalry(any(), any(), any())).thenReturn(Optional.empty());

    RivalryController.CreateRivalryRequest request =
        new RivalryController.CreateRivalryRequest(1L, 2L, "Some notes");

    mockMvc
        .perform(
            post("/api/rivalries")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  // ==================== getAllRivalries ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void getAllRivalries_returnsPagedResult() throws Exception {
    when(rivalryService.getAllRivalries(any(Pageable.class))).thenReturn(Page.empty());

    mockMvc.perform(get("/api/rivalries")).andExpect(status().isOk());
  }

  // ==================== getRivalryById ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void getRivalryById_found_returnsOk() throws Exception {
    when(rivalryService.getRivalryById(1L)).thenReturn(Optional.of(rivalry));
    when(rivalryMapper.toRivalryDTO(any(Rivalry.class))).thenReturn(rivalryDTO);

    mockMvc
        .perform(get("/api/rivalries/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1L));
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void getRivalryById_notFound_returns404() throws Exception {
    when(rivalryService.getRivalryById(999L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/rivalries/999")).andExpect(status().isNotFound());
  }

  // ==================== getActiveRivalries ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void getActiveRivalries_returnsList() throws Exception {
    when(rivalryService.getActiveRivalries()).thenReturn(List.of(rivalry));
    when(rivalryMapper.toRivalryDTO(any(Rivalry.class))).thenReturn(rivalryDTO);

    mockMvc
        .perform(get("/api/rivalries/active"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1L));
  }

  // ==================== getRivalriesForWrestler ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void getRivalriesForWrestler_returnsList() throws Exception {
    when(rivalryService.getRivalriesForWrestler(1L)).thenReturn(List.of(rivalry));
    when(rivalryMapper.toRivalryDTO(any(Rivalry.class))).thenReturn(rivalryDTO);

    mockMvc
        .perform(get("/api/rivalries/wrestler/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1L));
  }

  // ==================== addHeat ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void addHeat_success_returnsOk() throws Exception {
    when(rivalryService.addHeat(eq(1L), anyInt(), anyString())).thenReturn(Optional.of(rivalry));
    when(rivalryMapper.toRivalryDTO(any(Rivalry.class))).thenReturn(rivalryDTO);

    RivalryController.AddHeatRequest request = new RivalryController.AddHeatRequest(5, "big match");

    mockMvc
        .perform(
            post("/api/rivalries/1/heat")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void addHeat_notFound_returnsBadRequest() throws Exception {
    when(rivalryService.addHeat(eq(1L), anyInt(), anyString())).thenReturn(Optional.empty());

    RivalryController.AddHeatRequest request = new RivalryController.AddHeatRequest(5, "big match");

    mockMvc
        .perform(
            post("/api/rivalries/1/heat")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  // ==================== addHeatBetweenWrestlers ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void addHeatBetweenWrestlers_success_returnsOk() throws Exception {
    when(rivalryService.addHeatBetweenWrestlers(eq(1L), eq(2L), anyInt(), anyString()))
        .thenReturn(Optional.of(rivalry));
    when(rivalryMapper.toRivalryDTO(any(Rivalry.class))).thenReturn(rivalryDTO);

    RivalryController.AddHeatBetweenWrestlersRequest request =
        new RivalryController.AddHeatBetweenWrestlersRequest(1L, 2L, 10, "intense promo");

    mockMvc
        .perform(
            post("/api/rivalries/heat")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void addHeatBetweenWrestlers_notFound_returnsBadRequest() throws Exception {
    when(rivalryService.addHeatBetweenWrestlers(eq(1L), eq(2L), anyInt(), anyString()))
        .thenReturn(Optional.empty());

    RivalryController.AddHeatBetweenWrestlersRequest request =
        new RivalryController.AddHeatBetweenWrestlersRequest(1L, 2L, 10, "intense promo");

    mockMvc
        .perform(
            post("/api/rivalries/heat")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  // ==================== attemptResolution ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void attemptResolution_returnsOk() throws Exception {
    ResolutionResult<Rivalry> result =
        new ResolutionResult<>(true, "Rivalry resolved!", rivalry, 15, 10, 25);
    when(rivalryService.attemptResolution(eq(1L), anyInt(), anyInt())).thenReturn(result);
    when(rivalryMapper.toRivalryDTO(any(Rivalry.class))).thenReturn(rivalryDTO);

    RivalryController.ResolutionAttemptRequest request =
        new RivalryController.ResolutionAttemptRequest(15, 10);

    mockMvc
        .perform(
            post("/api/rivalries/1/resolve")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Rivalry resolved!"));
  }

  // ==================== endRivalry ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void endRivalry_success_returnsOk() throws Exception {
    when(rivalryService.endRivalry(eq(1L), anyString())).thenReturn(Optional.of(rivalry));
    when(rivalryMapper.toRivalryDTO(any(Rivalry.class))).thenReturn(rivalryDTO);

    RivalryController.EndRivalryRequest request =
        new RivalryController.EndRivalryRequest("Story concluded");

    mockMvc
        .perform(
            post("/api/rivalries/1/end")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void endRivalry_notFound_returnsBadRequest() throws Exception {
    when(rivalryService.endRivalry(eq(1L), anyString())).thenReturn(Optional.empty());

    RivalryController.EndRivalryRequest request =
        new RivalryController.EndRivalryRequest("Story concluded");

    mockMvc
        .perform(
            post("/api/rivalries/1/end")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  // ==================== getRivalriesRequiringSegments ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void getRivalriesRequiringSegments_returnsList() throws Exception {
    when(rivalryService.getRivalriesRequiringMatches()).thenReturn(List.of(rivalry));
    when(rivalryMapper.toRivalryDTO(any(Rivalry.class))).thenReturn(rivalryDTO);

    mockMvc
        .perform(get("/api/rivalries/requiring-matches"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1L));
  }

  // ==================== getRivalriesEligibleForResolution ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void getRivalriesEligibleForResolution_returnsList() throws Exception {
    when(rivalryService.getRivalriesEligibleForResolution()).thenReturn(List.of(rivalry));
    when(rivalryMapper.toRivalryDTO(any(Rivalry.class))).thenReturn(rivalryDTO);

    mockMvc
        .perform(get("/api/rivalries/eligible-for-resolution"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1L));
  }

  // ==================== getRivalriesRequiringStipulationSegments ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void getRivalriesRequiringStipulationSegments_returnsList() throws Exception {
    when(rivalryService.getRivalriesRequiringStipulationMatches()).thenReturn(List.of(rivalry));
    when(rivalryMapper.toRivalryDTO(any(Rivalry.class))).thenReturn(rivalryDTO);

    mockMvc
        .perform(get("/api/rivalries/requiring-rule"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1L));
  }

  // ==================== getRivalriesByIntensity ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void getRivalriesByIntensity_returnsList() throws Exception {
    when(rivalryService.getRivalriesByIntensity(RivalryIntensity.HEATED))
        .thenReturn(List.of(rivalry));
    when(rivalryMapper.toRivalryDTO(any(Rivalry.class))).thenReturn(rivalryDTO);

    mockMvc
        .perform(get("/api/rivalries/intensity/HEATED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1L));
  }

  // ==================== getHottestRivalries ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void getHottestRivalries_returnsTopList() throws Exception {
    when(rivalryService.getHottestRivalries(5)).thenReturn(List.of(rivalry));
    when(rivalryMapper.toRivalryDTO(any(Rivalry.class))).thenReturn(rivalryDTO);

    mockMvc
        .perform(get("/api/rivalries/hottest").param("limit", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1L));
  }

  // ==================== updateStorylineNotes ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void updateStorylineNotes_found_returnsOk() throws Exception {
    when(rivalryService.updateStorylineNotes(eq(1L), anyString())).thenReturn(Optional.of(rivalry));
    when(rivalryMapper.toRivalryDTO(any(Rivalry.class))).thenReturn(rivalryDTO);

    RivalryController.UpdateStorylineRequest request =
        new RivalryController.UpdateStorylineRequest("Updated storyline notes");

    mockMvc
        .perform(
            put("/api/rivalries/1/storyline")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void updateStorylineNotes_notFound_returns404() throws Exception {
    when(rivalryService.updateStorylineNotes(eq(1L), anyString())).thenReturn(Optional.empty());

    RivalryController.UpdateStorylineRequest request =
        new RivalryController.UpdateStorylineRequest("Updated storyline notes");

    mockMvc
        .perform(
            put("/api/rivalries/1/storyline")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  // ==================== getRivalryStats ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void getRivalryStats_found_returnsOk() throws Exception {
    RivalryService.RivalryStats stats =
        new RivalryService.RivalryStats(
            1L,
            "Test Wrestler 1",
            "Test Wrestler 2",
            15,
            RivalryIntensity.HEATED,
            true,
            false,
            false,
            7L,
            true,
            3);
    when(rivalryService.getRivalryStats(1L)).thenReturn(stats);

    mockMvc
        .perform(get("/api/rivalries/1/stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rivalryId").value(1L));
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void getRivalryStats_notFound_returns404() throws Exception {
    when(rivalryService.getRivalryStats(999L)).thenReturn(null);

    mockMvc.perform(get("/api/rivalries/999/stats")).andExpect(status().isNotFound());
  }

  // ==================== checkRivalryHistory ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void checkRivalryHistory_returnsResponse() throws Exception {
    when(rivalryService.hasRivalryHistory(1L, 2L)).thenReturn(true);

    mockMvc
        .perform(get("/api/rivalries/history/1/2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasHistory").value(true));
  }
}
