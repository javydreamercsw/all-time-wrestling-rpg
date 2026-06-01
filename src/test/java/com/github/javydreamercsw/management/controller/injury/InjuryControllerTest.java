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
package com.github.javydreamercsw.management.controller.injury;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WithMockUser(authorities = {"ADMIN", "ROLE_ADMIN", "ROLE_BOOKER"})
class InjuryControllerTest extends AbstractControllerTest {

  @MockitoBean private InjuryService injuryService;
  @MockitoBean private WrestlerRepository wrestlerRepository;

  private Wrestler testWrestler;
  private Injury testInjury;

  @BeforeEach
  void setUp() {
    testWrestler = Wrestler.builder().build();
    testWrestler.setId(1L);
    testWrestler.setName("Test Wrestler");

    testInjury = new Injury();
    testInjury.setId(1L);
    testInjury.setWrestler(testWrestler);
    testInjury.setName("Broken Leg");
    testInjury.setDescription("A severe break.");
    testInjury.setSeverity(InjurySeverity.SEVERE);
    testInjury.setInjuryDate(Instant.now());
    testInjury.setHealthPenalty(10);
    testInjury.setIsActive(true);
  }

  // ==================== POST /api/injuries ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void createInjury() throws Exception {
    when(injuryService.createInjury(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Optional.of(testInjury));

    InjuryController.CreateInjuryRequest request =
        new InjuryController.CreateInjuryRequest(
            1L, 1L, null, "Broken Leg", "A severe break.", InjurySeverity.SEVERE, "Some notes");

    mockMvc
        .perform(
            post("/api/injuries")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Broken Leg"));
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void createInjury_wrestlerNotFound_returnsBadRequest() throws Exception {
    when(injuryService.createInjury(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Optional.empty());

    InjuryController.CreateInjuryRequest request =
        new InjuryController.CreateInjuryRequest(
            999L, 1L, null, "Broken Leg", "A severe break.", InjurySeverity.SEVERE, null);

    mockMvc
        .perform(
            post("/api/injuries")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Cannot create injury - wrestler not found"));
  }

  // ==================== POST /api/injuries/from-bumps/{wrestlerId} ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void createInjuryFromBumps_success() throws Exception {
    WrestlerState state = WrestlerState.builder().bumps(5).build();
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(testWrestler));
    when(injuryService.getWrestlerState(1L, 1L)).thenReturn(state);
    when(injuryService.createInjuryFromBumps(1L, 1L)).thenReturn(Optional.of(testInjury));

    mockMvc
        .perform(post("/api/injuries/from-bumps/1").with(csrf()).param("universeId", "1"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Broken Leg"));
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void createInjuryFromBumps_wrestlerNotFound_returnsNotFound() throws Exception {
    when(wrestlerRepository.findById(999L)).thenReturn(Optional.empty());

    mockMvc
        .perform(post("/api/injuries/from-bumps/999").with(csrf()).param("universeId", "1"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Wrestler not found"));
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void createInjuryFromBumps_notEnoughBumps_returnsBadRequest() throws Exception {
    WrestlerState state = WrestlerState.builder().bumps(2).build();
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(testWrestler));
    when(injuryService.getWrestlerState(1L, 1L)).thenReturn(state);

    mockMvc
        .perform(post("/api/injuries/from-bumps/1").with(csrf()).param("universeId", "1"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message").value(org.hamcrest.Matchers.containsString("less than 3 bumps")));
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void createInjuryFromBumps_serviceReturnsEmpty_returnsBadRequest() throws Exception {
    WrestlerState state = WrestlerState.builder().bumps(4).build();
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(testWrestler));
    when(injuryService.getWrestlerState(1L, 1L)).thenReturn(state);
    when(injuryService.createInjuryFromBumps(1L, 1L)).thenReturn(Optional.empty());

    mockMvc
        .perform(post("/api/injuries/from-bumps/1").with(csrf()).param("universeId", "1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Failed to create injury"));
  }

  // ==================== GET /api/injuries ====================

  @Test
  void getAllInjuries_returnsPage() throws Exception {
    when(injuryService.getAllInjuries(any())).thenReturn(new PageImpl<>(List.of(testInjury)));

    mockMvc
        .perform(get("/api/injuries"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("Broken Leg"));
  }

  @Test
  void getAllInjuries_withPaginationParams_returnsOk() throws Exception {
    when(injuryService.getAllInjuries(any())).thenReturn(new PageImpl<>(List.of()));

    mockMvc
        .perform(
            get("/api/injuries")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "name")
                .param("sortDir", "asc"))
        .andExpect(status().isOk());
  }

  // ==================== GET /api/injuries/{id} ====================

  @Test
  void getInjuryById_found_returnsOk() throws Exception {
    when(injuryService.getInjuryById(1L)).thenReturn(Optional.of(testInjury));

    mockMvc
        .perform(get("/api/injuries/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Broken Leg"));
  }

  @Test
  void getInjuryById_notFound_returnsNotFound() throws Exception {
    when(injuryService.getInjuryById(999L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/injuries/999")).andExpect(status().isNotFound());
  }

  // ==================== GET /api/injuries/wrestler/{wrestlerId}/active ====================

  @Test
  void getActiveInjuriesForWrestler_returnsOk() throws Exception {
    when(injuryService.getActiveInjuriesForWrestler(1L, 1L)).thenReturn(List.of(testInjury));

    mockMvc
        .perform(get("/api/injuries/wrestler/1/active").param("universeId", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Broken Leg"));
  }

  @Test
  void getActiveInjuriesForWrestler_defaultUniverseId_returnsOk() throws Exception {
    when(injuryService.getActiveInjuriesForWrestler(anyLong(), anyLong())).thenReturn(List.of());

    mockMvc.perform(get("/api/injuries/wrestler/1/active")).andExpect(status().isOk());
  }

  // ==================== GET /api/injuries/wrestler/{wrestlerId} ====================

  @Test
  void getAllInjuriesForWrestler_returnsOk() throws Exception {
    when(injuryService.getAllInjuriesForWrestler(1L, 1L)).thenReturn(List.of(testInjury));

    mockMvc
        .perform(get("/api/injuries/wrestler/1").param("universeId", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Broken Leg"));
  }

  // ==================== GET /api/injuries/severity/{severity} ====================

  @Test
  void getInjuriesBySeverity_returnsOk() throws Exception {
    when(injuryService.getInjuriesBySeverity(InjurySeverity.SEVERE))
        .thenReturn(List.of(testInjury));

    mockMvc
        .perform(get("/api/injuries/severity/SEVERE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Broken Leg"));
  }

  // ==================== GET /api/injuries/active ====================

  @Test
  void getAllActiveInjuries_returnsOk() throws Exception {
    when(injuryService.getAllActiveInjuries()).thenReturn(List.of(testInjury));

    mockMvc
        .perform(get("/api/injuries/active"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Broken Leg"));
  }

  // ==================== GET /api/injuries/wrestlers-with-injuries ====================

  @Test
  void getWrestlersWithActiveInjuries_returnsOk() throws Exception {
    when(injuryService.getWrestlersWithActiveInjuries(1L)).thenReturn(List.of(testWrestler));

    mockMvc
        .perform(get("/api/injuries/wrestlers-with-injuries").param("universeId", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Test Wrestler"));
  }

  @Test
  void getWrestlersWithActiveInjuries_defaultUniverseId_returnsOk() throws Exception {
    when(injuryService.getWrestlersWithActiveInjuries(anyLong())).thenReturn(List.of());

    mockMvc.perform(get("/api/injuries/wrestlers-with-injuries")).andExpect(status().isOk());
  }

  // ==================== GET /api/injuries/healable ====================

  @Test
  void getHealableInjuries_returnsOk() throws Exception {
    when(injuryService.getHealableInjuries()).thenReturn(List.of(testInjury));

    mockMvc
        .perform(get("/api/injuries/healable"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Broken Leg"));
  }

  // ==================== POST /api/injuries/{id}/heal ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void attemptHealing_success() throws Exception {
    InjuryService.HealingResult result =
        new InjuryService.HealingResult(true, "Injury healed successfully", testInjury, 5, true);
    when(injuryService.attemptHealing(eq(1L), eq(5))).thenReturn(result);

    InjuryController.HealingAttemptRequest request = new InjuryController.HealingAttemptRequest(5);

    mockMvc
        .perform(
            post("/api/injuries/1/heal")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Injury healed successfully"));
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void attemptHealing_failure() throws Exception {
    InjuryService.HealingResult result =
        new InjuryService.HealingResult(
            false, "Healing attempt failed (Rolled: 2, Needed: 5+)", testInjury, 2, true);
    when(injuryService.attemptHealing(eq(1L), eq(2))).thenReturn(result);

    InjuryController.HealingAttemptRequest request = new InjuryController.HealingAttemptRequest(2);

    mockMvc
        .perform(
            post("/api/injuries/1/heal")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void attemptHealing_invalidDiceRoll_returnsBadRequest() throws Exception {
    InjuryController.HealingAttemptRequest request = new InjuryController.HealingAttemptRequest(7);

    mockMvc
        .perform(
            post("/api/injuries/1/heal")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  // ==================== GET /api/injuries/wrestler/{wrestlerId}/health-penalty
  // ====================

  @Test
  void getTotalHealthPenaltyForWrestler_returnsOk() throws Exception {
    when(injuryService.getTotalHealthPenaltyForWrestler(1L, 1L)).thenReturn(15);

    mockMvc
        .perform(get("/api/injuries/wrestler/1/health-penalty").param("universeId", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalHealthPenalty").value(15));
  }

  @Test
  void getTotalHealthPenaltyForWrestler_defaultUniverseId_returnsOk() throws Exception {
    when(injuryService.getTotalHealthPenaltyForWrestler(anyLong(), anyLong())).thenReturn(0);

    mockMvc
        .perform(get("/api/injuries/wrestler/1/health-penalty"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalHealthPenalty").value(0));
  }

  // ==================== PUT /api/injuries/{id} ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void updateInjury_found_returnsOk() throws Exception {
    testInjury.setName("Updated Name");
    when(injuryService.updateInjury(eq(1L), any(), any(), any()))
        .thenReturn(Optional.of(testInjury));

    InjuryController.UpdateInjuryRequest request =
        new InjuryController.UpdateInjuryRequest("Updated Name", "Updated desc", "Updated notes");

    mockMvc
        .perform(
            put("/api/injuries/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Name"));
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void updateInjury_notFound_returnsNotFound() throws Exception {
    when(injuryService.updateInjury(eq(999L), any(), any(), any())).thenReturn(Optional.empty());

    InjuryController.UpdateInjuryRequest request =
        new InjuryController.UpdateInjuryRequest("Name", null, null);

    mockMvc
        .perform(
            put("/api/injuries/999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  // ==================== GET /api/injuries/wrestler/{wrestlerId}/stats ====================

  @Test
  void getInjuryStatsForWrestler_found_returnsOk() throws Exception {
    InjuryService.InjuryStats stats =
        new InjuryService.InjuryStats(1L, "Test Wrestler", 2, 1, 20, 80, 500L);
    when(injuryService.getInjuryStatsForWrestler(1L, 1L)).thenReturn(stats);

    mockMvc
        .perform(get("/api/injuries/wrestler/1/stats").param("universeId", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.wrestlerName").value("Test Wrestler"))
        .andExpect(jsonPath("$.activeInjuries").value(2));
  }

  @Test
  void getInjuryStatsForWrestler_notFound_returnsNotFound() throws Exception {
    when(injuryService.getInjuryStatsForWrestler(anyLong(), anyLong())).thenReturn(null);

    mockMvc.perform(get("/api/injuries/wrestler/999/stats")).andExpect(status().isNotFound());
  }
}
