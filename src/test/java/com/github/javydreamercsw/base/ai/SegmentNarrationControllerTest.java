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
package com.github.javydreamercsw.base.ai;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentTypeContext;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory.ServiceInfo;
import com.github.javydreamercsw.base.service.segment.SegmentOutcomeProvider;
import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.service.world.ArenaService;
import com.github.javydreamercsw.management.service.world.LocationService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WithMockUser(roles = {"ADMIN", "BOOKER"})
class SegmentNarrationControllerTest extends AbstractControllerTest {

  @MockitoBean private SegmentOutcomeProvider segmentOutcomeProvider;
  @MockitoBean private SegmentNarrationServiceFactory serviceFactory;
  @MockitoBean private ArenaService arenaService;
  @MockitoBean private LocationService locationService;

  @Test
  void testNarrateMatch() throws Exception {
    // Given
    SegmentNarrationService service = mock(SegmentNarrationService.class);
    when(serviceFactory.getServiceByProvider("Mock AI")).thenReturn(service);
    when(service.narrateSegment(any(SegmentNarrationContext.class))).thenReturn("Test narration");
    when(segmentOutcomeProvider.determineOutcomeIfNeeded(any(SegmentNarrationContext.class)))
        .thenAnswer(i -> i.getArguments()[0]);

    SegmentNarrationContext context = new SegmentNarrationContext();
    context.setSegmentType(new SegmentNarrationService.SegmentTypeContext());
    context.setWrestlers(new ArrayList<>());

    // When & Then
    mockMvc
        .perform(
            post("/api/segment-narration/narrate/Mock AI")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(context)))
        .andExpect(status().isOk());
  }

  @Test
  void testTestSpecificProvider() throws Exception {
    // Given
    SegmentNarrationService service = mock(SegmentNarrationService.class);
    when(serviceFactory.getServiceByProvider("Mock AI")).thenReturn(service);
    when(service.narrateSegment(any(SegmentNarrationContext.class))).thenReturn("Test narration");
    when(service.getProviderName()).thenReturn("Mock AI");

    when(arenaService.findAll()).thenReturn(Collections.emptyList());
    when(locationService.findAll()).thenReturn(Collections.emptyList());

    // When & Then
    mockMvc
        .perform(post("/api/segment-narration/test/Mock AI").with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  void testTestSpecificProviderWithData() throws Exception {
    // Given
    SegmentNarrationService service = mock(SegmentNarrationService.class);
    when(serviceFactory.getServiceByProvider("Mock AI")).thenReturn(service);
    when(service.narrateSegment(any(SegmentNarrationContext.class))).thenReturn("Test narration");
    when(service.getProviderName()).thenReturn("Mock AI");

    Location location = new Location();
    location.setName("NYC");
    location.setCulturalTags(Collections.singleton("Urban"));

    Arena arena = new Arena();
    arena.setName("MSG");
    arena.setDescription("Mecca");
    arena.setLocation(location);
    arena.setCapacity(20000);
    arena.setAlignmentBias(Arena.AlignmentBias.NEUTRAL);
    arena.setEnvironmentalTraits(Collections.singleton("Historic"));

    when(arenaService.findAll()).thenReturn(Collections.singletonList(arena));
    when(locationService.findAll()).thenReturn(Collections.singletonList(location));

    // When & Then
    mockMvc
        .perform(post("/api/segment-narration/test/Mock AI").with(csrf()))
        .andExpect(status().isOk());
  }

  // ---- GET /api/segment-narration/limits ----

  @Test
  void testGetLimitsNoServiceAvailable() throws Exception {
    // Given: no services — in test profile the controller calls getTestingService()
    when(serviceFactory.getTestingService()).thenReturn(null);
    when(serviceFactory.getAvailableServices()).thenReturn(Collections.emptyList());

    // When & Then
    mockMvc
        .perform(get("/api/segment-narration/limits").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentProvider", is("None Available")))
        .andExpect(jsonPath("$.available", is(false)));
  }

  @Test
  void testGetLimitsWithServiceAvailable() throws Exception {
    // Given: a mock service is returned — in test profile the controller calls getTestingService()
    SegmentNarrationService service = mock(SegmentNarrationService.class);
    when(service.getProviderName()).thenReturn("Mock AI");
    when(serviceFactory.getTestingService()).thenReturn(service);
    when(serviceFactory.getAvailableServices())
        .thenReturn(
            List.of(new ServiceInfo("Mock AI", true, "MockAIService", 10, 0.0, "FREE", "Mock")));

    // When & Then
    mockMvc
        .perform(get("/api/segment-narration/limits").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentProvider", is("Mock AI")))
        .andExpect(jsonPath("$.available", is(true)));
  }

  // ---- POST /api/segment-narration/narrate (no provider) ----

  @Test
  void testNarrateSegmentNoProviderSuccess() throws Exception {
    // Given — in test profile getAppropriateService() calls getTestingService()
    SegmentNarrationService service = mock(SegmentNarrationService.class);
    when(service.getProviderName()).thenReturn("Mock AI");
    when(service.narrateSegment(any(SegmentNarrationContext.class))).thenReturn("Narration text");
    when(serviceFactory.getTestingService()).thenReturn(service);
    when(segmentOutcomeProvider.determineOutcomeIfNeeded(any(SegmentNarrationContext.class)))
        .thenAnswer(i -> i.getArguments()[0]);
    when(serviceFactory.getEstimatedSegmentCost("Mock AI")).thenReturn(0.0);

    SegmentNarrationContext context = new SegmentNarrationContext();
    context.setSegmentType(new SegmentTypeContext());
    context.setWrestlers(new ArrayList<>());

    // When & Then
    mockMvc
        .perform(
            post("/api/segment-narration/narrate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(context)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.narration", is("Narration text")))
        .andExpect(jsonPath("$.provider", is("Mock AI")));
  }

  @Test
  void testNarrateSegmentNoProviderNoServiceAvailable() throws Exception {
    // Given: no service available — in test profile getAppropriateService() calls
    // getTestingService()
    when(serviceFactory.getTestingService()).thenReturn(null);
    when(segmentOutcomeProvider.determineOutcomeIfNeeded(any(SegmentNarrationContext.class)))
        .thenAnswer(i -> i.getArguments()[0]);

    SegmentNarrationContext context = new SegmentNarrationContext();
    context.setSegmentType(new SegmentTypeContext());
    context.setWrestlers(new ArrayList<>());

    // When & Then
    mockMvc
        .perform(
            post("/api/segment-narration/narrate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(context)))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error", is("No AI provider is available.")));
  }

  // ---- POST /api/segment-narration/narrate/{provider} ----

  @Test
  void testNarrateWithProviderNotFound() throws Exception {
    // Given: provider lookup returns null
    when(serviceFactory.getServiceByProvider("Unknown")).thenReturn(null);
    when(segmentOutcomeProvider.determineOutcomeIfNeeded(any(SegmentNarrationContext.class)))
        .thenAnswer(i -> i.getArguments()[0]);

    SegmentNarrationContext context = new SegmentNarrationContext();
    context.setSegmentType(new SegmentTypeContext());
    context.setWrestlers(new ArrayList<>());

    // When & Then
    mockMvc
        .perform(
            post("/api/segment-narration/narrate/Unknown")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(context)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error", is("Provider 'Unknown' not available or not found")));
  }

  @Test
  void testNarrateWithProviderAIServiceException() throws Exception {
    // Given: service throws AIServiceException
    SegmentNarrationService service = mock(SegmentNarrationService.class);
    when(serviceFactory.getServiceByProvider("Mock AI")).thenReturn(service);
    when(segmentOutcomeProvider.determineOutcomeIfNeeded(any(SegmentNarrationContext.class)))
        .thenAnswer(i -> i.getArguments()[0]);
    when(service.narrateSegment(any(SegmentNarrationContext.class)))
        .thenThrow(new AIServiceException(429, "Too Many Requests", "Mock AI", "Rate limit hit"));
    when(serviceFactory.getAvailableServices()).thenReturn(Collections.emptyList());

    SegmentNarrationContext context = new SegmentNarrationContext();
    context.setSegmentType(new SegmentTypeContext());
    context.setWrestlers(new ArrayList<>());

    // When & Then
    mockMvc
        .perform(
            post("/api/segment-narration/narrate/Mock AI")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(context)))
        .andExpect(status().is(429))
        .andExpect(jsonPath("$.provider", is("Mock AI")))
        .andExpect(jsonPath("$.statusCode", is(429)));
  }

  // ---- POST /api/segment-narration/test/{provider} ----

  @Test
  void testTestSpecificProviderNotFound() throws Exception {
    // Given: provider lookup returns null
    when(serviceFactory.getServiceByProvider("Unknown")).thenReturn(null);

    // When & Then
    mockMvc
        .perform(post("/api/segment-narration/test/Unknown").with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error", is("Provider 'Unknown' not available or not found")));
  }

  @Test
  void testTestSpecificProviderAIServiceException() throws Exception {
    // Given: service throws AIServiceException
    SegmentNarrationService service = mock(SegmentNarrationService.class);
    when(serviceFactory.getServiceByProvider("Mock AI")).thenReturn(service);
    when(service.getProviderName()).thenReturn("Mock AI");
    when(service.narrateSegment(any(SegmentNarrationContext.class)))
        .thenThrow(new AIServiceException(503, "Service Unavailable", "Mock AI", "Upstream error"));
    when(serviceFactory.getAvailableServices()).thenReturn(Collections.emptyList());
    when(arenaService.findAll()).thenReturn(Collections.emptyList());
    when(locationService.findAll()).thenReturn(Collections.emptyList());

    // When & Then
    mockMvc
        .perform(post("/api/segment-narration/test/Mock AI").with(csrf()))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.provider", is("Mock AI")))
        .andExpect(jsonPath("$.statusCode", is(503)));
  }
}
