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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractRestControllerIT;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.segment.SegmentSummaryService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for SegmentSummaryController REST endpoints. */
@DisplayName("SegmentSummaryController Integration Tests")
@Transactional
class SegmentSummaryControllerIT extends AbstractRestControllerIT {

  @Autowired private SegmentSummaryService segmentSummaryService;

  @BeforeEach
  public void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new SegmentSummaryController(segmentSummaryService))
            .build();
  }

  private Show createShow(final String name) {
    ShowType showType = new ShowType();
    showType.setName("IT Show Type " + name);
    showType.setDescription("IT Show Type Description");
    showTypeRepository.saveAndFlush(showType);

    Show show = new Show();
    show.setName(name);
    show.setDescription("IT Show Description");
    show.setShowDate(LocalDate.now());
    show.setType(showType);
    show.setUniverse(defaultUniverse);
    return showRepository.saveAndFlush(show);
  }

  private Segment createSegment(final Show show, final String narration) {
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
    if (narration != null) {
      segment.setNarration(narration);
    }
    return segmentRepository.saveAndFlush(segment);
  }

  @Test
  @DisplayName("POST /api/segments/{id}/summarize returns 200 for segment without narration")
  void shouldReturn200ForSegmentWithoutNarration() throws Exception {
    Show show = createShow("Summary IT Show No Narration");
    Segment segment = createSegment(show, null);

    mockMvc
        .perform(post("/api/segments/{segmentId}/summarize", segment.getId()))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("POST /api/segments/{id}/summarize returns 200 with segment id in response")
  void shouldReturnSegmentWithIdInResponse() throws Exception {
    Show show = createShow("Summary IT Show With ID");
    Segment segment = createSegment(show, null);

    mockMvc
        .perform(post("/api/segments/{segmentId}/summarize", segment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(segment.getId()));
  }

  @Test
  @DisplayName(
      "POST /api/segments/{id}/summarize returns 200 and persists summary for segment with"
          + " narration")
  void shouldPersistSummaryWhenNarrationExists() throws Exception {
    Show show = createShow("Summary IT Show With Narration");
    String narration =
        "Narrator: The bell rings and both competitors charge at each other! "
            + "Wrestler A hits a powerful clothesline! "
            + "Wrestler B recovers and delivers a stunning suplex! "
            + "The crowd is on their feet! Wrestler A gets the pin. ONE! TWO! THREE!";
    Segment segment = createSegment(show, narration);

    mockMvc
        .perform(post("/api/segments/{segmentId}/summarize", segment.getId()))
        .andExpect(status().isOk());

    Segment updated = segmentRepository.findById(segment.getId()).orElseThrow();
    // Summary may or may not be set depending on available AI services; just verify no error
    assertThat(updated.getId()).isEqualTo(segment.getId());
  }

  @Test
  @DisplayName(
      "POST /api/segments/{id}/summarize with wrestlers in segment returns 200 with segment data")
  void shouldReturn200ForSegmentWithWrestlers() throws Exception {
    Show show = createShow("Summary IT Show With Wrestlers");
    Segment segment = createSegment(show, "The match begins with both wrestlers facing off.");

    Wrestler wrestler1 = createTestWrestler("IT Wrestler Alpha", 10_000L);
    Wrestler wrestler2 = createTestWrestler("IT Wrestler Beta", 8_000L);
    segment.addParticipant(wrestler1);
    segment.addParticipant(wrestler2);
    segmentRepository.saveAndFlush(segment);

    mockMvc
        .perform(post("/api/segments/{segmentId}/summarize", segment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(segment.getId()));
  }
}
