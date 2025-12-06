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
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.SeasonPage;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.dto.SeasonDTO;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/** Service responsible for synchronizing seasons from Notion to the database. */
@Service
@Slf4j
public class SeasonSyncService extends BaseSyncService {

  @Autowired private SeasonService seasonService;
  @Autowired private NotionPageDataExtractor notionPageDataExtractor;
  @Autowired private SyncSessionManager syncSessionManager;

  @Autowired
  public SeasonSyncService(
      ObjectMapper objectMapper, NotionSyncProperties syncProperties, NotionHandler notionHandler) {
    super(objectMapper, syncProperties, notionHandler);
  }

  /**
   * Synchronizes seasons from Notion to the database.
   *
   * @param operationId Operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncSeasons(@NonNull String operationId) {
    // Check if already synced in current session
    if (syncSessionManager.isAlreadySyncedInSession("seasons")) {
      return SyncResult.success("Seasons", 0, 0, 0);
    }

    log.info("📅 Starting seasons synchronization from Notion...");
    long startTime = System.currentTimeMillis();

    try {
      SyncResult result = performSeasonsSync(operationId, startTime);
      if (result.isSuccess()) {
        syncSessionManager.markAsSyncedInSession("seasons");
      }
      return result;
    } catch (Exception e) {
      log.error("Failed to sync seasons", e);
      return SyncResult.failure("Seasons", e.getMessage());
    }
  }

  private SyncResult performSeasonsSync(@NonNull String operationId, long startTime) {
    try {
      // Check if NOTION_TOKEN is available
      if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
        log.warn("NOTION_TOKEN not available. Creating default season instead.");
        return createDefaultSeasonIfNeeded();
      }

      // Initialize progress tracking if operation ID provided
      progressTracker.startOperation(operationId, "Sync Seasons", 4);
      progressTracker.updateProgress(operationId, 1, "Retrieving seasons from Notion...");

      // Retrieve seasons from Notion
      log.info("📥 Retrieving seasons from Notion...");

      // Check if NotionHandler is available
      if (!isNotionHandlerAvailable()) {
        log.warn("NotionHandler not available. Creating default season instead.");
        return createDefaultSeasonIfNeeded();
      }

      rateLimitService.acquirePermit();
      List<SeasonPage> seasonPages = notionHandler.loadAllSeasons();
      log.info(
          "✅ Retrieved {} seasons in {}ms",
          seasonPages.size(),
          System.currentTimeMillis() - startTime);

      if (seasonPages.isEmpty()) {
        log.info("No seasons found in Notion. Creating default season instead.");
        return createDefaultSeasonIfNeeded();
      }

      // Convert to DTOs
      progressTracker.updateProgress(operationId, 2, "Converting seasons to DTOs...");
      log.info("🔄 Converting seasons to DTOs...");
      List<SeasonDTO> seasonDTOs = convertSeasonPagesToDTO(seasonPages, operationId);
      log.info("✅ Converted {} seasons to DTOs", seasonDTOs.size());

      // Save to database
      progressTracker.updateProgress(operationId, 3, "Saving seasons to database...");
      log.info("💾 Saving seasons to database...");
      int savedCount = saveSeasonsToDB(seasonDTOs);
      log.info("✅ Processed {} seasons ({} new seasons created)", seasonDTOs.size(), savedCount);

      // Complete progress tracking
      progressTracker.updateProgress(operationId, 4, "Seasons sync completed");
      progressTracker.completeOperation(
          operationId, true, "Seasons sync completed successfully", savedCount);

      long totalTime = System.currentTimeMillis() - startTime;
      log.info("🎉 Successfully synchronized {} seasons in {}ms total", savedCount, totalTime);
      return SyncResult.success("Seasons", savedCount, 0, 0);

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;
      log.error("❌ Failed to synchronize seasons after {}ms", totalTime, e);

      progressTracker.failOperation(operationId, "Seasons sync failed: " + e.getMessage());

      return SyncResult.failure("Seasons", e.getMessage());
    }
  }

  private List<SeasonDTO> convertSeasonPagesToDTO(
      @NonNull List<SeasonPage> seasonPages, String operationId) {
    return processWithControlledParallelism(
        seasonPages,
        this::convertSeasonPageToDTO,
        10, // Batch size
        operationId,
        2, // Progress step
        "Converted %d/%d seasons");
  }

  private SeasonDTO convertSeasonPageToDTO(@NonNull SeasonPage seasonPage) {
    try {
      SeasonDTO dto = new SeasonDTO();

      // Extract name from Notion page
      String name = notionPageDataExtractor.extractNameFromNotionPage(seasonPage);
      dto.setName(name);

      // Set Notion ID for sync tracking
      dto.setNotionId(seasonPage.getId());

      // Extract description if available
      String description = notionPageDataExtractor.extractPropertyAsString(seasonPage.getRawProperties(), "Description");
      if (description != null && !description.trim().isEmpty()) {
        dto.setDescription(description);
      } else {
        dto.setDescription("Season synced from Notion");
      }

      log.debug("Converted season: {} (Notion ID: {})", name, seasonPage.getId());
      return dto;

    } catch (Exception e) {
      log.error("Failed to convert season page to DTO: {}", seasonPage.getId(), e);
      return null;
    }
  }

  /** Saves season DTOs to the database. */
  private int saveSeasonsToDB(@NonNull List<SeasonDTO> seasonDTOs) {
    int savedCount = 0;
    int updatedCount = 0;
    int skippedCount = 0;

    for (SeasonDTO seasonDTO : seasonDTOs) {
      try {
        Season existingSeason = null;

        // Find by external ID first
        if (seasonDTO.getNotionId() != null && !seasonDTO.getNotionId().isBlank()) {
          existingSeason = seasonService.findByExternalId(seasonDTO.getNotionId()).orElse(null);
        }

        // If not found, find by name
        if (existingSeason == null) {
          existingSeason = seasonService.findByName(seasonDTO.getName());
        }

        if (existingSeason != null) {
          boolean updated = false;
          if (existingSeason.getExternalId() == null
              || !existingSeason.getExternalId().equals(seasonDTO.getNotionId())) {
            existingSeason.setExternalId(seasonDTO.getNotionId());
            updated = true;
          }
          if (existingSeason.getName() == null
              || !existingSeason.getName().equals(seasonDTO.getName())) {
            existingSeason.setName(seasonDTO.getName());
            updated = true;
          }
          if (existingSeason.getDescription() == null
              || !existingSeason.getDescription().equals(seasonDTO.getDescription())) {
            existingSeason.setDescription(seasonDTO.getDescription());
            updated = true;
          }

          if (updated) {
            seasonService.save(existingSeason);
            updatedCount++;
            log.info("Updating existing season: {}", seasonDTO.getName());
          } else {
            skippedCount++;
          }
        } else {
          // Create new season
          Season newSeason = new Season();
          newSeason.setName(seasonDTO.getName());
          newSeason.setDescription(seasonDTO.getDescription());
          newSeason.setExternalId(seasonDTO.getNotionId());

          seasonService.save(newSeason);
          savedCount++;
          log.info("Created new season: {}", seasonDTO.getName());
        }

      } catch (Exception e) {
        log.error("Failed to save season '{}': {}", seasonDTO.getName(), e.getMessage(), e);
        skippedCount++;
      }
    }

    log.info(
        "Season sync summary: {} new, {} updated, {} skipped",
        savedCount,
        updatedCount,
        skippedCount);
    return savedCount;
  }

  /** Creates a default season if no seasons exist in the database. */
  private SyncResult createDefaultSeasonIfNeeded() {
    try {
      // Check if any seasons exist
      var existingSeasons = seasonService.getAllSeasons(Pageable.unpaged());
      if (existingSeasons == null || existingSeasons.isEmpty()) {
        log.info("No seasons found in database. Creating default season...");

        // Create a default season
        Season defaultSeason =
            seasonService.createSeason(
                "Season 1", "Default season created by sync process", 5 // 5 shows per PPV
                );

        log.info("Created new season: {}", defaultSeason.getName());
        return SyncResult.success("Seasons", 1, 0, 0);
      } else {
        log.info("Season already exists: {}", existingSeasons.iterator().next().getName());
        return SyncResult.success("Seasons", 0, 0, 0);
      }
    } catch (Exception e) {
      log.error("Failed to create default season", e);
      return SyncResult.failure("Seasons", "Failed to create default season: " + e.getMessage());
    }
  }
}
