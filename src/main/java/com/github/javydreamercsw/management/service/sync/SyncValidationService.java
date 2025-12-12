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
package com.github.javydreamercsw.management.service.sync;

import com.github.javydreamercsw.base.ai.notion.FactionPage;
import com.github.javydreamercsw.base.ai.notion.ShowPage;
import com.github.javydreamercsw.base.ai.notion.TeamPage;
import com.github.javydreamercsw.base.ai.notion.WrestlerPage;
import com.github.javydreamercsw.base.config.NotionSyncProperties;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for validating sync data before processing. Provides comprehensive validation for all
 * entity types and sync prerequisites.
 */
@Slf4j
@Service
public class SyncValidationService {

  private final NotionSyncProperties syncProperties;

  public SyncValidationService(NotionSyncProperties syncProperties) {
    this.syncProperties = syncProperties;
  }

  /** Validate sync prerequisites before starting any sync operation. */
  public ValidationResult validateSyncPrerequisites() {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    // Check if sync is enabled
    if (!syncProperties.isEnabled()) {
      errors.add("Sync is disabled in configuration");
    }

    // Check Notion token availability
    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      errors.add("NOTION_TOKEN environment variable is not available");
    }

    // Check scheduler configuration
    if (syncProperties.getScheduler().isEnabled()
        && syncProperties.getScheduler().getInterval() < 60000) {
      warnings.add("Sync interval is very short (< 1 minute), this may cause rate limiting");
    }

    // Entities are automatically determined based on database relationships
    // No manual configuration needed

