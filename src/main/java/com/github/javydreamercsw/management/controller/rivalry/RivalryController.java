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
package com.github.javydreamercsw.management.controller.rivalry;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryIntensity;
import com.github.javydreamercsw.management.dto.resolution.ResolutionResultDTO;
import com.github.javydreamercsw.management.dto.rivalry.RivalryDTO;
import com.github.javydreamercsw.management.mapper.RivalryMapper;
import com.github.javydreamercsw.management.service.resolution.ResolutionResult;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing rivalries in the ATW RPG system. Provides endpoints for heat
 * management, rivalry resolution, and storyline tracking.
 */
@RestController
@RequestMapping("/api/rivalries")
@Validated
@Tag(name = "Rivalry Management", description = "ATW RPG Rivalry and heat management operations")
public class RivalryController {

  private final RivalryService rivalryService;
  private final RivalryMapper rivalryMapper;

  public RivalryController(RivalryService rivalryService, RivalryMapper rivalryMapper) {
    this.rivalryService = rivalryService;
    this.rivalryMapper = rivalryMapper;
  }

  @Operation(
      summary = "Create a new rivalry",
      description = "Creates a new rivalry between two wrestlers")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Rivalry created successfully"),
        @ApiResponse(responseCode = "200", description = "Existing rivalry returned"),
        @ApiResponse(responseCode = "404", description = "One or both wrestlers not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
      })
  @PostMapping
  @Transactional
  public ResponseEntity<Object> createRivalry(@Valid @RequestBody CreateRivalryRequest request) {
    Optional<Rivalry> rivalry =
        rivalryService.createRivalry(
            request.wrestler1Id(), request.wrestler2Id(), request.storylineNotes());

    if (rivalry.isPresent()) {
      // Check if this is a new rivalry or existing one
      boolean isNew = rivalry.get().getHeat() == 0 && rivalry.get().getHeatEvents().isEmpty();
      HttpStatus status = isNew ? HttpStatus.CREATED : HttpStatus.OK;
      return ResponseEntity.status(status).body(rivalryMapper.toRivalryDTO(rivalry.get()));
    } else {
      return ResponseEntity.badRequest()
          .body(new ErrorResponse("Cannot create rivalry - wrestlers not found"));
    }
  }

  @Operation(summary = "Get all rivalries", description = "Retrieves all rivalries with pagination")
  @GetMapping
  @Transactional(readOnly = true)
  public ResponseEntity<Page<RivalryDTO>> getAllRivalries(
      @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "Sort field") @RequestParam(defaultValue = "heat") String sortBy,
      @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc")
          String sortDir) {

    Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<Rivalry> rivalries = rivalryService.getAllRivalries(pageable);
    return ResponseEntity.ok(rivalries.map(rivalryMapper::toRivalryDTO));
  }

  @Operation(summary = "Get rivalry by ID", description = "Retrieves a specific rivalry by its ID")
  @GetMapping("/{id}")
  @Transactional(readOnly = true)
  public ResponseEntity<RivalryDTO> getRivalryById(@PathVariable Long id) {
    Optional<Rivalry> rivalry = rivalryService.getRivalryById(id);
    return rivalry
        .map(rivalryMapper::toRivalryDTO)
        .map(ResponseEntity::ok)
        .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @Operation(summary = "Get active rivalries", description = "Retrieves all active rivalries")
  @GetMapping("/active")
  @Transactional(readOnly = true)
  public ResponseEntity<List<RivalryDTO>> getActiveRivalries() {
    List<Rivalry> rivalries = rivalryService.getActiveRivalries();
    return ResponseEntity.ok(rivalries.stream().map(rivalryMapper::toRivalryDTO).toList());
  }

  @Operation(
      summary = "Get rivalries for wrestler",
      description = "Gets all active rivalries for a specific wrestler")
  @GetMapping("/wrestler/{wrestlerId}")
  @Transactional(readOnly = true)
  public ResponseEntity<List<RivalryDTO>> getRivalriesForWrestler(@PathVariable Long wrestlerId) {
    List<Rivalry> rivalries = rivalryService.getRivalriesForWrestler(wrestlerId);
    return ResponseEntity.ok(rivalries.stream().map(rivalryMapper::toRivalryDTO).toList());
  }

  @Operation(summary = "Add heat to rivalry", description = "Adds heat to an existing rivalry")
  @PostMapping("/{id}/heat")
  @Transactional
  public ResponseEntity<Object> addHeat(
      @PathVariable Long id, @Valid @RequestBody AddHeatRequest request) {
    Optional<Rivalry> rivalry = rivalryService.addHeat(id, request.heatGain(), request.reason());

    return rivalry
        .map(rivalryMapper::toRivalryDTO)
        .<ResponseEntity<Object>>map(ResponseEntity::ok)
        .orElseGet(
            () ->
                ResponseEntity.badRequest()
                    .body(new ErrorResponse("Cannot add heat - rivalry not found or inactive")));
  }

  @Operation(
      summary = "Add heat between wrestlers",
      description = "Adds heat between two wrestlers (creates rivalry if needed)")
  @PostMapping("/heat")
  @Transactional
  public ResponseEntity<Object> addHeatBetweenWrestlers(
      @Valid @RequestBody AddHeatBetweenWrestlersRequest request) {
    Optional<Rivalry> rivalry =
        rivalryService.addHeatBetweenWrestlers(
            request.wrestler1Id(), request.wrestler2Id(), request.heatGain(), request.reason());

    return rivalry
        .map(rivalryMapper::toRivalryDTO)
        .<ResponseEntity<Object>>map(ResponseEntity::ok)
        .orElseGet(
            () ->
                ResponseEntity.badRequest()
                    .body(new ErrorResponse("Cannot add heat - wrestlers not found")));
  }

  @Operation(
      summary = "Attempt rivalry resolution",
      description = "Attempts to resolve a rivalry using dice rolls")
  @PostMapping("/{id}/resolve")
  @Transactional
  public ResponseEntity<ResolutionResultDTO> attemptResolution(
      @PathVariable Long id, @Valid @RequestBody ResolutionAttemptRequest request) {
    ResolutionResult<Rivalry> result =
        rivalryService.attemptResolution(id, request.wrestler1Roll(), request.wrestler2Roll());

    return ResponseEntity.ok(
        new ResolutionResultDTO(rivalryMapper.toRivalryDTO(result.entity()), result.message()));
  }

  @Operation(summary = "End rivalry", description = "Manually ends a rivalry")
  @PostMapping("/{id}/end")
  @Transactional
  public ResponseEntity<Object> endRivalry(
      @PathVariable Long id, @Valid @RequestBody EndRivalryRequest request) {
    Optional<Rivalry> rivalry = rivalryService.endRivalry(id, request.reason());

    return rivalry
        .map(rivalryMapper::toRivalryDTO)
        .<ResponseEntity<Object>>map(ResponseEntity::ok)
        .orElseGet(
            () ->
                ResponseEntity.badRequest()
                    .body(
                        new ErrorResponse(
                            "Cannot end rivalry - rivalry not found or already ended")));
  }

  @Operation(
      summary = "Get rivalries requiring segments",
      description = "Gets rivalries that require segments (10+ heat)")
  @GetMapping("/requiring-matches")
  @Transactional(readOnly = true)
  public ResponseEntity<List<RivalryDTO>> getRivalriesRequiringSegments() {
    List<Rivalry> rivalries = rivalryService.getRivalriesRequiringMatches();
    return ResponseEntity.ok(rivalries.stream().map(rivalryMapper::toRivalryDTO).toList());
  }

  @Operation(
      summary = "Get rivalries eligible for resolution",
      description = "Gets rivalries eligible for resolution (20+ heat)")
  @GetMapping("/eligible-for-resolution")
  @Transactional(readOnly = true)
  public ResponseEntity<List<RivalryDTO>> getRivalriesEligibleForResolution() {
    List<Rivalry> rivalries = rivalryService.getRivalriesEligibleForResolution();
    return ResponseEntity.ok(rivalries.stream().map(rivalryMapper::toRivalryDTO).toList());
  }

  @Operation(
      summary = "Get rivalries requiring rule segments",
      description = "Gets rivalries requiring rule segments (30+ heat)")
  @GetMapping("/requiring-rule")
  @Transactional(readOnly = true)
  public ResponseEntity<List<RivalryDTO>> getRivalriesRequiringStipulationSegments() {
    List<Rivalry> rivalries = rivalryService.getRivalriesRequiringStipulationMatches();
    return ResponseEntity.ok(rivalries.stream().map(rivalryMapper::toRivalryDTO).toList());
  }

  @Operation(
      summary = "Get rivalries by intensity",
      description = "Gets rivalries by intensity level")
  @GetMapping("/intensity/{intensity}")
  @Transactional(readOnly = true)
  public ResponseEntity<List<RivalryDTO>> getRivalriesByIntensity(
      @PathVariable RivalryIntensity intensity) {
    List<Rivalry> rivalries = rivalryService.getRivalriesByIntensity(intensity);
    return ResponseEntity.ok(rivalries.stream().map(rivalryMapper::toRivalryDTO).toList());
  }

  @Operation(summary = "Get hottest rivalries", description = "Gets the hottest rivalries")
  @GetMapping("/hottest")
  @Transactional(readOnly = true)
  public ResponseEntity<List<RivalryDTO>> getHottestRivalries(
      @Parameter(description = "Number of rivalries to return")
          @RequestParam(defaultValue = "10")
          @Min(1) @Max(50) int limit) {
    List<Rivalry> rivalries = rivalryService.getHottestRivalries(limit);
    return ResponseEntity.ok(rivalries.stream().map(rivalryMapper::toRivalryDTO).toList());
  }

  @Operation(
      summary = "Update storyline notes",
      description = "Updates the storyline notes for a rivalry")
  @PutMapping("/{id}/storyline")
  @Transactional
  public ResponseEntity<RivalryDTO> updateStorylineNotes(
      @PathVariable Long id, @Valid @RequestBody UpdateStorylineRequest request) {
    Optional<Rivalry> rivalry = rivalryService.updateStorylineNotes(id, request.storylineNotes());
    return rivalry
        .map(rivalryMapper::toRivalryDTO)
        .map(ResponseEntity::ok)
        .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @Operation(
      summary = "Get rivalry statistics",
      description = "Retrieves comprehensive statistics for a rivalry")
  @GetMapping("/{id}/stats")
  @Transactional(readOnly = true)
  public ResponseEntity<RivalryService.RivalryStats> getRivalryStats(@PathVariable Long id) {
    RivalryService.RivalryStats stats = rivalryService.getRivalryStats(id);
    if (stats != null) {
      return ResponseEntity.ok(stats);
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @Operation(
      summary = "Check rivalry history",
      description = "Checks if two wrestlers have rivalry history")
  @GetMapping("/history/{wrestler1Id}/{wrestler2Id}")
  @Transactional(readOnly = true)
  public ResponseEntity<RivalryHistoryResponse> checkRivalryHistory(
      @PathVariable Long wrestler1Id, @PathVariable Long wrestler2Id) {
    boolean hasHistory = rivalryService.hasRivalryHistory(wrestler1Id, wrestler2Id);
    return ResponseEntity.ok(new RivalryHistoryResponse(hasHistory));
  }

  // ==================== REQUEST/RESPONSE DTOs ====================

  public record CreateRivalryRequest(
      @NotNull(message = "Wrestler 1 ID is required") Long wrestler1Id,
      @NotNull(message = "Wrestler 2 ID is required") Long wrestler2Id,
      @Size(max = 2000, message = "Storyline notes must not exceed 2000 characters") String storylineNotes) {}

  public record AddHeatRequest(
      @Min(value = -50, message = "Heat gain must be at least -50") @Max(value = 50, message = "Heat gain must not exceed 50") int heatGain,
      @NotBlank(message = "Reason is required") @Size(max = 500, message = "Reason must not exceed 500 characters") String reason) {}

  public record AddHeatBetweenWrestlersRequest(
      @NotNull(message = "Wrestler 1 ID is required") Long wrestler1Id,
      @NotNull(message = "Wrestler 2 ID is required") Long wrestler2Id,
      @Min(value = -50, message = "Heat gain must be at least -50") @Max(value = 50, message = "Heat gain must not exceed 50") int heatGain,
      @NotBlank(message = "Reason is required") @Size(max = 500, message = "Reason must not exceed 500 characters") String reason) {}

  public record ResolutionAttemptRequest(
      @Min(value = 1, message = "Dice roll must be between 1 and 20") @Max(value = 20, message = "Dice roll must be between 1 and 20") Integer wrestler1Roll,
      @Min(value = 1, message = "Dice roll must be between 1 and 20") @Max(value = 20, message = "Dice roll must be between 1 and 20") Integer wrestler2Roll) {}

  public record EndRivalryRequest(
      @NotBlank(message = "Reason is required") @Size(max = 500, message = "Reason must not exceed 500 characters") String reason) {}

  public record UpdateStorylineRequest(
      @Size(max = 2000, message = "Storyline notes must not exceed 2000 characters") String storylineNotes) {}

  public record ErrorResponse(String message) {}

  public record RivalryHistoryResponse(boolean hasHistory) {}
}
