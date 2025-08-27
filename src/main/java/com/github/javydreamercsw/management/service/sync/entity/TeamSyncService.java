package com.github.javydreamercsw.management.service.sync.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.TeamPage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.TeamDTO;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service responsible for synchronizing teams from Notion to the database. */
@Service
@Slf4j
public class TeamSyncService extends BaseSyncService {

  @Autowired private TeamService teamService;
  @Autowired private TeamRepository teamRepository;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private FactionRepository factionRepository;

  public TeamSyncService(ObjectMapper objectMapper, NotionSyncProperties syncProperties) {
    super(objectMapper, syncProperties);
  }

  /**
   * Synchronizes teams from Notion to the local database.
   *
   * @return SyncResult containing the operation status and details
   */
  public SyncResult syncTeams(@NonNull String operationId) {
    log.info("🔄 Starting teams synchronization from Notion...");
    long startTime = System.currentTimeMillis();

    try {
      // Start progress tracking
      SyncProgressTracker.SyncProgress progress =
          progressTracker.startOperation("Teams Sync", "Synchronizing teams from Notion", 0);
      operationId = progress.getOperationId();

      // Load teams from Notion
      progressTracker.addLogMessage(
          operationId, "📥 Loading teams from Notion database...", "INFO");

      if (!isNotionHandlerAvailable()) {
        log.warn("NotionHandler not available. Cannot sync teams from Notion.");
        progressTracker.failOperation(
            operationId, "NotionHandler is not available for sync operations");
        return SyncResult.failure("Teams", "NotionHandler is not available for sync operations");
      }

      List<TeamPage> teamPages = notionHandler.loadAllTeams();

      if (teamPages.isEmpty()) {
        progressTracker.addLogMessage(operationId, "⚠️ No teams found in Notion database", "WARN");
        progressTracker.completeOperation(operationId, true, "No teams to sync", 0);

        // Record success in health monitor
        long totalTime = System.currentTimeMillis() - startTime;
        healthMonitor.recordSuccess("Teams", totalTime, 0);

        return SyncResult.success("Teams", 0, 0);
      }

      progressTracker.addLogMessage(
          operationId, String.format("📋 Found %d teams in Notion", teamPages.size()), "INFO");

      // Convert to DTOs
      List<TeamDTO> teamDTOs = new ArrayList<>();
      for (TeamPage teamPage : teamPages) {
        try {
          TeamDTO teamDTO = convertTeamPageToDTO(teamPage);
          if (teamDTO != null) {
            teamDTOs.add(teamDTO);
          }
        } catch (Exception e) {
          String teamName = extractNameFromNotionPage(teamPage);
          log.warn("Failed to convert team page to DTO: {}", teamName, e);
          progressTracker.addLogMessage(
              operationId,
              String.format("⚠️ Failed to convert team: %s - %s", teamName, e.getMessage()),
              "WARN");
        }
      }

      progressTracker.addLogMessage(
          operationId,
          String.format("✅ Successfully converted %d teams to DTOs", teamDTOs.size()),
          "INFO");

      // Save teams to database
      progressTracker.addLogMessage(operationId, "💾 Saving teams to database...", "INFO");
      int savedCount = 0;
      for (TeamDTO teamDTO : teamDTOs) {
        try {
          boolean saved = saveOrUpdateTeam(teamDTO);
          if (saved) {
            savedCount++;
          }
        } catch (Exception e) {
          log.warn("Failed to save team: {}", teamDTO.getName(), e);
          progressTracker.addLogMessage(
              operationId,
              String.format("⚠️ Failed to save team: %s - %s", teamDTO.getName(), e.getMessage()),
              "WARN");
        }
      }

      // Complete progress tracking
      progressTracker.completeOperation(
          operationId, true, String.format("Successfully synced %d teams", savedCount), savedCount);

      // Record success in health monitor
      long totalTime = System.currentTimeMillis() - startTime;
      healthMonitor.recordSuccess("Teams", totalTime, savedCount);

      return SyncResult.success("Teams", savedCount, 0);

    } catch (Exception e) {
      log.error("❌ Teams sync failed", e);

      // Fail progress tracking
      progressTracker.addLogMessage(operationId, "❌ Team sync failed: " + e.getMessage(), "ERROR");
      progressTracker.failOperation(operationId, "Sync failed: " + e.getMessage());

      // Record failure in health monitor
      healthMonitor.recordFailure("Teams", e.getMessage());

      return SyncResult.failure("Teams", e.getMessage());
    }
  }

