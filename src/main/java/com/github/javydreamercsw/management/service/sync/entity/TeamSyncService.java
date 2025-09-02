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

      // Log available properties for debugging
      log.debug(
          "Team '{}' available properties: {}",
          dto.getName(),
          teamPage.getRawProperties().keySet());

      // Extract wrestler names using enhanced relation-aware extraction
      String wrestler1Name = extractWrestlerNameFromTeamPage(teamPage, "Member 1");
      String wrestler2Name = extractWrestlerNameFromTeamPage(teamPage, "Member 2");

      log.debug(
          "Initial extraction for team '{}': Member 1='{}', Member 2='{}'",
          dto.getName(),
          wrestler1Name,
          wrestler2Name);

      // Try alternative property names if the standard ones don't work
      if (wrestler1Name == null || wrestler1Name.matches("\\d+ items?")) {
        String altWrestler1 = extractWrestlerNameFromTeamPage(teamPage, "Wrestler 1");
        if (altWrestler1 != null) {
          wrestler1Name = altWrestler1;
          log.debug(
              "Using alternative 'Wrestler 1' for team '{}': '{}'", dto.getName(), wrestler1Name);
        }
      }
      if (wrestler2Name == null || wrestler2Name.matches("\\d+ items?")) {
        String altWrestler2 = extractWrestlerNameFromTeamPage(teamPage, "Wrestler 2");
        if (altWrestler2 != null) {
          wrestler2Name = altWrestler2;
          log.debug(
              "Using alternative 'Wrestler 2' for team '{}': '{}'", dto.getName(), wrestler2Name);
        }
      }

      // If still null or relation counts, check if this is a Notion API limitation
      if (wrestler1Name == null
          || wrestler2Name == null
          || (wrestler1Name != null && wrestler1Name.matches("\\d+ items?"))
          || (wrestler2Name != null && wrestler2Name.matches("\\d+ items?"))) {

        log.warn(
            "Team '{}' has unresolved wrestler relations. Final values: '{}', '{}'. Available"
                + " properties: {}",
            dto.getName(),
            wrestler1Name,
            wrestler2Name,
            teamPage.getRawProperties().keySet());

        // Mark these as unresolved so we can handle them appropriately
        if (wrestler1Name == null) wrestler1Name = "UNRESOLVED_RELATION";
        if (wrestler2Name == null) wrestler2Name = "UNRESOLVED_RELATION";
      }

      dto.setWrestler1Name(wrestler1Name);
      dto.setWrestler2Name(wrestler2Name);

      log.debug(
          "Final extracted wrestlers for team '{}': '{}' and '{}'",
          dto.getName(),
          wrestler1Name,
          wrestler2Name);

      // Extract faction name if available
      String factionName = extractFactionFromNotionPage(teamPage);
      if (factionName != null && !factionName.trim().isEmpty()) {
        dto.setFactionName(factionName);
      }

      // Extract status
      String statusStr = extractPropertyAsString(teamPage.getRawProperties(), "Status");
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

  /** Extracts wrestler name from team page with enhanced relation handling. */
  private String extractWrestlerNameFromTeamPage(
      @NonNull TeamPage teamPage, @NonNull String propertyName) {
    if (teamPage.getRawProperties() == null) {
      return null;
    }

    Object property = teamPage.getRawProperties().get(propertyName);
    if (property == null) {
      return null;
    }

    String propertyStr = property.toString().trim();

    // If it shows as "X items" or "X relations", this means the relation isn't resolved
    if (propertyStr.matches("\\d+ (items?|relations?)")) {
      log.debug(
          "Property '{}' shows as relationship count ({}), attempting alternative resolution",
          propertyName,
          propertyStr);

      // Try to resolve the relation using the NotionHandler if available
      String resolvedName = resolveWrestlerRelation(teamPage, propertyName);
      if (resolvedName != null) {
        log.debug("Successfully resolved '{}' to '{}'", propertyName, resolvedName);
        return resolvedName;
      }

      // If we can't resolve it, return the count so we can detect this case later
      return propertyStr;
    }

    // If it's a UUID, it's likely a relation ID that wasn't resolved
    if (propertyStr.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
      log.debug(
          "Property '{}' appears to be a relation ID that wasn't resolved: {}",
          propertyName,
          propertyStr);
      return null;
    }

    // If it contains comma-separated values, it might be multiple wrestlers
    if (propertyStr.contains(",")) {
      String[] parts = propertyStr.split(",");
      for (String part : parts) {
        String trimmedPart = part.trim();
        if (!trimmedPart.isEmpty()
            && !trimmedPart.matches("\\d+ (items?|relations?)")
            && !trimmedPart.matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
          log.debug(
              "Property '{}' contains multiple values, using: '{}'", propertyName, trimmedPart);
          return trimmedPart;
        }
      }
    }

    // If it's a clean string that's not empty and not a relation count/ID, use it
    if (!propertyStr.isEmpty()) {
      return propertyStr;
    }

    return null;
  }

  /**
   * Attempts to resolve wrestler relation using alternative methods. This method tries to work
   * around Notion API limitations for unresolved relations.
   */
  private String resolveWrestlerRelation(@NonNull TeamPage teamPage, @NonNull String propertyName) {
    if (!isNotionHandlerAvailable()) {
      return null;
    }

    try {
      // Try to get more detailed information about the relation
      // This is a workaround for when Notion returns "X items" instead of actual names

      // For now, we'll implement a simple fallback strategy
      // In a production environment, you might want to make additional API calls
      // to resolve the relation IDs to actual wrestler names

      log.debug(
          "Attempting to resolve relation '{}' for team '{}'", propertyName, teamPage.getId());

      // Since we can't easily resolve the relations without additional API calls,
      // we'll return null and let the higher-level logic handle it
      return null;

    } catch (Exception e) {
      log.warn("Failed to resolve wrestler relation '{}': {}", propertyName, e.getMessage());
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
      // Check if we have unresolved relations (null, relation counts, or our UNRESOLVED_RELATION
      // marker)
      boolean hasUnresolvedRelations =
          (dto.getWrestler1Name() != null
                  && (dto.getWrestler1Name().matches("\\d+ items?")
                      || dto.getWrestler1Name().equals("UNRESOLVED_RELATION")))
              || (dto.getWrestler2Name() != null
                  && (dto.getWrestler2Name().matches("\\d+ items?")
                      || dto.getWrestler2Name().equals("UNRESOLVED_RELATION")))
              || dto.getWrestler1Name() == null
              || dto.getWrestler2Name() == null;

      if (hasUnresolvedRelations) {
        log.warn(
            "Team '{}' has unresolved wrestler relations ('{}', '{}') - this is likely a Notion API"
                + " limitation. Preserving existing team data if available.",
            dto.getName(),
            dto.getWrestler1Name(),
            dto.getWrestler2Name());

        // Try to find existing team and preserve it without updating wrestlers
        Team existingTeam = null;
        if (dto.getExternalId() != null && !dto.getExternalId().trim().isEmpty()) {
          existingTeam = teamService.getTeamByExternalId(dto.getExternalId()).orElse(null);
        }
        if (existingTeam == null) {
          existingTeam = teamService.getTeamByName(dto.getName()).orElse(null);
        }

        if (existingTeam != null) {
          // Update only the non-wrestler properties
          log.info(
              "Updating existing team '{}' metadata only (preserving wrestlers)", dto.getName());

          existingTeam.setName(dto.getName());
          if (dto.getDescription() != null) {
            existingTeam.setDescription(dto.getDescription());
          }
          existingTeam.setExternalId(dto.getExternalId());

          if (dto.getStatus() != null) {
            existingTeam.setStatus(dto.getStatus());
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
          log.info("✅ Updated team metadata: {}", dto.getName());
          return true;
        } else {
          // Can't create new team without wrestlers
          log.warn(
              "Cannot create new team '{}' without resolved wrestler relations", dto.getName());
          return false;
        }
      }

      // Original logic for when we have actual wrestler names
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

      // Both wrestlers are required for a new team
      if (wrestler1 == null || wrestler2 == null) {
        if (existingTeam != null) {
          // Update existing team without changing wrestlers if we can't resolve them
          log.info(
              "Updating existing team '{}' metadata only (couldn't resolve wrestlers)",
              dto.getName());

          existingTeam.setName(dto.getName());
          if (dto.getDescription() != null) {
            existingTeam.setDescription(dto.getDescription());
          }
          existingTeam.setExternalId(dto.getExternalId());

          if (dto.getStatus() != null) {
            existingTeam.setStatus(dto.getStatus());
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
          log.info("✅ Updated team metadata: {}", dto.getName());
          return true;
        } else {
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

  /** Extracts faction from a team page. */
  private String extractFactionFromNotionPage(
      @NonNull com.github.javydreamercsw.base.ai.notion.NotionPage page) {
    if (page.getRawProperties() != null) {
      // Try different possible property names for faction
      Object faction = page.getRawProperties().get("Faction");
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
