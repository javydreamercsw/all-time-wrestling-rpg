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
package com.github.javydreamercsw.management.service.sync.parallel;

import com.github.javydreamercsw.management.config.EntitySyncConfiguration;
import com.github.javydreamercsw.management.service.sync.SyncEntityType;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import com.github.javydreamercsw.management.service.sync.entity.notion.NotionSyncServicesManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Orchestrator for parallel entity synchronization providing improved performance through
 * concurrent processing of multiple entity types.
 */
@Service
@Slf4j
public class ParallelSyncOrchestrator {

  private final NotionSyncServicesManager notionSyncServicesManager;
  private final EntitySyncConfiguration entityConfig;

  @Autowired
  public ParallelSyncOrchestrator(
      NotionSyncServicesManager notionSyncServicesManager, EntitySyncConfiguration entityConfig) {
    this.notionSyncServicesManager = notionSyncServicesManager;
    this.entityConfig = entityConfig;
  }

  /**
   * Executes parallel synchronization of all enabled entities.
   *
   * @return ParallelSyncResult containing results for all entity syncs
   */
  @org.springframework.cache.annotation.CacheEvict(
      value = {
        com.github.javydreamercsw.management.config.CacheConfig.NOTION_SYNC_CACHE,
        com.github.javydreamercsw.management.config.CacheConfig.NOTION_PAGES_CACHE,
        com.github.javydreamercsw.management.config.CacheConfig.NOTION_QUERIES_CACHE
      },
      allEntries = true)
  public ParallelSyncResult executeParallelSync() {
    return executeParallelSync(null);
  }

  /**
   * Executes parallel synchronization of all enabled entities with operation tracking.
   *
   * @param baseOperationId Base operation ID for tracking (will generate sub-operations)
   * @return ParallelSyncResult containing results for all entity syncs
   */
  @org.springframework.cache.annotation.CacheEvict(
      value = {
        com.github.javydreamercsw.management.config.CacheConfig.NOTION_SYNC_CACHE,
        com.github.javydreamercsw.management.config.CacheConfig.NOTION_PAGES_CACHE,
        com.github.javydreamercsw.management.config.CacheConfig.NOTION_QUERIES_CACHE
      },
      allEntries = true)
  public ParallelSyncResult executeParallelSync(String baseOperationId) {
    String operationId = baseOperationId != null ? baseOperationId : UUID.randomUUID().toString();
    log.info("🚀 Starting parallel entity synchronization with operation ID: {}", operationId);

    long startTime = System.currentTimeMillis();

    // Determine optimal thread pool size based on enabled entities
    int enabledEntities = countEnabledEntities();
    int maxThreads =
        Math.max(1, Math.min(enabledEntities, entityConfig.getDefaults().getMaxThreads()));

    ExecutorService executor = Executors.newFixedThreadPool(maxThreads);

    try {
      // Submit sync tasks for each enabled entity type
      List<Future<EntitySyncResult>> futures =
          new ArrayList<>(submitSyncTasks(executor, operationId));

      // Wait for all tasks to complete and collect results
      List<EntitySyncResult> results = collectResults(futures);

      long totalTime = System.currentTimeMillis() - startTime;
      log.info("✅ Parallel sync completed in {}ms", totalTime);

      boolean allSuccess =
          results.stream()
              .allMatch(r -> r.getSyncResult() != null && r.getSyncResult().isSuccess());

      return new ParallelSyncResult(
          results,
          totalTime,
          allSuccess,
          allSuccess ? null : "One or more entities failed to sync.");
    } catch (Exception e) {
      log.error("❌ Parallel sync failed", e);
      return new ParallelSyncResult(
          new ArrayList<>(), System.currentTimeMillis() - startTime, false, e.getMessage());
    } finally {
      shutdownExecutor(executor);
    }
  }

