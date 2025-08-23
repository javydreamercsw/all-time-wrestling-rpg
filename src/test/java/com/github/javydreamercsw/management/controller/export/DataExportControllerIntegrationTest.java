package com.github.javydreamercsw.management.controller.export;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.DataInitializer.ShowDTO;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.dto.ShowTemplateDTO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for DataExportController. Tests the full export functionality with real
 * database and file system operations.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DataExportControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ShowRepository showRepository;

  @Autowired private ShowTemplateRepository showTemplateRepository;

  @Autowired private ShowTypeRepository showTypeRepository;

  @Autowired private SeasonRepository seasonRepository;

  @Autowired private ObjectMapper objectMapper;

  private ShowType testShowType;
  private Season testSeason;
  private Show testShow;
  private ShowTemplate testShowTemplate;

  @BeforeEach
  void setUp() {
    // Clean up any existing test data
    showRepository.deleteAll();
    showTemplateRepository.deleteAll();
    seasonRepository.deleteAll();
    showTypeRepository.deleteAll();

    // Create test data
    testShowType = new ShowType();
    testShowType.setName("Integration Test Show Type");
    testShowType.setDescription("Integration test show type description");
    testShowType.setCreationDate(Instant.now());
    testShowType = showTypeRepository.save(testShowType);

    testSeason = new Season();
    testSeason.setName("Integration Test Season");
    testSeason.setDescription("Integration test season description");
    testSeason.setCreationDate(Instant.now());
    testSeason = seasonRepository.save(testSeason);

    testShow = new Show();
    testShow.setName("Integration Test Show");
    testShow.setDescription("Integration test description");
    testShow.setShowDate(LocalDate.of(2024, 6, 15));
    testShow.setType(testShowType);
    testShow.setSeason(testSeason);
    testShow.setExternalId("integration-test-123");
    testShow.setCreationDate(Instant.now());
    testShow = showRepository.save(testShow);

    testShowTemplate = new ShowTemplate();
    testShowTemplate.setName("Integration Test Template");
    testShowTemplate.setDescription("Integration test template description");
    testShowTemplate.setShowType(testShowType);
    testShowTemplate.setNotionUrl("https://notion.so/integration-test");
    testShowTemplate.setExternalId("template-integration-456");
    testShowTemplate.setCreationDate(Instant.now());
    testShowTemplate = showTemplateRepository.save(testShowTemplate);
  }

  @AfterEach
  void tearDown() throws IOException {
    // Clean up exported files
    Path exportsDir = Paths.get("target/exports");
    if (Files.exists(exportsDir)) {
      Files.walk(exportsDir)
          .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (IOException e) {
                  // Ignore cleanup errors
                }
              });
    }
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
    List<ShowDTO> exportedShows =
        objectMapper.readValue(fileContent, new TypeReference<List<ShowDTO>>() {});

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
    List<ShowDTO> exportedShows =
        objectMapper.readValue(showsContent, new TypeReference<List<ShowDTO>>() {});
    assertEquals(1, exportedShows.size());

    String templatesContent = Files.readString(templatesFile);
    List<ShowTemplateDTO> exportedTemplates =
        objectMapper.readValue(templatesContent, new TypeReference<List<ShowTemplateDTO>>() {});
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
          .sorted((a, b) -> b.compareTo(a))
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
