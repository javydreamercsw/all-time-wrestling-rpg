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
package com.github.javydreamercsw.management.controller.segment;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.controller.rivalry.RivalryController;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.segment.SegmentSummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RivalryController.class)
class SegmentSummaryControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private SegmentSummaryService segmentSummaryService;

  @Test
  void testSummarizeSegment() throws Exception {
    Long segmentId = 1L;
    Segment mockSegment = new Segment();
    mockSegment.setId(segmentId);
    mockSegment.setSummary("Test summary");
    when(segmentSummaryService.summarizeSegment(segmentId)).thenReturn(mockSegment);

    mockMvc
        .perform(post("/api/segments/{segmentId}/summarize", segmentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.summary", is("Test summary")));

    verify(segmentSummaryService, times(1)).summarizeSegment(segmentId);
  }

  @Test
  void testSummarizeSegmentNotFound() throws Exception {
    Long segmentId = 999L;
    when(segmentSummaryService.summarizeSegment(segmentId))
        .thenThrow(new IllegalArgumentException("Invalid segment Id:" + segmentId));

    mockMvc
        .perform(post("/api/segments/{segmentId}/summarize", segmentId))
        .andExpect(status().isBadRequest()); // or isNotFound() if you map to 404

    verify(segmentSummaryService, times(1)).summarizeSegment(segmentId);
  }
}
