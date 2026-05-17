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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WithMockUser(authorities = {"ADMIN", "ROLE_ADMIN", "ROLE_BOOKER"})
class SegmentControllerTest extends AbstractControllerTest {

  @MockitoBean private SegmentService segmentService;

  private Segment testSegment;

  @BeforeEach
  void setUp() {
    testSegment = new Segment();
    testSegment.setId(1L);
  }

  @Test
  void updateNarration_segmentExists_returnsOk() throws Exception {
    Mockito.when(segmentService.findById(1L)).thenReturn(Optional.of(testSegment));
    Mockito.when(segmentService.updateSegment(testSegment)).thenReturn(testSegment);

    mockMvc
        .perform(
            put("/api/segments/1/narration")
                .with(csrf())
                .contentType(MediaType.TEXT_PLAIN)
                .content("This was an epic match!"))
        .andExpect(status().isOk());

    Mockito.verify(segmentService).findById(1L);
    Mockito.verify(segmentService).updateSegment(testSegment);
  }

  @Test
  void updateNarration_segmentNotFound_returnsOk() throws Exception {
    Mockito.when(segmentService.findById(99L)).thenReturn(Optional.empty());

    mockMvc
        .perform(
            put("/api/segments/99/narration")
                .with(csrf())
                .contentType(MediaType.TEXT_PLAIN)
                .content("Narration text"))
        .andExpect(status().isOk());

    Mockito.verify(segmentService).findById(99L);
    Mockito.verify(segmentService, Mockito.never()).updateSegment(Mockito.any());
  }
}
