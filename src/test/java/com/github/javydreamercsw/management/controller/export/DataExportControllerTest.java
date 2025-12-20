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
package com.github.javydreamercsw.management.controller.export;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(DataExportController.class)
class DataExportControllerTest extends AbstractControllerTest {

  @MockitoBean private ShowService showService;
  @MockitoBean private ShowTemplateService showTemplateService;
  @MockitoBean private WrestlerRepository wrestlerRepository;

  @Test
  @WithMockUser
  void exportShows() throws Exception {
    when(showService.findAllWithRelationships()).thenReturn(new ArrayList<>());

    mockMvc
        .perform(post("/api/export/shows").with(csrf()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  void exportShowTemplates() throws Exception {
    when(showTemplateService.findAll()).thenReturn(new ArrayList<>());

    mockMvc
        .perform(
            post("/api/export/show-templates").with(csrf()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  void exportAll() throws Exception {
    when(showService.findAllWithRelationships()).thenReturn(new ArrayList<>());
    when(showTemplateService.findAll()).thenReturn(new ArrayList<>());

    mockMvc
        .perform(post("/api/export/all").with(csrf()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }
}
