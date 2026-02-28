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
package com.github.javydreamercsw.management.service.sync.entity.notion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionApiExecutor;
import com.github.javydreamercsw.base.ai.notion.TeamPage;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.TeamDTO;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service responsible for synchronizing teams from Notion to the database. */
@Service
@Slf4j
public class TeamSyncService extends BaseSyncService {

  private final TeamService teamService;
  private final WrestlerService wrestlerService;

  @Autowired
  public TeamSyncService(
      ObjectMapper objectMapper,
      SyncServiceDependencies syncServiceDependencies,
      TeamService teamService,
      WrestlerService wrestlerService,
      NotionApiExecutor notionApiExecutor) {
    super(objectMapper, syncServiceDependencies, notionApiExecutor);
    this.teamService = teamService;
    this.wrestlerService = wrestlerService;
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
          syncServiceDependencies
              .getProgressTracker()
              .startOperation("Teams Sync", "Synchronizing teams from Notion", 0);
      operationId = progress.getOperationId();

      // Load teams from Notion
      syncServiceDependencies
          .getProgressTracker()
          .addLogMessage(operationId, "📥 Loading teams from Notion database...", "INFO");

      if (!isNotionHandlerAvailable()) {
        log.warn("NotionHandler not available. Cannot sync teams from Notion.");
        syncServiceDependencies
            .getProgressTracker()
            .failOperation(operationId, "NotionHandler is not available for sync operations");
        return SyncResult.failure("Teams", "NotionHandler is not available for sync operations");
      }

      List<TeamPage> teamPages =
          executeWithRateLimit(syncServiceDependencies.getNotionHandler()::loadAllTeams);

      if (teamPages.isEmpty()) {
        syncServiceDependencies
            .getProgressTracker()
            .addLogMessage(operationId, "⚠️ No teams found in Notion database", "WARN");
        syncServiceDependencies
            .getProgressTracker()
            .completeOperation(operationId, true, "No teams to sync", 0);

        // Record success in health monitor
        long totalTime = System.currentTimeMillis() - startTime;
        syncServiceDependencies.getHealthMonitor().recordSuccess("Teams", totalTime, 0);

        return SyncResult.success("Teams", 0, 0, 0);
      }

      syncServiceDependencies
          .getProgressTracker()
          .addLogMessage(
              operationId, String.format("📋 Found %d teams in Notion", teamPages.size()), "INFO");

      // Convert to DTOs
      List<TeamDTO> teamDTOs =
          processWithControlledParallelism(
              teamPages,
              this::convertTeamPageToDTO,
              10, // Batch size
              operationId,
              2, // Progress step
              "Converted %d/%d teams");

      syncServiceDependencies
          .getProgressTracker()
          .addLogMessage(
              operationId,
              String.format("✅ Successfully converted %d teams to DTOs", teamDTOs.size()),
              "INFO");

      // Save teams to database
      syncServiceDependencies
          .getProgressTracker()
          .addLogMessage(operationId, "💾 Saving teams to database...", "INFO");
      int savedCount =
          (int)
              processWithControlledParallelism(
                      teamDTOs,
                      this::saveOrUpdateTeam,
                      10, // Batch size
                      operationId,
                      3, // Progress step
                      "Saved %d/%d teams")
                  .stream()
                  .filter(java.util.Objects::nonNull)
                  .count();

      // Complete progress tracking
      syncServiceDependencies
          .getProgressTracker()
          .completeOperation(
              operationId,
              true,
              String.format("Successfully synced %d teams", savedCount),
              savedCount);

      // Record success in health monitor
      long totalTime = System.currentTimeMillis() - startTime;
      syncServiceDependencies.getHealthMonitor().recordSuccess("Teams", totalTime, savedCount);

      return SyncResult.success("Teams", savedCount, 0, 0);

    } catch (Exception e) {
      log.error("❌ Teams sync failed", e);

      // Fail progress tracking
      syncServiceDependencies
          .getProgressTracker()
          .addLogMessage(operationId, "❌ Team sync failed: " + e.getMessage(), "ERROR");
      syncServiceDependencies
          .getProgressTracker()
          .failOperation(operationId, "Sync failed: " + e.getMessage());

      // Record failure in health monitor
      syncServiceDependencies.getHealthMonitor().recordFailure("Teams", e.getMessage());

      return SyncResult.failure("Teams", e.getMessage());
    }
  }

  /** Converts a TeamPage from Notion to a TeamDTO. */
  private TeamDTO convertTeamPageToDTO(@NonNull TeamPage teamPage) {
    try {
      TeamDTO dto = new TeamDTO();
      Map<String, Object> rawProperties = teamPage.getRawProperties();

      // Extract basic properties using existing methods
      dto.setName(
          syncServiceDependencies.getNotionPageDataExtractor().extractNameFromNotionPage(teamPage));
      dto.setExternalId(teamPage.getId());
      dto.setDescription(
          syncServiceDependencies
              .getNotionPageDataExtractor()
              .extractDescriptionFromNotionPage(teamPage));

      // Extract wrestler IDs
      dto.setWrestler1ExternalId(extractRelationId(rawProperties.get("Member 1")));
      dto.setWrestler2ExternalId(extractRelationId(rawProperties.get("Member 2")));

      // Fallback to names if IDs aren't present (less reliable but preserves old behavior)
      if (dto.getWrestler1ExternalId() == null) {
        dto.setWrestler1Name(extractWrestlerNameFromTeamPage(teamPage, "Member 1"));
      }
      if (dto.getWrestler2ExternalId() == null) {
        dto.setWrestler2Name(extractWrestlerNameFromTeamPage(teamPage, "Member 2"));
      }

      // Extract relationship IDs
      dto.setManagerExternalId(extractRelationId(rawProperties.get("Manager")));
      String factionId = extractRelationId(rawProperties.get("Faction"));
      if (factionId != null) {
        // We'll resolve the name during saving, or we could resolve it here if needed
        dto.setFactionName(factionId); // Using ID as placeholder
      }

      // Extract new fields
      Object themeSongObj = rawProperties.get("Theme Song");
      if (themeSongObj instanceof String) {
        dto.setThemeSong((String) themeSongObj);
      }

      Object artistObj = rawProperties.get("Artist");
      if (artistObj instanceof String) {
        dto.setArtist((String) artistObj);
      }

      Object teamFinisherObj = rawProperties.get("Team Finisher");
      if (teamFinisherObj instanceof String) {
        dto.setTeamFinisher((String) teamFinisherObj);
      }

      // Extract status
      Object statusObj = rawProperties.get("Status");
      if (statusObj instanceof Boolean) {
        dto.setStatus((Boolean) statusObj ? TeamStatus.ACTIVE : TeamStatus.INACTIVE);
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

  /** Extracts a single relation ID from a Notion property. */
  private String extractRelationId(Object property) {
    if (property == null) return null;
    if (property instanceof String str) {
      if (str.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
        return str;
      }
    } else if (property instanceof java.util.List<?> list && !list.isEmpty()) {
      Object first = list.get(0);
      if (first instanceof String str) return str;
      if (first instanceof Map<?, ?> map) {
        Object id = map.get("id");
        if (id instanceof String str) return str;
      }
    } else if (property instanceof Map<?, ?> map) {
      Object id = map.get("id");
      if (id instanceof String str) return str;
    }
    return null;
  }

  /** Extracts wrestler name from team page with enhanced relation handling. */
  private String extractWrestlerNameFromTeamPage(
      @NonNull TeamPage teamPage, @NonNull String propertyName) {
    if (teamPage.getRawProperties() == null) {
      return null;
    }

    // Use NotionPageDataExtractor to get the property string
    String propertyStr =
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractPropertyAsString(teamPage.getRawProperties(), propertyName);

    if (propertyStr == null || propertyStr.isEmpty()) {
      return null;
    }

    // If it shows as "X items" or "X relations", this means the relation isn't resolved
    if (propertyStr.matches("\\d+ (items?|relations?)")) {
      return null;
    }

    // If it's a UUID, it's likely a relation ID that wasn't resolved
    if (propertyStr.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
      return null;
    }

    return propertyStr;
  }

  /** Saves or updates a team in the database. */
  private Team saveOrUpdateTeam(TeamDTO dto) {
    if (dto == null || dto.getName() == null || dto.getName().trim().isEmpty()) {
      log.warn("Cannot save team: DTO is null or has no name");
      return null;
    }

    try {
      // Find wrestlers
      Wrestler wrestler1 = null;
      Wrestler wrestler2 = null;

      if (dto.getWrestler1ExternalId() != null) {
        wrestler1 = wrestlerService.findByExternalId(dto.getWrestler1ExternalId()).orElse(null);
      }
      if (wrestler1 == null && dto.getWrestler1Name() != null) {
        wrestler1 = wrestlerService.findByName(dto.getWrestler1Name()).orElse(null);
      }

      if (dto.getWrestler2ExternalId() != null) {
        wrestler2 = wrestlerService.findByExternalId(dto.getWrestler2ExternalId()).orElse(null);
      }
      if (wrestler2 == null && dto.getWrestler2Name() != null) {
        wrestler2 = wrestlerService.findByName(dto.getWrestler2Name()).orElse(null);
      }

      // Try to find existing team
      Team existingTeam = null;
      if (dto.getExternalId() != null && !dto.getExternalId().trim().isEmpty()) {
        existingTeam = teamService.getTeamByExternalId(dto.getExternalId()).orElse(null);
      }
      if (existingTeam == null) {
        existingTeam = teamService.getTeamByName(dto.getName()).orElse(null);
      }

      Team team = existingTeam;
      boolean isNew = (team == null);

      if (isNew) {
        if (wrestler1 == null || wrestler2 == null) {
          log.warn("Skipping new team '{}' due to missing wrestlers", dto.getName());
          return null;
        }
        team = new Team();
        team.setName(dto.getName());
        team.setWrestler1(wrestler1);
        team.setWrestler2(wrestler2);
      }

      // Update basic fields
      team.setName(dto.getName());
      team.setDescription(dto.getDescription());
      team.setExternalId(dto.getExternalId());
      team.setThemeSong(dto.getThemeSong());
      team.setArtist(dto.getArtist());
      team.setTeamFinisher(dto.getTeamFinisher());
      if (dto.getStatus() != null) team.setStatus(dto.getStatus());

      if (wrestler1 != null) team.setWrestler1(wrestler1);
      if (wrestler2 != null) team.setWrestler2(wrestler2);

      // Resolve relationships
      if (dto.getManagerExternalId() != null) {
        syncServiceDependencies
            .getNpcRepository()
            .findByExternalId(dto.getManagerExternalId())
            .ifPresent(team::setManager);
      }

      if (dto.getFactionName() != null) {
        // If it's a UUID, resolve by external ID
        if (dto.getFactionName()
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
          syncServiceDependencies
              .getFactionRepository()
              .findByExternalId(dto.getFactionName())
              .ifPresent(team::setFaction);
        } else {
          syncServiceDependencies
              .getFactionRepository()
              .findByName(dto.getFactionName())
              .ifPresent(team::setFaction);
        }
      }

      syncServiceDependencies.getTeamRepository().saveAndFlush(team);
      log.info("✅ {} team: {}", isNew ? "Created" : "Updated", team.getName());
      return team;

    } catch (Exception e) {
      log.error("Failed to save team '{}': {}", dto.getName(), e.getMessage(), e);
      throw new RuntimeException("Failed to save team: " + dto.getName(), e);
    }
  }
}
