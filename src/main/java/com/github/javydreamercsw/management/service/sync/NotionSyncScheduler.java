package com.github.javydreamercsw.management.service.sync;

import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.service.sync.NotionSyncService.SyncResult;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
      List<SyncResult> results = new ArrayList<>();

      // Sync each configured entity
      for (String entity : syncProperties.getEntities()) {
        try {
          SyncResult result = syncEntity(entity);
          results.add(result);

          if (result.isSuccess()) {
            log.info("✅ {} sync completed: {} items synchronized", entity, result.getSyncedCount());
          } else {
            log.error("❌ {} sync failed: {}", entity, result.getErrorMessage());
          }

        } catch (Exception e) {
          log.error("❌ Unexpected error syncing {}: {}", entity, e.getMessage(), e);
          results.add(SyncResult.failure(entity, e.getMessage()));
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
   * Syncs a specific entity based on its name.
   *
   * @param entityName The name of the entity to sync
   * @return SyncResult containing the outcome of the sync operation
   */
  private SyncResult syncEntity(String entityName) {
    log.debug("Syncing entity: {}", entityName);

    // Generate operation ID for progress tracking
    String operationId = "sync-" + entityName.toLowerCase() + "-" + System.currentTimeMillis();

    switch (entityName.toLowerCase()) {
      case "shows":
        return notionSyncService.syncShows(operationId);

      case "wrestlers":
        return notionSyncService.syncWrestlers(operationId);

      case "factions":
        return notionSyncService.syncFactions(operationId);

      case "teams":
        // TODO: Implement team sync (NotionHandler.loadAllTeams() is ready)
        log.warn("Team sync not yet implemented");
        return SyncResult.success("Teams", 0, 0);

      case "matches":
        // TODO: Implement match sync
        log.warn("Match sync not yet implemented");
        return SyncResult.success("Matches", 0, 0);

      case "templates":
        return notionSyncService.syncShowTemplates(operationId);

      default:
        log.warn("Unknown entity type for sync: {}", entityName);
        return SyncResult.failure(entityName, "Unknown entity type");
    }
  }

  /**
   * Logs a summary of all sync operations performed.
   *
   * @param results List of sync results to summarize
   */
  private void logSyncSummary(List<SyncResult> results) {
    int successCount = 0;
    int failureCount = 0;
    int totalSynced = 0;

    for (SyncResult result : results) {
      if (result.isSuccess()) {
        successCount++;
        totalSynced += result.getSyncedCount();
      } else {
        failureCount++;
      }
    }

    log.info("=== SYNC SUMMARY ===");
    log.info("Total entities processed: {}", results.size());
    log.info("Successful syncs: {}", successCount);
    log.info("Failed syncs: {}", failureCount);
    log.info("Total items synchronized: {}", totalSynced);

    if (failureCount > 0) {
      log.warn("Some sync operations failed. Check logs above for details.");
    }
  }

  /**
   * Manual trigger for immediate sync of all entities. This method can be called programmatically
   * to force a sync operation outside of the regular schedule.
   *
   * @return List of sync results for all entities
   */
  public List<SyncResult> triggerManualSync() {
    log.info("=== MANUAL NOTION SYNC TRIGGERED ===");

    List<SyncResult> results = new ArrayList<>();

    for (String entity : syncProperties.getEntities()) {
      try {
        SyncResult result = syncEntity(entity);
        results.add(result);

        if (result.isSuccess()) {
          log.info(
              "✅ Manual {} sync completed: {} items synchronized", entity, result.getSyncedCount());
        } else {
          log.error("❌ Manual {} sync failed: {}", entity, result.getErrorMessage());
        }

      } catch (Exception e) {
        log.error("❌ Unexpected error during manual {} sync: {}", entity, e.getMessage(), e);
        results.add(SyncResult.failure(entity, e.getMessage()));
      }
    }

    logSyncSummary(results);
    log.info("=== MANUAL NOTION SYNC COMPLETED ===");

    return results;
  }

  /**
   * Manual trigger for syncing a specific entity.
   *
   * @param entityName The name of the entity to sync
   * @return SyncResult for the specified entity
   */
  public SyncResult triggerEntitySync(String entityName) {
    log.info("=== MANUAL {} SYNC TRIGGERED ===", entityName.toUpperCase());

    try {
      SyncResult result = syncEntity(entityName);

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
      return SyncResult.failure(entityName, e.getMessage());
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
    status
        .append("- Entities: ")
        .append(String.join(", ", syncProperties.getEntities()))
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
}
