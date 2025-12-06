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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.test.BaseControllerTest;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = ShowTemplateController.class,
    excludeAutoConfiguration = {DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class})
@DisplayName("ShowTemplate Controller Tests")
class ShowTemplateControllerTest extends BaseControllerTest {

  @MockitoBean private CommandLineRunner commandLineRunner;

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private ShowTemplateService showTemplateService;

  private ShowTemplate testTemplate;
  private ShowType testShowType;

  @BeforeEach
  void setUp() {
    testShowType = new ShowType();
    testShowType.setName("PLE");

    testTemplate = new ShowTemplate();
    testTemplate.setName("Test Template");
    testTemplate.setDescription("Test Description");
    testTemplate.setShowType(testShowType);
    testTemplate.setNotionUrl("https://notion.so/test");
    testTemplate.setCreationDate(Instant.now());
  }

  @Test
  void getAllShowTemplates_ShouldReturnPagedResults() throws Exception {
    // Given
    List<ShowTemplate> templates = Arrays.asList(testTemplate);
    Page<ShowTemplate> page = new PageImpl<>(templates, PageRequest.of(0, 20), 1);
    when(showTemplateService.getAllTemplates(any())).thenReturn(page);

    // When & Then
    mockMvc
        .perform(get("/api/show-templates"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].name").value("Test Template"))
        .andExpect(jsonPath("$.content[0].description").value("Test Description"));
  }

  @Test
  void getShowTemplateById_WhenExists_ShouldReturnTemplate() throws Exception {
    // Given
    when(showTemplateService.getTemplateById(1L)).thenReturn(Optional.of(testTemplate));

    // When & Then
    mockMvc
        .perform(get("/api/show-templates/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Template"))
        .andExpect(jsonPath("$.description").value("Test Description"));
  }

  @Test
  void getShowTemplateById_WhenNotExists_ShouldReturn404() throws Exception {
    // Given
    when(showTemplateService.getTemplateById(999L)).thenReturn(Optional.empty());

    // When & Then
    mockMvc.perform(get("/api/show-templates/999")).andExpect(status().isNotFound());
  }

  @Test
  void getShowTemplatesByShowType_ShouldReturnTemplates() throws Exception {
    // Given
    List<ShowTemplate> templates = Arrays.asList(testTemplate);
    when(showTemplateService.getTemplatesByShowType("PLE")).thenReturn(templates);

    // When & Then
    mockMvc
        .perform(get("/api/show-templates/by-show-type/PLE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].name").value("Test Template"));
  }

  @Test
  void getPremiumLiveEventTemplates_ShouldReturnPLETemplates() throws Exception {
    // Given
    List<ShowTemplate> templates = Arrays.asList(testTemplate);
    when(showTemplateService.getPremiumLiveEventTemplates()).thenReturn(templates);

    // When & Then
    mockMvc
        .perform(get("/api/show-templates/ple"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].name").value("Test Template"));
  }

  @Test
  void getWeeklyShowTemplates_ShouldReturnWeeklyTemplates() throws Exception {
    // Given
    List<ShowTemplate> templates = Arrays.asList(testTemplate);
    when(showTemplateService.getWeeklyShowTemplates()).thenReturn(templates);

    // When & Then
    mockMvc
        .perform(get("/api/show-templates/weekly"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].name").value("Test Template"));
  }

  @Test
  void createShowTemplate_WithValidData_ShouldReturnCreated() throws Exception {
    // Given
    ShowTemplateController.CreateShowTemplateRequest request =
        new ShowTemplateController.CreateShowTemplateRequest(
            "New Template", "New Description", "PLE", "https://notion.so/new");

    when(showTemplateService.createOrUpdateTemplate(
            anyString(), anyString(), anyString(), anyString()))
        .thenReturn(testTemplate);

    // When & Then
    mockMvc
        .perform(
            post("/api/show-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Test Template"));
  }

  @Test
  void createShowTemplate_WhenServiceReturnsNull_ShouldReturnBadRequest() throws Exception {
    // Given
    ShowTemplateController.CreateShowTemplateRequest request =
        new ShowTemplateController.CreateShowTemplateRequest(
            "New Template", "New Description", "InvalidType", "https://notion.so/new");

    when(showTemplateService.createOrUpdateTemplate(
            anyString(), anyString(), anyString(), anyString()))
        .thenReturn(null);

    // When & Then
    mockMvc
        .perform(
            post("/api/show-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateShowTemplate_WhenExists_ShouldReturnUpdated() throws Exception {
    // Given
    ShowTemplateController.UpdateShowTemplateRequest request =
        new ShowTemplateController.UpdateShowTemplateRequest(
            "Updated Template", "Updated Description", "PLE", "https://notion.so/updated");

    when(showTemplateService.updateTemplate(
            anyLong(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Optional.of(testTemplate));

    // When & Then
    mockMvc
        .perform(
            put("/api/show-templates/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Template"));
  }

  @Test
  void updateShowTemplate_WhenNotExists_ShouldReturn404() throws Exception {
    // Given
    ShowTemplateController.UpdateShowTemplateRequest request =
        new ShowTemplateController.UpdateShowTemplateRequest(
            "Updated Template", "Updated Description", "PLE", "https://notion.so/updated");

    when(showTemplateService.updateTemplate(
            anyLong(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Optional.empty());

    // When & Then
    mockMvc
        .perform(
            put("/api/show-templates/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteShowTemplate_WhenExists_ShouldReturnNoContent() throws Exception {
    // Given
    when(showTemplateService.deleteTemplate(1L)).thenReturn(true);

    // When & Then
    mockMvc.perform(delete("/api/show-templates/1")).andExpect(status().isNoContent());
  }

  @Test
  void deleteShowTemplate_WhenNotExists_ShouldReturn404() throws Exception {
    // Given
    when(showTemplateService.deleteTemplate(999L)).thenReturn(false);

    // When & Then
    mockMvc.perform(delete("/api/show-templates/999")).andExpect(status().isNotFound());
  }
}
