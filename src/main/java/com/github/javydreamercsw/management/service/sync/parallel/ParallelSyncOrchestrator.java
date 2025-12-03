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
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import com.github.javydreamercsw.management.service.sync.entity.notion.FactionSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.InjurySyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.NpcSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.SeasonSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.SegmentSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.ShowSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.ShowTemplateSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.ShowTypeSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.TeamSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.TitleReignSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.TitleSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.WrestlerSyncService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Orchestrator for parallel entity synchronization providing improved performance through
 * concurrent processing of multiple entity types.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ParallelSyncOrchestrator {

  @Autowired private ShowSyncService showSyncService;
  @Autowired private WrestlerSyncService wrestlerSyncService;
  @Autowired private FactionSyncService factionSyncService;
  @Autowired private TeamSyncService teamSyncService;
  @Autowired private SegmentSyncService segmentSyncService;
  @Autowired private SeasonSyncService seasonSyncService;
  @Autowired private ShowTypeSyncService showTypeSyncService;
  @Autowired private ShowTemplateSyncService showTemplateSyncService;
  @Autowired private InjurySyncService injurySyncService;
  @Autowired private NpcSyncService npcSyncService;
  @Autowired private TitleSyncService titleSyncService;
  @Autowired private TitleReignSyncService titleReignSyncService;
  @Autowired private EntitySyncConfiguration entityConfig;

  /**
   * Executes parallel synchronization of all enabled entities.
   *
   * @return ParallelSyncResult containing results for all entity syncs
   */
  public ParallelSyncResult executeParallelSync() {
    return executeParallelSync(null);
  }

  /**
   * Executes parallel synchronization of all enabled entities with operation tracking.
   *
   * @param baseOperationId Base operation ID for tracking (will generate sub-operations)
   * @return ParallelSyncResult containing results for all entity syncs
   */
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
    if (entityConfig.isEntityEnabled("shows")) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      "shows", () -> showSyncService.syncShows(baseOperationId + "-shows"))));
    }

    if (entityConfig.isEntityEnabled("wrestlers")) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      "wrestlers",
                      () -> wrestlerSyncService.syncWrestlers(baseOperationId + "-wrestlers"))));
    }

    if (entityConfig.isEntityEnabled("factions")) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      "factions",
                      () -> factionSyncService.syncFactions(baseOperationId + "-factions"))));
    }

    if (entityConfig.isEntityEnabled("teams")) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      "teams", () -> teamSyncService.syncTeams(baseOperationId + "-teams"))));
    }

    if (entityConfig.isEntityEnabled("segments")) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      "segments",
                      () -> segmentSyncService.syncSegments(baseOperationId + "-segments"))));
    }

    if (entityConfig.isEntityEnabled("seasons")) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      "seasons",
                      () -> seasonSyncService.syncSeasons(baseOperationId + "-seasons"))));
    }

    if (entityConfig.isEntityEnabled("showtypes")) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      "showtypes",
                      () -> showTypeSyncService.syncShowTypes(baseOperationId + "-showtypes"))));
    }

    if (entityConfig.isEntityEnabled("showtemplates")) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      "showtemplates",
                      () ->
                          showTemplateSyncService.syncShowTemplates(
                              baseOperationId + "-showtemplates"))));
    }

    if (entityConfig.isEntityEnabled("injuries")) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      "injuries",
                      () -> injurySyncService.syncInjuryTypes(baseOperationId + "-injuries"))));
    }

    if (entityConfig.isEntityEnabled("npcs")) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      "npcs",
                      () ->
                          npcSyncService.syncNpcs(
                              baseOperationId + "-npcs",
                              com.github.javydreamercsw.management.service.sync.base.SyncDirection
                                  .INBOUND))));
    }

    if (entityConfig.isEntityEnabled("titles")) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      "titles", () -> titleSyncService.syncTitles(baseOperationId + "-titles"))));
    }

    if (entityConfig.isEntityEnabled("titlereigns")) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      "titlereigns",
                      () ->
                          titleReignSyncService.syncTitleReigns(
                              baseOperationId + "-titlereigns"))));
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
    String[] entities = {
      "seasons",
      "showtypes",
      "showtemplates",
      "shows",
      "wrestlers",
      "factions",
      "teams",
      "injuries",
      "npcs",
      "segments",
      "titles",
      "titlereigns"
    };

    for (String entity : entities) {
      if (entityConfig.isEntityEnabled(entity)) {
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
