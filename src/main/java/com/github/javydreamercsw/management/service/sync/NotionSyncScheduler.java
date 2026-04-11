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

import com.github.javydreamercsw.base.config.NotionSyncProperties;
import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Scheduler service for automated Notion data synchronization. This service runs periodically to
 *
 * <p>sync data from Notion databases to local JSON files based on configuration settings.
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

  private final BackupService backupService;

  private final SyncServiceDependencies syncServiceDependencies;

  /**
   * Gets the list of entities to sync using automatic dependency analysis. Analyzes entity
   *
   * <p>relationships to determine the optimal sync order.
   *
   * @return List of SyncEntityType in dependency order
   */
  private List<SyncEntityType> getSyncEntities() {
    log.info("🤖 Using automatic dependency analysis for sync order");
    List<SyncEntityType> automaticEntities = dependencyAnalyzer.getAutomaticSyncOrder();
    log.info("📋 Determined sync order: {}", automaticEntities);
    return automaticEntities;
  }

  /**
   * Performs automatic synchronization of all configured entities. The interval is configurable via
   *
   * <p>notion.sync.scheduler.interval property.
   */
  @Scheduled(
      fixedRateString = "${notion.sync.scheduler.interval:3600000}",
      initialDelayString = "${notion.sync.scheduler.initial-delay:300000}")
  public void performScheduledSync() {

    GeneralSecurityUtils.runAsAdmin(
        (Supplier<Void>)
            () -> {
              if (!syncProperties.isSchedulerEnabled()
                  || !notionSyncService.isNotionHandlerAvailable()) {
                log.debug("Notion sync scheduler is disabled, skipping scheduled sync");
                return null;
              }

              log.info("=== STARTING SCHEDULED NOTION SYNC ===");
              try {
                List<NotionSyncService.SyncResult> results = new ArrayList<>();
                // Sync each entity in dependency order
                for (SyncEntityType entity : getSyncEntities()) {
                  try {
                    NotionSyncService.SyncResult result =
                        syncEntity(entity, SyncDirection.OUTBOUND);
                    results.add(result);
                    if (result.isSuccess()) {
                      log.info(
                          "✅ {} sync completed: {} items synchronized",
                          entity,
                          result.getSyncedCount());
                    } else {
                      log.error("❌ {} sync failed: {}", entity, result.getErrorMessage());
                    }
                  } catch (Exception e) {
                    log.error("❌ Unexpected error syncing {}: {}", entity, e.getMessage(), e);
                    results.add(
                        NotionSyncService.SyncResult.failure(entity.getKey(), e.getMessage()));
                  }
                }

                // Log summary
                logSyncSummary(results);
              } catch (Exception e) {
                log.error("❌ Critical error during scheduled sync", e);
              }
              log.info("=== SCHEDULED NOTION SYNC COMPLETED ===");
              return null;
            });
  }

  /**
   * Synchronizes a specific entity type with custom operation ID for progress tracking.
   *
   * @param entityType The type of entity to sync
   * @param operationId Custom operation ID for progress tracking
   * @return SyncResult containing the outcome of the sync operation
   */
  @SneakyThrows
  @PreAuthorize("hasRole('ADMIN')")
  public NotionSyncService.SyncResult syncEntity(
      @NonNull SyncEntityType entityType,
      @NonNull String operationId,
      @NonNull SyncDirection direction) {
    log.debug("Syncing entity: {} with operation ID: {}", entityType, operationId);

    NotionSyncService.SyncResult result =
        switch (entityType) {
          case SHOWS -> notionSyncService.syncShows(operationId, direction);
          case WRESTLERS -> notionSyncService.syncWrestlers(operationId, direction);
          case FACTIONS -> notionSyncService.syncFactions(operationId, direction);
          case TEAMS -> notionSyncService.syncTeams(operationId, direction);
          case TEMPLATES -> notionSyncService.syncShowTemplates(operationId, direction);
          case SEASONS -> notionSyncService.syncSeasons(operationId, direction);
          case SHOW_TYPES -> notionSyncService.syncShowTypes(operationId, direction);
          case INJURY_TYPES -> notionSyncService.syncInjuryTypes(operationId, direction);
          case NPCS -> notionSyncService.syncNpcs(operationId, direction);
          case TITLES -> notionSyncService.syncTitles(operationId, direction);
          case RIVALRIES -> notionSyncService.syncRivalries(operationId, direction);
          case FACTION_RIVALRIES -> notionSyncService.syncFactionRivalries(operationId, direction);
          case SEGMENTS -> notionSyncService.syncSegments(operationId, direction);
          case TITLE_REIGN -> notionSyncService.syncTitleReigns(operationId);
        };
    if (result.isSuccess()) {
      syncProperties.setLastSyncTime(entityType.getKey(), LocalDateTime.now());
    }
    return result;
  }

  /**
   * Syncs a specific entity based on its type.
   *
   * @param entityType The type of entity to sync
   * @return SyncResult containing the outcome of the sync operation
   */
  @PreAuthorize("hasRole('ADMIN')")
  public NotionSyncService.SyncResult syncEntity(
      @NonNull SyncEntityType entityType, @NonNull SyncDirection direction) {
    log.debug("Syncing entity: {}", entityType);

    // Generate operation ID for progress tracking
    String operationId = "sync-" + entityType.getKey() + "-" + System.currentTimeMillis();

    return syncEntity(entityType, operationId, direction);
  }

  /**
   * Syncs a specific entity based on its name (for backward compatibility).
   *
   * @param entityName The name of the entity to sync
   * @return SyncResult containing the outcome of the sync operation
   */
  @PreAuthorize("hasRole('ADMIN')")
  public NotionSyncService.SyncResult syncEntity(
      @NonNull String entityName, @NonNull SyncDirection direction) {
    log.debug("Syncing entity by name: {}", entityName);

    return SyncEntityType.fromKey(entityName)
        .map(entityType -> syncEntity(entityType, direction))
        .orElseGet(
            () -> {
              log.warn("Unknown entity type for sync: {}", entityName);
              return NotionSyncService.SyncResult.failure(entityName, "Unknown entity type");
            });
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
  @PreAuthorize("hasRole('ADMIN')")
  public List<NotionSyncService.SyncResult> triggerManualSync() {
    log.info("=== MANUAL NOTION SYNC TRIGGERED ===");

    // Clear sync session at the start of batch operation
    syncServiceDependencies.getSyncSessionManager().clearSyncSession();

    List<NotionSyncService.SyncResult> results = new ArrayList<>();

    try {
      for (SyncEntityType entity : getSyncEntities()) {
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
          results.add(NotionSyncService.SyncResult.failure(entity.getKey(), e.getMessage()));
        }
      }

      logSyncSummary(results);
      log.info("=== MANUAL NOTION SYNC COMPLETED ===");

    } finally {
      // Clean up sync session thread local
      syncServiceDependencies.getSyncSessionManager().cleanupSyncSession();
    }

    return results;
  }

  /**
   * Manual trigger for syncing a specific entity.
   *
   * @param entityName The name of the entity to sync
   * @return SyncResult for the specified entity
   */
  @PreAuthorize("hasRole('ADMIN')")
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
  @PreAuthorize("hasRole('ADMIN')")
  public String getSyncStatus() {
    StringBuilder status = new StringBuilder();
    status.append("Notion Sync Status:\n");
    status.append("- Sync Enabled: ").append(syncProperties.isEnabled()).append("\n");
    status.append("- Scheduler Enabled: ").append(syncProperties.isSchedulerEnabled()).append("\n");
    status
        .append("- Sync Interval: ")
        .append(syncProperties.getScheduler().getInterval())
        .append("ms\n");
    status
        .append("- Entities: ")
        .append(
            getSyncEntities().stream()
                .map(SyncEntityType::getKey)
                .collect(Collectors.joining(", ")))
        .append("\n");
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
  @PreAuthorize("hasRole('ADMIN')")
  public LocalDateTime getLastSyncTime(String entityName) {
    return syncProperties.getLastSyncTime(entityName);
  }
}
