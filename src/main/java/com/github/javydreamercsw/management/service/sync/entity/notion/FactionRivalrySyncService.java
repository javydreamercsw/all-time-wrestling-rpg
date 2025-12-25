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
import com.github.javydreamercsw.base.ai.notion.FactionRivalryPage;
import com.github.javydreamercsw.base.ai.notion.NotionApiExecutor;
import com.github.javydreamercsw.base.domain.faction.Faction;
import com.github.javydreamercsw.base.domain.faction.FactionRepository;
import com.github.javydreamercsw.base.domain.faction.FactionRivalry;
import com.github.javydreamercsw.management.service.faction.FactionRivalryService;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FactionRivalrySyncService extends BaseSyncService {

  private final FactionRivalryService factionRivalryService;
  private final FactionRepository factionRepository;

  @Autowired
  public FactionRivalrySyncService(
      ObjectMapper objectMapper,
      SyncServiceDependencies syncServiceDependencies,
      FactionRivalryService factionRivalryService,
      FactionRepository factionRepository,
      NotionApiExecutor notionApiExecutor) {
    super(objectMapper, syncServiceDependencies, notionApiExecutor);
    this.factionRivalryService = factionRivalryService;
    this.factionRepository = factionRepository;
  }

  public SyncResult syncFactionRivalries(@NonNull String operationId) {
    if (syncServiceDependencies
        .getSyncSessionManager()
        .isAlreadySyncedInSession("faction-rivalries")) {
      log.info("⏭️ Faction Rivalries already synced in current session, skipping");
      return SyncResult.success("Faction Rivalries", 0, 0, 0);
    }

    log.info("🔥 Starting faction rivalries synchronization from Notion...");

    try {
      SyncResult result = performFactionRivalriesSync(operationId);
      if (result.isSuccess()) {
        syncServiceDependencies.getSyncSessionManager().markAsSyncedInSession("faction-rivalries");
      }
      return result;
    } catch (Exception e) {
      log.error("Failed to sync faction rivalries", e);
      syncServiceDependencies.getHealthMonitor().recordFailure("Faction Rivalries", e.getMessage());
      return SyncResult.failure("Faction Rivalries", e.getMessage());
    }
  }

  @SneakyThrows
  private SyncResult performFactionRivalriesSync(@NonNull String operationId) {
    if (!isNotionHandlerAvailable()) {
      return SyncResult.failure("Faction Rivalries", "NotionHandler is not available.");
    }

    syncServiceDependencies
        .getProgressTracker()
        .startOperation(operationId, "Sync Faction Rivalries", 3);

    // 1. Load all rivalry pages from Notion
    syncServiceDependencies
        .getProgressTracker()
        .updateProgress(operationId, 1, "Retrieving faction rivalries from Notion...");
    List<FactionRivalryPage> pages =
        executeWithRateLimit(notionApiExecutor.getNotionHandler()::loadAllFactionRivalries);
    log.info("Found {} faction rivalries in Notion.", pages.size());
    syncServiceDependencies
        .getProgressTracker()
        .updateProgress(operationId, 1, "Retrieved " + pages.size() + " faction rivalries.");

    // 2. Convert pages to DTOs
    syncServiceDependencies
        .getProgressTracker()
        .updateProgress(operationId, 2, "Processing faction rivalry data...");
    List<FactionRivalryDTO> dtos = pages.stream().map(this::toDto).collect(Collectors.toList());
    syncServiceDependencies
        .getProgressTracker()
        .updateProgress(operationId, 2, "Processed " + dtos.size() + " faction rivalries.");

    // 3. Save to database
    syncServiceDependencies
        .getProgressTracker()
        .updateProgress(operationId, 3, "Saving faction rivalries to database...");
    AtomicInteger createdCount = new AtomicInteger(0);
    AtomicInteger updatedCount = new AtomicInteger(0);
    saveFactionRivalriesToDatabase(dtos, createdCount, updatedCount);
    syncServiceDependencies
        .getProgressTracker()
        .updateProgress(
            operationId,
            3,
            "Saved to database. Created: "
                + createdCount.get()
                + ", Updated: "
                + updatedCount.get());

    syncServiceDependencies
        .getProgressTracker()
        .completeOperation(
            operationId, true, "Sync complete.", createdCount.get() + updatedCount.get());
    syncServiceDependencies
        .getHealthMonitor()
        .recordSuccess(
            "Faction Rivalries",
            System.currentTimeMillis() - System.currentTimeMillis(),
            createdCount.get() + updatedCount.get());

    return SyncResult.success("Faction Rivalries", createdCount.get(), updatedCount.get(), 0);
  }

  private FactionRivalryDTO toDto(FactionRivalryPage page) {
    FactionRivalryDTO dto = new FactionRivalryDTO();
    Map<String, Object> props = page.getRawProperties();
    dto.setExternalId(page.getId());
    try {
      dto.setFaction1Name(extractStringPropertyFromNotionPage(page, "Faction 1"));
      dto.setFaction2Name(extractStringPropertyFromNotionPage(page, "Faction 2"));
    } catch (ClassCastException e) {
      log.warn("Failed to cast faction name for rivalry page {}: {}", page.getId(), e.getMessage());
    }
    try {
      Object heatObj = props.get("Heat");
      if (heatObj instanceof Number) {
        dto.setHeat(((Number) heatObj).intValue());
      } else if (heatObj instanceof String) {
        dto.setHeat(Integer.parseInt((String) heatObj));
      } else {
        dto.setHeat(0);
      }
    } catch (NumberFormatException e) {
      log.warn(
          "Invalid heat value for faction rivalry page {}: {}", page.getId(), props.get("Heat"));
      dto.setHeat(0);
    }
    return dto;
  }

  private void saveFactionRivalriesToDatabase(
      List<FactionRivalryDTO> dtos, AtomicInteger createdCount, AtomicInteger updatedCount) {
    for (FactionRivalryDTO dto : dtos) {
      if (dto.getFaction1Name() == null || dto.getFaction2Name() == null) {
        log.warn(
            "Skipping faction rivalry with missing faction names (ID: {})", dto.getExternalId());
        continue;
      }

      Optional<Faction> faction1Opt = factionRepository.findByName(dto.getFaction1Name());
      Optional<Faction> faction2Opt = factionRepository.findByName(dto.getFaction2Name());

      if (faction1Opt.isEmpty() || faction2Opt.isEmpty()) {
        log.warn(
            "Skipping faction rivalry for '{}' and '{}' because one or both factions were not"
                + " found.",
            dto.getFaction1Name(),
            dto.getFaction2Name());
        continue;
      }

      Faction f1 = faction1Opt.get();
      Faction f2 = faction2Opt.get();

      // Find by externalId first
      Optional<FactionRivalry> existingRivalryOpt =
          factionRivalryService.findByExternalId(dto.getExternalId());
      if (existingRivalryOpt.isEmpty()) {
        // Fallback to factions
        existingRivalryOpt =
            factionRivalryService.getFactionRivalryBetweenFactions(f1.getId(), f2.getId());
      }

      FactionRivalry rivalry;
      boolean newRivalry = false;
      if (existingRivalryOpt.isPresent()) {
        rivalry = existingRivalryOpt.get();
        if (rivalry.getHeat() != dto.getHeat()) {
          int heatChange = dto.getHeat() - rivalry.getHeat();
          rivalry =
              factionRivalryService
                  .addHeat(rivalry.getId(), heatChange, "Notion Sync")
                  .orElse(rivalry);
        }
      } else {
        Optional<FactionRivalry> newRivalryOpt =
            factionRivalryService.createFactionRivalry(
                f1.getId(), f2.getId(), "Created from Notion Sync");
        if (newRivalryOpt.isEmpty()) {
          log.warn("Failed to create faction rivalry for {} and {}", f1.getName(), f2.getName());
          continue;
        }
        rivalry = newRivalryOpt.get();
        rivalry.setHeat(dto.getHeat());
        newRivalry = true;
      }

      rivalry.setExternalId(dto.getExternalId());

      factionRivalryService.save(rivalry);

      if (newRivalry) {
        createdCount.incrementAndGet();
        log.info("Created new faction rivalry: {} vs {}", f1.getName(), f2.getName());
      } else {
        updatedCount.incrementAndGet();
        log.info("Updated faction rivalry: {} vs {}", f1.getName(), f2.getName());
      }
    }
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

  @Data
  private static class FactionRivalryDTO {
    private String externalId;
    private String faction1Name;
    private String faction2Name;
    private int heat;
  }
}
