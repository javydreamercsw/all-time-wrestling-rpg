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

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.planning.ProposedShow;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningAiService;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningService;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(ShowPlanningController.class)
class ShowPlanningControllerTest extends AbstractControllerTest {

  @MockitoBean private ShowPlanningService showPlanningService;

  @MockitoBean private ShowPlanningAiService showPlanningAiService;

  @MockitoBean private ShowService showService;
  @MockitoBean private WrestlerRepository wrestlerRepository;

  @Test
  void getShowPlanningContext() throws Exception {
    // Given
    Show show = new Show();
    show.setId(1L);
    when(showService.getShowById(1L)).thenReturn(Optional.of(show));
    when(showPlanningService.getShowPlanningContext(show)).thenReturn(new ShowPlanningContextDTO());

    // When & Then
    mockMvc.perform(get("/api/show-planning/context/1")).andExpect(status().isOk());
  }

  @Test
  void planShow() throws Exception {
    // Given
    ShowPlanningContextDTO context = new ShowPlanningContextDTO();
    when(showPlanningAiService.planShow(context)).thenReturn(new ProposedShow());

    // When & Then
    mockMvc
        .perform(
            post("/api/show-planning/plan")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(context)))
        .andExpect(status().isOk());
  }
}
