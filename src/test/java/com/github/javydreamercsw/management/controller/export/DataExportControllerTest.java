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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WithMockUser
class DataExportControllerTest extends AbstractControllerTest {

  @MockitoBean private ShowService showService;
  @MockitoBean private ShowTemplateService showTemplateService;

  // ==================== exportShows ====================

  @Test
  void exportShows() throws Exception {
    when(showService.findAllWithRelationships()).thenReturn(new ArrayList<>());

    mockMvc
        .perform(post("/api/export/shows").with(csrf()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  void exportShows_serviceThrowsException_returns500() throws Exception {
    when(showService.findAllWithRelationships())
        .thenThrow(new RuntimeException("Database unavailable"));

    mockMvc
        .perform(post("/api/export/shows").with(csrf()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError())
        .andExpect(content().string(containsString("Failed to export shows")));
  }

  @Test
  void exportShows_withMultipleShows_responseBodyIsNonEmpty() throws Exception {
    Show show1 = new Show();
    show1.setName("Monday Night Raw");
    Show show2 = new Show();
    show2.setName("SmackDown");
    when(showService.findAllWithRelationships()).thenReturn(List.of(show1, show2));

    mockMvc
        .perform(post("/api/export/shows").with(csrf()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().string(not("")));
  }

  // ==================== exportShowTemplates ====================

  @Test
  void exportShowTemplates() throws Exception {
    when(showTemplateService.findAll()).thenReturn(new ArrayList<>());

    mockMvc
        .perform(
            post("/api/export/show-templates").with(csrf()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  void exportShowTemplates_serviceThrowsException_returns500() throws Exception {
    when(showTemplateService.findAll()).thenThrow(new RuntimeException("Template service failure"));

    mockMvc
        .perform(
            post("/api/export/show-templates").with(csrf()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError())
        .andExpect(content().string(containsString("Failed to export show templates")));
  }

  @Test
  void exportShowTemplates_withLargeList_responseBodyIsNonEmpty() throws Exception {
    List<ShowTemplate> templates = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      ShowTemplate template = new ShowTemplate();
      template.setName("Template " + i);
      templates.add(template);
    }
    when(showTemplateService.findAll()).thenReturn(templates);

    mockMvc
        .perform(
            post("/api/export/show-templates").with(csrf()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().string(not("")));
  }

  // ==================== exportAll ====================

  @Test
  void exportAll() throws Exception {
    when(showService.findAllWithRelationships()).thenReturn(new ArrayList<>());
    when(showTemplateService.findAll()).thenReturn(new ArrayList<>());

    mockMvc
        .perform(post("/api/export/all").with(csrf()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  void exportAll_showServiceFails_returns500() throws Exception {
    when(showService.findAllWithRelationships()).thenThrow(new RuntimeException("Shows DB error"));

    mockMvc
        .perform(post("/api/export/all").with(csrf()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError());
  }

  @Test
  void exportAll_templateServiceFails_returns500() throws Exception {
    when(showService.findAllWithRelationships()).thenReturn(new ArrayList<>());
    when(showTemplateService.findAll()).thenThrow(new RuntimeException("Templates DB error"));

    mockMvc
        .perform(post("/api/export/all").with(csrf()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError());
  }
}
