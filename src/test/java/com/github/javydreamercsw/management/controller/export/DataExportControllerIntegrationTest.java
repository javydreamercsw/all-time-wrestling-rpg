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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.dto.ShowDTO;
import com.github.javydreamercsw.management.dto.ShowTemplateDTO;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(
    controllers = DataExportController.class,
    excludeAutoConfiguration = {DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class})
class DataExportControllerIntegrationTest extends AbstractControllerTest {

  @MockitoBean private ShowTemplateRepository showTemplateRepository;
  @MockitoBean private ShowTypeRepository showTypeRepository;
  @MockitoBean private SeasonRepository seasonRepository;

  @MockitoBean private ShowService showService;
  @MockitoBean private ShowTemplateService showTemplateService;
  @MockitoBean private ShowRepository showRepository;

  private Show testShow;
  private ShowTemplate testShowTemplate;
  private ShowType testShowType;
  private Season testSeason;

  @BeforeEach
  void setUp() throws IOException {
    // Clear the exports directory
    Path exportsDir = Paths.get("target/exports");
    if (Files.exists(exportsDir)) {
      Files.walk(exportsDir)
          .sorted(Comparator.reverseOrder())
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (IOException e) {
                  // Ignore
                }
              });
    }
    Files.createDirectories(exportsDir);

    // Reset mocks for a clean slate for each test
    reset(
        showTemplateRepository,
        showTypeRepository,
        seasonRepository,
        showService,
        showTemplateService,
        showRepository);

    // --- Setup common test data ---
    testShowType = mock(ShowType.class);
    when(testShowType.getId()).thenReturn(1L);
    when(testShowType.getName()).thenReturn("Integration Test Show Type");
    when(testShowType.getDescription()).thenReturn("Integration test description");

    testSeason = mock(Season.class);
    when(testSeason.getId()).thenReturn(1L);
    when(testSeason.getName()).thenReturn("Integration Test Season");
    when(testSeason.getDescription()).thenReturn("Integration test description");
    when(testSeason.getShowsPerPpv()).thenReturn(5);
    when(testSeason.getIsActive()).thenReturn(true);

    testShow = mock(Show.class);
    when(testShow.getId()).thenReturn(1L);
    when(testShow.getName()).thenReturn("Integration Test Show");
    when(testShow.getDescription()).thenReturn("Integration test description");
    when(testShow.getType()).thenReturn(testShowType);
    when(testShow.getShowDate()).thenReturn(LocalDate.of(2024, 6, 15));
    when(testShow.getSeason()).thenReturn(testSeason);
    when(testShow.getExternalId()).thenReturn("integration-test-123");

    testShowTemplate = mock(ShowTemplate.class);
    when(testShowTemplate.getId()).thenReturn(1L);
    when(testShowTemplate.getName()).thenReturn("Integration Test Template");
    when(testShowTemplate.getDescription()).thenReturn("Integration test template description");
    when(testShowTemplate.getShowType()).thenReturn(testShowType);
    when(testShowTemplate.getNotionUrl()).thenReturn("https://notion.so/integration-test");
    when(testShowTemplate.getExternalId()).thenReturn("template-integration-456");

    // --- Mock repository and service behavior ---
    // Mock save methods to return the entity (important for services that return saved entities)
    when(showTypeRepository.save(any(ShowType.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(seasonRepository.save(any(Season.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(showRepository.save(any(Show.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(showTemplateRepository.save(any(ShowTemplate.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Mock findById for repositories
    when(showTypeRepository.findById(1L)).thenReturn(Optional.of(testShowType));
    when(seasonRepository.findById(1L)).thenReturn(Optional.of(testSeason));
    when(showTemplateRepository.findById(1L)).thenReturn(Optional.of(testShowTemplate));

    // Mock service findAll methods to return our test data
    when(showService.findAllWithRelationships()).thenReturn(List.of(testShow));
    when(showTemplateService.findAll()).thenReturn(List.of(testShowTemplate));

    // Mock save for services - these are the ones called in the test @BeforeEach (implicitly by
    // controller, but we need to mock it)
    when(showService.save(any(Show.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(showTemplateService.save(any(ShowTemplate.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void exportShows_CreatesFileWithCorrectContent() throws Exception {
    // Act
    mockMvc
        .perform(post("/api/export/shows").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("Successfully exported 1 shows")))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("target/exports/shows.json")));

    // Assert - Check file was created and has correct content
    Path exportedFile = Paths.get("target/exports/shows.json");
    assertTrue(Files.exists(exportedFile), "Exported shows.json file should exist");

    String fileContent = Files.readString(exportedFile);
    List<ShowDTO> exportedShows = objectMapper.readValue(fileContent, new TypeReference<>() {});

    assertEquals(1, exportedShows.size());
    ShowDTO exportedShow = exportedShows.get(0);
    assertEquals("Integration Test Show", exportedShow.getName());
    assertEquals("Integration test description", exportedShow.getDescription());
    assertEquals("Integration Test Show Type", exportedShow.getShowType());
    assertEquals("2024-06-15", exportedShow.getShowDate());
    assertEquals("Integration Test Season", exportedShow.getSeasonName());
    assertEquals("integration-test-123", exportedShow.getExternalId());
    assertNull(exportedShow.getTemplateName()); // No template assigned to show
  }

  @Test
  void exportShowTemplates_CreatesFileWithCorrectContent() throws Exception {
    // Act
    mockMvc
        .perform(post("/api/export/show-templates").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString("Successfully exported 1 show templates")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString("target/exports/show_templates.json")));

    // Assert - Check file was created and has correct content
    Path exportedFile = Paths.get("target/exports/show_templates.json");
    assertTrue(Files.exists(exportedFile), "Exported show_templates.json file should exist");

    String fileContent = Files.readString(exportedFile);
    List<ShowTemplateDTO> exportedTemplates =
        objectMapper.readValue(fileContent, new TypeReference<List<ShowTemplateDTO>>() {});

    assertEquals(1, exportedTemplates.size());
    ShowTemplateDTO exportedTemplate = exportedTemplates.get(0);
    assertEquals("Integration Test Template", exportedTemplate.getName());
    assertEquals("Integration test template description", exportedTemplate.getDescription());
    assertEquals("Integration Test Show Type", exportedTemplate.getShowTypeName());
    assertEquals("https://notion.so/integration-test", exportedTemplate.getNotionUrl());
    assertEquals("template-integration-456", exportedTemplate.getExternalId());
  }

  @Test
  void exportAll_CreatesBothFiles() throws Exception {
    // Act
    mockMvc
        .perform(post("/api/export/all").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "Successfully exported all data to target/exports/ directory in")));

    // Assert - Check both files were created
    Path showsFile = Paths.get("target/exports/shows.json");
    Path templatesFile = Paths.get("target/exports/show_templates.json");

    assertTrue(Files.exists(showsFile), "Exported shows.json file should exist");
    assertTrue(Files.exists(templatesFile), "Exported show_templates.json file should exist");

    // Verify content of both files
    String showsContent = Files.readString(showsFile);
    List<ShowDTO> exportedShows = objectMapper.readValue(showsContent, new TypeReference<>() {});
    assertEquals(1, exportedShows.size());

    String templatesContent = Files.readString(templatesFile);
    List<ShowTemplateDTO> exportedTemplates =
        objectMapper.readValue(templatesContent, new TypeReference<>() {});
    assertEquals(1, exportedTemplates.size());
  }

  @Test
  void exportShows_EmptyDatabase_CreatesEmptyFile() throws Exception {
    // Arrange - Clear all shows and mock service to return empty list
    showRepository.deleteAll();
    when(showService.findAllWithRelationships()).thenReturn(List.of());

    // Act
    mockMvc
        .perform(post("/api/export/shows").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(org.hamcrest.Matchers.containsString("Successfully exported 0 shows")));

    // Assert - Check file was created with empty array
    Path exportedFile = Paths.get("target/exports/shows.json");
    assertTrue(Files.exists(exportedFile), "Exported shows.json file should exist");

    String fileContent = Files.readString(exportedFile);
    List<ShowDTO> exportedShows =
        objectMapper.readValue(fileContent, new TypeReference<List<ShowDTO>>() {});
    assertEquals(0, exportedShows.size());
  }

  @Test
  void exportShowTemplates_EmptyDatabase_CreatesEmptyFile() throws Exception {
    // Arrange - Clear all show templates and mock service to return empty list
    showTemplateRepository.deleteAll();
    when(showTemplateService.findAll()).thenReturn(List.of());

    // Act
    mockMvc
        .perform(post("/api/export/show-templates").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "Successfully exported 0 show templates")));

    // Assert - Check file was created with empty array
    Path exportedFile = Paths.get("target/exports/show_templates.json");
    assertTrue(Files.exists(exportedFile), "Exported show_templates.json file should exist");

    String fileContent = Files.readString(exportedFile);
    List<ShowTemplateDTO> exportedTemplates =
        objectMapper.readValue(fileContent, new TypeReference<List<ShowTemplateDTO>>() {});
    assertEquals(0, exportedTemplates.size());
  }

  @Test
  void exportShows_CreatesExportsDirectoryIfNotExists() throws Exception {
    // Arrange - Ensure exports directory doesn't exist
    Path exportsDir = Paths.get("target/exports");
    if (Files.exists(exportsDir)) {
      Files.walk(exportsDir)
          .sorted(Comparator.reverseOrder())
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (IOException e) {
                  // Ignore
                }
              });
    }
    assertFalse(Files.exists(exportsDir), "Exports directory should not exist initially");

    // Act
    mockMvc
        .perform(post("/api/export/shows").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    // Assert - Directory should be created
    assertTrue(Files.exists(exportsDir), "Exports directory should be created");
    assertTrue(Files.isDirectory(exportsDir), "Exports path should be a directory");

    Path showsFile = exportsDir.resolve("shows.json");
    assertTrue(Files.exists(showsFile), "Shows file should exist in exports directory");
  }
}
