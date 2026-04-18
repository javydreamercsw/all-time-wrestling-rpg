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
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    // Description/Narration (from page content)
    dto.setDescription(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractDescriptionFromNotionPage(teamPage));

    return dto;
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
            "🆕 Creating new team: {} with external ID: {}", dto.getName(), dto.getExternalId());
      } else {
        log.info(
            "🔄 Updating existing team: {} (ID: {}) with external ID: {}",
            dto.getName(),
            team.getId(),
            dto.getExternalId());
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
      if (dto.getMemberExternalIds() != null && dto.getMemberExternalIds().size() >= 2) {
        Optional<Wrestler> w1 = wrestlerService.findByExternalId(dto.getMemberExternalIds().get(0));
        Optional<Wrestler> w2 = wrestlerService.findByExternalId(dto.getMemberExternalIds().get(1));

        if (w1.isPresent() && !w1.get().equals(team.getWrestler1())) {
          team.setWrestler1(w1.get());
          changed = true;
        }
        if (w2.isPresent() && !w2.get().equals(team.getWrestler2())) {
          team.setWrestler2(w2.get());
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
      log.error("❌ Failed to save team: {} - {}", dto.getName(), e.getMessage());
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
  }
}
