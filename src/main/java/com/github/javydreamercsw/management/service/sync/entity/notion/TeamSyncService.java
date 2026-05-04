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
import com.github.javydreamercsw.base.util.LogSanitizer;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamStatus;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Service responsible for synchronizing teams from Notion to the database. */
@Service
@Slf4j
public class TeamSyncService extends BaseSyncService {

  private final TeamRepository teamRepository;
  private final WrestlerService wrestlerService;
  private final UniverseRepository universeRepository;

  @Autowired @Lazy protected TeamSyncService self;

  protected TeamSyncService getSelf() {
    return self != null ? self : this;
  }

  public TeamSyncService(
      ObjectMapper objectMapper,
      SyncServiceDependencies syncServiceDependencies,
      NotionApiExecutor notionApiExecutor,
      TeamRepository teamRepository,
      WrestlerService wrestlerService,
      UniverseRepository universeRepository) {
    super(objectMapper, syncServiceDependencies, notionApiExecutor);
    this.teamRepository = teamRepository;
    this.wrestlerService = wrestlerService;
    this.universeRepository = universeRepository;
    this.self = this;
  }

  public SyncResult syncTeams(@NonNull String operationId) {
    log.info("🏘️ Starting teams synchronization from Notion with operation ID: {}", operationId);
    syncServiceDependencies.getProgressTracker().startOperation(operationId, "Teams Sync", 4);

    if (!syncServiceDependencies.getNotionSyncProperties().isEnabled()
        || !syncServiceDependencies.getNotionSyncProperties().isEntityEnabled("teams")) {
      log.debug("Teams synchronization is disabled, skipping.");
      return SyncResult.success("Teams", 0, 0, 0);
    }

    try {
      // Step 1: Load all teams from Notion
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(operationId, 1, "Loading teams from Notion...");
      log.info("📥 Loading teams from Notion...");
      long notionStart = System.currentTimeMillis();
      List<TeamPage> teamPages =
          executeWithRateLimit(() -> syncServiceDependencies.getNotionHandler().loadAllTeams());
      log.info(
          "✅ Retrieved {} teams from Notion in {}ms",
          teamPages.size(),
          System.currentTimeMillis() - notionStart);

      // Step 2: Convert to DTOs
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(operationId, 2, "Processing Notion data...");
      log.info("⚙️ Processing Notion data...");
      List<TeamSyncDTO> teamDTOs = new ArrayList<>();
      for (TeamPage page : teamPages) {
        teamDTOs.add(convertTeamPageToDTO(page));
      }

      // Step 3: Save to database
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(
              operationId, 3, String.format("Saving %d teams to database...", teamDTOs.size()));
      log.info("🗄️ Saving teams to database...");
      long dbStart = System.currentTimeMillis();
      int savedCount = 0;
      int processedItems = 0;
      for (TeamSyncDTO dto : teamDTOs) {
        processedItems++;
        if (processedItems % 5 == 0) {
          syncServiceDependencies
              .getProgressTracker()
              .updateProgress(
                  operationId,
                  4,
                  String.format(
                      "Saving teams to database... (%d/%d processed)",
                      processedItems, teamDTOs.size()));
        }
        if (getSelf().processSingleTeam(dto)) {
          savedCount++;
        }
      }
      log.info(
          "✅ Saved {} teams to database in {}ms", savedCount, System.currentTimeMillis() - dbStart);

      syncServiceDependencies
          .getProgressTracker()
          .completeOperation(
              operationId,
              true,
              String.format("Successfully synchronized %d teams", savedCount),
              savedCount);

      return SyncResult.success("Teams", savedCount, 0, 0);

    } catch (Exception e) {
      String errorMessage = "Failed to synchronize teams from Notion: " + e.getMessage();
      log.error(errorMessage, e);
      syncServiceDependencies.getProgressTracker().failOperation(operationId, errorMessage);
      return SyncResult.failure("Teams", errorMessage);
    }
  }

