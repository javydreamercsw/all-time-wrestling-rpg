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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractRestControllerIT;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningAiService;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningService;
import java.time.LocalDate;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for ShowPlanningController REST endpoints. */
@DisplayName("ShowPlanningController Integration Tests")
@Transactional
class ShowPlanningControllerIT extends AbstractRestControllerIT {

  @Autowired private ShowPlanningService showPlanningService;
  @Autowired private ShowPlanningAiService showPlanningAiService;
  @Autowired private ShowService showService;

  @BeforeEach
  public void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new ShowPlanningController(showPlanningService, showPlanningAiService, showService))
            .build();
  }

  private Show createTestShow(final String name) {
    ShowType showType = new ShowType();
    showType.setName("Planning IT ShowType " + name);
    showType.setDescription("Planning IT ShowType Description");
    showTypeRepository.saveAndFlush(showType);

    Show show = new Show();
    show.setName(name);
    show.setDescription("Planning IT Show Description");
    show.setShowDate(LocalDate.now().plusDays(7));
    show.setType(showType);
    show.setUniverse(defaultUniverse);
    return showRepository.saveAndFlush(show);
  }

  @Test
  @DisplayName("GET /api/show-planning/context/{showId} with valid show returns 200")
  void getShowPlanningContext_ValidShowId_Returns200() throws Exception {
    Show show = createTestShow("Monday Night RAW");

    mockMvc
        .perform(get("/api/show-planning/context/{showId}", show.getId()))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /api/show-planning/context/{showId} with invalid show returns 404")
  void getShowPlanningContext_InvalidShowId_Returns404() throws Exception {
    mockMvc
        .perform(get("/api/show-planning/context/{showId}", 999_999L))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("POST /api/show-planning/approve/{showId} with empty segment list returns 200")
  void approveSegments_EmptyList_Returns200() throws Exception {
    Show show = createTestShow("SmackDown");

    mockMvc
        .perform(
            post("/api/show-planning/approve/{showId}", show.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Collections.emptyList())))
        .andExpect(status().isOk());
  }
}
