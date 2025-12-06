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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.dto.ShowDTO;
import com.github.javydreamercsw.management.dto.ShowTemplateDTO;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for exporting database data to JSON files. This provides endpoints to export
 * current database state to JSON files that can be used for backup, import, or data migration
 * purposes.
 */
@Slf4j
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Tag(name = "Data Export", description = "Export database data to JSON files")
public class DataExportController {

  private final ShowService showService;
  private final ShowTemplateService showTemplateService;
  private final ObjectMapper objectMapper;

  /**
   * Export all shows from database to target/exports/shows.json file. This creates a new export
   * file in the build directory without overwriting the source import file.
   *
   * @return ResponseEntity with export result
   */
  @PostMapping("/shows")
  @Operation(
      summary = "Export shows to JSON",
      description = "Exports all shows from the database to target/exports/shows.json file")
  @ApiResponse(responseCode = "200", description = "Shows exported successfully")
  @ApiResponse(responseCode = "500", description = "Export failed")
  @Transactional(readOnly = true)
  public ResponseEntity<String> exportShows() {
    try {
      log.info("🚀 Starting shows export to JSON file...");
      long startTime = System.currentTimeMillis();

      // Get all shows from database with eagerly loaded relationships
      List<Show> shows = showService.findAllWithRelationships();
      log.info("📥 Retrieved {} shows from database", shows.size());

      // Convert to DTOs
      List<ShowDTO> showDTOs =
          shows.stream().map(this::convertShowToDTO).collect(Collectors.toList());

      // Write to JSON file
      writeShowsToJsonFile(showDTOs);

      long totalTime = System.currentTimeMillis() - startTime;
      String message =
          String.format(
              "✅ Successfully exported %d shows to target/exports/shows.json in %dms",
              shows.size(), totalTime);
      log.info(message);

      return ResponseEntity.ok(message);

    } catch (Exception e) {
      String errorMessage = "❌ Failed to export shows due to an internal error.";
      log.error("Failed to export shows.", e);
      return ResponseEntity.internalServerError().body(errorMessage);
    }
  }

  /**
   * Export all show templates from database to target/exports/show_templates.json file. This
   * creates a new export file in the build directory without overwriting the source import file.
   *
   * @return ResponseEntity with export result
   */
  @PostMapping("/show-templates")
  @Operation(
      summary = "Export show templates to JSON",
      description =
          "Exports all show templates from the database to target/exports/show_templates.json"
              + " file")
  @ApiResponse(responseCode = "200", description = "Show templates exported successfully")
  @ApiResponse(responseCode = "500", description = "Export failed")
  @Transactional(readOnly = true)
  public ResponseEntity<String> exportShowTemplates() {
    try {
      log.info("🎭 Starting show templates export to JSON file...");
      long startTime = System.currentTimeMillis();

      // Get all show templates from database
      List<ShowTemplate> templates = showTemplateService.findAll();
      log.info("📥 Retrieved {} show templates from database", templates.size());

      // Convert to DTOs
      List<ShowTemplateDTO> templateDTOs =
          templates.stream().map(this::convertShowTemplateToDTO).collect(Collectors.toList());

      // Write to JSON file
      writeShowTemplatesToJsonFile(templateDTOs);

      long totalTime = System.currentTimeMillis() - startTime;
      String message =
          String.format(
              "✅ Successfully exported %d show templates to target/exports/show_templates.json in"
                  + " %dms",
              templates.size(), totalTime);
      log.info(message);

      return ResponseEntity.ok(message);

    } catch (Exception e) {
      String errorMessage = "❌ Failed to export show templates due to an internal error.";
      log.error("Failed to export show templates.", e);
      return ResponseEntity.internalServerError().body(errorMessage);
    }
  }

  /**
   * Export all data (shows and show templates) to target/exports/ directory.
   *
   * @return ResponseEntity with export result
   */
  @PostMapping("/all")
  @Operation(
      summary = "Export all data to JSON",
      description =
          "Exports all shows and show templates from the database to target/exports/ directory")
  @ApiResponse(responseCode = "200", description = "All data exported successfully")
  @ApiResponse(responseCode = "500", description = "Export failed")
  @Transactional(readOnly = true)
  public ResponseEntity<String> exportAll() {
    try {
      log.info("🚀 Starting full data export to JSON files...");
      long startTime = System.currentTimeMillis();

      // Export shows
      ResponseEntity<String> showsResult = exportShows();
      if (!showsResult.getStatusCode().is2xxSuccessful()) {
        return showsResult;
      }

      // Export show templates
      ResponseEntity<String> templatesResult = exportShowTemplates();
      if (!templatesResult.getStatusCode().is2xxSuccessful()) {
        return templatesResult;
      }

      long totalTime = System.currentTimeMillis() - startTime;
      String message =
          String.format(
              "✅ Successfully exported all data to target/exports/ directory in %dms", totalTime);
      log.info(message);

      return ResponseEntity.ok(message);

    } catch (Exception e) {
      String errorMessage = "❌ Failed to export all data due to an unexpected error.";
      log.error("❌ Failed to export all data", e);
      return ResponseEntity.internalServerError().body(errorMessage);
    }
  }

  // ==================== PRIVATE HELPER METHODS ====================

  /** Convert Show entity to ShowDTO for JSON serialization. */
  private ShowDTO convertShowToDTO(Show show) {
    ShowDTO dto = new ShowDTO();
    dto.setName(show.getName());
    dto.setDescription(show.getDescription());
    dto.setShowType(show.getType() != null ? show.getType().getName() : null);
    dto.setShowDate(show.getShowDate() != null ? show.getShowDate().toString() : null);
    dto.setSeasonName(show.getSeason() != null ? show.getSeason().getName() : null);
    dto.setTemplateName(show.getTemplate() != null ? show.getTemplate().getName() : null);
    dto.setExternalId(show.getExternalId());
    return dto;
  }

  /** Convert ShowTemplate entity to ShowTemplateDTO for JSON serialization. */
  private ShowTemplateDTO convertShowTemplateToDTO(ShowTemplate template) {
    ShowTemplateDTO dto = new ShowTemplateDTO();
    dto.setName(template.getName());
    dto.setDescription(template.getDescription());
    dto.setShowTypeName(template.getShowType() != null ? template.getShowType().getName() : null);
    dto.setNotionUrl(template.getNotionUrl());
    dto.setExternalId(template.getExternalId());
    return dto;
  }

  /** Write shows to the target/exports/shows.json file. */
  private void writeShowsToJsonFile(List<ShowDTO> showDTOs) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());

    // Create exports directory if it doesn't exist
    Path exportsDir = Paths.get("target/exports");
    Files.createDirectories(exportsDir);

    Path exportPath = exportsDir.resolve("shows.json");
    String jsonContent = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(showDTOs);
    Files.write(exportPath, jsonContent.getBytes());

    log.info("💾 Successfully wrote {} shows to target/exports/shows.json", showDTOs.size());
  }

  /** Write show templates to the target/exports/show_templates.json file. */
  private void writeShowTemplatesToJsonFile(List<ShowTemplateDTO> templateDTOs) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());

    // Create exports directory if it doesn't exist
    Path exportsDir = Paths.get("target/exports");
    Files.createDirectories(exportsDir);

    Path exportPath = exportsDir.resolve("show_templates.json");
    String jsonContent = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(templateDTOs);
    Files.write(exportPath, jsonContent.getBytes());

    log.info(
        "💾 Successfully wrote {} show templates to target/exports/show_templates.json",
        templateDTOs.size());
  }
}
