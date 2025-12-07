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
package com.github.javydreamercsw.management.service.sync;

import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduler service for automated Notion data synchronization. This service runs periodically to
 * sync data from Notion databases to local JSON files based on configuration settings.
 *
 * <p>Can be enabled/disabled via application properties: notion.sync.scheduler.enabled=true/false
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "notion.sync.scheduler.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class NotionSyncScheduler {

  private final NotionSyncService notionSyncService;
  private final NotionSyncProperties syncProperties;
  private final EntityDependencyAnalyzer dependencyAnalyzer;

  /**
   * Gets the list of entities to sync using automatic dependency analysis. Analyzes entity
   * relationships to determine the optimal sync order.
   *
   * @return List of entity names in dependency order
   */
  private List<String> getSyncEntities() {
    log.info("🤖 Using automatic dependency analysis for sync order");
    List<String> automaticEntities = dependencyAnalyzer.getAutomaticSyncOrder();
    log.info("📋 Determined sync order: {}", automaticEntities);
    return automaticEntities;
  }

  /**
   * Performs automatic synchronization of all configured entities. The interval is configurable via
   * notion.sync.scheduler.interval property.
   */
  @Scheduled(
      fixedRateString = "${notion.sync.scheduler.interval:3600000}",
      initialDelayString = "${notion.sync.scheduler.initial-delay:300000}")
  public void performScheduledSync() {
    if (!syncProperties.isSchedulerEnabled()) {
      log.debug("Notion sync scheduler is disabled, skipping scheduled sync");
      return;
    }

    log.info("=== STARTING SCHEDULED NOTION SYNC ===");

    try {
      List<NotionSyncService.SyncResult> results = new ArrayList<>();

      // Sync each entity in dependency order
      for (String entity : getSyncEntities()) {
        try {
          NotionSyncService.SyncResult result = syncEntity(entity, SyncDirection.OUTBOUND);
          results.add(result);

          if (result.isSuccess()) {
            log.info("✅ {} sync completed: {} items synchronized", entity, result.getSyncedCount());
          } else {
            log.error("❌ {} sync failed: {}", entity, result.getErrorMessage());
          }

        } catch (Exception e) {
          log.error("❌ Unexpected error syncing {}: {}", entity, e.getMessage(), e);
          results.add(NotionSyncService.SyncResult.failure(entity, e.getMessage()));
        }
      }

      // Log summary
      logSyncSummary(results);

    } catch (Exception e) {
      log.error("❌ Critical error during scheduled sync", e);
    }

    log.info("=== SCHEDULED NOTION SYNC COMPLETED ===");
  }

  /**
   * Synchronizes a specific entity type with custom operation ID for progress tracking.
   *
   * @param entityName The name of the entity to sync
   * @param operationId Custom operation ID for progress tracking
   * @return SyncResult containing the outcome of the sync operation
   */
  @SneakyThrows
  public NotionSyncService.SyncResult syncEntity(
      @NonNull String entityName, @NonNull String operationId, @NonNull SyncDirection direction) {
    log.debug("Syncing entity: {} with operation ID: {}", entityName, operationId);

    NotionSyncService.SyncResult result =
        switch (entityName.toLowerCase()) {
          case "shows" -> notionSyncService.syncShows(operationId, direction);
          case "wrestlers" -> notionSyncService.syncWrestlers(operationId, direction);
          case "factions" -> notionSyncService.syncFactions(operationId, direction);
          case "teams" -> notionSyncService.syncTeams(operationId, direction);
          case "templates" -> notionSyncService.syncShowTemplates(operationId, direction);
          case "seasons" -> notionSyncService.syncSeasons(operationId, direction);
          case "show-types" -> notionSyncService.syncShowTypes(operationId, direction);
          case "injuries" -> notionSyncService.syncInjuryTypes(operationId, direction);
          case "npcs" -> notionSyncService.syncNpcs(operationId, direction);
          case "titles" -> notionSyncService.syncTitles(operationId, direction);
          case "rivalries" -> notionSyncService.syncRivalries(operationId, direction);
          case "faction-rivalries" ->
              notionSyncService.syncFactionRivalries(operationId, direction);
          case "segments" -> notionSyncService.syncSegments(operationId, direction);
          default -> {
            log.warn("Unknown entity type for sync: {}", entityName);
            yield NotionSyncService.SyncResult.failure(entityName, "Unknown entity type");
          }
        };
    if (result.isSuccess()) {
      syncProperties.setLastSyncTime(entityName, LocalDateTime.now());
    }
    return result;
  }

  /**
   * Syncs a specific entity based on its name.
   *
   * @param entityName The name of the entity to sync
   * @return SyncResult containing the outcome of the sync operation
   */
  public NotionSyncService.SyncResult syncEntity(
      @NonNull String entityName, @NonNull SyncDirection direction) {
    log.debug("Syncing entity: {}", entityName);

    // Generate operation ID for progress tracking
    String operationId = "sync-" + entityName.toLowerCase() + "-" + System.currentTimeMillis();

    return syncEntity(entityName, operationId, direction);
  }

  /**
   * Logs a summary of all sync operations performed.
   *
   * @param results List of sync results to summarize
   */
  private void logSyncSummary(
      List<com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult>
          results) {
    int successCount = 0;
    int failureCount = 0;
    int totalSynced = 0;

    log.info("=== SYNC SUMMARY ===");
    for (com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult result :
        results) {
      if (result.isSuccess()) {
        successCount++;
        totalSynced += result.getSyncedCount();
      } else {
        failureCount++;
        log.error("❌ {} sync failed: {}", result.getEntityType(), result.getErrorMessage());
      }
      if (!result.getMessages().isEmpty()) {
        log.info("Messages for {}:", result.getEntityType());
        result.getMessages().forEach(log::warn);
      }
    }

    log.info("Total entities processed: {}", results.size());
    log.info("Successful syncs: {}", successCount);
    log.info("Failed syncs: {}", failureCount);
    log.info("Total items synchronized: {}", totalSynced);

    if (failureCount > 0) {
      log.warn("Some sync operations failed. Check logs for details.");
    }
  }

  /**
   * Manual trigger for immediate sync of all entities. This method can be called programmatically
   * to force a sync operation outside of the regular schedule.
   *
   * @return List of sync results for all entities
   */
  public List<NotionSyncService.SyncResult> triggerManualSync() {
    log.info("=== MANUAL NOTION SYNC TRIGGERED ===");

    // Clear sync session at the start of batch operation
    notionSyncService.clearSyncSession();

    List<NotionSyncService.SyncResult> results = new ArrayList<>();

    try {
      for (String entity : getSyncEntities()) {
        try {
          NotionSyncService.SyncResult result = syncEntity(entity, SyncDirection.OUTBOUND);
          results.add(result);

          if (result.isSuccess()) {
            log.info(
                "✅ Manual {} sync completed: {} items synchronized",
                entity,
                result.getSyncedCount());
          } else {
            log.error("❌ Manual {} sync failed: {}", entity, result.getErrorMessage());
          }

        } catch (Exception e) {
          log.error("❌ Unexpected error during manual {} sync: {}", entity, e.getMessage(), e);
          results.add(NotionSyncService.SyncResult.failure(entity, e.getMessage()));
        }
      }

      logSyncSummary(results);
      log.info("=== MANUAL NOTION SYNC COMPLETED ===");

    } finally {
      // Clean up sync session thread local
      notionSyncService.cleanupSyncSession();
    }

    return results;
  }

  /**
   * Manual trigger for syncing a specific entity.
   *
   * @param entityName The name of the entity to sync
   * @return SyncResult for the specified entity
   */
  public NotionSyncService.SyncResult triggerEntitySync(@NonNull String entityName) {
    log.info("=== MANUAL {} SYNC TRIGGERED ===", entityName.toUpperCase());

    try {
      NotionSyncService.SyncResult result = syncEntity(entityName, SyncDirection.OUTBOUND);

      if (result.isSuccess()) {
        log.info(
            "✅ Manual {} sync completed: {} items synchronized",
            entityName,
            result.getSyncedCount());
      } else {
        log.error("❌ Manual {} sync failed: {}", entityName, result.getErrorMessage());
      }

      return result;

    } catch (Exception e) {
      log.error("❌ Unexpected error during manual {} sync: {}", entityName, e.getMessage(), e);
      return NotionSyncService.SyncResult.failure(entityName, e.getMessage());
    }
  }

  /**
   * Get the current sync configuration status.
   *
   * @return String describing the current sync configuration
   */
  public String getSyncStatus() {
    StringBuilder status = new StringBuilder();
    status.append("Notion Sync Status:\n");
    status.append("- Sync Enabled: ").append(syncProperties.isEnabled()).append("\n");
    status.append("- Scheduler Enabled: ").append(syncProperties.isSchedulerEnabled()).append("\n");
    status
        .append("- Sync Interval: ")
        .append(syncProperties.getScheduler().getInterval())
        .append("ms\n");
    status.append("- Entities: ").append(String.join(", ", getSyncEntities())).append("\n");
    status.append("- Backup Enabled: ").append(syncProperties.isBackupEnabled()).append("\n");

    if (syncProperties.isBackupEnabled()) {
      status
          .append("- Backup Directory: ")
          .append(syncProperties.getBackup().getDirectory())
          .append("\n");
      status
          .append("- Max Backup Files: ")
          .append(syncProperties.getBackup().getMaxFiles())
          .append("\n");
    }

    return status.toString();
  }

  /**
   * Retrieves the last sync time for a specific entity.
   *
   * @param entityName The name of the entity.
   * @return The last sync time, or null if it has never been synced.
   */
  public LocalDateTime getLastSyncTime(String entityName) {
    return syncProperties.getLastSyncTime(entityName);
  }
}
