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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractRestControllerIT;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for SegmentController REST endpoints. */
@DisplayName("SegmentController Integration Tests")
@Transactional
class SegmentControllerIT extends AbstractRestControllerIT {

  @Autowired private SegmentService segmentService;

  @BeforeEach
  public void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new SegmentController(segmentService)).build();
  }

  private Show createShow(final String name) {
    ShowType showType = new ShowType();
    showType.setName("IT ShowType " + name);
    showType.setDescription("IT ShowType Description");
    showTypeRepository.saveAndFlush(showType);

    Show show = new Show();
    show.setName(name);
    show.setDescription("IT Show Description");
    show.setShowDate(LocalDate.now());
    show.setType(showType);
    show.setUniverse(defaultUniverse);
    return showRepository.saveAndFlush(show);
  }

  private Segment createSegment(final Show show) {
    SegmentType segmentType =
        segmentTypeRepository
            .findByName("IT Match")
            .orElseGet(
                () -> {
                  SegmentType st = new SegmentType();
                  st.setName("IT Match");
                  return segmentTypeRepository.saveAndFlush(st);
                });

    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(segmentType);
    return segmentRepository.saveAndFlush(segment);
  }

  @Test
  @DisplayName(
      "PUT /api/segments/{id}/narration returns 200 and persists narration for existing segment")
  void shouldUpdateNarrationForExistingSegment() throws Exception {
    Show show = createShow("Narration IT Show");
    Segment segment = createSegment(show);
    String expectedNarration = "The champion delivers a stunning finisher to close out the show!";

    mockMvc
        .perform(
            put("/api/segments/{segmentId}/narration", segment.getId())
                .contentType(MediaType.TEXT_PLAIN)
                .content(expectedNarration))
        .andExpect(status().isOk());

    Segment updated = segmentRepository.findById(segment.getId()).orElseThrow();
    assertThat(updated.getNarration()).isEqualTo(expectedNarration);
  }

  @Test
  @DisplayName("PUT /api/segments/{id}/narration returns 200 silently when segment does not exist")
  void shouldReturn200WhenSegmentDoesNotExist() throws Exception {
    mockMvc
        .perform(
            put("/api/segments/{segmentId}/narration", -999L)
                .contentType(MediaType.TEXT_PLAIN)
                .content("This narration will be ignored"))
        .andExpect(status().isOk());
  }
}
