package com.github.javydreamercsw.management.service.season;

import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.service.show.ShowBookingService;
import com.github.javydreamercsw.management.service.storyline.StorylineContinuityService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduler service for automated season and show management in the ATW RPG system. This service
 * runs periodically to book shows, schedule PPVs, and manage season progression.
 *
 * <p>Can be enabled/disabled via application properties:
 * season.management.scheduler.enabled=true/false
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "season.management.scheduler.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class SeasonManagementScheduler {

  private final SeasonService seasonService;
  private final SeasonProgressionService seasonProgressionService;
  private final ShowBookingService showBookingService;
  private final StorylineContinuityService storylineContinuityService;
  private final Random random = new Random();

  /** Book weekly shows automatically. Runs every 3 days to simulate regular show scheduling. */
  @Scheduled(fixedRate = 259200000) // Every 3 days (259,200,000 milliseconds)
  public void scheduleWeeklyShows() {
    try {
      log.debug("Starting automated weekly show booking...");

      Optional<Season> activeSeasonOpt = seasonService.getActiveSeason();
      if (activeSeasonOpt.isEmpty()) {
        log.debug("No active season found, skipping show booking");
        return;
      }

      Season activeSeason = activeSeasonOpt.get();

      // Check if it's time for a PPV
      if (activeSeason.isTimeForPpv()) {
        bookPPVShow(activeSeason);
      } else {
        bookRegularShow(activeSeason);
      }

      log.info("Completed automated show booking for season: {}", activeSeason.getName());

    } catch (Exception e) {
      log.error("Error during automated show booking", e);
    }
  }

  /** Check season progression and manage season lifecycle. Runs weekly. */
  @Scheduled(cron = "0 0 10 * * SUN") // Every Sunday at 10 AM
  public void manageSeasonProgression() {
    try {
      log.debug("Starting season progression management...");

      // Check if current season should end
      if (seasonProgressionService.shouldEndActiveSeason()) {
        progressToNextSeason();
      } else {
        // Provide season health report
        generateSeasonHealthReport();
      }

      log.info("Completed season progression management");

    } catch (Exception e) {
      log.error("Error during season progression management", e);
    }
  }

  /** Generate storyline continuity reports. Runs twice weekly. */
  @Scheduled(cron = "0 0 9 * * WED,SAT") // Wednesday and Saturday at 9 AM
  public void generateStorylineReports() {
    try {
      log.info("=== STORYLINE CONTINUITY REPORT ===");

      // Get active storylines
      List<StorylineContinuityService.ActiveStoryline> activeStorylines =
          storylineContinuityService.getActiveStorylines();

      if (activeStorylines.isEmpty()) {
        log.info("No active storylines found");
      } else {
        log.info("Active storylines: {}", activeStorylines.size());
        for (StorylineContinuityService.ActiveStoryline storyline : activeStorylines) {
          log.info("- {} ({}): {}", storyline.title(), storyline.type(), storyline.description());
        }
      }

      // Get storyline suggestions
      List<StorylineContinuityService.StorylineSuggestion> suggestions =
          storylineContinuityService.getStorylineSuggestions();

      if (!suggestions.isEmpty()) {
        log.info("Storyline suggestions:");
        for (StorylineContinuityService.StorylineSuggestion suggestion : suggestions) {
          log.info(
              "- [{}] {}: {}", suggestion.priority(), suggestion.title(), suggestion.description());
        }
      }

      // Get continuity health
      StorylineContinuityService.StorylineContinuityHealth health =
          storylineContinuityService.assessContinuityHealth();

      log.info("Storyline Health Score: {}/100", health.healthScore());
      log.info("Active Rivalries: {}", health.activeRivalries());
      log.info("Recent Drama Events: {}", health.recentDramaEvents());

      if (!health.recommendations().isEmpty()) {
        log.info("Recommendations:");
        health.recommendations().forEach(rec -> log.info("- {}", rec));
      }

      log.info("=== END STORYLINE REPORT ===");

    } catch (Exception e) {
      log.error("Error generating storyline reports", e);
    }
  }

  /** Monthly season statistics report. */
  @Scheduled(cron = "0 0 8 1 * *") // First day of each month at 8 AM
  public void generateMonthlySeasonReport() {
    try {
      log.info("=== MONTHLY SEASON REPORT ===");

      Optional<SeasonProgressionService.SeasonStatistics> statsOpt =
          seasonProgressionService.getActiveSeasonStatistics();

      if (statsOpt.isPresent()) {
        SeasonProgressionService.SeasonStatistics stats = statsOpt.get();

        log.info("Season: {} (Season #{})", stats.seasonName(), stats.seasonNumber());
        log.info("Duration: {} days", stats.durationDays());
        log.info(
            "Shows: {} total ({} regular, {} PPV)",
            stats.totalShows(),
            stats.regularShows(),
            stats.ppvShows());
        log.info(
            "Matches: {} total (Avg Rating: {:.1f})",
            stats.totalMatches(),
            stats.averageMatchRating());
        log.info("Wrestlers: {} unique", stats.uniqueWrestlers());
        log.info("Most Active: {}", stats.mostActiveWrestler());
        log.info("Most Successful: {}", stats.mostSuccessfulWrestler());
      } else {
        log.info("No active season statistics available");
      }

      // Get season progression recommendations
      List<String> recommendations = seasonProgressionService.getSeasonProgressionRecommendations();
      if (!recommendations.isEmpty()) {
        log.info("Season Recommendations:");
        recommendations.forEach(rec -> log.info("- {}", rec));
      }

      log.info("=== END MONTHLY REPORT ===");

    } catch (Exception e) {
      log.error("Error generating monthly season report", e);
    }
  }

  // ==================== PRIVATE HELPER METHODS ====================

  private void bookRegularShow(Season activeSeason) {
    String showName = generateShowName(activeSeason);
    String showDescription =
        "Regular weekly wrestling show featuring exciting matches and storyline development";

    // Book 4-6 matches for regular show
    int matchCount = 4 + random.nextInt(3); // 4-6 matches

    Optional<ShowBookingService.ShowStatistics> result =
        showBookingService
            .bookShow(showName, showDescription, "Weekly Show", matchCount)
            .map(show -> showBookingService.getShowStatistics(show.getId()));

    if (result.isPresent()) {
      ShowBookingService.ShowStatistics stats = result.get();
      log.info(
          "Booked regular show '{}' - {} matches, {} wrestlers, avg rating: {:.1f}",
          showName,
          stats.totalMatches(),
          stats.totalWrestlers(),
          stats.averageRating());
    } else {
      log.warn("Failed to book regular show '{}'", showName);
    }
  }

  private void bookPPVShow(Season activeSeason) {
    String ppvName = generatePPVName(activeSeason);
    String ppvDescription =
        "Special pay-per-view event featuring the biggest matches and storyline conclusions";

    Optional<ShowBookingService.ShowStatistics> result =
        showBookingService
            .bookPPV(ppvName, ppvDescription)
            .map(show -> showBookingService.getShowStatistics(show.getId()));

    if (result.isPresent()) {
      ShowBookingService.ShowStatistics stats = result.get();
      log.info(
          "Booked PPV '{}' - {} matches, {} wrestlers, avg rating: {:.1f}",
          ppvName,
          stats.totalMatches(),
          stats.totalWrestlers(),
          stats.averageRating());
    } else {
      log.warn("Failed to book PPV '{}'", ppvName);
    }
  }

  private void progressToNextSeason() {
    String newSeasonName = generateNewSeasonName();
    String newSeasonDescription =
        "New season bringing fresh storylines, rivalries, and championship opportunities";

    Optional<Season> newSeasonOpt =
        seasonProgressionService.progressToNextSeason(newSeasonName, newSeasonDescription);

    if (newSeasonOpt.isPresent()) {
      Season newSeason = newSeasonOpt.get();
      log.info(
          "SEASON PROGRESSION: Started new season '{}' (Season #{})",
          newSeason.getName(),
          newSeason.getSeasonNumber());

      // Book inaugural show for new season
      bookRegularShow(newSeason);
    } else {
      log.error("Failed to progress to next season");
    }
  }

  private void generateSeasonHealthReport() {
    Optional<SeasonProgressionService.SeasonStatistics> statsOpt =
        seasonProgressionService.getActiveSeasonStatistics();

    if (statsOpt.isPresent()) {
      SeasonProgressionService.SeasonStatistics stats = statsOpt.get();
      log.info(
          "Season Health Check - '{}': {} shows, {:.1f} avg rating, {} days running",
          stats.seasonName(),
          stats.totalShows(),
          stats.averageMatchRating(),
          stats.durationDays());
    }
  }

  private String generateShowName(Season season) {
    String[] showPrefixes = {
      "Monday Night Wrestling",
      "Wrestling Showcase",
      "Ring Warriors",
      "Championship Wrestling",
      "Wrestling Mayhem",
      "Battle Arena"
    };

    String prefix = showPrefixes[random.nextInt(showPrefixes.length)];
    String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd"));

    return prefix + " - " + date;
  }

  private String generatePPVName(Season season) {
    String[] ppvNames = {
      "Clash of Champions",
      "Ultimate Showdown",
      "Wrestling Revolution",
      "Battle for Glory",
      "Championship Chaos",
      "Ring of Fire",
      "Wrestling Supremacy",
      "Title Tournament",
      "Grand Slam Wrestling"
    };

    return ppvNames[random.nextInt(ppvNames.length)]
        + " "
        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy"));
  }

  private String generateNewSeasonName() {
    String[] seasonThemes = {
      "New Era",
      "Revolution",
      "Championship Season",
      "Ultimate Competition",
      "Wrestling Renaissance",
      "Battle Season",
      "Glory Days",
      "Championship Chase"
    };

    return seasonThemes[random.nextInt(seasonThemes.length)]
        + " "
        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy"));
  }
}
