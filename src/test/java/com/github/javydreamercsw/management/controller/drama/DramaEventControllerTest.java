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
package com.github.javydreamercsw.management.controller.drama;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.domain.drama.DramaEvent;
import com.github.javydreamercsw.management.domain.drama.DramaEventSeverity;
import com.github.javydreamercsw.management.domain.drama.DramaEventType;
import com.github.javydreamercsw.management.service.drama.DramaEventService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class DramaEventControllerTest extends AbstractControllerTest {

  @MockitoBean private DramaEventService dramaEventService;

  private DramaEvent event;

  @BeforeEach
  void setUpDramaEvent() {
    event = new DramaEvent();
    event.setId(1L);
    event.setTitle("Test Event");
    event.setSeverity(DramaEventSeverity.NEUTRAL);
    event.setEventType(DramaEventType.BACKSTAGE_INCIDENT);
  }

  // ==================== createDramaEvent ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void createDramaEvent() throws Exception {
    when(dramaEventService.createDramaEvent(
            eq(1L),
            isNull(),
            eq(DramaEventType.BACKSTAGE_INCIDENT),
            eq(DramaEventSeverity.NEUTRAL),
            eq("Test Event"),
            eq("Description"),
            eq(1L)))
        .thenReturn(Optional.of(event));

    DramaEventController.CreateDramaEventRequest request =
        new DramaEventController.CreateDramaEventRequest(
            1L,
            null,
            DramaEventType.BACKSTAGE_INCIDENT,
            DramaEventSeverity.NEUTRAL,
            "Test Event",
            "Description",
            1L);

    mockMvc
        .perform(
            post("/api/drama-events")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Test Event"));
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void createDramaEvent_wrestlerNotFound_returnsBadRequest() throws Exception {
    when(dramaEventService.createDramaEvent(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Optional.empty());

    DramaEventController.CreateDramaEventRequest request =
        new DramaEventController.CreateDramaEventRequest(
            999L,
            null,
            DramaEventType.BACKSTAGE_INCIDENT,
            DramaEventSeverity.NEUTRAL,
            "Test Event",
            "Description",
            1L);

    mockMvc
        .perform(
            post("/api/drama-events")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void createDramaEvent_withSecondaryWrestler_returnsCreated() throws Exception {
    when(dramaEventService.createDramaEvent(
            eq(1L),
            eq(2L),
            eq(DramaEventType.BETRAYAL),
            eq(DramaEventSeverity.MAJOR),
            eq("Betrayal Event"),
            eq("He turned heel"),
            eq(1L)))
        .thenReturn(Optional.of(event));

    DramaEventController.CreateDramaEventRequest request =
        new DramaEventController.CreateDramaEventRequest(
            1L,
            2L,
            DramaEventType.BETRAYAL,
            DramaEventSeverity.MAJOR,
            "Betrayal Event",
            "He turned heel",
            1L);

    mockMvc
        .perform(
            post("/api/drama-events")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void createDramaEvent_missingRequiredFields_returnsBadRequest() throws Exception {
    // Missing required fields: primaryWrestlerId, eventType, severity, title, description,
    // universeId
    String invalidJson = "{\"secondaryWrestlerId\": null}";

    mockMvc
        .perform(
            post("/api/drama-events")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
        .andExpect(status().isBadRequest());
  }

  // ==================== generateRandomDramaEvent ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void generateRandomDramaEvent_success_returnsCreated() throws Exception {
    when(dramaEventService.generateRandomDramaEvent(1L, 1L)).thenReturn(Optional.of(event));

    mockMvc
        .perform(post("/api/drama-events/generate/1/1").with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Test Event"));
  }

  @Test
  @WithMockUser(roles = "BOOKER")
  void generateRandomDramaEvent_wrestlerNotFound_returnsNotFound() throws Exception {
    when(dramaEventService.generateRandomDramaEvent(999L, 1L)).thenReturn(Optional.empty());

    mockMvc
        .perform(post("/api/drama-events/generate/999/1").with(csrf()))
        .andExpect(status().isNotFound());
  }

  // ==================== processUnprocessedEvents ====================

  @Test
  @WithMockUser(roles = "BOOKER")
  void processUnprocessedEvents_returnsOk() throws Exception {
    doNothing().when(dramaEventService).processUnprocessedEvents();

    mockMvc
        .perform(post("/api/drama-events/process").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.processedCount").value(0))
        .andExpect(jsonPath("$.message").value("Successfully triggered drama event processing"));
  }

  // ==================== getEventsForWrestler ====================

  @Test
  void getEventsForWrestler_returnsList() throws Exception {
    when(dramaEventService.getEventsForWrestler(1L)).thenReturn(List.of(event));

    mockMvc
        .perform(get("/api/drama-events/wrestler/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1L))
        .andExpect(jsonPath("$[0].title").value("Test Event"));
  }

  @Test
  void getEventsForWrestler_emptyList_returnsOk() throws Exception {
    when(dramaEventService.getEventsForWrestler(2L)).thenReturn(Collections.emptyList());

    mockMvc
        .perform(get("/api/drama-events/wrestler/2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  // ==================== getEventsForWrestlerPaginated ====================

  @Test
  void getEventsForWrestlerPaginated_returnsPage() throws Exception {
    when(dramaEventService.getEventsForWrestler(eq(1L), any(Pageable.class)))
        .thenReturn(Page.empty());

    mockMvc.perform(get("/api/drama-events/wrestler/1/paginated")).andExpect(status().isOk());
  }

  // ==================== getRecentEvents ====================

  @Test
  void getRecentEvents_returnsList() throws Exception {
    when(dramaEventService.getRecentEvents()).thenReturn(List.of(event));

    mockMvc
        .perform(get("/api/drama-events/recent"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1L));
  }

  @Test
  void getRecentEvents_emptyList_returnsOk() throws Exception {
    when(dramaEventService.getRecentEvents()).thenReturn(Collections.emptyList());

    mockMvc
        .perform(get("/api/drama-events/recent"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  // ==================== getEventsBetweenWrestlers ====================

  @Test
  void getEventsBetweenWrestlers_returnsList() throws Exception {
    when(dramaEventService.getEventsBetweenWrestlers(1L, 2L)).thenReturn(List.of(event));

    mockMvc
        .perform(get("/api/drama-events/between/1/2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1L));
  }

  @Test
  void getEventsBetweenWrestlers_noEvents_returnsEmptyList() throws Exception {
    when(dramaEventService.getEventsBetweenWrestlers(1L, 99L)).thenReturn(Collections.emptyList());

    mockMvc
        .perform(get("/api/drama-events/between/1/99"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  // ==================== getEventTypes ====================

  @Test
  void getEventTypes_returnsAllTypes() throws Exception {
    mockMvc
        .perform(get("/api/drama-events/types"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  // ==================== getSeverities ====================

  @Test
  void getSeverities_returnsAllSeverities() throws Exception {
    mockMvc
        .perform(get("/api/drama-events/severities"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }
}
