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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.service.show.ShowService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(ShowController.class)
class ShowControllerTest extends AbstractControllerTest {

  @MockitoBean private ShowService showService;

  @Test
  void createShow() throws Exception {
    Show show = new Show();
    show.setId(1L);
    show.setName("Test Show");
    show.setShowDate(LocalDate.now());

    when(showService.createShow(any(), any(), any(), any(), any(), any())).thenReturn(show);

    ShowController.CreateShowRequest request =
        new ShowController.CreateShowRequest(
            "Test Show", "Description", 1L, LocalDate.now(), 1L, 1L);

    mockMvc
        .perform(
            post("/api/shows")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Test Show"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void testAdjudicateShow() throws Exception {
    mockMvc.perform(post("/api/shows/1/adjudicate").with(csrf())).andExpect(status().isOk());

    verify(showService, times(1)).adjudicateShow(1L);
  }
}
