package com.github.javydreamercsw.management.controller.export;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.dto.ShowDTO;
import com.github.javydreamercsw.management.dto.ShowTemplateDTO;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/** Unit tests for DataExportController. Tests the controller logic without Spring context. */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class DataExportControllerTest {

  @Mock private ShowService showService;

  @Mock private ShowTemplateService showTemplateService;

  @Mock private ObjectMapper objectMapper;

  @Mock private ObjectWriter objectWriter;

  @InjectMocks private DataExportController dataExportController;

  @TempDir Path tempDir;

  private Show testShow;
  private ShowTemplate testShowTemplate;
  private ShowType testShowType;

  @BeforeEach
  void setUp() {
    // Create test data
    testShowType = new ShowType();
    testShowType.setName("Weekly Show");

    Season testSeason = new Season();
    testSeason.setName("Season 1");

    testShow = new Show();
    testShow.setName("Test Show");
    testShow.setDescription("Test Description");
    testShow.setShowDate(LocalDate.of(2024, 1, 15));
    testShow.setType(testShowType);
    testShow.setSeason(testSeason);
    testShow.setExternalId("notion-123");

    testShowTemplate = new ShowTemplate();
    testShowTemplate.setName("Test Template");
    testShowTemplate.setDescription("Template Description");
    testShowTemplate.setShowType(testShowType);
    testShowTemplate.setNotionUrl("https://notion.so/template");
    testShowTemplate.setExternalId("template-456");
    testShowTemplate.setCreationDate(Instant.now());
  }

  @Test
  void exportShows_Success() throws IOException {
    // Arrange
    List<Show> shows = Arrays.asList(testShow);
    when(showService.findAllWithRelationships()).thenReturn(shows);

    try (MockedStatic<Paths> pathsMock = mockStatic(Paths.class);
        MockedStatic<Files> filesMock = mockStatic(Files.class)) {

      Path mockExportsDir = tempDir.resolve("exports");
      Path mockPath = mockExportsDir.resolve("shows.json");
      pathsMock.when(() -> Paths.get("target/exports")).thenReturn(mockExportsDir);

      // Act
      ResponseEntity<String> response = dataExportController.exportShows();

      // Assert
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertTrue(response.getBody().contains("Successfully exported 1 shows"));
      verify(showService).findAllWithRelationships();
      filesMock.verify(() -> Files.createDirectories(mockExportsDir));
      filesMock.verify(() -> Files.write(eq(mockPath), any(byte[].class)));
    }
  }

  @Test
  void exportShows_ServiceException() {
    // Arrange
    when(showService.findAllWithRelationships()).thenThrow(new RuntimeException("Database error"));

    // Act
    ResponseEntity<String> response = dataExportController.exportShows();

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(response.getBody());

    // Make test more robust by checking for key parts of the error message
    // Updated to segment the new error message format (changed due to AI suggestion in GitHub)
    String responseBody = response.getBody();
    assertTrue(
        responseBody.contains("Failed to export shows"),
        "Response should contain 'Failed to export shows', but was: " + responseBody);
    assertTrue(
        responseBody.contains("due to an internal error"),
        "Response should contain 'due to an internal error', but was: " + responseBody);
  }

  @Test
  void exportShows_EmptyList() throws IOException {
    // Arrange
    when(showService.findAllWithRelationships()).thenReturn(Arrays.asList());

    try (MockedStatic<Paths> pathsMock = mockStatic(Paths.class);
        MockedStatic<Files> filesMock = mockStatic(Files.class)) {

      Path mockExportsDir = tempDir.resolve("exports");
      Path mockPath = mockExportsDir.resolve("shows.json");
      pathsMock.when(() -> Paths.get("target/exports")).thenReturn(mockExportsDir);

      // Act
      ResponseEntity<String> response = dataExportController.exportShows();

      // Assert
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertTrue(response.getBody().contains("Successfully exported 0 shows"));
      filesMock.verify(() -> Files.createDirectories(mockExportsDir));
      filesMock.verify(() -> Files.write(eq(mockPath), any(byte[].class)));
    }
  }

  @Test
  void exportShowTemplates_Success() throws IOException {
    // Arrange
    List<ShowTemplate> templates = Arrays.asList(testShowTemplate);
    when(showTemplateService.findAll()).thenReturn(templates);

    try (MockedStatic<Paths> pathsMock = mockStatic(Paths.class);
        MockedStatic<Files> filesMock = mockStatic(Files.class)) {

      Path mockExportsDir = tempDir.resolve("exports");
      Path mockPath = mockExportsDir.resolve("show_templates.json");
      pathsMock.when(() -> Paths.get("target/exports")).thenReturn(mockExportsDir);

      // Act
      ResponseEntity<String> response = dataExportController.exportShowTemplates();

      // Assert
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertTrue(response.getBody().contains("Successfully exported 1 show templates"));
      verify(showTemplateService).findAll();
      filesMock.verify(() -> Files.createDirectories(mockExportsDir));
      filesMock.verify(() -> Files.write(eq(mockPath), any(byte[].class)));
    }
  }

  @Test
  void exportShowTemplates_ServiceException() {
    // Arrange
    when(showTemplateService.findAll()).thenThrow(new RuntimeException("Database error"));

    // Act
    ResponseEntity<String> response = dataExportController.exportShowTemplates();

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    // Make test more robust by checking for key parts of the error message
    // Updated to segment the new consistent error message format
    String responseBody = response.getBody();
    assertTrue(
        responseBody.contains("Failed to export show templates"),
        "Response should contain 'Failed to export show templates', but was: " + responseBody);
    assertTrue(
        responseBody.contains("due to an internal error"),
        "Response should contain 'due to an internal error', but was: " + responseBody);
  }

  @Test
  void exportAll_Success() throws IOException {
    // Arrange
    when(showService.findAllWithRelationships()).thenReturn(Arrays.asList(testShow));
    when(showTemplateService.findAll()).thenReturn(Arrays.asList(testShowTemplate));

    try (MockedStatic<Paths> pathsMock = mockStatic(Paths.class);
        MockedStatic<Files> filesMock = mockStatic(Files.class)) {

      Path mockExportsDir = tempDir.resolve("exports");
      Path showsPath = mockExportsDir.resolve("shows.json");
      Path templatesPath = mockExportsDir.resolve("show_templates.json");
      pathsMock.when(() -> Paths.get("target/exports")).thenReturn(mockExportsDir);

      // Act
      ResponseEntity<String> response = dataExportController.exportAll();

      // Assert
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertTrue(response.getBody().contains("Successfully exported all data"));
      verify(showService).findAllWithRelationships();
      verify(showTemplateService).findAll();
      filesMock.verify(() -> Files.createDirectories(mockExportsDir), times(2));
      filesMock.verify(() -> Files.write(eq(showsPath), any(byte[].class)));
      filesMock.verify(() -> Files.write(eq(templatesPath), any(byte[].class)));
    }
  }

  @Test
  void exportAll_ShowsFailure() {
    // Arrange
    when(showService.findAllWithRelationships()).thenThrow(new RuntimeException("Shows error"));

    // Act
    ResponseEntity<String> response = dataExportController.exportAll();

    // Assert
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertTrue(response.getBody().contains("Failed to export shows"));
    verify(showTemplateService, never()).findAll();
  }

  @Test
  void convertShowToDTO_AllFields() {
    // Act - Use reflection to access private method
    ShowDTO dto = invokeConvertShowToDTO(testShow);

    // Assert
    assertEquals("Test Show", dto.getName());
    assertEquals("Test Description", dto.getDescription());
    assertEquals("Weekly Show", dto.getShowType());
    assertEquals("2024-01-15", dto.getShowDate());
    assertEquals("notion-123", dto.getExternalId());
    assertEquals("Season 1", dto.getSeasonName());
    assertNull(dto.getTemplateName()); // No template set
  }

  @Test
  void convertShowToDTO_MinimalFields() {
    // Arrange
    Show minimalShow = new Show();
    minimalShow.setName("Minimal Show");
    minimalShow.setDescription("Minimal Description");
    minimalShow.setType(testShowType);

    // Act
    ShowDTO dto = invokeConvertShowToDTO(minimalShow);

    // Assert
    assertEquals("Minimal Show", dto.getName());
    assertEquals("Minimal Description", dto.getDescription());
    assertEquals("Weekly Show", dto.getShowType());
    assertNull(dto.getShowDate());
    assertNull(dto.getExternalId());
    assertNull(dto.getSeasonName());
    assertNull(dto.getTemplateName());
  }

  @Test
  void convertShowTemplateToDTO_AllFields() {
    // Act - Use reflection to access private method
    ShowTemplateDTO dto = invokeConvertShowTemplateToDTO(testShowTemplate);

    // Assert
    assertEquals("Test Template", dto.getName());
    assertEquals("Template Description", dto.getDescription());
    assertEquals("Weekly Show", dto.getShowTypeName());
    assertEquals("https://notion.so/template", dto.getNotionUrl());
    assertEquals("template-456", dto.getExternalId());
  }

  @Test
  void convertShowTemplateToDTO_MinimalFields() {
    // Arrange
    ShowTemplate minimalTemplate = new ShowTemplate();
    minimalTemplate.setName("Minimal Template");
    minimalTemplate.setDescription("Minimal Description");
    minimalTemplate.setCreationDate(Instant.now());

    // Act
    ShowTemplateDTO dto = invokeConvertShowTemplateToDTO(minimalTemplate);

    // Assert
    assertEquals("Minimal Template", dto.getName());
    assertEquals("Minimal Description", dto.getDescription());
    assertNull(dto.getShowTypeName());
    assertNull(dto.getNotionUrl());
    assertNull(dto.getExternalId());
  }

  // Helper methods to access private methods via reflection
  private ShowDTO invokeConvertShowToDTO(Show show) {
    try {
      var method = DataExportController.class.getDeclaredMethod("convertShowToDTO", Show.class);
      method.setAccessible(true);
      return (ShowDTO) method.invoke(dataExportController, show);
    } catch (Exception e) {
      throw new RuntimeException("Failed to invoke convertShowToDTO", e);
    }
  }

  private ShowTemplateDTO invokeConvertShowTemplateToDTO(ShowTemplate template) {
    try {
      var method =
          DataExportController.class.getDeclaredMethod(
              "convertShowTemplateToDTO", ShowTemplate.class);
      method.setAccessible(true);
      return (ShowTemplateDTO) method.invoke(dataExportController, template);
    } catch (Exception e) {
      throw new RuntimeException("Failed to invoke convertShowTemplateToDTO", e);
    }
  }
}