  private TeamSyncDTO convertTeamPageToDTO(@NonNull TeamPage teamPage) {
    TeamSyncDTO dto = new TeamSyncDTO();
    dto.setExternalId(teamPage.getId());

    Map<String, Object> rawProperties = teamPage.getRawProperties();

    // Extract Name
    dto.setName(
        syncServiceDependencies.getNotionPageDataExtractor().extractNameFromNotionPage(teamPage));

    // Extract Members (relations to Wrestlers)
    dto.setMemberExternalIds(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractRelationIds(teamPage, "Members"));

    // Fallback to Member 1 and Member 2 if Members is empty
    if (dto.getMemberExternalIds().isEmpty()) {
      dto.setWrestler1ExternalId(extractRelationId(rawProperties.get("Member 1")));
      dto.setWrestler2ExternalId(extractRelationId(rawProperties.get("Member 2")));
    }

    // Fallback to names if IDs aren't present (less reliable but preserves old behavior)
    if (dto.getMemberExternalIds().isEmpty() && dto.getWrestler1ExternalId() == null) {
      dto.setWrestler1Name(extractWrestlerNameFromTeamPage(teamPage, "Member 1"));
    }
    if (dto.getMemberExternalIds().isEmpty() && dto.getWrestler2ExternalId() == null) {
      dto.setWrestler2Name(extractWrestlerNameFromTeamPage(teamPage, "Member 2"));
    }

    // Extract relationship IDs
    dto.setManagerExternalId(extractRelationId(rawProperties.get("Manager")));
    String factionId = extractRelationId(rawProperties.get("Faction"));
    if (factionId != null) {
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

    // Description/Narration (from page content)
    dto.setDescription(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractDescriptionFromNotionPage(teamPage));

    log.debug("Successfully converted team page '{}' to DTO", dto.getName());
    return dto;
  }

  /** Extracts a single relation ID from a Notion property. */
  private String extractRelationId(Object property) {
    switch (property) {
      case null -> {
        return null;
      }
      case String str -> {
        if (str.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
          return str;
        }
      }
      case List<?> list when !list.isEmpty() -> {
        Object first = list.getFirst();
        if (first instanceof String str) {
          return str;
        }
        if (first instanceof Map<?, ?> map) {
          Object id = map.get("id");
          if (id instanceof String str) {
            return str;
          }
        }
      }
      case Map<?, ?> map -> {
        Object id = map.get("id");
        if (id instanceof String str) {
          return str;
        }
      }
      default -> {}
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
    return syncServiceDependencies
        .getNotionPageDataExtractor()
        .extractPropertyAsString(teamPage.getRawProperties(), propertyName);
  }

  /** Saves a single team DTO to the database. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean processSingleTeam(@NonNull TeamSyncDTO dto) {
    try {
      // Smart duplicate handling - prefer external ID, fallback to name
      Team team = null;
      boolean isNewTeam = false;

      // 1. Try to find by external ID first (most reliable)
      if (dto.getExternalId() != null && !dto.getExternalId().trim().isEmpty()) {
        team = teamRepository.findByExternalId(dto.getExternalId()).orElse(null);
      }

      // 2. Fallback to name matching if external ID didn't work
      if (team == null && dto.getName() != null && !dto.getName().trim().isEmpty()) {
        team = teamRepository.findByName(dto.getName()).orElse(null);
      }

      // 3. Create new team if no segment found
      if (team == null) {
        team = new Team();
        isNewTeam = true;
        log.info(
            "🆕 Creating new team: {} with external ID: {}",
            LogSanitizer.sanitize(dto.getName()),
            LogSanitizer.sanitize(dto.getExternalId()));
      } else {
        log.info(
            "🔄 Updating existing team: {} (ID: {}) with external ID: {}",
            LogSanitizer.sanitize(dto.getName()),
            team.getId(),
            LogSanitizer.sanitize(dto.getExternalId()));
      }

      // Set properties
      boolean changed = false;
      if (!Objects.equals(team.getName(), dto.getName())) {
        team.setName(dto.getName());
        changed = true;
      }
      if (!Objects.equals(team.getExternalId(), dto.getExternalId())) {
        team.setExternalId(dto.getExternalId());
        changed = true;
      }
      if (dto.getDescription() != null
          && !dto.getDescription().trim().isEmpty()
          && !Objects.equals(team.getDescription(), dto.getDescription())) {
        team.setDescription(dto.getDescription());
        changed = true;
      }

      // Resolve Members (Team expects exactly 2 wrestlers)
      Wrestler wrestler1 = null;
      Wrestler wrestler2 = null;

      if (dto.getMemberExternalIds() != null && dto.getMemberExternalIds().size() >= 2) {
        wrestler1 =
            wrestlerService.findByExternalId(dto.getMemberExternalIds().get(0)).orElse(null);
        wrestler2 =
            wrestlerService.findByExternalId(dto.getMemberExternalIds().get(1)).orElse(null);
      }

      // Fallback to individual external IDs
      if (wrestler1 == null && dto.getWrestler1ExternalId() != null) {
        wrestler1 = wrestlerService.findByExternalId(dto.getWrestler1ExternalId()).orElse(null);
      }
      if (wrestler2 == null && dto.getWrestler2ExternalId() != null) {
        wrestler2 = wrestlerService.findByExternalId(dto.getWrestler2ExternalId()).orElse(null);
      }

      // Fallback to names
      if (wrestler1 == null && dto.getWrestler1Name() != null) {
        wrestler1 = wrestlerService.findByName(dto.getWrestler1Name()).orElse(null);
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
      boolean isNew = team == null;

      if (isNew) {
        if (wrestler1 == null || wrestler2 == null) {
          log.warn("Skipping new team '{}' due to missing wrestlers", dto.getName());
          return null;
        }
        team = new Team();
        team.setName(dto.getName());
        team.setWrestler1(wrestler1);
        team.setWrestler2(wrestler2);
        changed = true;
      }

      if (wrestler1 != null && !Objects.equals(wrestler1, team.getWrestler1())) {
        team.setWrestler1(wrestler1);
        changed = true;
      }
      if (wrestler2 != null && !Objects.equals(wrestler2, team.getWrestler2())) {
        team.setWrestler2(wrestler2);
        changed = true;
      }

      // Update basic fields
      if (!Objects.equals(team.getThemeSong(), dto.getThemeSong())) {
        team.setThemeSong(dto.getThemeSong());
        changed = true;
      }
      if (!Objects.equals(team.getArtist(), dto.getArtist())) {
        team.setArtist(dto.getArtist());
        changed = true;
      }
      if (!Objects.equals(team.getTeamFinisher(), dto.getTeamFinisher())) {
        team.setTeamFinisher(dto.getTeamFinisher());
        changed = true;
      }
      if (dto.getStatus() != null && !Objects.equals(team.getStatus(), dto.getStatus())) {
        team.setStatus(dto.getStatus());
        changed = true;
      }

      // Resolve relationships
      if (dto.getManagerExternalId() != null) {
        Npc manager =
            syncServiceDependencies
                .getNpcRepository()
                .findByExternalId(dto.getManagerExternalId())
                .orElse(null);
        if (manager != null && !Objects.equals(team.getManager(), manager)) {
          team.setManager(manager);
          changed = true;
        }
      }

      if (dto.getFactionName() != null) {
        Faction faction = null;
        // If it's a UUID, resolve by external ID
        if (dto.getFactionName()
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
          faction =
              syncServiceDependencies
                  .getFactionRepository()
                  .findByExternalId(dto.getFactionName())
                  .orElse(null);
        } else {
          faction =
              syncServiceDependencies
                  .getFactionRepository()
                  .findByName(dto.getFactionName())
                  .orElse(null);
        }
        if (faction != null && !Objects.equals(team.getFaction(), faction)) {
          team.setFaction(faction);
          changed = true;
        }
      }

      // Associate with Universe (Default ID 1)
      if (team.getUniverse() == null) {
        universeRepository.findById(1L).ifPresent(team::setUniverse);
        changed = true;
      }

      if (changed) {
        if (isNewTeam) {
          teamRepository.save(team);
        } else {
          teamRepository.saveAndFlush(team);
        }
        return true;
      }

      return false;
    } catch (Exception e) {
      log.error(
          "❌ Failed to save team: {} - {}",
          LogSanitizer.sanitize(dto.getName()),
          LogSanitizer.sanitize(e.getMessage()));
      return false;
    }
  }

  /** DTO for Team data from Notion. */
  @Setter
  @Getter
  public static class TeamSyncDTO {
    private String name;
    private String description;
    private String externalId; // Notion page ID
    private List<String> memberExternalIds = new ArrayList<>();
    private String wrestler1ExternalId;
    private String wrestler2ExternalId;
    private String wrestler1Name;
    private String wrestler2Name;
    private String managerExternalId;
    private String factionName;
    private String themeSong;
    private String artist;
    private String teamFinisher;
    private TeamStatus status;
  }
}
