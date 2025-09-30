package com.github.javydreamercsw.management.service.season;

import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing season progression in the ATW RPG system. Handles season lifecycle,
 * statistics tracking, and automated season management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SeasonProgressionService {

  private final SeasonRepository seasonRepository;
  private final SegmentRepository segmentRepository;
  private final Clock clock;

  /**
   * Get comprehensive statistics for a season.
   *
   * @param seasonId ID of the season
   * @return Season statistics
   */
  public Optional<SeasonStatistics> getSeasonStatistics(@NonNull Long seasonId) {
    Optional<Season> seasonOpt = seasonRepository.findById(seasonId);
    if (seasonOpt.isEmpty()) {
      return Optional.empty();
    }

    Season season = seasonOpt.get();
    List<Show> shows = season.getShows();

    // Calculate basic statistics
    int totalShows = shows.size();
    int regularShows =
        (int)
            shows.stream()
                .filter(show -> !show.getType().getName().toLowerCase().contains("ppv"))
                .count();
    int ppvShows = totalShows - regularShows;

    // Get all matches from the season
    List<Segment> allMatches =
        shows.stream().flatMap(show -> segmentRepository.findByShow(show).stream()).toList();

    int totalMatches = allMatches.size();

    // Calculate wrestler statistics
    Map<Wrestler, Long> wrestlerMatchCounts =
        allMatches.stream()
            .flatMap(match -> match.getWrestlers().stream())
            .collect(Collectors.groupingBy(wrestler -> wrestler, Collectors.counting()));

    Map<Wrestler, Long> wrestlerWinCounts =
        allMatches.stream()
            .flatMap(match -> match.getWinners().stream())
            .collect(Collectors.groupingBy(wrestler -> wrestler, Collectors.counting()));

    // Find most active wrestler
    Optional<Wrestler> mostActiveWrestler =
        wrestlerMatchCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey);

    // Find most successful wrestler (highest win rate with minimum 3 matches)
    Optional<Wrestler> mostSuccessfulWrestler =
        wrestlerWinCounts.entrySet().stream()
            .filter(entry -> wrestlerMatchCounts.getOrDefault(entry.getKey(), 0L) >= 3)
            .max(
                (e1, e2) -> {
                  double winRate1 = (double) e1.getValue() / wrestlerMatchCounts.get(e1.getKey());
                  double winRate2 = (double) e2.getValue() / wrestlerMatchCounts.get(e2.getKey());
                  return Double.compare(winRate1, winRate2);
                })
            .map(Map.Entry::getKey);

    // Calculate season duration
    long seasonDurationDays = 0;
    if (season.getStartDate() != null) {
      Instant endDate = season.getEndDate() != null ? season.getEndDate() : clock.instant();
      seasonDurationDays = ChronoUnit.DAYS.between(season.getStartDate(), endDate);
    }

    return Optional.of(
        new SeasonStatistics(
            season.getId(),
            season.getName(),
            totalShows,
            regularShows,
            ppvShows,
            totalMatches,
            wrestlerMatchCounts.size(),
            mostActiveWrestler.map(Wrestler::getName).orElse("N/A"),
            mostSuccessfulWrestler.map(Wrestler::getName).orElse("N/A"),
            seasonDurationDays,
            season.getIsActive()));
  }

  /**
   * Get statistics for the currently active season.
   *
   * @return Active season statistics
   */
  public Optional<SeasonStatistics> getActiveSeasonStatistics() {
    return seasonRepository
        .findActiveSeason()
        .flatMap(season -> getSeasonStatistics(season.getId()));
  }

  /**
   * Check if the active season should be ended based on criteria.
   *
   * @return true if season should end
   */
  public boolean shouldEndActiveSeason() {
    Optional<Season> activeSeasonOpt = seasonRepository.findActiveSeason();
    if (activeSeasonOpt.isEmpty()) {
      return false;
    }

    Season activeSeason = activeSeasonOpt.get();

    // End season if it has been running for more than 6 months
    if (activeSeason.getStartDate() != null) {
      long monthsRunning =
          ChronoUnit.DAYS.between(activeSeason.getStartDate(), clock.instant()) / 30;
      if (monthsRunning >= 6) {
        log.info(
            "Season {} should end - running for {} months", activeSeason.getName(), monthsRunning);
        return true;
      }
    }

    // End season if it has more than 20 shows
    if (activeSeason.getShows().size() >= 20) {
      log.info(
          "Season {} should end - has {} shows",
          activeSeason.getName(),
          activeSeason.getShows().size());
      return true;
    }

    return false;
  }

  /**
   * End the active season and create a new one.
   *
   * @param newSeasonName Name for the new season
   * @param newSeasonDescription Description for the new season
   * @return The new season
   */
  @Transactional
  public Optional<Season> progressToNextSeason(
      @NonNull String newSeasonName, @NonNull String newSeasonDescription) {
    try {
      // End current season
      Optional<Season> activeSeasonOpt = seasonRepository.findActiveSeason();
      if (activeSeasonOpt.isPresent()) {
        Season activeSeason = activeSeasonOpt.get();
        activeSeason.endSeason();
        seasonRepository.save(activeSeason);

        log.info(
            "Ended season: {} (Duration: {} days, Shows: {})",
            activeSeason.getName(),
            ChronoUnit.DAYS.between(activeSeason.getStartDate(), clock.instant()),
            activeSeason.getShows().size());
      }

      // Create new season
      Season newSeason = new Season();
      newSeason.setName(newSeasonName);
      newSeason.setDescription(newSeasonDescription);

      newSeason.setShowsPerPpv(5); // Default
      newSeason.setIsActive(true);
      newSeason.setStartDate(clock.instant());
      newSeason.setCreationDate(clock.instant());

      Season savedSeason = seasonRepository.save(newSeason);
      log.info("Started new season: {}", savedSeason.getName());

      return Optional.of(savedSeason);

    } catch (Exception e) {
      log.error("Error progressing to next season: {}", e.getMessage(), e);
      return Optional.empty();
    }
  }

  /**
   * Get season progression recommendations.
   *
   * @return List of recommendations for season management
   */
  public List<String> getSeasonProgressionRecommendations() {
    List<String> recommendations = new java.util.ArrayList<>();

    Optional<Season> activeSeasonOpt = seasonRepository.findActiveSeason();
    if (activeSeasonOpt.isEmpty()) {
      recommendations.add("No active season found. Consider creating a new season.");
      return recommendations;
    }

    Season activeSeason = activeSeasonOpt.get();

    // Check if PPV is needed
    if (activeSeason.isTimeForPpv()) {
      recommendations.add(
          "Time for a PPV! The active season has enough shows for the next pay-per-view event.");
    }

    // Check season duration
    if (activeSeason.getStartDate() != null) {
      long daysRunning = ChronoUnit.DAYS.between(activeSeason.getStartDate(), clock.instant());
      if (daysRunning > 150) { // ~5 months
        recommendations.add(
            "Season has been running for "
                + daysRunning
                + " days. Consider planning season finale.");
      }
    }

    // Check show count
    int showCount = activeSeason.getShows().size();
    if (showCount >= 15) {
      recommendations.add(
          "Season has " + showCount + " shows. Consider building toward season climax.");
    }

    if (recommendations.isEmpty()) {
      recommendations.add("Season is progressing well. Continue with regular show booking.");
    }

    return recommendations;
  }

  /**
   * Get historical season comparison.
   *
   * @return List of all seasons with basic statistics
   */
  public List<SeasonSummary> getSeasonHistory() {
    return seasonRepository.findAll().stream()
        .map(
            season -> {
              int showCount = season.getShows().size();
              long durationDays = 0;
              if (season.getStartDate() != null) {
                Instant endDate =
                    season.getEndDate() != null ? season.getEndDate() : clock.instant();
                durationDays = ChronoUnit.DAYS.between(season.getStartDate(), endDate);
              }

              return new SeasonSummary(
                  season.getId(), season.getName(), showCount, durationDays, season.getIsActive());
            })
        .toList();
  }

  /** Record class for comprehensive season statistics. */
  public record SeasonStatistics(
      Long seasonId,
      String seasonName,
      int totalShows,
      int regularShows,
      int ppvShows,
      int totalMatches,
      int uniqueWrestlers,
      String mostActiveWrestler,
      String mostSuccessfulWrestler,
      long durationDays,
      boolean isActive) {}

  /** Record class for season summary information. */
  public record SeasonSummary(
      Long seasonId, String seasonName, int totalShows, long durationDays, boolean isActive) {}
}
