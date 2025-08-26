package com.github.javydreamercsw.management.service.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.entity.FactionSyncService;
import com.github.javydreamercsw.management.service.sync.entity.MatchSyncService;
import com.github.javydreamercsw.management.service.sync.entity.SeasonSyncService;
import com.github.javydreamercsw.management.service.sync.entity.ShowSyncService;
import com.github.javydreamercsw.management.service.sync.entity.ShowTemplateSyncService;
import com.github.javydreamercsw.management.service.sync.entity.ShowTypeSyncService;
import com.github.javydreamercsw.management.service.sync.entity.TeamSyncService;
import com.github.javydreamercsw.management.service.sync.entity.WrestlerSyncService;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Main service responsible for orchestrating synchronization operations between Notion databases
 * and the local application. This service delegates to entity-specific sync services to handle the
 * actual synchronization logic for each entity type.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "notion.sync.enabled", havingValue = "true", matchIfMissing = false)
public class NotionSyncService extends BaseSyncService {

  // Entity-specific sync services
  @Autowired private ShowSyncService showSyncService;
  @Autowired private WrestlerSyncService wrestlerSyncService;
  @Autowired private FactionSyncService factionSyncService;
  @Autowired private TeamSyncService teamSyncService;
  @Autowired private MatchSyncService matchSyncService;
  @Autowired private SeasonSyncService seasonSyncService;
  @Autowired private ShowTypeSyncService showTypeSyncService;
  @Autowired private ShowTemplateSyncService showTemplateSyncService;

  // Thread pool for async processing
  private final ExecutorService executorService = Executors.newFixedThreadPool(10);

  /** Constructor for NotionSyncService. */
  public NotionSyncService(ObjectMapper objectMapper, NotionSyncProperties syncProperties) {
    super(objectMapper, syncProperties);
  }

  /**
   * Synchronizes shows from Notion Shows database directly to the database.
   *
   * @return SyncResult containing the outcome of the sync operation
   */
  public SyncResult syncShows() {
    return showSyncService.syncShows();
  }

  /**
   * Synchronizes shows from Notion Shows database directly to the database with optional progress
   * tracking.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult containing the outcome of the sync operation
   */
  public SyncResult syncShows(@NonNull String operationId) {
    return showSyncService.syncShows(operationId);
  }

  /**
   * Synchronizes wrestlers from Notion to the local JSON file and database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncWrestlers(@NonNull String operationId) {
    return wrestlerSyncService.syncWrestlers(operationId);
  }

  /**
   * Synchronizes factions from Notion to the database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncFactions(@NonNull String operationId) {
    return factionSyncService.syncFactions(operationId);
  }

  /**
   * Synchronizes teams from Notion to the local database.
   *
   * @return SyncResult containing the operation status and details
   */
  public SyncResult syncTeams() {
    return teamSyncService.syncTeams();
  }

  /**
   * Synchronizes matches from Notion Matches database directly to the database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult containing the outcome of the sync operation
   */
  public SyncResult syncMatches(@NonNull String operationId) {
    return matchSyncService.syncMatches(operationId);
  }

  /**
   * Synchronizes show templates from Notion to the local JSON file and database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncShowTemplates(@NonNull String operationId) {
    return showTemplateSyncService.syncShowTemplates(operationId);
  }

  /**
   * Synchronizes seasons from Notion to the database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncSeasons(@NonNull String operationId) {
    return seasonSyncService.syncSeasons(operationId);
  }

  /**
   * Synchronizes show types from Notion or creates default show types.
   *
   * @param operationId Operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncShowTypes(@NonNull String operationId) {
    return showTypeSyncService.syncShowTypes(operationId);
  }

  /**
   * Synchronizes injury types from Notion Injuries database to the local database. TODO: Create
   * InjurySyncService and move implementation there
   *
   * @param operationId Operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncInjuryTypes(@NonNull String operationId) {
    // TODO: Move to InjurySyncService
    log.info("🏥 Injury types sync - to be moved to dedicated service");
    return SyncResult.success("Injuries", 0, 0);
  }

  /** Cleanup method to shutdown the executor service. */
  @PreDestroy
  public void shutdown() {
    log.info("Shutting down NotionSyncService executor...");
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
        log.warn("Executor did not terminate gracefully, forcing shutdown");
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      log.warn("Interrupted while waiting for executor termination");
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
