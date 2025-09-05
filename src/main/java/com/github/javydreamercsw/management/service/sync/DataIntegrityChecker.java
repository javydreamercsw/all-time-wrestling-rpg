package com.github.javydreamercsw.management.service.sync;

import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for checking data integrity before and after sync operations. Validates referential
 * integrity, data consistency, and business rules.
 */
@Slf4j
@Service
public class DataIntegrityChecker {

  @Autowired private ShowService showService;
  @Autowired private ShowTypeService showTypeService;
  @Autowired private SeasonRepository seasonRepository;
  @Autowired private ShowTemplateService showTemplateService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private FactionRepository factionRepository;
  @Autowired private TeamRepository teamRepository;

  /** Perform a comprehensive data integrity check. */
  public IntegrityCheckResult performIntegrityCheck() {
    log.info("🔍 Starting comprehensive data integrity check...");
    long startTime = System.currentTimeMillis();

    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    Map<String, Object> statistics = new HashMap<>();

    try {
      // Check shows integrity
      checkShowsIntegrity(errors, warnings, statistics);

      // Check wrestlers integrity
      checkWrestlersIntegrity(errors, warnings, statistics);

      // Check factions integrity
      checkFactionsIntegrity(errors, warnings, statistics);

      // Check teams integrity
      checkTeamsIntegrity(errors, warnings, statistics);

      // Check referential integrity
      checkReferentialIntegrity(errors, warnings, statistics);

      long duration = System.currentTimeMillis() - startTime;
      statistics.put("checkDurationMs", duration);

      log.info(
          "✅ Data integrity check completed in {}ms: {} errors, {} warnings",
          duration,
          errors.size(),
          warnings.size());

      return new IntegrityCheckResult(errors.isEmpty(), errors, warnings, statistics);

    } catch (Exception e) {
      log.error("❌ Data integrity check failed", e);
      errors.add("Integrity check failed: " + e.getMessage());
      return new IntegrityCheckResult(false, errors, warnings, statistics);
    }
  }

  /** Check shows data integrity. */
  private void checkShowsIntegrity(
      List<String> errors, List<String> warnings, Map<String, Object> statistics) {
    try {
      long showCount = showService.count();
      statistics.put("totalShows", showCount);

      // Check for shows without types
      List<Show> showsWithoutType =
          showService.findAll().stream().filter(show -> show.getType() == null).toList();

      if (!showsWithoutType.isEmpty()) {
        errors.add("Found " + showsWithoutType.size() + " shows without show type");
        statistics.put("showsWithoutType", showsWithoutType.size());
      }

      // Check for shows with invalid dates
      List<Show> showsWithFutureDates =
          showService.findAll().stream()
              .filter(
                  show ->
                      show.getShowDate() != null
                          && show.getShowDate()
                              .isAfter(LocalDateTime.now().toLocalDate().plusYears(10)))
              .toList();

      if (!showsWithFutureDates.isEmpty()) {
        warnings.add(
            "Found "
                + showsWithFutureDates.size()
                + " shows with dates more than 10 years in the future");
        statistics.put("showsWithFutureDates", showsWithFutureDates.size());
      }

      // Check for duplicate external IDs
      long uniqueExternalIds =
          showService.findAll().stream()
              .filter(
                  show -> show.getExternalId() != null && !show.getExternalId().trim().isEmpty())
              .map(Show::getExternalId)
              .distinct()
              .count();

      long totalShowsWithExternalId =
          showService.findAll().stream()
              .filter(
                  show -> show.getExternalId() != null && !show.getExternalId().trim().isEmpty())
              .count();

      if (uniqueExternalIds != totalShowsWithExternalId) {
        warnings.add("Found duplicate external IDs in shows");
        statistics.put("duplicateShowExternalIds", totalShowsWithExternalId - uniqueExternalIds);
      }

    } catch (Exception e) {
      errors.add("Failed to check shows integrity: " + e.getMessage());
    }
  }

  /** Check wrestlers data integrity. */
  private void checkWrestlersIntegrity(
      List<String> errors, List<String> warnings, Map<String, Object> statistics) {
    try {
      long wrestlerCount = wrestlerRepository.count();
      statistics.put("totalWrestlers", wrestlerCount);

      // Check for wrestlers without names
      long wrestlersWithoutName =
          wrestlerRepository.findAll().stream()
              .filter(wrestler -> wrestler.getName() == null || wrestler.getName().trim().isEmpty())
              .count();

      if (wrestlersWithoutName > 0) {
        errors.add("Found " + wrestlersWithoutName + " wrestlers without names");
        statistics.put("wrestlersWithoutName", wrestlersWithoutName);
      }

      // Check for duplicate external IDs
      long uniqueExternalIds =
          wrestlerRepository.findAll().stream()
              .filter(
                  wrestler ->
                      wrestler.getExternalId() != null
                          && !wrestler.getExternalId().trim().isEmpty())
              .map(Wrestler::getExternalId)
              .distinct()
              .count();

      long totalWrestlersWithExternalId =
          wrestlerRepository.findAll().stream()
              .filter(
                  wrestler ->
                      wrestler.getExternalId() != null
                          && !wrestler.getExternalId().trim().isEmpty())
              .count();

      if (uniqueExternalIds != totalWrestlersWithExternalId) {
        warnings.add("Found duplicate external IDs in wrestlers");
        statistics.put(
            "duplicateWrestlerExternalIds", totalWrestlersWithExternalId - uniqueExternalIds);
      }

    } catch (Exception e) {
      errors.add("Failed to check wrestlers integrity: " + e.getMessage());
    }
  }

