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

import com.github.javydreamercsw.management.domain.injury.InjuryType;
import com.github.javydreamercsw.management.service.injury.InjuryTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing injury types in the ATW RPG system. Provides endpoints for CRUD
 * operations on injury type reference data.
 */
@RestController
@RequestMapping("/api/injury-types")
@RequiredArgsConstructor
@Tag(name = "Injury Types", description = "Injury type reference data management operations")
public class InjuryTypeController {

  private final InjuryTypeService injuryTypeService;

  @Operation(
      summary = "Get all injury types",
      description = "Retrieve all injury types with pagination")
  @GetMapping
  public ResponseEntity<Page<InjuryType>> getAllInjuryTypes(
      @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "Sort field") @RequestParam(defaultValue = "injuryName")
          String sortBy,
      @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc")
          String sortDir) {

    Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<InjuryType> injuryTypes = injuryTypeService.getAllInjuryTypes(pageable);
    return ResponseEntity.ok(injuryTypes);
  }

  @Operation(
      summary = "Get injury type by ID",
      description = "Retrieve a specific injury type by its ID")
  @GetMapping("/{id}")
  public ResponseEntity<InjuryType> getInjuryTypeById(@PathVariable Long id) {
    Optional<InjuryType> injuryType = injuryTypeService.getInjuryTypeById(id);
    return injuryType.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @Operation(
      summary = "Get injury type by name",
      description = "Retrieve a specific injury type by its name")
  @GetMapping("/by-name/{name}")
  public ResponseEntity<InjuryType> getInjuryTypeByName(@PathVariable String name) {
    Optional<InjuryType> injuryType = injuryTypeService.findByName(name);
    return injuryType.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @Operation(
      summary = "Get injury types with special effects",
      description = "Retrieve injury types that have special effects")
  @GetMapping("/with-special-effects")
  public ResponseEntity<List<InjuryType>> getInjuryTypesWithSpecialEffects() {
    List<InjuryType> injuryTypes = injuryTypeService.findWithSpecialEffects();
    return ResponseEntity.ok(injuryTypes);
  }

  @Operation(
      summary = "Get injury type statistics",
      description = "Retrieve statistics about injury type effects")
  @GetMapping("/stats")
  public ResponseEntity<InjuryTypeService.InjuryTypeStats> getInjuryTypeStats() {
    InjuryTypeService.InjuryTypeStats stats = injuryTypeService.getInjuryTypeStats();
    return ResponseEntity.ok(stats);
  }

  @Operation(summary = "Create injury type", description = "Create a new injury type")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Injury type created successfully"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data or injury type already exists"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @PostMapping
  public ResponseEntity<Object> createInjuryType(
      @Valid @RequestBody CreateInjuryTypeRequest request) {
    try {
      InjuryType injuryType =
          injuryTypeService.createInjuryType(
              request.injuryName(),
              request.healthEffect(),
              request.staminaEffect(),
              request.cardEffect(),
              request.specialEffects());
      return ResponseEntity.status(HttpStatus.CREATED).body(injuryType);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }
  }

  @Operation(summary = "Update injury type", description = "Update an existing injury type")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Injury type updated successfully"),
        @ApiResponse(responseCode = "404", description = "Injury type not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
      })
  @PutMapping("/{id}")
  public ResponseEntity<Object> updateInjuryType(
      @PathVariable Long id, @Valid @RequestBody UpdateInjuryTypeRequest request) {
    try {
      Optional<InjuryType> updatedInjuryType =
          injuryTypeService.updateInjuryType(
              id,
              request.injuryName(),
              request.healthEffect(),
              request.staminaEffect(),
              request.cardEffect(),
              request.specialEffects());

      if (updatedInjuryType.isPresent()) {
        return ResponseEntity.ok(updatedInjuryType.get());
      } else {
        return ResponseEntity.notFound().build();
      }
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }
  }

  @Operation(summary = "Delete injury type", description = "Delete an injury type")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Injury type deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Injury type not found"),
        @ApiResponse(
            responseCode = "400",
            description = "Cannot delete injury type (may be in use)")
      })
  @DeleteMapping("/{id}")
  public ResponseEntity<Object> deleteInjuryType(@PathVariable Long id) {
    try {
      boolean deleted = injuryTypeService.deleteInjuryType(id);
      if (deleted) {
        return ResponseEntity.noContent().build();
      } else {
        return ResponseEntity.notFound().build();
      }
    } catch (IllegalStateException e) {
      return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }
  }

  // ==================== REQUEST/RESPONSE DTOs ====================

  /** Request DTO for creating injury types. */
  public record CreateInjuryTypeRequest(
      @Size(min = 1, max = 100, message = "Injury name must be between 1 and 100 characters") String injuryName,
      Integer healthEffect,
      Integer staminaEffect,
      Integer cardEffect,
      @Size(max = 2000, message = "Special effects must not exceed 2000 characters") String specialEffects) {}

  /** Request DTO for updating injury types. */
  public record UpdateInjuryTypeRequest(
      @Size(min = 1, max = 100, message = "Injury name must be between 1 and 100 characters") String injuryName,
      Integer healthEffect,
      Integer staminaEffect,
      Integer cardEffect,
      @Size(max = 2000, message = "Special effects must not exceed 2000 characters") String specialEffects) {}

  /** Error response DTO. */
  public record ErrorResponse(String message) {}
}
