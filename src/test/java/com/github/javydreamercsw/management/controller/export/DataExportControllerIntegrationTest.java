package com.github.javydreamercsw.management.controller.export;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for DataExportController. Tests the full export functionality with real
 * database and file system operations.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DataExportControllerIntegrationTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTemplateRepository showTemplateRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SeasonRepository seasonRepository;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    showRepository.deleteAll();
    showTemplateRepository.deleteAll();
    showTypeRepository.deleteAll();
    seasonRepository.deleteAll();

    ShowType showType = new ShowType();
    showType.setName("Integration Test Show Type");
    showType.setDescription("Integration test description");
    showTypeRepository.save(showType);

    Season season = new Season();
    season.setName("Integration Test Season");
    season.setDescription("Integration test description");
    season.setShowsPerPpv(5);
    season.setIsActive(true);
    seasonRepository.save(season);

    Show show = new Show();
    show.setName("Integration Test Show");
    show.setDescription("Integration test description");
    show.setType(showType);
    show.setShowDate(LocalDate.of(2024, 6, 15));
    show.setSeason(season);
    show.setExternalId("integration-test-123");
    showRepository.save(show);

    ShowTemplate showTemplate = new ShowTemplate();
    showTemplate.setName("Integration Test Template");
    showTemplate.setDescription("Integration test template description");
    showTemplate.setShowType(showType);
    showTemplate.setNotionUrl("https://notion.so/integration-test");
    showTemplate.setExternalId("template-integration-456");
    showTemplateRepository.save(showTemplate);
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
                .string(org.hamcrest.Matchers.containsString("Successfully exported all data")))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("target/exports/ directory")));

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
    // Arrange - Clear all shows
    showRepository.deleteAll();

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
    // Arrange - Clear all show templates
    showTemplateRepository.deleteAll();

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
