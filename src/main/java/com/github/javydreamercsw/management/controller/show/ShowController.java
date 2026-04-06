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
package com.github.javydreamercsw.management.controller.show;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.service.show.ShowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing shows in the ATW RPG system. Provides endpoints for show management,
 * calendar views, and scheduling.
 */
@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
@Validated
@Tag(name = "Show Management", description = "ATW RPG Show management operations")
public class ShowController {

  private final ShowService showService;

  @Operation(
      summary = "Get all shows",
      description = "Retrieves all shows with optional pagination and sorting")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Shows retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
      })
  @GetMapping
  public ResponseEntity<Page<Show>> getAllShows(
      @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "Sort field") @RequestParam(defaultValue = "showDate") String sortBy,
      @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc")
          String sortDir) {

    Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<Show> shows = showService.getAllShows(pageable);
    return ResponseEntity.ok(shows);
  }

  @Operation(summary = "Get show by ID", description = "Retrieves a specific show by its ID")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Show found"),
        @ApiResponse(responseCode = "404", description = "Show not found")
      })
  @GetMapping("/{id}")
  public ResponseEntity<Show> getShowById(
      @Parameter(description = "Show ID") @PathVariable Long id) {
    Optional<Show> show = showService.getShowById(id);
    return show.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @Operation(
      summary = "Get shows by date range",
      description = "Retrieves shows within a specific date range for calendar view")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Shows retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid date parameters")
      })
  @GetMapping("/calendar")
  public ResponseEntity<List<Show>> getShowsByDateRange(
      @Parameter(description = "Start date (YYYY-MM-DD)")
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @Parameter(description = "End date (YYYY-MM-DD)")
          @RequestParam
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate) {

    List<Show> shows = showService.getShowsByDateRange(startDate, endDate);
    return ResponseEntity.ok(shows);
  }

  @Operation(
      summary = "Get shows for specific month",
      description = "Retrieves all shows for a specific month and year")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Shows retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid month/year parameters")
      })
  @GetMapping("/calendar/month")
  public ResponseEntity<List<Show>> getShowsForMonth(
      @Parameter(description = "Year (e.g., 2024)") @RequestParam int year,
      @Parameter(description = "Month (1-12)") @RequestParam int month) {

    List<Show> shows = showService.getShowsForMonth(year, month);
    return ResponseEntity.ok(shows);
  }

  @Operation(
      summary = "Get upcoming shows",
      description = "Retrieves upcoming shows from today onwards")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Shows retrieved successfully")})
  @GetMapping("/upcoming")
  public ResponseEntity<List<Show>> getUpcomingShows(
      @Parameter(description = "Maximum number of shows to return")
          @RequestParam(defaultValue = "10")
          int limit) {

    List<Show> shows = showService.getUpcomingShows(limit);
    return ResponseEntity.ok(shows);
  }

  @Operation(
      summary = "Create a new show",
      description = "Creates a new show with the provided details")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Show created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @PostMapping
  public ResponseEntity<Show> createShow(@Valid @RequestBody CreateShowRequest request) {
    Show show =
        showService.createShow(
            request.name(),
            request.description(),
            request.showTypeId(),
            request.showDate(),
            request.seasonId(),
            request.templateId(),
            request.leagueId(),
            request.commentaryTeamId(),
            request.arenaId());
    return ResponseEntity.status(HttpStatus.CREATED).body(show);
  }

  @Operation(
      summary = "Update an existing show",
      description = "Updates an existing show with the provided details")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Show updated successfully"),
        @ApiResponse(responseCode = "404", description = "Show not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
      })
  @PutMapping("/{id}")
  public ResponseEntity<Show> updateShow(
      @Parameter(description = "Show ID") @PathVariable Long id,
      @Valid @RequestBody UpdateShowRequest request) {

    Optional<Show> updatedShow =
        showService.updateShow(
            id,
            request.name(),
            request.description(),
            request.showTypeId(),
            request.showDate(),
            request.seasonId(),
            request.templateId(),
            request.leagueId(),
            request.commentaryTeamId(),
            request.arenaId());

    return updatedShow.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @Operation(summary = "Delete a show", description = "Deletes a show by its ID")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Show deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Show not found")
      })
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteShow(
      @Parameter(description = "Show ID") @PathVariable Long id) {

    boolean deleted = showService.deleteShow(id);
    return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }

  @Operation(
      summary = "Adjudicate a show",
      description = "Adjudicates all pending segments in a show.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Show adjudicated successfully"),
        @ApiResponse(responseCode = "404", description = "Show not found")
      })
  @PostMapping("/{id}/adjudicate")
  public ResponseEntity<Void> adjudicateShow(
      @Parameter(description = "Show ID") @PathVariable Long id) {
    showService.adjudicateShow(id);
    return ResponseEntity.ok().build();
  }

  // ==================== REQUEST/RESPONSE DTOs ====================

  /** Request DTO for creating shows. */
  public record CreateShowRequest(
      @NotBlank String name,
      @NotBlank String description,
      @NotNull Long showTypeId,
      LocalDate showDate,
      Long seasonId,
      Long templateId,
      Long leagueId,
      Long commentaryTeamId,
      Long arenaId) {}

  /** Request DTO for updating shows. */
  public record UpdateShowRequest(
      String name,
      String description,
      Long showTypeId,
      LocalDate showDate,
      Long seasonId,
      Long templateId,
      Long leagueId,
      Long commentaryTeamId,
      Long arenaId) {}
}
