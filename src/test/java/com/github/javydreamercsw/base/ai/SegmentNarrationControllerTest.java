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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;
import com.github.javydreamercsw.base.service.ranking.RankingService;
import com.github.javydreamercsw.base.service.segment.SegmentOutcomeProvider;
import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@EnableConfigurationProperties(SegmentNarrationConfig.class)
@WithMockUser(roles = {"ADMIN", "BOOKER"})
class SegmentNarrationControllerTest extends AbstractControllerTest {
  @MockitoBean private SegmentNarrationServiceFactory serviceFactory;
  @MockitoBean private SegmentOutcomeProvider matchOutcomeService;
  @MockitoBean private RankingService rankingService;

  @Test
  void testNarrateMatch() throws Exception {
    // Given
    SegmentNarrationService service = mock(SegmentNarrationService.class);
    when(serviceFactory.getServiceByProvider("Mock AI")).thenReturn(service);
    when(service.narrateSegment(any(SegmentNarrationContext.class))).thenReturn("Test narration");
    when(matchOutcomeService.determineOutcomeIfNeeded(any(SegmentNarrationContext.class)))
        .thenAnswer(i -> i.getArguments()[0]);

    SegmentNarrationContext context = new SegmentNarrationContext();
    context.setSegmentType(new SegmentNarrationService.SegmentTypeContext());

    // When & Then
    mockMvc
        .perform(
            post("/api/segment-narration/narrate/Mock AI")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(context)))
        .andExpect(status().isOk());
  }
}
