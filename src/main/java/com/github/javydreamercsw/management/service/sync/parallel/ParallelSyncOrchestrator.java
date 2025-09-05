package com.github.javydreamercsw.management.service.sync.parallel;

import com.github.javydreamercsw.management.config.EntitySyncConfiguration;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import com.github.javydreamercsw.management.service.sync.entity.*;
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
  @Autowired private MatchSyncService matchSyncService;
  @Autowired private SeasonSyncService seasonSyncService;
  @Autowired private ShowTypeSyncService showTypeSyncService;
  @Autowired private ShowTemplateSyncService showTemplateSyncService;
  @Autowired private InjurySyncService injurySyncService;
  @Autowired private NpcSyncService npcSyncService;
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

      return new ParallelSyncResult(results, totalTime, true, null);

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
                      "shows",
                      baseOperationId,
                      () -> showSyncService.syncShows(baseOperationId + "-shows"))));
    }

    if (entityConfig.isEntityEnabled("wrestlers")) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      "wrestlers",
                      baseOperationId,
                      () -> wrestlerSyncService.syncWrestlers(baseOperationId + "-wrestlers"))));
    }

    if (entityConfig.isEntityEnabled("factions")) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      "factions",
                      baseOperationId,
                      () -> factionSyncService.syncFactions(baseOperationId + "-factions"))));
    }

    if (entityConfig.isEntityEnabled("teams")) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      "teams",
                      baseOperationId,
                      () -> teamSyncService.syncTeams(baseOperationId + "-teams"))));
    }

    if (entityConfig.isEntityEnabled("matches")) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      "matches",
                      baseOperationId,
                      () -> matchSyncService.syncMatches(baseOperationId + "-matches"))));
    }

    if (entityConfig.isEntityEnabled("seasons")) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      "seasons",
                      baseOperationId,
                      () -> seasonSyncService.syncSeasons(baseOperationId + "-seasons"))));
    }

    if (entityConfig.isEntityEnabled("showtypes")) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      "showtypes",
                      baseOperationId,
                      () -> showTypeSyncService.syncShowTypes(baseOperationId + "-showtypes"))));
    }

    if (entityConfig.isEntityEnabled("showtemplates")) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      "showtemplates",
                      baseOperationId,
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
                      baseOperationId,
                      () -> injurySyncService.syncInjuryTypes(baseOperationId + "-injuries"))));
    }

    if (entityConfig.isEntityEnabled("npcs")) {
      futures.add(
          executor.submit(
              () ->
                  syncEntity(
                      "npcs",
                      baseOperationId,
                      () -> npcSyncService.syncNpcs(baseOperationId + "-npcs"))));
    }

    return futures;
  }

  /** Wraps entity sync execution with error handling and timing. */
  private EntitySyncResult syncEntity(
      String entityType, String operationId, Callable<SyncResult> syncTask) {
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
      "shows",
      "wrestlers",
      "factions",
      "teams",
      "matches",
      "seasons",
      "showtypes",
      "showtemplates",
      "injuries",
      "npcs"
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
      return (int) entityResults.stream().filter(r -> r.getSyncResult().isSuccess()).count();
    }

    public int getFailedSyncs() {
      return (int) entityResults.stream().filter(r -> !r.getSyncResult().isSuccess()).count();
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