  /** Converts a TeamPage from Notion to a TeamDTO. */
  private TeamDTO convertTeamPageToDTO(@NonNull TeamPage teamPage) {
    try {
      TeamDTO dto = new TeamDTO();

      // Extract basic properties using existing methods
      dto.setName(extractNameFromNotionPage(teamPage));
      dto.setExternalId(teamPage.getId());
      dto.setDescription(extractDescriptionFromNotionPage(teamPage));

      // Try Members property first - use relation-aware extraction
      String wrestler1Name = extractWrestlerNameFromRelation(teamPage, "Member 1");
      String wrestler2Name = extractWrestlerNameFromRelation(teamPage, "Member 2");

      dto.setWrestler1Name(wrestler1Name);
      dto.setWrestler2Name(wrestler2Name);

      log.debug(
          "Extracted wrestlers for team '{}': '{}' and '{}'",
          dto.getName(),
          wrestler1Name,
          wrestler2Name);

      // Extract faction name if available
      String factionName = extractFactionFromNotionPage(teamPage);
      if (factionName != null && !factionName.trim().isEmpty()) {
        dto.setFactionName(factionName);
      }

      // Extract status
      String statusStr = extractStringPropertyFromNotionPage(teamPage, "Status");
      if (statusStr != null && !statusStr.trim().isEmpty()) {
        try {
          dto.setStatus(TeamStatus.valueOf(statusStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
          log.warn(
              "Invalid team status '{}' for team '{}', defaulting to ACTIVE",
              statusStr,
              dto.getName());
          dto.setStatus(TeamStatus.ACTIVE);
        }
      } else {
        dto.setStatus(TeamStatus.ACTIVE); // Default status
      }

      log.debug("Successfully converted team page '{}' to DTO", dto.getName());
      return dto;

    } catch (Exception e) {
      log.error("Failed to convert team page to DTO", e);
      return null;
    }
  }

  /** Saves or updates a team in the database. */
  private boolean saveOrUpdateTeam(TeamDTO dto) {
    if (dto == null || dto.getName() == null || dto.getName().trim().isEmpty()) {
      log.warn("Cannot save team: DTO is null or has no name");
      return false;
    }

    try {
      // Try to find existing team by external ID first
      Team existingTeam = null;
      if (dto.getExternalId() != null && !dto.getExternalId().trim().isEmpty()) {
        existingTeam = teamService.getTeamByExternalId(dto.getExternalId()).orElse(null);
      }

      if (existingTeam == null) {
        existingTeam = teamService.getTeamByName(dto.getName()).orElse(null);
      }

      // Find wrestlers by name
      Wrestler wrestler1 = null;
      Wrestler wrestler2 = null;

      if (dto.getWrestler1Name() != null && !dto.getWrestler1Name().trim().isEmpty()) {
        wrestler1 = wrestlerService.findByName(dto.getWrestler1Name()).orElse(null);
        if (wrestler1 == null) {
          log.warn("Wrestler '{}' not found for team '{}'", dto.getWrestler1Name(), dto.getName());
        }
      }

      if (dto.getWrestler2Name() != null && !dto.getWrestler2Name().trim().isEmpty()) {
        wrestler2 = wrestlerService.findByName(dto.getWrestler2Name()).orElse(null);
        if (wrestler2 == null) {
          log.warn("Wrestler '{}' not found for team '{}'", dto.getWrestler2Name(), dto.getName());
        }
      }

      // Both wrestlers are required for a team
      if (wrestler1 == null || wrestler2 == null) {
        String missingWrestlers =
            (wrestler1 == null
                    ? (dto.getWrestler1Name() != null ? dto.getWrestler1Name() : "null")
                    : "")
                + (wrestler1 == null && wrestler2 == null ? ", " : "")
                + (wrestler2 == null
                    ? (dto.getWrestler2Name() != null ? dto.getWrestler2Name() : "null")
                    : "");

        log.warn(
            "⚠️ Skipping team '{}' due to missing required wrestlers: {}",
            dto.getName(),
            missingWrestlers);
        return false;
      }

      if (existingTeam != null) {
        // Update existing team
        log.debug("Updating existing team: {}", dto.getName());

        existingTeam.setName(dto.getName());
        existingTeam.setDescription(dto.getDescription());
        existingTeam.setWrestler1(wrestler1);
        existingTeam.setWrestler2(wrestler2);
        existingTeam.setExternalId(dto.getExternalId());

        if (dto.getStatus() != null) {
          existingTeam.setStatus(dto.getStatus());
        }

        if (dto.getFormedDate() != null) {
          existingTeam.setFormedDate(dto.getFormedDate());
        }

        if (dto.getDisbandedDate() != null) {
          existingTeam.setDisbandedDate(dto.getDisbandedDate());
        }

        // Find and set faction if specified
        if (dto.getFactionName() != null && !dto.getFactionName().trim().isEmpty()) {
          Faction faction = factionRepository.findByName(dto.getFactionName()).orElse(null);
          existingTeam.setFaction(faction);
          if (faction == null) {
            log.warn("Faction '{}' not found for team '{}'", dto.getFactionName(), dto.getName());
          }
        }

        teamRepository.saveAndFlush(existingTeam);
        log.info("✅ Updated team: {}", dto.getName());
        return true;

      } else {
        // Create new team using TeamService
        log.debug("Creating new team: {}", dto.getName());

        Long wrestler1Id = wrestler1.getId();
        Long wrestler2Id = wrestler2.getId();
        Long factionId = null;

        // Find faction ID if specified
        if (dto.getFactionName() != null && !dto.getFactionName().trim().isEmpty()) {
          Faction faction = factionRepository.findByName(dto.getFactionName()).orElse(null);
          if (faction != null) {
            factionId = faction.getId();
          } else {
            log.warn("Faction '{}' not found for team '{}'", dto.getFactionName(), dto.getName());
          }
        }

        // Use TeamService to create the team
        Optional<Team> createdTeam =
            teamService.createTeam(
                dto.getName(), dto.getDescription(), wrestler1Id, wrestler2Id, factionId);

        if (createdTeam.isPresent()) {
          Team newTeam = createdTeam.get();

          // Set additional properties
          newTeam.setExternalId(dto.getExternalId());

          if (dto.getStatus() != null) {
            newTeam.setStatus(dto.getStatus());
          }

          if (dto.getFormedDate() != null) {
            newTeam.setFormedDate(dto.getFormedDate());
          }

          if (dto.getDisbandedDate() != null) {
            newTeam.setDisbandedDate(dto.getDisbandedDate());
          }

          // Save additional properties
          if (dto.getExternalId() != null
              || dto.getStatus() != null
              || dto.getFormedDate() != null
              || dto.getDisbandedDate() != null) {
            teamRepository.saveAndFlush(newTeam);
          }

          log.info("✅ Created new team: {}", dto.getName());
          return true;
        } else {
          log.warn("Failed to create team '{}' - TeamService validation failed", dto.getName());
          return false;
        }
      }

    } catch (Exception e) {
      log.error("Failed to save team '{}': {}", dto.getName(), e.getMessage(), e);
      throw new RuntimeException("Failed to save team: " + dto.getName(), e);
    }
  }

  /** Extracts wrestler name from a relation property in a TeamPage. */
  private String extractWrestlerNameFromRelation(
      @NonNull TeamPage teamPage, @NonNull String memberPropertyName) {
    if (teamPage.getRawProperties() == null) {
      return null;
    }

    Object memberProperty = teamPage.getRawProperties().get(memberPropertyName);
    if (memberProperty == null) {
      return null;
    }

    String memberStr = memberProperty.toString().trim();

    // If it's already a resolved name, use it
    if (!memberStr.matches("\\d+ items?")
        && !memberStr.isEmpty()
        && !memberStr.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
      return memberStr;
    }

    // If it's in "X items" format, we can't easily resolve it in sync mode
    if (memberStr.matches("\\d+ items?")) {
      log.debug(
          "Could not extract wrestler name from property '{}': {}", memberPropertyName, memberStr);
      return null;
    }

    return null;
  }

  /** Extracts faction from a team page. */
  private String extractFactionFromNotionPage(
      @NonNull com.github.javydreamercsw.base.ai.notion.NotionPage page) {
    if (page.getRawProperties() != null) {
      // Try different possible property names for faction
      Object faction = page.getRawProperties().get("Faction");
      if (faction == null) {
        faction = page.getRawProperties().get("Team");
      }
      if (faction == null) {
        faction = page.getRawProperties().get("faction");
      }

      if (faction != null && !faction.toString().trim().isEmpty()) {
        String factionStr = faction.toString().trim();

        // If it shows as "X relations", preserve existing faction data
        if (factionStr.matches("\\d+ relations?")) {
          log.debug(
              "Faction shows as relationship count ({}), preserving existing faction", factionStr);
          return null;
        }

        // If it looks like a relationship ID, don't use it
        if (factionStr.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
          log.debug(
              "Faction appears to be a relationship ID, preserving existing faction: {}",
              factionStr);
          return null;
        }

        // If it's a readable name, use it
        log.debug("Found faction name: {}", factionStr);
        return factionStr;
      }
    }
    return null;
  }

  /** Extracts a string property from any NotionPage type using raw properties. */
  private String extractStringPropertyFromNotionPage(
      @NonNull com.github.javydreamercsw.base.ai.notion.NotionPage page,
      @NonNull String propertyName) {
    if (page.getRawProperties() != null) {
      Object property = page.getRawProperties().get(propertyName);
      if (property != null) {
        String propertyStr = property.toString().trim();

        // Handle relationship properties that show as "X items" or "X relations"
        if (propertyStr.matches("\\d+ (items?|relations?)")) {
          log.debug(
              "Property '{}' shows as relationship count ({}), cannot resolve in sync mode",
              propertyName,
              propertyStr);
          return null;
        }

        // Handle comma-separated values
        if (propertyStr.contains(",")) {
          String[] parts = propertyStr.split(",");
          String firstPart = parts[0].trim();
          if (!firstPart.isEmpty()) {
            log.debug(
                "Property '{}' contains multiple values, using first: '{}'",
                propertyName,
                firstPart);
            return firstPart;
          }
        }

        // Return the property value if it's not empty and not a UUID
        if (!propertyStr.isEmpty()
            && !propertyStr.matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
          return propertyStr;
        }
      }
    }
    return null;
  }
}
