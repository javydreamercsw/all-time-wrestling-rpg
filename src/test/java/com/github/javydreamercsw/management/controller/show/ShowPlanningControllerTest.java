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
package com.github.javydreamercsw.management.controller.show;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.planning.ProposedSegment;
import com.github.javydreamercsw.management.service.show.planning.ProposedShow;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningAiService;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningService;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class ShowPlanningControllerTest extends AbstractControllerTest {

  @MockitoBean private ShowPlanningService showPlanningService;
  @MockitoBean private ShowPlanningAiService showPlanningAiService;
  @MockitoBean private ShowService showService;

  @Test
  void getShowPlanningContext() throws Exception {
    // Given
    Show show = new Show();
    show.setId(1L);
    when(showService.getShowById(1L)).thenReturn(Optional.of(show));
    when(showPlanningService.getShowPlanningContext(show)).thenReturn(new ShowPlanningContextDTO());

    // When & Then
    mockMvc.perform(get("/api/show-planning/context/1")).andExpect(status().isOk());
  }

  @Test
  void getShowPlanningContext_showNotFound_returns404() throws Exception {
    // Given
    when(showService.getShowById(99L)).thenReturn(Optional.empty());

    // When & Then
    mockMvc.perform(get("/api/show-planning/context/99")).andExpect(status().isNotFound());
  }

  @Test
  void planShow() throws Exception {
    // Given
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    when(showPlanningAiService.planShow(context)).thenReturn(new ProposedShow());

    // When & Then
    mockMvc
        .perform(
            post("/api/show-planning/plan")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(context)))
        .andExpect(status().isOk());
  }

  @Test
  void planShow_withNullFields_returnsOk() throws Exception {
    // Given - context with all null fields
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    when(showPlanningAiService.planShow(any(ShowPlanningContextDTO.class)))
        .thenReturn(new ProposedShow());

    // When & Then
    mockMvc
        .perform(
            post("/api/show-planning/plan")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk());
  }

  @Test
  void planShow_withEmptyBody_returnsOk() throws Exception {
    // Given
    when(showPlanningAiService.planShow(any(ShowPlanningContextDTO.class)))
        .thenReturn(new ProposedShow());

    // When & Then - empty JSON object is a valid ShowPlanningContextDTO
    mockMvc
        .perform(
            post("/api/show-planning/plan")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ShowPlanningContextDTO())))
        .andExpect(status().isOk());
  }

  @Test
  void approveSegments_showFound_returns200() throws Exception {
    // Given
    Show show = new Show();
    show.setId(1L);
    when(showService.getShowById(1L)).thenReturn(Optional.of(show));

    List<ProposedSegment> segments = List.of(new ProposedSegment());

    // When & Then
    mockMvc
        .perform(
            post("/api/show-planning/approve/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(segments)))
        .andExpect(status().isOk());

    verify(showPlanningService).approveSegments(eq(show), any());
  }

  @Test
  void approveSegments_showNotFound_returns200WithoutCallingService() throws Exception {
    // Given - show does not exist; controller still returns 200 (ifPresent silently skips)
    when(showService.getShowById(99L)).thenReturn(Optional.empty());

    List<ProposedSegment> segments = Collections.emptyList();

    // When & Then
    mockMvc
        .perform(
            post("/api/show-planning/approve/99")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(segments)))
        .andExpect(status().isOk());

    verify(showPlanningService, never()).approveSegments(any(), any());
  }

  @Test
  void approveSegments_emptySegmentList_returns200() throws Exception {
    // Given
    Show show = new Show();
    show.setId(1L);
    when(showService.getShowById(1L)).thenReturn(Optional.of(show));

    // When & Then
    mockMvc
        .perform(
            post("/api/show-planning/approve/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
        .andExpect(status().isOk());

    verify(showPlanningService).approveSegments(eq(show), eq(Collections.emptyList()));
  }
}
