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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractRestControllerIT;
import com.github.javydreamercsw.management.domain.drama.DramaEventSeverity;
import com.github.javydreamercsw.management.domain.drama.DramaEventType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.drama.DramaEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for DramaEventController. Tests the complete REST API functionality for drama
 * event management.
 */
@DisplayName("DramaEventController Integration Tests")
@Transactional
class DramaEventControllerIT extends AbstractRestControllerIT {

  @Autowired private DramaEventService dramaEventService;

  @BeforeEach
  public void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new DramaEventController(dramaEventService)).build();
  }

  @Test
  @DisplayName("GET /api/drama-events/types should return 200 with list of event types")
  void shouldReturnAllEventTypes() throws Exception {
    mockMvc
        .perform(get("/api/drama-events/types"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(DramaEventType.values().length));
  }

  @Test
  @DisplayName("GET /api/drama-events/severities should return 200 with list of severities")
  void shouldReturnAllSeverities() throws Exception {
    mockMvc
        .perform(get("/api/drama-events/severities"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(DramaEventSeverity.values().length));
  }

  @Test
  @DisplayName("POST /api/drama-events should create event and return 201 for valid request")
  void shouldCreateDramaEventSuccessfully() throws Exception {
    Wrestler wrestler = createTestWrestler("Drama Test Wrestler", 50_000L);

    DramaEventController.CreateDramaEventRequest request =
        new DramaEventController.CreateDramaEventRequest(
            wrestler.getId(),
            null,
            DramaEventType.SOCIAL_MEDIA_DRAMA,
            DramaEventSeverity.NEUTRAL,
            "Test Drama Title",
            "Test drama description",
            defaultUniverse.getId());

    mockMvc
        .perform(
            post("/api/drama-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName(
      "POST /api/drama-events should return 400 when required primaryWrestlerId is missing")
  void shouldReturn400WhenPrimaryWrestlerIdMissing() throws Exception {
    DramaEventController.CreateDramaEventRequest request =
        new DramaEventController.CreateDramaEventRequest(
            null, // missing required field
            null,
            DramaEventType.SOCIAL_MEDIA_DRAMA,
            DramaEventSeverity.NEUTRAL,
            "Test Drama Title",
            "Test drama description",
            defaultUniverse.getId());

    mockMvc
        .perform(
            post("/api/drama-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /api/drama-events should return 400 when title is blank")
  void shouldReturn400WhenTitleIsBlank() throws Exception {
    Wrestler wrestler = createTestWrestler("Blank Title Wrestler", 50_000L);

    DramaEventController.CreateDramaEventRequest request =
        new DramaEventController.CreateDramaEventRequest(
            wrestler.getId(),
            null,
            DramaEventType.SOCIAL_MEDIA_DRAMA,
            DramaEventSeverity.NEUTRAL,
            "", // blank title - should fail @NotBlank
            "Test drama description",
            defaultUniverse.getId());

    mockMvc
        .perform(
            post("/api/drama-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("GET /api/drama-events/wrestler/{wrestlerId} should return events for wrestler")
  void shouldReturnEventsForWrestler() throws Exception {
    Wrestler wrestler = createTestWrestler("Events Wrestler", 50_000L);

    // Create an event for this wrestler
    dramaEventService.createDramaEvent(
        wrestler.getId(),
        null,
        DramaEventType.FAN_INTERACTION,
        DramaEventSeverity.POSITIVE,
        "Fan Meet-up",
        "Wrestler met fans",
        defaultUniverse.getId());

    mockMvc
        .perform(get("/api/drama-events/wrestler/{wrestlerId}", wrestler.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  @DisplayName(
      """
      GET /api/drama-events/wrestler/{wrestlerId} should return empty list for wrestler with no\
       events\
      """)
  void shouldReturnEmptyListForWrestlerWithNoEvents() throws Exception {
    Wrestler wrestler = createTestWrestler("No Events Wrestler", 50_000L);

    mockMvc
        .perform(get("/api/drama-events/wrestler/{wrestlerId}", wrestler.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @DisplayName("GET /api/drama-events/recent should return 200 with list")
  void shouldReturnRecentEvents() throws Exception {
    mockMvc
        .perform(get("/api/drama-events/recent"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  @DisplayName(
      "POST /api/drama-events/generate/{wrestlerId}/{universeId} should generate random event")
  void shouldGenerateRandomDramaEvent() throws Exception {
    Wrestler wrestler = createTestWrestler("Random Event Wrestler", 50_000L);

    mockMvc
        .perform(
            post(
                "/api/drama-events/generate/{wrestlerId}/{universeId}",
                wrestler.getId(),
                defaultUniverse.getId()))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("GET /api/drama-events/between/{wrestler1Id}/{wrestler2Id} should return events")
  void shouldReturnEventsBetweenWrestlers() throws Exception {
    Wrestler wrestler1 = createTestWrestler("Between Wrestler 1", 50_000L);
    Wrestler wrestler2 = createTestWrestler("Between Wrestler 2", 50_000L);

    // Create an event between these two wrestlers
    dramaEventService.createDramaEvent(
        wrestler1.getId(),
        wrestler2.getId(),
        DramaEventType.BACKSTAGE_INCIDENT,
        DramaEventSeverity.NEGATIVE,
        "Backstage Fight",
        "They had a confrontation",
        defaultUniverse.getId());

    mockMvc
        .perform(
            get(
                "/api/drama-events/between/{wrestler1Id}/{wrestler2Id}",
                wrestler1.getId(),
                wrestler2.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  @DisplayName("POST /api/drama-events/process should process unprocessed events and return 200")
  void shouldProcessUnprocessedEvents() throws Exception {
    mockMvc
        .perform(post("/api/drama-events/process"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").exists());
  }
}
