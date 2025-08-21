package com.github.javydreamercsw.management.controller.season;

import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.service.season.SeasonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
 * REST controller for managing seasons in the ATW RPG system. Provides endpoints for season
 * lifecycle, PPV scheduling, and statistics.
 */
@RestController
@RequestMapping("/api/seasons")
@Validated
@Tag(name = "Season Management", description = "ATW RPG Season management operations")
public class SeasonController {

  private final SeasonService seasonService;

  public SeasonController(SeasonService seasonService) {
    this.seasonService = seasonService;
  }

  @Operation(
      summary = "Create a new season",
      description = "Creates a new season and ends any currently active season")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Season created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  @PostMapping
  public ResponseEntity<Season> createSeason(@Valid @RequestBody CreateSeasonRequest request) {
    Season season =
        seasonService.createSeason(request.name(), request.description(), request.showsPerPpv());
    return ResponseEntity.status(HttpStatus.CREATED).body(season);
  }

  @Operation(summary = "Get all seasons", description = "Retrieves all seasons with pagination")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Seasons retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
      })
  @GetMapping
  public ResponseEntity<Page<Season>> getAllSeasons(
      @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") @Min(0) int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "20") @Min(1) int size,
      @Parameter(description = "Sort field") @RequestParam(defaultValue = "seasonNumber")
          String sortBy,
      @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc")
          String sortDir) {

    Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<Season> seasons = seasonService.getAllSeasons(pageable);
    return ResponseEntity.ok(seasons);
  }

  @Operation(summary = "Get season by ID", description = "Retrieves a specific season by its ID")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Season found"),
        @ApiResponse(responseCode = "404", description = "Season not found")
      })
  @GetMapping("/{id}")
  public ResponseEntity<Season> getSeasonById(
      @Parameter(description = "Season ID") @PathVariable Long id) {
    Optional<Season> season = seasonService.getSeasonById(id);
    return season.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @Operation(summary = "Get active season", description = "Retrieves the currently active season")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Active season found"),
        @ApiResponse(responseCode = "404", description = "No active season")
      })
  @GetMapping("/active")
  public ResponseEntity<Season> getActiveSeason() {
    Optional<Season> season = seasonService.getActiveSeason();
    return season.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @Operation(summary = "End current season", description = "Ends the currently active season")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Season ended successfully"),
        @ApiResponse(responseCode = "404", description = "No active season to end")
      })
  @PostMapping("/end-current")
  public ResponseEntity<Season> endCurrentSeason() {
    Optional<Season> season = seasonService.endCurrentSeason();
    return season.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @Operation(summary = "Update season", description = "Updates season information")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Season updated successfully"),
        @ApiResponse(responseCode = "404", description = "Season not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
      })
  @PutMapping("/{id}")
  public ResponseEntity<Season> updateSeason(
      @Parameter(description = "Season ID") @PathVariable Long id,
      @Valid @RequestBody UpdateSeasonRequest request) {
    Optional<Season> season =
        seasonService.updateSeason(
            id, request.name(), request.description(), request.showsPerPpv());
    return season.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @Operation(
      summary = "Delete season",
      description = "Deletes a season (only if inactive and has no shows)")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Season deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Season not found"),
        @ApiResponse(
            responseCode = "409",
            description = "Season cannot be deleted (active or has shows)")
      })
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteSeason(
      @Parameter(description = "Season ID") @PathVariable Long id) {
    boolean deleted = seasonService.deleteSeason(id);
    if (deleted) {
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
  }

  @Operation(
      summary = "Check if time for PPV",
      description = "Checks if it's time to schedule a PPV in the active season")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "PPV check completed")})
  @GetMapping("/ppv-check")
  public ResponseEntity<PpvCheckResponse> checkTimeForPpv() {
    boolean timeForPpv = seasonService.isTimeForPpv();
    List<Season> seasonsNeedingPpv = seasonService.getSeasonsNeedingPpv();
    return ResponseEntity.ok(new PpvCheckResponse(timeForPpv, seasonsNeedingPpv));
  }

  @Operation(
      summary = "Get season statistics",
      description = "Retrieves comprehensive statistics for a season")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Season not found")
      })
  @GetMapping("/{id}/stats")
  public ResponseEntity<SeasonService.SeasonStats> getSeasonStats(
      @Parameter(description = "Season ID") @PathVariable Long id) {
    SeasonService.SeasonStats stats = seasonService.getSeasonStats(id);
    if (stats != null) {
      return ResponseEntity.ok(stats);
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  // ==================== REQUEST/RESPONSE DTOs ====================

  public record CreateSeasonRequest(
      @NotBlank(message = "Season name is required") @Size(max = 255, message = "Season name must not exceed 255 characters") String name,
      @Size(max = 1000, message = "Description must not exceed 1000 characters") String description,
      @Min(value = 1, message = "Shows per PPV must be at least 1") Integer showsPerPpv) {}

  public record UpdateSeasonRequest(
      @Size(max = 255, message = "Season name must not exceed 255 characters") String name,
      @Size(max = 1000, message = "Description must not exceed 1000 characters") String description,
      @Min(value = 1, message = "Shows per PPV must be at least 1") Integer showsPerPpv) {}

  public record PpvCheckResponse(boolean timeForPpv, List<Season> seasonsNeedingPpv) {}
}
