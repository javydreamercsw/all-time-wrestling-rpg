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
package com.github.javydreamercsw.management.controller.title;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.title.TitleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing titles in the ATW RPG system. Provides endpoints for championship
 * management, challenges, and title tracking.
 */
@RestController
@RequestMapping("/api/titles")
@Validated
@Tag(name = "Title Management", description = "ATW RPG Championship management operations")
public class TitleController {

  private final TitleService titleService;
  private final WrestlerRepository wrestlerRepository;

  public TitleController(TitleService titleService, WrestlerRepository wrestlerRepository) {
    this.titleService = titleService;
    this.wrestlerRepository = wrestlerRepository;
  }

  @Operation(summary = "Create a new title", description = "Creates a new championship title")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Title created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "Title name already exists")
      })
  @PostMapping
  public ResponseEntity<Object> createTitle(@Valid @RequestBody CreateTitleRequest request) {
    // Check if title name already exists
    if (titleService.titleNameExists(request.name())) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(new ErrorResponse("Title name already exists: " + request.name()));
    }

    Title title = titleService.createTitle(request.name(), request.description(), request.tier());
    return ResponseEntity.status(HttpStatus.CREATED).body(title);
  }

  @Operation(summary = "Get all titles", description = "Retrieves all titles with pagination")
  @GetMapping
  public ResponseEntity<Page<Title>> getAllTitles(
      @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "Sort field") @RequestParam(defaultValue = "name") String sortBy,
      @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc")
          String sortDir) {

    Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<Title> titles = titleService.getAllTitles(pageable);
    return ResponseEntity.ok(titles);
  }

  @Operation(summary = "Get title by ID", description = "Retrieves a specific title by its ID")
  @GetMapping("/{id}")
  public ResponseEntity<Title> getTitleById(@PathVariable Long id) {
    Optional<Title> title = titleService.getTitleById(id);
    return title.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @Operation(summary = "Get active titles", description = "Retrieves all active titles")
  @GetMapping("/active")
  public ResponseEntity<List<Title>> getActiveTitles() {
    List<Title> titles = titleService.getActiveTitles();
    return ResponseEntity.ok(titles);
  }

  @Operation(summary = "Get vacant titles", description = "Retrieves all vacant active titles")
  @GetMapping("/vacant")
  public ResponseEntity<List<Title>> getVacantTitles() {
    List<Title> titles = titleService.getVacantTitles();
    return ResponseEntity.ok(titles);
  }

  @Operation(
      summary = "Get titles by tier",
      description = "Retrieves all active titles of a specific tier")
  @GetMapping("/tier/{tier}")
  public ResponseEntity<List<Title>> getTitlesByTier(@PathVariable WrestlerTier tier) {
    List<Title> titles = titleService.getTitlesByTier(tier);
    return ResponseEntity.ok(titles);
  }

  @Operation(summary = "Award title", description = "Awards a title to a list of wrestlers")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Title awarded successfully"),
        @ApiResponse(responseCode = "404", description = "Title or wrestler not found"),
        @ApiResponse(responseCode = "400", description = "Wrestler not eligible for title")
      })
  @PostMapping("/{titleId}/award")
  public ResponseEntity<Object> awardTitle(
      @PathVariable Long titleId, @RequestBody List<Long> wrestlerIds) {
    Optional<Title> titleOpt = titleService.getTitleById(titleId);
    if (titleOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    Title title = titleOpt.get();

    List<Wrestler> wrestlers = wrestlerRepository.findAllById(wrestlerIds);
    if (wrestlers.size() != wrestlerIds.size()) {
      return ResponseEntity.badRequest().body(new ErrorResponse("One or more wrestlers not found"));
    }

    titleService.awardTitleTo(title, wrestlers);
    return ResponseEntity.ok(title);
  }

  @Operation(summary = "Vacate title", description = "Vacates a title (removes current champion)")
  @PostMapping("/{id}/vacate")
  public ResponseEntity<Title> vacateTitle(@PathVariable Long id) {
    Optional<Title> title = titleService.vacateTitle(id);
    return title.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @Operation(
      summary = "Challenge for title",
      description = "Allows a wrestler to challenge for a title")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Challenge processed"),
        @ApiResponse(responseCode = "404", description = "Title or wrestler not found"),
        @ApiResponse(responseCode = "400", description = "Challenge failed - see response message")
      })
  @PostMapping("/{titleId}/challenge/{wrestlerId}")
  public ResponseEntity<TitleService.ChallengeResult> challengeForTitle(
      @PathVariable Long titleId, @PathVariable Long wrestlerId) {
    TitleService.ChallengeResult result = titleService.challengeForTitle(wrestlerId, titleId);

    if (result.success()) {
      return ResponseEntity.ok(result);
    } else {
      return ResponseEntity.badRequest().body(result);
    }
  }

  @Operation(
      summary = "Get eligible challengers",
      description = "Gets wrestlers eligible to challenge for a title")
  @GetMapping("/{id}/eligible-challengers")
  public ResponseEntity<List<Wrestler>> getEligibleChallengers(@PathVariable Long id) {
    List<Wrestler> challengers = titleService.getEligibleChallengers(id);
    return ResponseEntity.ok(challengers);
  }

  @Operation(
      summary = "Get titles held by wrestler",
      description = "Gets all titles currently held by a wrestler")
  @GetMapping("/wrestler/{wrestlerId}")
  public ResponseEntity<List<Title>> getTitlesHeldBy(@PathVariable Long wrestlerId) {
    List<Title> titles = titleService.getTitlesHeldBy(wrestlerId);
    return ResponseEntity.ok(titles);
  }

  @Operation(summary = "Update title", description = "Updates title information")
  @PutMapping("/{id}")
  public ResponseEntity<Title> updateTitle(
      @PathVariable Long id, @Valid @RequestBody UpdateTitleRequest request) {
    Optional<Title> title =
        titleService.updateTitle(id, request.name(), request.description(), request.isActive());
    return title.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @Operation(
      summary = "Delete title",
      description = "Deletes a title (only if inactive and vacant)")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTitle(@PathVariable Long id) {
    boolean deleted = titleService.deleteTitle(id);
    if (deleted) {
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
  }

  @Operation(
      summary = "Get title statistics",
      description = "Retrieves comprehensive statistics for a title")
  @GetMapping("/{id}/stats")
  public ResponseEntity<TitleService.TitleStats> getTitleStats(@PathVariable Long id) {
    TitleService.TitleStats stats = titleService.getTitleStats(id);
    if (stats != null) {
      return ResponseEntity.ok(stats);
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  // ==================== REQUEST/RESPONSE DTOs ====================

  public record CreateTitleRequest(
      @NotBlank(message = "Title name is required") @Size(max = 255, message = "Title name must not exceed 255 characters") String name,
      @Size(max = 1000, message = "Description must not exceed 1000 characters") String description,
      @NotNull(message = "Title tier is required") WrestlerTier tier) {}

  public record UpdateTitleRequest(
      @Size(max = 255, message = "Title name must not exceed 255 characters") String name,
      @Size(max = 1000, message = "Description must not exceed 1000 characters") String description,
      Boolean isActive) {}

  public record ErrorResponse(String message) {}
}
