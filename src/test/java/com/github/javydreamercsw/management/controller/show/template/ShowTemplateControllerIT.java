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
package com.github.javydreamercsw.management.controller.show.template;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.controller.AbstractRestControllerIT;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for ShowTemplateController. */
@DisplayName("ShowTemplateController Integration Tests")
@Transactional
class ShowTemplateControllerIT extends AbstractRestControllerIT {

  private static final String SHOW_TYPE_NAME = "Weekly";
  private ShowType testShowType;

  @BeforeEach
  public void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ShowTemplateController(showTemplateService))
            .setCustomArgumentResolvers(
                new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
            .build();

    showTemplateRepository.deleteAll();
    showTemplateRepository.flush();

    testShowType = showTypeService.createOrUpdateShowType(SHOW_TYPE_NAME, "Weekly show", 5, 2);
  }

  private ShowTemplate createTemplate(final String name) {
    ShowTemplate template =
        showTemplateService.createOrUpdateTemplate(name, "Test description", SHOW_TYPE_NAME, null);
    showTemplateRepository.flush();
    return template;
  }

  @Test
  @DisplayName("GET /api/show-templates should return 200 with page")
  void shouldReturnPagedShowTemplates() throws Exception {
    createTemplate("Raw");
    createTemplate("SmackDown");

    mockMvc
        .perform(get("/api/show-templates"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  @DisplayName("GET /api/show-templates/{id} should return 404 when not found")
  void shouldReturn404WhenTemplateNotFound() throws Exception {
    mockMvc
        .perform(get("/api/show-templates/{id}", Long.MAX_VALUE))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET /api/show-templates/by-show-type/{showTypeName} should return 200 with list")
  void shouldReturnTemplatesByShowType() throws Exception {
    createTemplate("Raw");

    mockMvc
        .perform(get("/api/show-templates/by-show-type/{showTypeName}", SHOW_TYPE_NAME))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  @DisplayName("GET /api/show-templates/ple should return 200 with list")
  void shouldReturnPremiumLiveEventTemplates() throws Exception {
    mockMvc
        .perform(get("/api/show-templates/ple"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  @DisplayName("GET /api/show-templates/weekly should return 200 with list")
  void shouldReturnWeeklyShowTemplates() throws Exception {
    mockMvc
        .perform(get("/api/show-templates/weekly"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  @DisplayName("POST /api/show-templates should return 201 when created")
  void shouldCreateShowTemplate() throws Exception {
    ShowTemplateController.CreateShowTemplateRequest request =
        new ShowTemplateController.CreateShowTemplateRequest(
            "New Event", "New Description", SHOW_TYPE_NAME, null);

    mockMvc
        .perform(
            post("/api/show-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("New Event"));
  }

  @Test
  @DisplayName("PUT /api/show-templates/{id} should return 200 when found")
  void shouldUpdateShowTemplate() throws Exception {
    ShowTemplate template = createTemplate("Raw");

    ShowTemplateController.UpdateShowTemplateRequest request =
        new ShowTemplateController.UpdateShowTemplateRequest(
            "Updated Raw", "Updated description", SHOW_TYPE_NAME, null);

    mockMvc
        .perform(
            put("/api/show-templates/{id}", template.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Raw"));
  }

  @Test
  @DisplayName("DELETE /api/show-templates/{id} should return 204 when deleted")
  void shouldDeleteShowTemplate() throws Exception {
    ShowTemplate template = createTemplate("Raw");

    mockMvc
        .perform(delete("/api/show-templates/{id}", template.getId()))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("DELETE /api/show-templates/{id} should return 404 when not found")
  void shouldReturn404WhenDeletingNonExistentTemplate() throws Exception {
    mockMvc
        .perform(delete("/api/show-templates/{id}", Long.MAX_VALUE))
        .andExpect(status().isNotFound());
  }
}