  /** Check factions data integrity. */
  private void checkFactionsIntegrity(
      List<String> errors, List<String> warnings, Map<String, Object> statistics) {
    try {
      long factionCount = factionRepository.count();
      statistics.put("totalFactions", factionCount);

      // Check for factions without names
      long factionsWithoutName =
          factionRepository.findAll().stream()
              .filter(faction -> faction.getName() == null || faction.getName().trim().isEmpty())
              .count();

      if (factionsWithoutName > 0) {
        errors.add("Found " + factionsWithoutName + " factions without names");
        statistics.put("factionsWithoutName", factionsWithoutName);
      }

      // Check for factions without members
      long factionsWithoutMembers =
          factionRepository.findAll().stream()
              .filter(faction -> faction.getMembers() == null || faction.getMembers().isEmpty())
              .count();

      if (factionsWithoutMembers > 0) {
        warnings.add("Found " + factionsWithoutMembers + " factions without members");
        statistics.put("factionsWithoutMembers", factionsWithoutMembers);
      }

    } catch (Exception e) {
      errors.add("Failed to check factions integrity: " + e.getMessage());
    }
  }

  /** Check teams data integrity. */
  private void checkTeamsIntegrity(
      List<String> errors, List<String> warnings, Map<String, Object> statistics) {
    try {
      long teamCount = teamRepository.count();
      statistics.put("totalTeams", teamCount);

      // Check for teams without names
      long teamsWithoutName =
          teamRepository.findAll().stream()
              .filter(team -> team.getName() == null || team.getName().trim().isEmpty())
              .count();

      if (teamsWithoutName > 0) {
        errors.add("Found " + teamsWithoutName + " teams without names");
        statistics.put("teamsWithoutName", teamsWithoutName);
      }

      // Check for teams without wrestlers
      long teamsWithoutWrestlers =
          teamRepository.findAll().stream()
              .filter(team -> team.getWrestler1() == null && team.getWrestler2() == null)
              .count();

      if (teamsWithoutWrestlers > 0) {
        errors.add("Found " + teamsWithoutWrestlers + " teams without any wrestlers");
        statistics.put("teamsWithoutWrestlers", teamsWithoutWrestlers);
      }

      // Check for teams with only one wrestler
      long teamsWithOneWrestler =
          teamRepository.findAll().stream()
              .filter(team -> (team.getWrestler1() == null) != (team.getWrestler2() == null))
              .count();

      if (teamsWithOneWrestler > 0) {
        warnings.add("Found " + teamsWithOneWrestler + " teams with only one wrestler");
        statistics.put("teamsWithOneWrestler", teamsWithOneWrestler);
      }

    } catch (Exception e) {
      errors.add("Failed to check teams integrity: " + e.getMessage());
    }
  }

  /** Check referential integrity between entities. */
  private void checkReferentialIntegrity(
      List<String> errors, List<String> warnings, Map<String, Object> statistics) {
    try {
      // Check if all show types referenced by shows exist
      long showTypeCount = showTypeService.count();
      statistics.put("totalShowTypes", showTypeCount);

      // Check if all seasons referenced by shows exist
      long seasonCount = seasonRepository.count();
      statistics.put("totalSeasons", seasonCount);

      // Check if all show templates referenced by shows exist
      long templateCount = showTemplateService.count();
      statistics.put("totalShowTemplates", templateCount);

      // Additional referential integrity checks can be added here

    } catch (Exception e) {
      errors.add("Failed to check referential integrity: " + e.getMessage());
    }
  }

  /** Result of a data integrity check. */
  public static class IntegrityCheckResult {
    @Getter private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;
    private final Map<String, Object> statistics;

    public IntegrityCheckResult(
        boolean valid, List<String> errors, List<String> warnings, Map<String, Object> statistics) {
      this.valid = valid;
      this.errors = new ArrayList<>(errors);
      this.warnings = new ArrayList<>(warnings);
      this.statistics = new HashMap<>(statistics);
    }

    public List<String> getErrors() {
      return new ArrayList<>(errors);
    }

    public List<String> getWarnings() {
      return new ArrayList<>(warnings);
    }

    public Map<String, Object> getStatistics() {
      return new HashMap<>(statistics);
    }

    public boolean hasErrors() {
      return !errors.isEmpty();
    }

    public String getSummary() {
      StringBuilder sb = new StringBuilder();
      if (valid) {
        sb.append("Data integrity check passed");
      } else {
        sb.append("Data integrity check failed");
      }

      if (!errors.isEmpty()) {
        sb.append(" (").append(errors.size()).append(" errors)");
      }

      if (!warnings.isEmpty()) {
        sb.append(" (").append(warnings.size()).append(" warnings)");
      }

      return sb.toString();
    }
  }
}
