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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.service.season.SeasonService;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WithMockUser(authorities = {"ADMIN", "ROLE_ADMIN", "ROLE_BOOKER"})
class SeasonControllerTest extends AbstractControllerTest {

  @MockitoBean private SeasonService seasonService;

  private Season season;

  @BeforeEach
  void setUp() {
    season = new Season();
    season.setId(1L);
    season.setName("Test Season");
    season.setDescription("A test season");
    season.setShowsPerPpv(5);
    season.setIsActive(true);
    season.setStartDate(Instant.now());
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void createSeason() throws Exception {
    when(seasonService.createSeason(any(), any(), any())).thenReturn(season);

    SeasonController.CreateSeasonRequest request =
        new SeasonController.CreateSeasonRequest("Test Season", "Description", 5);

    mockMvc
        .perform(
            post("/api/seasons")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Test Season"));
  }

  @Test
  void getAllSeasons_defaultPagination_returnsOk() throws Exception {
    Page<Season> page = new PageImpl<>(List.of(season));
    when(seasonService.getAllSeasons(any())).thenReturn(page);

    mockMvc.perform(get("/api/seasons").with(csrf())).andExpect(status().isOk());
  }

  @Test
  void getAllSeasons_withCustomPagination_returnsOk() throws Exception {
    Page<Season> page = new PageImpl<>(List.of(season));
    when(seasonService.getAllSeasons(any())).thenReturn(page);

    mockMvc
        .perform(
            get("/api/seasons")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "name")
                .param("sortDir", "asc")
                .with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  void getAllSeasons_emptyResult_returnsOk() throws Exception {
    Page<Season> empty = new PageImpl<>(Collections.emptyList());
    when(seasonService.getAllSeasons(any())).thenReturn(empty);

    mockMvc.perform(get("/api/seasons").with(csrf())).andExpect(status().isOk());
  }

  @Test
  void getSeasonById_found_returnsOk() throws Exception {
    when(seasonService.getSeasonById(1L)).thenReturn(Optional.of(season));

    mockMvc
        .perform(get("/api/seasons/1").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Season"));
  }

  @Test
  void getSeasonById_notFound_returns404() throws Exception {
    when(seasonService.getSeasonById(99L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/seasons/99").with(csrf())).andExpect(status().isNotFound());
  }

  @Test
  void getActiveSeason_found_returnsOk() throws Exception {
    when(seasonService.getActiveSeason()).thenReturn(Optional.of(season));

    mockMvc
        .perform(get("/api/seasons/active").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Season"));
  }

  @Test
  void getActiveSeason_notFound_returns404() throws Exception {
    when(seasonService.getActiveSeason()).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/seasons/active").with(csrf())).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void endCurrentSeason_found_returnsOk() throws Exception {
    when(seasonService.endCurrentSeason()).thenReturn(Optional.of(season));

    mockMvc
        .perform(post("/api/seasons/end-current").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Season"));
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void endCurrentSeason_noActiveSeason_returns404() throws Exception {
    when(seasonService.endCurrentSeason()).thenReturn(Optional.empty());

    mockMvc.perform(post("/api/seasons/end-current").with(csrf())).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void updateSeason_found_returnsOk() throws Exception {
    when(seasonService.updateSeason(eq(1L), any(), any(), any())).thenReturn(Optional.of(season));

    SeasonController.UpdateSeasonRequest request =
        new SeasonController.UpdateSeasonRequest("Updated Season", "Updated Description", 8);

    mockMvc
        .perform(
            put("/api/seasons/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Season"));
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void updateSeason_notFound_returns404() throws Exception {
    when(seasonService.updateSeason(eq(99L), any(), any(), any())).thenReturn(Optional.empty());

    SeasonController.UpdateSeasonRequest request =
        new SeasonController.UpdateSeasonRequest("Updated Season", "Updated Description", 8);

    mockMvc
        .perform(
            put("/api/seasons/99")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void deleteSeason_deleted_returnsNoContent() throws Exception {
    when(seasonService.deleteSeason(1L)).thenReturn(true);

    mockMvc.perform(delete("/api/seasons/1").with(csrf())).andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void deleteSeason_cannotDelete_returnsConflict() throws Exception {
    when(seasonService.deleteSeason(1L)).thenReturn(false);

    mockMvc.perform(delete("/api/seasons/1").with(csrf())).andExpect(status().isConflict());
  }

  @Test
  void checkTimeForPpv_returnsOk() throws Exception {
    when(seasonService.isTimeForPpv()).thenReturn(true);
    when(seasonService.getSeasonsNeedingPpv()).thenReturn(List.of(season));

    mockMvc
        .perform(get("/api/seasons/ppv-check").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.timeForPpv").value(true));
  }

  @Test
  void checkTimeForPpv_notTimForPpv_returnsOk() throws Exception {
    when(seasonService.isTimeForPpv()).thenReturn(false);
    when(seasonService.getSeasonsNeedingPpv()).thenReturn(Collections.emptyList());

    mockMvc
        .perform(get("/api/seasons/ppv-check").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.timeForPpv").value(false));
  }

  @Test
  void getSeasonStats_found_returnsOk() throws Exception {
    SeasonService.SeasonStats stats =
        new SeasonService.SeasonStats(1L, "Test Season", 10, 8, 2, 2, false, 90L, true);
    when(seasonService.getSeasonStats(1L)).thenReturn(stats);

    mockMvc
        .perform(get("/api/seasons/1/stats").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Season"))
        .andExpect(jsonPath("$.totalShows").value(10));
  }

  @Test
  void getSeasonStats_notFound_returns404() throws Exception {
    when(seasonService.getSeasonStats(99L)).thenReturn(null);

    mockMvc.perform(get("/api/seasons/99/stats").with(csrf())).andExpect(status().isNotFound());
  }
}
