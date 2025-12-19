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
package com.github.javydreamercsw.management.controller.title;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.service.title.TitleService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class TitleControllerTest extends AbstractControllerTest {

  @MockitoBean private TitleService titleService;

  @Test
  @WithMockUser(roles = "BOOKER")
  void createTitle() throws Exception {
    Title title = new Title();
    title.setId(1L);
    title.setName("Test Title");
    title.setTier(WrestlerTier.MIDCARDER);

    when(titleService.titleNameExists(any())).thenReturn(false);
    when(titleService.createTitle(any(), any(), any())).thenReturn(title);

    TitleController.CreateTitleRequest request =
        new TitleController.CreateTitleRequest("Test Title", "Description", WrestlerTier.MIDCARDER);

    mockMvc
        .perform(
            post("/api/titles")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Test Title"));
  }
}
