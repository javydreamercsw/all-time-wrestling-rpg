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
import com.github.javydreamercsw.base.ai.notion.FactionPage;
import com.github.javydreamercsw.base.ai.notion.NotionApiExecutor;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.dto.FactionDTO;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.sync.SyncEntityType;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FactionSyncService extends BaseSyncService {
  private final FactionService factionService;

  public FactionSyncService(
      ObjectMapper objectMapper,
      SyncServiceDependencies syncServiceDependencies,
      FactionService factionService,
      NotionApiExecutor notionApiExecutor) {
    super(objectMapper, syncServiceDependencies, notionApiExecutor);
    this.factionService = factionService;
  }

  public SyncResult syncFactions(@NonNull String operationId) {
    if (syncServiceDependencies
        .getSyncSessionManager()
        .isAlreadySyncedInSession(SyncEntityType.FACTIONS.getKey())) {
      log.info("⏭️ Factions already synced in current session, skipping");
      return SyncResult.success(SyncEntityType.FACTIONS.getKey(), 0, 0, 0);
    }

    log.info("🛡️ Starting factions synchronization from Notion...");
    long startTime = System.currentTimeMillis();

    try {
      SyncResult result = performFactionsSync(operationId, startTime);
      if (result.isSuccess()) {
        syncServiceDependencies
            .getSyncSessionManager()
            .markAsSyncedInSession(SyncEntityType.FACTIONS.getKey());
      }
      return result;
    } catch (Exception e) {
      log.error("Failed to sync factions", e);
      return SyncResult.failure(SyncEntityType.FACTIONS.getKey(), e.getMessage());
    }
  }

  private SyncResult performFactionsSync(@NonNull String operationId, long startTime) {
    try {
      if (!syncServiceDependencies
          .getNotionSyncProperties()
          .isEntityEnabled(SyncEntityType.FACTIONS.getKey())) {
        log.info("Factions sync is disabled in configuration");
        return SyncResult.success(SyncEntityType.FACTIONS.getKey(), 0, 0, 0);
      }

      syncServiceDependencies.getProgressTracker().startOperation(operationId, "Sync Factions", 3);
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(operationId, 1, "Retrieving factions from Notion...");

      if (!isNotionHandlerAvailable()) {
        log.warn("NotionHandler not available. Cannot sync factions from Notion.");
        return SyncResult.failure(
            SyncEntityType.FACTIONS.getKey(), "NotionHandler is not available for sync operations");
      }

      List<FactionPage> factionPages =
          executeWithRateLimit(syncServiceDependencies.getNotionHandler()::loadAllFactions);

      // Convert to DTOs
      List<FactionDTO> factionDTOs =
          processWithControlledParallelism(
              factionPages,
              this::convertFactionPageToDTO,
              10,
              operationId,
              2,
              "Converted %d/%d factions");

      // Save factions to database
      int savedCount =
          (int)
              processWithControlledParallelism(
                      factionDTOs,
                      this::saveOrUpdateFaction,
                      10,
                      operationId,
                      3,
                      "Saved %d/%d factions")
                  .stream()
                  .filter(Objects::nonNull)
                  .count();

      long totalTime = System.currentTimeMillis() - startTime;
      syncServiceDependencies
          .getProgressTracker()
          .completeOperation(
              operationId,
              true,
              String.format("Successfully synced %d factions", savedCount),
              savedCount);

      syncServiceDependencies
          .getHealthMonitor()
          .recordSuccess(SyncEntityType.FACTIONS.getKey(), totalTime, savedCount);

      return SyncResult.success(SyncEntityType.FACTIONS.getKey(), savedCount, 0, 0);

    } catch (Exception e) {
      log.error("❌ Faction sync failed", e);
      syncServiceDependencies
          .getProgressTracker()
          .failOperation(operationId, "Sync failed: " + e.getMessage());
      syncServiceDependencies
          .getHealthMonitor()
          .recordFailure(SyncEntityType.FACTIONS.getKey(), e.getMessage());
      return SyncResult.failure(SyncEntityType.FACTIONS.getKey(), e.getMessage());
    }
  }

  private FactionDTO convertFactionPageToDTO(@NonNull FactionPage factionPage) {
    try {
      FactionDTO dto = new FactionDTO();
      Map<String, Object> rawProperties = factionPage.getRawProperties();

      dto.setName(
          syncServiceDependencies
              .getNotionPageDataExtractor()
              .extractNameFromNotionPage(factionPage));
      dto.setExternalId(factionPage.getId());
      dto.setDescription(
          syncServiceDependencies
              .getNotionPageDataExtractor()
              .extractDescriptionFromNotionPage(factionPage));

      if (rawProperties != null) {
        Object isActiveObj = rawProperties.get("Active");
        if (isActiveObj instanceof Boolean) {
          dto.setIsActive((Boolean) isActiveObj);
        } else if (isActiveObj instanceof String str) {
          dto.setIsActive(str.equalsIgnoreCase("true") || str.equalsIgnoreCase("active"));
        }

        dto.setLeaderExternalId(extractRelationId(rawProperties.get("Leader")));
        if (dto.getLeaderExternalId() == null) {
          dto.setLeader(
              syncServiceDependencies
                  .getNotionPageDataExtractor()
                  .extractPropertyAsString(rawProperties, "Leader"));
        }
        dto.setMemberExternalIds(extractRelationIds(rawProperties.get("Members")));
        dto.setTeamExternalIds(extractRelationIds(rawProperties.get("Teams")));
      }
      return dto;
    } catch (Exception e) {
      log.error("Failed to convert faction page to DTO", e);
      return null;
    }
  }

  private String extractRelationId(Object property) {
    List<String> ids = extractRelationIds(property);
    return ids.isEmpty() ? null : ids.get(0);
  }

  private List<String> extractRelationIds(Object property) {
    List<String> ids = new ArrayList<>();
    if (property == null) return ids;

    if (property instanceof String str) {
      if (str.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
        ids.add(str);
      }
    } else if (property instanceof List<?> list) {
      for (Object item : list) {
        if (item instanceof String str
            && str.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
          ids.add(str);
        } else if (item instanceof Map<?, ?> map) {
          Object id = map.get("id");
          if (id instanceof String str) ids.add(str);
        }
      }
    } else if (property instanceof Map<?, ?> map) {
      Object id = map.get("id");
      if (id instanceof String str) ids.add(str);
    }
    return ids;
  }

  private Faction saveOrUpdateFaction(FactionDTO dto) {
    if (dto == null || dto.getName() == null) return null;

    try {
      Faction faction =
          factionService
              .findByExternalId(dto.getExternalId())
              .orElseGet(
                  () ->
                      factionService
                          .getFactionByName(dto.getName())
                          .orElseGet(() -> Faction.builder().build()));

      faction.setName(dto.getName());
      faction.setExternalId(dto.getExternalId());
      faction.setDescription(dto.getDescription());
      faction.setActive(dto.getIsActive() != null ? dto.getIsActive() : true);
      faction.setAlignment(dto.getAlignment());

      if (dto.getFormedDate() != null) {
        faction.setFormedDate(Instant.parse(dto.getFormedDate()));
      }
      if (dto.getDisbandedDate() != null) {
        faction.setDisbandedDate(Instant.parse(dto.getDisbandedDate()));
      }

      // Resolve relationships
      if (dto.getLeaderExternalId() != null) {
        syncServiceDependencies
            .getWrestlerRepository()
            .findByExternalId(dto.getLeaderExternalId())
            .ifPresent(faction::setLeader);
      }
      if (faction.getLeader() == null && dto.getLeader() != null) {
        syncServiceDependencies
            .getWrestlerRepository()
            .findByName(dto.getLeader())
            .ifPresent(faction::setLeader);
      }

      if (dto.getManagerExternalId() != null) {
        syncServiceDependencies
            .getNpcRepository()
            .findByExternalId(dto.getManagerExternalId())
            .ifPresent(faction::setManager);
      }

      // Sync members
      if (!dto.getMemberExternalIds().isEmpty()) {
        for (String extId : dto.getMemberExternalIds()) {
          syncServiceDependencies
              .getWrestlerRepository()
              .findByExternalId(extId)
              .ifPresent(faction::addMember);
        }
      }

      // Sync teams
      if (!dto.getTeamExternalIds().isEmpty()) {
        for (String extId : dto.getTeamExternalIds()) {
          syncServiceDependencies
              .getTeamRepository()
              .findByExternalId(extId)
              .ifPresent(
                  team -> {
                    team.setFaction(faction);
                    faction.getTeams().add(team);
                  });
        }
      }

      factionService.save(faction);
      return faction;
    } catch (Exception e) {
      log.error("Failed to save faction '{}': {}", dto.getName(), e.getMessage());
      return null;
    }
  }
}
