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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.service.faction.FactionService;
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
class FactionControllerTest extends AbstractControllerTest {

  @MockitoBean private FactionService factionService;

  private Faction testFaction;

  @BeforeEach
  void setUp() {
    testFaction = new Faction();
    testFaction.setId(1L);
    testFaction.setName("The Shield");
  }

  @Test
  void getAllFactions_returnsPage() throws Exception {
    when(factionService.getAllFactions(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(testFaction)));

    mockMvc
        .perform(get("/api/factions").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("The Shield"));
  }

  @Test
  void getFactionById_found() throws Exception {
    when(factionService.getFactionById(1L)).thenReturn(Optional.of(testFaction));

    mockMvc
        .perform(get("/api/factions/1").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("The Shield"));
  }

  @Test
  void getFactionById_notFound() throws Exception {
    when(factionService.getFactionById(anyLong())).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/factions/99").with(csrf())).andExpect(status().isNotFound());
  }

  @Test
  void getFactionByName_found() throws Exception {
    when(factionService.getFactionByName("The Shield")).thenReturn(Optional.of(testFaction));

    mockMvc
        .perform(get("/api/factions/name/The Shield").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("The Shield"));
  }

  @Test
  void getFactionByName_notFound() throws Exception {
    when(factionService.getFactionByName(anyString())).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/factions/name/Unknown").with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void getActiveFactions_returnsList() throws Exception {
    when(factionService.getActiveFactions()).thenReturn(List.of(testFaction));

    mockMvc
        .perform(get("/api/factions/active").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("The Shield"));
  }

  @Test
  void getFactionsByType_returnsList() throws Exception {
    when(factionService.getFactionsByType("stable")).thenReturn(List.of(testFaction));

    mockMvc
        .perform(get("/api/factions/type/stable").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("The Shield"));
  }

  @Test
  void getLargestFactions_returnsList() throws Exception {
    when(factionService.getLargestFactions(anyInt())).thenReturn(List.of(testFaction));

    mockMvc
        .perform(get("/api/factions/largest").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("The Shield"));
  }

  @Test
  void getFactionsWithRivalries_returnsList() throws Exception {
    when(factionService.getFactionsWithActiveRivalries()).thenReturn(List.of(testFaction));

    mockMvc
        .perform(get("/api/factions/with-rivalries").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("The Shield"));
  }

  @Test
  void getFactionForWrestler_found() throws Exception {
    when(factionService.getFactionForWrestler(1L)).thenReturn(Optional.of(testFaction));

    mockMvc
        .perform(get("/api/factions/wrestler/1").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("The Shield"));
  }

  @Test
  void getFactionForWrestler_notFound() throws Exception {
    when(factionService.getFactionForWrestler(anyLong())).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/factions/wrestler/99").with(csrf())).andExpect(status().isNotFound());
  }

  @Test
  void createFaction_success() throws Exception {
    when(factionService.createFaction(anyString(), anyString(), any(Long.class), anyLong()))
        .thenReturn(Optional.of(testFaction));

    FactionController.CreateFactionRequest request =
        new FactionController.CreateFactionRequest("The Shield", "Elite faction", 1L, 1L);

    mockMvc
        .perform(
            post("/api/factions")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("The Shield"));
  }

  @Test
  void createFaction_failure() throws Exception {
    when(factionService.createFaction(anyString(), anyString(), any(Long.class), anyLong()))
        .thenReturn(Optional.empty());

    FactionController.CreateFactionRequest request =
        new FactionController.CreateFactionRequest("Duplicate", null, null, null);

    mockMvc
        .perform(
            post("/api/factions")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void addMember_success() throws Exception {
    when(factionService.addMemberToFaction(anyLong(), anyLong()))
        .thenReturn(Optional.of(testFaction));

    FactionController.AddMemberRequest request = new FactionController.AddMemberRequest(2L);

    mockMvc
        .perform(
            post("/api/factions/1/members")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void addMember_failure() throws Exception {
    when(factionService.addMemberToFaction(anyLong(), anyLong())).thenReturn(Optional.empty());

    FactionController.AddMemberRequest request = new FactionController.AddMemberRequest(99L);

    mockMvc
        .perform(
            post("/api/factions/1/members")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void removeMember_success() throws Exception {
    when(factionService.removeMemberFromFaction(anyLong(), anyLong(), anyString()))
        .thenReturn(Optional.of(testFaction));

    mockMvc
        .perform(
            delete("/api/factions/1/members/2").with(csrf()).param("reason", "Left the faction"))
        .andExpect(status().isOk());
  }

  @Test
  void removeMember_failure() throws Exception {
    when(factionService.removeMemberFromFaction(anyLong(), anyLong(), anyString()))
        .thenReturn(Optional.empty());

    mockMvc
        .perform(delete("/api/factions/1/members/99").with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void changeLeader_success() throws Exception {
    when(factionService.changeFactionLeader(anyLong(), anyLong()))
        .thenReturn(Optional.of(testFaction));

    FactionController.ChangeLeaderRequest request = new FactionController.ChangeLeaderRequest(2L);

    mockMvc
        .perform(
            put("/api/factions/1/leader")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void changeLeader_failure() throws Exception {
    when(factionService.changeFactionLeader(anyLong(), anyLong())).thenReturn(Optional.empty());

    FactionController.ChangeLeaderRequest request = new FactionController.ChangeLeaderRequest(99L);

    mockMvc
        .perform(
            put("/api/factions/1/leader")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void disbandFaction_success() throws Exception {
    when(factionService.disbandFaction(anyLong(), anyString()))
        .thenReturn(Optional.of(testFaction));

    mockMvc
        .perform(delete("/api/factions/1").with(csrf()).param("reason", "Story ended"))
        .andExpect(status().isOk());
  }

  @Test
  void disbandFaction_failure() throws Exception {
    when(factionService.disbandFaction(anyLong(), anyString())).thenReturn(Optional.empty());

    mockMvc.perform(delete("/api/factions/99").with(csrf())).andExpect(status().isBadRequest());
  }
}
