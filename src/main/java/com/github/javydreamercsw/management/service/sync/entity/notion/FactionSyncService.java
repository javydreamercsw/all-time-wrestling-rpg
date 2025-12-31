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
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.sync.SyncEntityType;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.entity.FactionDTO;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

      log.info("📥 Retrieving factions from Notion...");
      long retrieveStart = System.currentTimeMillis();

      if (!isNotionHandlerAvailable()) {
        log.warn("NotionHandler not available. Cannot sync factions from Notion.");
        return SyncResult.failure(
            SyncEntityType.FACTIONS.getKey(), "NotionHandler is not available for sync operations");
      }

      List<FactionPage> factionPages =
          executeWithRateLimit(syncServiceDependencies.getNotionHandler()::loadAllFactions);
      log.info(
          "✅ Retrieved {} factions in {}ms",
          factionPages.size(),
          System.currentTimeMillis() - retrieveStart);

      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(
              operationId,
              1,
              String.format(
                  "✅ Retrieved %d factions from Notion in %dms",
                  factionPages.size(), System.currentTimeMillis() - retrieveStart));

      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(
              operationId,
              2,
              String.format(
                  "Converting %d factions to DTOs and merging with existing data...",
                  factionPages.size()));
      log.info("🔄 Converting factions to DTOs and merging with existing data...");
      long convertStart = System.currentTimeMillis();
      List<FactionDTO> factionDTOs = convertAndMergeFactionData(factionPages);
      log.info(
          "✅ Converted and merged {} factions in {}ms",
          factionDTOs.size(),
          System.currentTimeMillis() - convertStart);

      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(
              operationId,
              2,
              String.format(
                  "✅ Converted and merged %d factions in %dms",
                  factionDTOs.size(), System.currentTimeMillis() - convertStart));

      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(
              operationId,
              3,
              String.format("Saving %d factions to database...", factionDTOs.size()));
      log.info("🗄️ Saving factions to database...");
      long dbStart = System.currentTimeMillis();
      int[] result = saveFactionsToDatabase(factionDTOs, operationId);
      int savedCount = result[0];
      int skippedCount = result[1];
      log.info(
          "✅ Saved {} factions to database in {}ms",
          savedCount,
          System.currentTimeMillis() - dbStart);

      long totalTime = System.currentTimeMillis() - startTime;
      log.info(
          "🎉 Successfully synchronized {} factions in {}ms total", factionDTOs.size(), totalTime);

      syncServiceDependencies
          .getProgressTracker()
          .completeOperation(
              operationId,
              true,
              String.format("Successfully synced %d factions", factionDTOs.size()),
              factionDTOs.size());

      syncServiceDependencies
          .getHealthMonitor()
          .recordSuccess(SyncEntityType.FACTIONS.getKey(), totalTime, factionDTOs.size());

      if (skippedCount > 0) {
        return SyncResult.failure(
            SyncEntityType.FACTIONS.getKey(),
            "Some factions failed to sync. Check logs for details.");
      } else {
        return SyncResult.success(SyncEntityType.FACTIONS.getKey(), factionDTOs.size(), 0, 0);
      }

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;
      log.error("❌ Failed to synchronize factions from Notion after {}ms", totalTime, e);

      syncServiceDependencies
          .getProgressTracker()
          .failOperation(operationId, "Sync failed: " + e.getMessage());

      syncServiceDependencies
          .getHealthMonitor()
          .recordFailure(SyncEntityType.FACTIONS.getKey(), e.getMessage());

      return SyncResult.failure(SyncEntityType.FACTIONS.getKey(), e.getMessage());
    }
  }

  private List<FactionDTO> convertAndMergeFactionData(@NonNull List<FactionPage> factionPages) {
    Map<String, FactionDTO> existingFactions = new HashMap<>();
    if (syncServiceDependencies.getNotionSyncProperties().isLoadFromJson()) {
      // DTO does not exist for factions yet.
    }

    List<FactionDTO> mergedFactions = new ArrayList<>();

    for (FactionPage factionPage : factionPages) {
      FactionDTO notionDTO = convertFactionPageToDTO(factionPage);

      FactionDTO existingDTO = null;
      if (notionDTO.getExternalId() != null) {
        existingDTO =
            existingFactions.values().stream()
                .filter(w -> notionDTO.getExternalId().equals(w.getExternalId()))
                .findFirst()
                .orElse(null);
      }
      if (existingDTO == null && notionDTO.getName() != null) {
        existingDTO = existingFactions.get(notionDTO.getName());
      }

      FactionDTO mergedDTO = mergeFactionData(existingDTO, notionDTO);
      mergedFactions.add(mergedDTO);
    }

    for (FactionDTO existing : existingFactions.values()) {
      boolean foundInNotion =
          mergedFactions.stream()
              .anyMatch(
                  merged ->
                      (existing.getExternalId() != null
                              && existing.getExternalId().equals(merged.getExternalId()))
                          || (existing.getName() != null
                              && existing.getName().equals(merged.getName())));

      if (!foundInNotion) {
        mergedFactions.add(existing);
        log.debug("Preserved local-only faction: {}", existing.getName());
      }
    }

    return mergedFactions;
  }

  private FactionDTO convertFactionPageToDTO(@NonNull FactionPage factionPage) {
    FactionDTO dto = new FactionDTO();
    dto.setName(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractNameFromNotionPage(factionPage));
    dto.setExternalId(factionPage.getId());

    Map<String, Object> rawProperties = factionPage.getRawProperties();
    if (rawProperties != null) {
      Object isActiveObj = rawProperties.get("Active");
      if (isActiveObj instanceof Boolean) {
        dto.setIsActive((Boolean) isActiveObj);
      }
      Object leaderObj = rawProperties.get("Leader");
      if (leaderObj instanceof String) {
        dto.setLeader((String) leaderObj);
      }
    }
    return dto;
  }

  private FactionDTO mergeFactionData(FactionDTO existing, @NonNull FactionDTO notion) {
    FactionDTO merged = new FactionDTO();

    // These are the source of truth from Notion
    merged.setName(notion.getName());
    merged.setExternalId(notion.getExternalId());

    // Smart merge for isActive
    if (notion.getIsActive() != null) {
      merged.setIsActive(notion.getIsActive());
    } else if (existing != null && existing.getIsActive() != null) {
      merged.setIsActive(existing.getIsActive());
    } else {
      merged.setIsActive(false); // Default to not active
    }

    // Smart merge for leader
    if (notion.getLeader() != null && !notion.getLeader().isBlank()) {
      merged.setLeader(notion.getLeader());
    } else if (existing != null && existing.getLeader() != null) {
      merged.setLeader(existing.getLeader());
    }

    // Preserve fields not sourced from Notion
    if (existing != null) {
      merged.setFormedDate(existing.getFormedDate());
      merged.setDisbandedDate(existing.getDisbandedDate());
    }

    return merged;
  }

  private int[] saveFactionsToDatabase(
      @NonNull List<FactionDTO> factionDTOs, @NonNull String operationId) {
    log.info("Starting database persistence for {} factions", factionDTOs.size());

    int savedCount = 0;
    int skippedCount = 0;
    int processedCount = 0;

    for (FactionDTO dto : factionDTOs) {
      processedCount++;

      if (processedCount % 5 == 0) {
        syncServiceDependencies
            .getProgressTracker()
            .updateProgress(
                operationId,
                4,
                String.format(
                    "Saving factions to database... (%d/%d processed)",
                    processedCount, factionDTOs.size()));
      }

      try {
        Faction faction = null;
        boolean isNewFaction = false;

        if (dto.getExternalId() != null && !dto.getExternalId().trim().isEmpty()) {
          faction = factionService.findByExternalId(dto.getExternalId()).orElse(null);
        }

        if (faction == null && dto.getName() != null && !dto.getName().trim().isEmpty()) {
          faction = factionService.getFactionByName(dto.getName()).orElse(null);
        }

        if (faction == null) {
          faction = Faction.builder().build();
          isNewFaction = true;
        }

        faction.setName(dto.getName());
        faction.setExternalId(dto.getExternalId());
        faction.setIsActive(dto.getIsActive());

        if (dto.getLeader() != null) {
          Optional<Wrestler> leader =
              syncServiceDependencies.getWrestlerRepository().findByName(dto.getLeader());
          if (leader.isPresent()) {
            faction.setLeader(leader.get());
          }
        }
        if (dto.getFormedDate() != null) {
          faction.setFormedDate(Instant.parse(dto.getFormedDate()));
        }
        if (dto.getDisbandedDate() != null) {
          faction.setDisbandedDate(Instant.parse(dto.getDisbandedDate()));
        }

        if (isNewFaction) {
          factionService.save(faction);
        } else {
          syncServiceDependencies.getFactionRepository().saveAndFlush(faction);
        }
        savedCount++;

      } catch (Exception e) {
        log.error("❌ Failed to save faction: {} - {}", dto.getName(), e.getMessage());
        skippedCount++;
      }
    }

    log.info(
        "Database persistence completed: {} saved/updated, {} skipped", savedCount, skippedCount);

    syncServiceDependencies
        .getProgressTracker()
        .updateProgress(
            operationId,
            4,
            String.format(
                "✅ Completed database save: %d factions saved/updated, %d skipped",
                savedCount, skippedCount));

    return new int[] {savedCount, skippedCount};
  }
}
