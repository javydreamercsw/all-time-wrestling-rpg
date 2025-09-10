package com.github.javydreamercsw.management.controller.season;

import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.service.season.SeasonProgressionService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.ShowBookingService;
import com.github.javydreamercsw.management.service.storyline.StorylineContinuityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for enhanced season and show management in the ATW RPG system. Provides endpoints
 * for automated show booking, season progression, and storyline continuity.
 */
@RestController
@RequestMapping("/api/season-management")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Season Management",
    description = "Enhanced season and show management with automation")
public class SeasonManagementController {

  private final SeasonService seasonService;
  private final SeasonProgressionService seasonProgressionService;
  private final ShowBookingService showBookingService;
  private final StorylineContinuityService storylineContinuityService;

  // ==================== SHOW BOOKING ENDPOINTS ====================

  @Operation(
      summary = "Book a show with automated segments",
      description =
          "Books a complete show with automatically generated segments based on storylines")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Show booked successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid booking parameters"),
        @ApiResponse(responseCode = "404", description = "Show type not found")
      })
  @PostMapping("/book-show")
  public ResponseEntity<Object> bookShow(@Valid @RequestBody BookShowRequest request) {
    Optional<Show> show =
        showBookingService.bookShow(
            request.showName(),
            request.showDescription(),
            request.showType(),
            request.segmentCount());

    if (show.isPresent()) {
      ShowBookingService.ShowStatistics stats =
          showBookingService.getShowStatistics(show.get().getId());
      return ResponseEntity.status(201).body(new BookShowResponse(show.get(), stats));
    } else {
      return ResponseEntity.badRequest()
          .body("Failed to book show - check parameters and try again");
    }
  }

  @Operation(
      summary = "Book a PPV event",
      description = "Books a pay-per-view event with enhanced segment quality and storyline focus")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "PPV booked successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid PPV parameters")
      })
  @PostMapping("/book-ppv")
  public ResponseEntity<Object> bookPPV(@Valid @RequestBody BookPPVRequest request) {
    Optional<Show> ppv = showBookingService.bookPPV(request.ppvName(), request.ppvDescription());

    if (ppv.isPresent()) {
      ShowBookingService.ShowStatistics stats =
          showBookingService.getShowStatistics(ppv.get().getId());
      return ResponseEntity.status(201).body(new BookShowResponse(ppv.get(), stats));
    } else {
      return ResponseEntity.badRequest()
          .body("Failed to book PPV - check parameters and try again");
    }
  }

  @Operation(
      summary = "Get show statistics",
      description = "Gets detailed statistics for a specific show")
  @GetMapping("/show/{showId}/statistics")
  public ResponseEntity<ShowBookingService.ShowStatistics> getShowStatistics(
      @PathVariable @NonNull Long showId) {
    ShowBookingService.ShowStatistics stats = showBookingService.getShowStatistics(showId);
    return ResponseEntity.ok(stats);
  }

  // ==================== SEASON PROGRESSION ENDPOINTS ====================

  @Operation(
      summary = "Get season statistics",
      description = "Gets comprehensive statistics for a season")
  @GetMapping("/season/{seasonId}/statistics")
  public ResponseEntity<SeasonProgressionService.SeasonStatistics> getSeasonStatistics(
      @PathVariable Long seasonId) {
    Optional<SeasonProgressionService.SeasonStatistics> stats =
        seasonProgressionService.getSeasonStatistics(seasonId);
    return stats.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @Operation(
      summary = "Get active season statistics",
      description = "Gets statistics for the currently active season")
  @GetMapping("/active-season/statistics")
  public ResponseEntity<SeasonProgressionService.SeasonStatistics> getActiveSeasonStatistics() {
    Optional<SeasonProgressionService.SeasonStatistics> stats =
        seasonProgressionService.getActiveSeasonStatistics();
    return stats.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @Operation(
      summary = "Get season progression recommendations",
      description = "Gets recommendations for season management")
  @GetMapping("/season/recommendations")
  public ResponseEntity<List<String>> getSeasonRecommendations() {
    List<String> recommendations = seasonProgressionService.getSeasonProgressionRecommendations();
    return ResponseEntity.ok(recommendations);
  }

  @Operation(
      summary = "Progress to next season",
      description = "Ends the current season and starts a new one")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "New season started successfully"),
        @ApiResponse(responseCode = "400", description = "Failed to progress season")
      })
  @PostMapping("/progress-season")
  public ResponseEntity<Object> progressToNextSeason(
      @Valid @RequestBody ProgressSeasonRequest request) {
    Optional<Season> newSeason =
        seasonProgressionService.progressToNextSeason(
            request.newSeasonName(), request.newSeasonDescription());

    return newSeason
        .<ResponseEntity<Object>>map(season -> ResponseEntity.status(201).body(season))
        .orElseGet(() -> ResponseEntity.badRequest().body("Failed to progress to next season"));
  }

  @Operation(
      summary = "Get season history",
      description = "Gets historical information about all seasons")
  @GetMapping("/season/history")
  public ResponseEntity<List<SeasonProgressionService.SeasonSummary>> getSeasonHistory() {
    List<SeasonProgressionService.SeasonSummary> history =
        seasonProgressionService.getSeasonHistory();
    return ResponseEntity.ok(history);
  }

  // ==================== STORYLINE CONTINUITY ENDPOINTS ====================

  @Operation(
      summary = "Get active storylines",
      description = "Gets all currently active storylines")
  @GetMapping("/storylines/active")
  public ResponseEntity<List<StorylineContinuityService.ActiveStoryline>> getActiveStorylines() {
    List<StorylineContinuityService.ActiveStoryline> storylines =
        storylineContinuityService.getActiveStorylines();
    return ResponseEntity.ok(storylines);
  }

  @Operation(
      summary = "Get storyline suggestions",
      description = "Gets suggestions for upcoming storyline developments")
  @GetMapping("/storylines/suggestions")
  public ResponseEntity<List<StorylineContinuityService.StorylineSuggestion>>
      getStorylineSuggestions() {
    List<StorylineContinuityService.StorylineSuggestion> suggestions =
        storylineContinuityService.getStorylineSuggestions();
    return ResponseEntity.ok(suggestions);
  }

  @Operation(
      summary = "Analyze storyline progression",
      description = "Analyzes storyline progression for a specific show")
  @GetMapping("/storylines/progression/{showId}")
  public ResponseEntity<StorylineContinuityService.StorylineProgression>
      analyzeStorylineProgression(@PathVariable Long showId) {
    StorylineContinuityService.StorylineProgression progression =
        storylineContinuityService.analyzeStorylineProgression(showId);
    return ResponseEntity.ok(progression);
  }

  @Operation(
      summary = "Get storyline continuity health",
      description = "Assesses the health of storyline continuity")
  @GetMapping("/storylines/health")
  public ResponseEntity<StorylineContinuityService.StorylineContinuityHealth>
      getStorylineContinuityHealth() {
    StorylineContinuityService.StorylineContinuityHealth health =
        storylineContinuityService.assessContinuityHealth();
    return ResponseEntity.ok(health);
  }

  // ==================== UTILITY ENDPOINTS ====================

  @Operation(
      summary = "Check if season should end",
      description = "Checks if the active season should be ended based on criteria")
  @GetMapping("/season/should-end")
  public ResponseEntity<Boolean> shouldEndActiveSeason() {
    boolean shouldEnd = seasonProgressionService.shouldEndActiveSeason();
    return ResponseEntity.ok(shouldEnd);
  }

  @Operation(
      summary = "Get management dashboard",
      description = "Gets comprehensive management information for dashboard")
  @GetMapping("/dashboard")
  public ResponseEntity<ManagementDashboard> getManagementDashboard() {
    // Get active season stats
    Optional<SeasonProgressionService.SeasonStatistics> seasonStats =
        seasonProgressionService.getActiveSeasonStatistics();

    // Get storyline health
    StorylineContinuityService.StorylineContinuityHealth storylineHealth =
        storylineContinuityService.assessContinuityHealth();

    // Get recommendations
    List<String> recommendations = seasonProgressionService.getSeasonProgressionRecommendations();

    // Get active storylines count
    int activeStorylinesCount = storylineContinuityService.getActiveStorylines().size();

    ManagementDashboard dashboard =
        new ManagementDashboard(
            seasonStats.orElse(null),
            storylineHealth,
            recommendations,
            activeStorylinesCount,
            seasonProgressionService.shouldEndActiveSeason());

    return ResponseEntity.ok(dashboard);
  }

  // ==================== REQUEST/RESPONSE DTOs ====================

  public record BookShowRequest(
      @NotBlank String showName,
      @NotBlank String showDescription,
      @NotBlank String showType,
      @Min(3) @Max(10) int segmentCount) {}

  public record BookPPVRequest(@NotBlank String ppvName, @NotBlank String ppvDescription) {}

  public record ProgressSeasonRequest(
      @NotBlank String newSeasonName, @NotBlank String newSeasonDescription) {}

  public record BookShowResponse(Show show, ShowBookingService.ShowStatistics statistics) {}

  public record ManagementDashboard(
      SeasonProgressionService.SeasonStatistics seasonStatistics,
      StorylineContinuityService.StorylineContinuityHealth storylineHealth,
      List<String> recommendations,
      int activeStorylinesCount,
      boolean shouldEndSeason) {}
}