  /** Submits sync tasks for all enabled entity types. */
  private List<Future<EntitySyncResult>> submitSyncTasks(
      ExecutorService executor, String baseOperationId) {
    List<Future<EntitySyncResult>> futures = new ArrayList<>();

    // Submit tasks for each entity type if enabled
    if (entityConfig.isEntityEnabled(SyncEntityType.SHOWS.getKey())) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      SyncEntityType.SHOWS.getKey(),
                      () ->
                          notionSyncServicesManager
                              .getShowSyncService()
                              .syncShows(baseOperationId + "-shows"))));
    }

    if (entityConfig.isEntityEnabled(SyncEntityType.WRESTLERS.getKey())) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      SyncEntityType.WRESTLERS.getKey(),
                      () ->
                          notionSyncServicesManager
                              .getWrestlerSyncService()
                              .syncWrestlers(baseOperationId + "-wrestlers"))));
    }

    if (entityConfig.isEntityEnabled(SyncEntityType.FACTIONS.getKey())) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      SyncEntityType.FACTIONS.getKey(),
                      () ->
                          notionSyncServicesManager
                              .getFactionSyncService()
                              .syncFactions(baseOperationId + "-factions"))));
    }

    if (entityConfig.isEntityEnabled(SyncEntityType.TEAMS.getKey())) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      SyncEntityType.TEAMS.getKey(),
                      () ->
                          notionSyncServicesManager
                              .getTeamSyncService()
                              .syncTeams(baseOperationId + "-teams"))));
    }

    if (entityConfig.isEntityEnabled(SyncEntityType.SEGMENTS.getKey())) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      SyncEntityType.SEGMENTS.getKey(),
                      () ->
                          notionSyncServicesManager
                              .getSegmentSyncService()
                              .syncSegments(baseOperationId + "-segments"))));
    }

    if (entityConfig.isEntityEnabled(SyncEntityType.SEASONS.getKey())) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      SyncEntityType.SEASONS.getKey(),
                      () ->
                          notionSyncServicesManager
                              .getSeasonSyncService()
                              .syncSeasons(baseOperationId + "-seasons"))));
    }

    if (entityConfig.isEntityEnabled(SyncEntityType.SHOW_TYPES.getKey())) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      SyncEntityType.SHOW_TYPES.getKey(),
                      () ->
                          notionSyncServicesManager
                              .getShowTypeSyncService()
                              .syncShowTypes(baseOperationId + "-showtypes"))));
    }

    if (entityConfig.isEntityEnabled(SyncEntityType.TEMPLATES.getKey())) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      SyncEntityType.TEMPLATES.getKey(),
                      () ->
                          notionSyncServicesManager
                              .getShowTemplateSyncService()
                              .syncShowTemplates(baseOperationId + "-showtemplates"))));
    }

    if (entityConfig.isEntityEnabled(SyncEntityType.INJURY_TYPES.getKey())) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      SyncEntityType.INJURY_TYPES.getKey(),
                      () ->
                          notionSyncServicesManager
                              .getInjuryTypeSyncService()
                              .syncInjuryTypes(baseOperationId + "-injuries"))));
    }

    if (entityConfig.isEntityEnabled(SyncEntityType.NPCS.getKey())) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      SyncEntityType.NPCS.getKey(),
                      () ->
                          notionSyncServicesManager
                              .getNpcSyncService()
                              .syncNpcs(
                                  baseOperationId + "-npcs",
                                  com.github.javydreamercsw.management.service.sync.base
                                      .SyncDirection.INBOUND))));
    }

    if (entityConfig.isEntityEnabled(SyncEntityType.TITLES.getKey())) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      SyncEntityType.TITLES.getKey(),
                      () ->
                          notionSyncServicesManager
                              .getTitleSyncService()
                              .syncTitles(baseOperationId + "-titles"))));
    }

    if (entityConfig.isEntityEnabled(SyncEntityType.TITLE_REIGN.getKey())) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      SyncEntityType.TITLE_REIGN.getKey(),
                      () ->
                          notionSyncServicesManager
                              .getTitleReignSyncService()
                              .syncTitleReigns(baseOperationId + "-titlereigns"))));
    }

    return futures;
  }

  /** Wraps entity sync execution with error handling and timing. */
  private EntitySyncResult syncEntity(String entityType, Callable<SyncResult> syncTask) {
    long startTime = System.currentTimeMillis();
    try {
      log.debug("Starting sync for entity: {}", entityType);
      SyncResult result = syncTask.call();
      long duration = System.currentTimeMillis() - startTime;

      return new EntitySyncResult(entityType, result, duration, null);
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error("Failed to sync entity: {}", entityType, e);

      return new EntitySyncResult(
          entityType, SyncResult.failure(entityType, e.getMessage()), duration, e.getMessage());
    }
  }

  /** Collects results from all futures with timeout handling. */
  private List<EntitySyncResult> collectResults(List<Future<EntitySyncResult>> futures) {
    List<EntitySyncResult> results = new ArrayList<>();

    for (Future<EntitySyncResult> future : futures) {
      try {
        // Use timeout from default configuration
        int timeoutSeconds = entityConfig.getDefaults().getTimeoutSeconds();
        EntitySyncResult result = future.get(timeoutSeconds, TimeUnit.SECONDS);
        results.add(result);
      } catch (TimeoutException e) {
        log.warn("Entity sync timed out");
        results.add(
            new EntitySyncResult(
                "unknown", SyncResult.failure("unknown", "Timeout"), 0, "Timeout"));
      } catch (Exception e) {
        log.error("Error collecting sync result", e);
        results.add(
            new EntitySyncResult(
                "unknown", SyncResult.failure("unknown", e.getMessage()), 0, e.getMessage()));
      }
    }

    return results;
  }

  /** Counts the number of enabled entities for thread pool sizing. */
  private int countEnabledEntities() {
    int count = 0;
    for (SyncEntityType entity : SyncEntityType.values()) {
      if (entityConfig.isEntityEnabled(entity.getKey())) {
        count++;
      }
    }
    return count;
  }

  /** Safely shuts down the executor service. */
  private void shutdownExecutor(ExecutorService executor) {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
        log.warn("Executor did not terminate gracefully, forcing shutdown");
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      log.warn("Interrupted while waiting for executor termination");
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /** Result of a parallel sync operation containing results for all entities. */
  @Getter
  public static class ParallelSyncResult {
    // Getters
    private final List<EntitySyncResult> entityResults;
    private final long totalDurationMs;
    private final boolean success;
    private final String errorMessage;

    public ParallelSyncResult(
        List<EntitySyncResult> entityResults,
        long totalDurationMs,
        boolean success,
        String errorMessage) {
      this.entityResults = entityResults;
      this.totalDurationMs = totalDurationMs;
      this.success = success;
      this.errorMessage = errorMessage;
    }

    public int getSuccessfulSyncs() {
      return (int)
          entityResults.stream()
              .filter(r -> r.getSyncResult() != null && r.getSyncResult().isSuccess())
              .count();
    }

    public int getFailedSyncs() {
      return (int)
          entityResults.stream()
              .filter(r -> r.getSyncResult() != null && !r.getSyncResult().isSuccess())
              .count();
    }
  }

  /** Result of syncing a specific entity type. */
  @Getter
  public static class EntitySyncResult {
    // Getters
    private final String entityType;
    private final SyncResult syncResult;
    private final long durationMs;
    private final String errorMessage;

    public EntitySyncResult(
        String entityType, SyncResult syncResult, long durationMs, String errorMessage) {
      this.entityType = entityType;
      this.syncResult = syncResult;
      this.durationMs = durationMs;
      this.errorMessage = errorMessage;
    }
  }
}