    return new ValidationResult(errors.isEmpty(), errors, warnings);
  }

  /** Validate a list of shows before sync. */
  public ValidationResult validateShows(List<ShowPage> shows) {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    if (shows == null || shows.isEmpty()) {
      warnings.add("No shows to validate");
      return new ValidationResult(true, errors, warnings);
    }

    // Check for duplicate names
    Set<String> names =
        shows.stream()
            .map(this::extractNameFromNotionPage)
            .filter(name -> name != null && !name.trim().isEmpty())
            .collect(Collectors.toSet());

    if (names.size() != shows.size()) {
      warnings.add("Duplicate show names detected - some shows may be overwritten");
    }

    // Validate individual shows
    for (int i = 0; i < shows.size(); i++) {
      ShowPage show = shows.get(i);
      List<String> showErrors = validateSingleShow(show, i);
      errors.addAll(showErrors);
    }

    // Check for reasonable data size
    if (shows.size() > 10000) {
      warnings.add("Very large dataset (" + shows.size() + " shows) - sync may take a long time");
    }

    return new ValidationResult(errors.isEmpty(), errors, warnings);
  }

  /** Validate a single show. */
  private List<String> validateSingleShow(ShowPage show, int index) {
    List<String> errors = new ArrayList<>();

    if (show == null) {
      errors.add("Show at index " + index + " is null");
      return errors;
    }

    // Validate required fields
    String showName = extractNameFromNotionPage(show);
    if (showName == null || showName.trim().isEmpty() || "Unknown".equals(showName)) {
      errors.add("Show at index " + index + " has no name");
    }

    if (show.getId() == null || show.getId().trim().isEmpty()) {
      errors.add("Show at index " + index + " has no ID");
    }

    // Validate date format if present and not a placeholder
    String showDate = extractShowDateFromNotionPage(show);
    if (showDate != null && !showDate.trim().isEmpty() && !isDatePlaceholder(showDate)) {
      try {
        LocalDate.parse(showDate);
      } catch (DateTimeParseException e) {
        // Only log as warning, don't fail validation for date format issues
        log.warn(
            "Show '{}' has invalid date format: {} - skipping date validation", showName, showDate);
      }
    }

    return errors;
  }

  /** Validate a list of wrestlers before sync. */
  public ValidationResult validateWrestlers(List<WrestlerPage> wrestlers) {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    if (wrestlers == null || wrestlers.isEmpty()) {
      warnings.add("No wrestlers to validate");
      return new ValidationResult(true, errors, warnings);
    }

    // Check for duplicate names
    Set<String> names =
        wrestlers.stream()
            .map(this::extractNameFromNotionPage)
            .filter(name -> name != null && !name.trim().isEmpty())
            .collect(Collectors.toSet());

    if (names.size() != wrestlers.size()) {
      warnings.add("Duplicate wrestler names detected - some wrestlers may be overwritten");
    }

    // Validate individual wrestlers
    for (int i = 0; i < wrestlers.size(); i++) {
      WrestlerPage wrestler = wrestlers.get(i);
      List<String> wrestlerErrors = validateSingleWrestler(wrestler, i);
      errors.addAll(wrestlerErrors);
    }

    // Check for reasonable data size
    if (wrestlers.size() > 50000) {
      warnings.add(
          "Very large dataset (" + wrestlers.size() + " wrestlers) - sync may take a long time");
    }

    return new ValidationResult(errors.isEmpty(), errors, warnings);
  }

  /** Validate a single wrestler. */
  private List<String> validateSingleWrestler(WrestlerPage wrestler, int index) {
    List<String> errors = new ArrayList<>();

    if (wrestler == null) {
      errors.add("Wrestler at index " + index + " is null");
      return errors;
    }

    // Validate required fields
    String wrestlerName = extractNameFromNotionPage(wrestler);
    if (wrestlerName == null || wrestlerName.trim().isEmpty() || "Unknown".equals(wrestlerName)) {
      errors.add("Wrestler at index " + index + " has no name");
    }

    if (wrestler.getId() == null || wrestler.getId().trim().isEmpty()) {
      errors.add("Wrestler at index " + index + " has no ID");
    }

    return errors;
  }

  /** Validate a list of factions before sync. */
  public ValidationResult validateFactions(List<FactionPage> factions) {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    if (factions == null || factions.isEmpty()) {
      warnings.add("No factions to validate");
      return new ValidationResult(true, errors, warnings);
    }

    // Check for duplicate names
    Set<String> names =
        factions.stream()
            .map(this::extractNameFromNotionPage)
            .filter(name -> name != null && !name.trim().isEmpty())
            .collect(Collectors.toSet());

    if (names.size() != factions.size()) {
      warnings.add("Duplicate faction names detected - some factions may be overwritten");
    }

    // Validate individual factions
    for (int i = 0; i < factions.size(); i++) {
      FactionPage faction = factions.get(i);
      List<String> factionErrors = validateSingleFaction(faction, i);
      errors.addAll(factionErrors);
    }

    return new ValidationResult(errors.isEmpty(), errors, warnings);
  }

  /** Validate a single faction. */
  private List<String> validateSingleFaction(FactionPage faction, int index) {
    List<String> errors = new ArrayList<>();

    if (faction == null) {
      errors.add("Faction at index " + index + " is null");
      return errors;
    }

    // Validate required fields
    String factionName = extractNameFromNotionPage(faction);
    if (factionName == null || factionName.trim().isEmpty() || "Unknown".equals(factionName)) {
      errors.add("Faction at index " + index + " has no name");
    }

    if (faction.getId() == null || faction.getId().trim().isEmpty()) {
      errors.add("Faction at index " + index + " has no ID");
    }

    return errors;
  }

  /** Validate a list of teams before sync. */
  public ValidationResult validateTeams(List<TeamPage> teams) {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    if (teams == null || teams.isEmpty()) {
      warnings.add("No teams to validate");
      return new ValidationResult(true, errors, warnings);
    }

    // Check for duplicate names
    Set<String> names =
        teams.stream()
            .map(this::extractNameFromNotionPage)
            .filter(name -> name != null && !name.trim().isEmpty())
            .collect(Collectors.toSet());

    if (names.size() != teams.size()) {
      warnings.add("Duplicate team names detected - some teams may be overwritten");
    }

    // Validate individual teams
    for (int i = 0; i < teams.size(); i++) {
      TeamPage team = teams.get(i);
      List<String> teamErrors = validateSingleTeam(team, i);
      errors.addAll(teamErrors);
    }

    return new ValidationResult(errors.isEmpty(), errors, warnings);
  }

  /** Validate a single team. */
  private List<String> validateSingleTeam(TeamPage team, int index) {
    List<String> errors = new ArrayList<>();

    if (team == null) {
      errors.add("Team at index " + index + " is null");
      return errors;
    }

    // Validate required fields
    String teamName = extractNameFromNotionPage(team);
    if (teamName == null || teamName.trim().isEmpty() || "Unknown".equals(teamName)) {
      errors.add("Team at index " + index + " has no name");
    }

    if (team.getId() == null || team.getId().trim().isEmpty()) {
      errors.add("Team at index " + index + " has no ID");
    }

    // Validate team members
    String wrestler1 = extractWrestler1NameFromNotionPage(team);
    String wrestler2 = extractWrestler2NameFromNotionPage(team);
    if ((wrestler1 == null || wrestler1.trim().isEmpty())
        && (wrestler2 == null || wrestler2.trim().isEmpty())) {
      errors.add("Team '" + teamName + "' has no wrestlers assigned");
    }

    return errors;
  }

  /** Result of a validation operation. */
  public static class ValidationResult {
    @Getter private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;

    public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
      this.valid = valid;
      this.errors = new ArrayList<>(errors);
      this.warnings = new ArrayList<>(warnings);
    }

    public List<String> getErrors() {
      return new ArrayList<>(errors);
    }

    public List<String> getWarnings() {
      return new ArrayList<>(warnings);
    }

    public boolean hasWarnings() {
      return !warnings.isEmpty();
    }

    public boolean hasErrors() {
      return !errors.isEmpty();
    }

    public String getSummary() {
      StringBuilder sb = new StringBuilder();
      if (valid) {
        sb.append("Validation passed");
      } else {
        sb.append("Validation failed");
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

  // ==================== HELPER METHODS ====================

  /** Extracts name from any NotionPage type using raw properties. */
  private String extractNameFromNotionPage(ShowPage page) {
    if (page.getRawProperties() != null) {
      Object name = page.getRawProperties().get("Name");
      return name != null ? name.toString() : "Unknown";
    }
    return "Unknown";
  }

  /** Extracts name from any NotionPage type using raw properties. */
  private String extractNameFromNotionPage(WrestlerPage page) {
    if (page.getRawProperties() != null) {
      Object name = page.getRawProperties().get("Name");
      return name != null ? name.toString() : "Unknown";
    }
    return "Unknown";
  }

  /** Extracts name from any NotionPage type using raw properties. */
  private String extractNameFromNotionPage(FactionPage page) {
    if (page.getRawProperties() != null) {
      Object name = page.getRawProperties().get("Name");
      return name != null ? name.toString() : "Unknown";
    }
    return "Unknown";
  }

  /** Extracts name from any NotionPage type using raw properties. */
  private String extractNameFromNotionPage(TeamPage page) {
    if (page.getRawProperties() != null) {
      Object name = page.getRawProperties().get("Name");
      return name != null ? name.toString() : "Unknown";
    }
    return "Unknown";
  }

  /** Extracts show date from ShowPage using raw properties. */
  private String extractShowDateFromNotionPage(ShowPage page) {
    if (page.getRawProperties() != null) {
      Object date = page.getRawProperties().get("Date");
      if (date == null) {
        date = page.getRawProperties().get("Show Date");
      }
      return date != null ? date.toString() : null;
    }
    return null;
  }

  /** Check if a date string is a placeholder value that should be ignored. */
  private boolean isDatePlaceholder(String dateStr) {
    if (dateStr == null) return false;

    String normalized = dateStr.trim().toLowerCase();
    return normalized.equals("date")
        || normalized.equals("n/a")
        || normalized.equals("tbd")
        || normalized.equals("unknown")
        || normalized.equals("null")
        || normalized.isEmpty();
  }

  /** Extracts wrestler names from TeamPage using raw properties. */
  private String extractWrestler1NameFromNotionPage(TeamPage page) {
    if (page.getRawProperties() != null) {
      Object members = page.getRawProperties().get("Members");
      if (members != null) {
        String membersStr = members.toString();
        String[] memberArray = membersStr.split(",");
        if (memberArray.length >= 1) {
          return memberArray[0].trim();
        }
      }
    }
    return null;
  }

  /** Extracts wrestler names from TeamPage using raw properties. */
  private String extractWrestler2NameFromNotionPage(TeamPage page) {
    if (page.getRawProperties() != null) {
      Object members = page.getRawProperties().get("Members");
      if (members != null) {
        String membersStr = members.toString();
        String[] memberArray = membersStr.split(",");
        if (memberArray.length >= 2) {
          return memberArray[1].trim();
        }
      }
    }
    return null;
  }
}
