package com.github.javydreamercsw.management.service.sync.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
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

  public SeasonSyncService(ObjectMapper objectMapper, NotionSyncProperties syncProperties) {
    super(objectMapper, syncProperties);
  }

  /**
   * Synchronizes seasons from Notion to the database.
   *
   * @param operationId Operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncSeasons(@NonNull String operationId) {
    // Check if already synced in current session
    if (isAlreadySyncedInSession("seasons")) {
      log.info("⏭️ Seasons already synced in current session, skipping");
      return SyncResult.success("Seasons", 0, 0);
    }

    log.info("📅 Starting seasons synchronization from Notion...");
    long startTime = System.currentTimeMillis();

    try {
      SyncResult result = performSeasonsSync(operationId, startTime);
      if (result.isSuccess()) {
        markAsSyncedInSession("seasons");
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
      return SyncResult.success("Seasons", savedCount, 0);

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
      String name = extractNameFromNotionPage(seasonPage);
      dto.setName(name);

      // Set Notion ID for sync tracking
      dto.setNotionId(seasonPage.getId());

      // Extract description if available
      String description = extractPropertyAsString(seasonPage.getRawProperties(), "Description");
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
        // Check if season already exists by name
        Season existingSeason = seasonService.findByName(seasonDTO.getName());

        if (existingSeason != null) {
          log.info("Season already exists: {}", seasonDTO.getName());
          // Update Notion ID if not set
          if (existingSeason.getNotionId() == null && seasonDTO.getNotionId() != null) {
            existingSeason.setNotionId(seasonDTO.getNotionId());
            seasonService.save(existingSeason);
            updatedCount++;
            log.info("Updated Notion ID for existing season: {}", seasonDTO.getName());
          } else {
            skippedCount++;
            log.debug("Season '{}' already up-to-date, skipping", seasonDTO.getName());
          }
        } else {
          // Create new season
          Season newSeason = new Season();
          newSeason.setName(seasonDTO.getName());
          newSeason.setDescription(seasonDTO.getDescription());
          newSeason.setNotionId(seasonDTO.getNotionId());

          seasonService.save(newSeason);
          savedCount++;
          log.info("Created new season: {}", seasonDTO.getName());
        }

      } catch (Exception e) {
        log.error("Failed to save season '{}': {}", seasonDTO.getName(), e.getMessage(), e);
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

        log.info(
            "✅ Created default season: {} (ID: {})",
            defaultSeason.getName(),
            defaultSeason.getId());
        return SyncResult.success("Seasons", 1, 0);
      } else {
        log.info("Found {} existing seasons in database", existingSeasons.getTotalElements());
        return SyncResult.success("Seasons", 0, 0);
      }
    } catch (Exception e) {
      log.error("Failed to create default season", e);
      return SyncResult.failure("Seasons", "Failed to create default season: " + e.getMessage());
    }
  }
}
