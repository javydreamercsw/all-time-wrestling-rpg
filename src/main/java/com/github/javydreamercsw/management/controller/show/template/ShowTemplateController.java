package com.github.javydreamercsw.management.controller.show.template;

import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing Show Templates. Provides endpoints for CRUD operations on show
 * templates.
 */
@RestController
@RequestMapping("/api/show-templates")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Show Templates", description = "Show Template management operations")
public class ShowTemplateController {

  private final ShowTemplateService showTemplateService;

  @Operation(
      summary = "Get all show templates",
      description = "Retrieves a paginated list of all show templates")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Show templates retrieved successfully")
      })
  @GetMapping
  public ResponseEntity<Page<ShowTemplate>> getAllShowTemplates(
      @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "Sort field") @RequestParam(defaultValue = "name") String sortBy,
      @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc")
          String sortDir) {

    Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<ShowTemplate> templates = showTemplateService.getAllTemplates(pageable);
    return ResponseEntity.ok(templates);
  }

  @Operation(
      summary = "Get show template by ID",
      description = "Retrieves a specific show template by its ID")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Show template found"),
        @ApiResponse(responseCode = "404", description = "Show template not found")
      })
  @GetMapping("/{id}")
  public ResponseEntity<ShowTemplate> getShowTemplateById(
      @Parameter(description = "Show template ID") @PathVariable Long id) {
    Optional<ShowTemplate> template = showTemplateService.getTemplateById(id);
    return template.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @Operation(
      summary = "Get show templates by show type",
      description = "Retrieves all show templates for a specific show type")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Show templates retrieved successfully")
      })
  @GetMapping("/by-show-type/{showTypeName}")
  public ResponseEntity<List<ShowTemplate>> getShowTemplatesByShowType(
      @Parameter(description = "Show type name") @PathVariable String showTypeName) {
    List<ShowTemplate> templates = showTemplateService.getTemplatesByShowType(showTypeName);
    return ResponseEntity.ok(templates);
  }

  @Operation(
      summary = "Get Premium Live Event templates",
      description = "Retrieves all show templates for Premium Live Events")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "PLE templates retrieved successfully")
      })
  @GetMapping("/ple")
  public ResponseEntity<List<ShowTemplate>> getPremiumLiveEventTemplates() {
    List<ShowTemplate> templates = showTemplateService.getPremiumLiveEventTemplates();
    return ResponseEntity.ok(templates);
  }

  @Operation(
      summary = "Get Weekly show templates",
      description = "Retrieves all show templates for Weekly shows")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Weekly templates retrieved successfully")
      })
  @GetMapping("/weekly")
  public ResponseEntity<List<ShowTemplate>> getWeeklyShowTemplates() {
    List<ShowTemplate> templates = showTemplateService.getWeeklyShowTemplates();
    return ResponseEntity.ok(templates);
  }

  @Operation(
      summary = "Create a new show template",
      description = "Creates a new show template with the provided details")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Show template created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @PostMapping
  public ResponseEntity<ShowTemplate> createShowTemplate(
      @Valid @RequestBody CreateShowTemplateRequest request) {
    ShowTemplate template =
        showTemplateService.createOrUpdateTemplate(
            request.name(), request.description(), request.showTypeName(), request.notionUrl());

    if (template == null) {
      return ResponseEntity.badRequest().build();
    }

    return ResponseEntity.status(HttpStatus.CREATED).body(template);
  }

  @Operation(
      summary = "Update an existing show template",
      description = "Updates an existing show template with the provided details")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Show template updated successfully"),
        @ApiResponse(responseCode = "404", description = "Show template not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
      })
  @PutMapping("/{id}")
  public ResponseEntity<ShowTemplate> updateShowTemplate(
      @Parameter(description = "Show template ID") @PathVariable Long id,
      @Valid @RequestBody UpdateShowTemplateRequest request) {

    Optional<ShowTemplate> updatedTemplate =
        showTemplateService.updateTemplate(
            id, request.name(), request.description(), request.showTypeName(), request.notionUrl());

    return updatedTemplate.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @Operation(summary = "Delete a show template", description = "Deletes a show template by its ID")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Show template deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Show template not found")
      })
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteShowTemplate(
      @Parameter(description = "Show template ID") @PathVariable Long id) {

    boolean deleted = showTemplateService.deleteTemplate(id);
    return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }

  // Request DTOs
  public record CreateShowTemplateRequest(
      @NotBlank @Size(max = 255) String name,
      @Size(max = 1000) String description,
      @NotBlank String showTypeName,
      @Size(max = 500) String notionUrl) {}

  public record UpdateShowTemplateRequest(
      @NotBlank @Size(max = 255) String name,
      @Size(max = 1000) String description,
      @NotBlank String showTypeName,
      @Size(max = 500) String notionUrl) {}
}
