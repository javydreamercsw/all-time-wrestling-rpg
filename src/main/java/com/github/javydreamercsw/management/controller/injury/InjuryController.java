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
package com.github.javydreamercsw.management.controller.injury;

import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.dto.InjuryResponseDTO;
import com.github.javydreamercsw.management.service.injury.InjuryService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing injuries in the ATW RPG system. Provides endpoints for injury
 * tracking, healing mechanics, and health management.
 */
@RestController
@RequestMapping("/api/injuries")
@Validated
@Tag(name = "Injury Management", description = "ATW RPG Injury tracking and healing operations")
public class InjuryController {

  private final InjuryService injuryService;
  private final WrestlerRepository wrestlerRepository;

  public InjuryController(InjuryService injuryService, WrestlerRepository wrestlerRepository) {
    this.injuryService = injuryService;
    this.wrestlerRepository = wrestlerRepository;
  }

  @Operation(summary = "Create a new injury", description = "Creates a new injury for a wrestler")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Injury created successfully"),
        @ApiResponse(responseCode = "404", description = "Wrestler not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
      })
  @PostMapping
  public ResponseEntity<Object> createInjury(@Valid @RequestBody CreateInjuryRequest request) {
    Optional<Injury> injury =
        injuryService.createInjury(
            request.wrestlerId(),
            request.name(),
            request.description(),
            request.severity(),
            request.injuryNotes());

    if (injury.isPresent()) {
      return ResponseEntity.status(HttpStatus.CREATED).body(new InjuryResponseDTO(injury.get()));
    } else {
      return ResponseEntity.badRequest()
          .body(new ErrorResponse("Cannot create injury - wrestler not found"));
    }
  }

  @Operation(
      summary = "Create injury from bumps",
      description = "Converts 3+ bumps into an injury for a wrestler")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Injury created from bumps"),
        @ApiResponse(
            responseCode = "400",
            description = "Wrestler doesn't have enough bumps or not found"),
        @ApiResponse(responseCode = "404", description = "Wrestler not found")
      })
  @PostMapping("/from-bumps/{wrestlerId}")
  public ResponseEntity<Object> createInjuryFromBumps(@PathVariable Long wrestlerId) {
    // First check if wrestler exists and has enough bumps
    Optional<Wrestler> wrestlerOpt = wrestlerRepository.findById(wrestlerId);
    if (wrestlerOpt.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new ErrorResponse("Wrestler not found"));
    }

    Wrestler wrestler = wrestlerOpt.get();
    if (wrestler.getBumps() < 3) {
      return ResponseEntity.badRequest()
          .body(new ErrorResponse("Wrestler has less than 3 bumps (" + wrestler.getBumps() + ")"));
    }

    Optional<Injury> injury = injuryService.createInjuryFromBumps(wrestlerId);

    if (injury.isPresent()) {
      return ResponseEntity.status(HttpStatus.CREATED).body(new InjuryResponseDTO(injury.get()));
    } else {
      return ResponseEntity.badRequest().body(new ErrorResponse("Failed to create injury"));
    }
  }

  @Operation(summary = "Get all injuries", description = "Retrieves all injuries with pagination")
  @GetMapping
  public ResponseEntity<Page<Injury>> getAllInjuries(
      @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "Sort field") @RequestParam(defaultValue = "injuryDate")
          String sortBy,
      @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc")
          String sortDir) {

    Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<Injury> injuries = injuryService.getAllInjuries(pageable);
    return ResponseEntity.ok(injuries);
  }

  @Operation(summary = "Get injury by ID", description = "Retrieves a specific injury by its ID")
  @GetMapping("/{id}")
  public ResponseEntity<Injury> getInjuryById(@PathVariable Long id) {
    Optional<Injury> injury = injuryService.getInjuryById(id);
    return injury.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @Operation(
      summary = "Get active injuries for wrestler",
      description = "Gets all active injuries for a specific wrestler")
  @GetMapping("/wrestler/{wrestlerId}/active")
  public ResponseEntity<List<Injury>> getActiveInjuriesForWrestler(@PathVariable Long wrestlerId) {
    List<Injury> injuries = injuryService.getActiveInjuriesForWrestler(wrestlerId);
    return ResponseEntity.ok(injuries);
  }

  @Operation(
      summary = "Get all injuries for wrestler",
      description = "Gets all injuries (active and healed) for a specific wrestler")
  @GetMapping("/wrestler/{wrestlerId}")
  public ResponseEntity<List<Injury>> getAllInjuriesForWrestler(@PathVariable Long wrestlerId) {
    List<Injury> injuries = injuryService.getAllInjuriesForWrestler(wrestlerId);
    return ResponseEntity.ok(injuries);
  }

  @Operation(
      summary = "Get injuries by severity",
      description = "Gets all injuries of a specific severity level")
  @GetMapping("/severity/{severity}")
  public ResponseEntity<List<Injury>> getInjuriesBySeverity(@PathVariable InjurySeverity severity) {
    List<Injury> injuries = injuryService.getInjuriesBySeverity(severity);
    return ResponseEntity.ok(injuries);
  }

  @Operation(
      summary = "Get all active injuries",
      description = "Gets all currently active injuries")
  @GetMapping("/active")
  public ResponseEntity<List<Injury>> getAllActiveInjuries() {
    List<Injury> injuries = injuryService.getAllActiveInjuries();
    return ResponseEntity.ok(injuries);
  }

  @Operation(
      summary = "Get wrestlers with active injuries",
      description = "Gets all wrestlers who have active injuries")
  @GetMapping("/wrestlers-with-injuries")
  public ResponseEntity<List<Wrestler>> getWrestlersWithActiveInjuries() {
    List<Wrestler> wrestlers = injuryService.getWrestlersWithActiveInjuries();
    return ResponseEntity.ok(wrestlers);
  }

  @Operation(
      summary = "Get healable injuries",
      description = "Gets all injuries that can be healed")
  @GetMapping("/healable")
  public ResponseEntity<List<Injury>> getHealableInjuries() {
    List<Injury> injuries = injuryService.getHealableInjuries();
    return ResponseEntity.ok(injuries);
  }

  @Operation(
      summary = "Attempt healing",
      description = "Attempts to heal an injury using dice roll and fan cost")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Healing attempt completed"),
        @ApiResponse(responseCode = "404", description = "Injury not found"),
        @ApiResponse(responseCode = "400", description = "Healing failed - see response message")
      })
  @PostMapping("/{id}/heal")
  public ResponseEntity<InjuryService.HealingResult> attemptHealing(
      @PathVariable Long id, @Valid @RequestBody HealingAttemptRequest request) {
    InjuryService.HealingResult result = injuryService.attemptHealing(id, request.diceRoll());
    return ResponseEntity.ok(result);
  }

  @Operation(
      summary = "Get total health penalty for wrestler",
      description = "Gets the total health penalty from all active injuries for a wrestler")
  @GetMapping("/wrestler/{wrestlerId}/health-penalty")
  public ResponseEntity<HealthPenaltyResponse> getTotalHealthPenaltyForWrestler(
      @PathVariable Long wrestlerId) {
    Integer penalty = injuryService.getTotalHealthPenaltyForWrestler(wrestlerId);
    return ResponseEntity.ok(new HealthPenaltyResponse(penalty));
  }

  @Operation(summary = "Update injury", description = "Updates injury information")
  @PutMapping("/{id}")
  public ResponseEntity<Injury> updateInjury(
      @PathVariable Long id, @Valid @RequestBody UpdateInjuryRequest request) {
    Optional<Injury> injury =
        injuryService.updateInjury(
            id, request.name(), request.description(), request.injuryNotes());
    return injury.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @Operation(
      summary = "Get injury statistics for wrestler",
      description = "Retrieves comprehensive injury statistics for a wrestler")
  @GetMapping("/wrestler/{wrestlerId}/stats")
  public ResponseEntity<InjuryService.InjuryStats> getInjuryStatsForWrestler(
      @PathVariable Long wrestlerId) {
    InjuryService.InjuryStats stats = injuryService.getInjuryStatsForWrestler(wrestlerId);
    if (stats != null) {
      return ResponseEntity.ok(stats);
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  // ==================== REQUEST/RESPONSE DTOs ====================

  public record CreateInjuryRequest(
      @NotNull(message = "Wrestler ID is required") Long wrestlerId,
      @NotBlank(message = "Injury name is required") @Size(max = 255, message = "Injury name must not exceed 255 characters") String name,
      @Size(max = 1000, message = "Description must not exceed 1000 characters") String description,
      @NotNull(message = "Severity is required") InjurySeverity severity,
      @Size(max = 2000, message = "Injury notes must not exceed 2000 characters") String injuryNotes) {}

  public record HealingAttemptRequest(
      @Min(value = 1, message = "Dice roll must be between 1 and 6") @Max(value = 6, message = "Dice roll must be between 1 and 6") Integer diceRoll) {}

  public record UpdateInjuryRequest(
      @Size(max = 255, message = "Injury name must not exceed 255 characters") String name,
      @Size(max = 1000, message = "Description must not exceed 1000 characters") String description,
      @Size(max = 2000, message = "Injury notes must not exceed 2000 characters") String injuryNotes) {}

  public record ErrorResponse(String message) {}

  public record HealthPenaltyResponse(Integer totalHealthPenalty) {}
}
