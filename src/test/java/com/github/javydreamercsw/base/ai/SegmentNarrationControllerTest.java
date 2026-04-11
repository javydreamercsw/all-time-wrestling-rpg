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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;
import com.github.javydreamercsw.base.service.segment.SegmentOutcomeProvider;
import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.service.world.ArenaService;
import com.github.javydreamercsw.management.service.world.LocationService;
import java.util.ArrayList;
import java.util.Collections;
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
}
