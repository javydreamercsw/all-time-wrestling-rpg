package com.github.javydreamercsw.management.service.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.config.EntitySyncConfiguration;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.FactionRivalrySyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.FactionSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.InjurySyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.NpcSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.RivalrySyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.SeasonSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.SegmentSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.ShowSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.ShowTemplateSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.ShowTypeSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.TeamSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.TitleReignSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.TitleSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.WrestlerSyncService;
import com.github.javydreamercsw.management.service.sync.parallel.ParallelSyncOrchestrator;
import com.github.javydreamercsw.management.service.sync.parallel.ParallelSyncOrchestrator.ParallelSyncResult;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Main service responsible for orchestrating synchronization operations between Notion databases
 * and the local application. This service now supports both individual entity sync and
 * high-performance parallel sync operations with fine-grained configuration control.
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
  @Autowired private SegmentSyncService segmentSyncService;
  @Autowired private SeasonSyncService seasonSyncService;
  @Autowired private ShowTypeSyncService showTypeSyncService;
  @Autowired private ShowTemplateSyncService showTemplateSyncService;
  @Autowired private InjurySyncService injurySyncService;
  @Autowired private NpcSyncService npcSyncService;
  @Autowired private TitleSyncService titleSyncService;
  @Autowired private TitleReignSyncService titleReignSyncService;
  @Autowired private RivalrySyncService rivalrySyncService;
  @Autowired private FactionRivalrySyncService factionRivalrySyncService;

  // New parallel sync capabilities
  @Autowired private ParallelSyncOrchestrator parallelSyncOrchestrator;
  @Autowired private EntitySyncConfiguration entitySyncConfiguration;

  /** Constructor for NotionSyncService. */
  public NotionSyncService(ObjectMapper objectMapper, NotionSyncProperties syncProperties) {
    super(objectMapper, syncProperties);
  }

  // ==================== PARALLEL SYNC OPERATIONS ====================

  /**
   * Executes high-performance parallel synchronization of all enabled entities. This is the
   * recommended method for full system synchronization as it provides better performance through
   * concurrent processing.
   *
   * @return ParallelSyncResult containing results for all entity syncs
   */
  public ParallelSyncResult syncAllEntitiesParallel() {
    log.info("🚀 Starting parallel synchronization of all enabled entities");
    return parallelSyncOrchestrator.executeParallelSync();
  }

  /**
   * Executes parallel synchronization with operation tracking for monitoring.
   *
   * @param operationId Operation ID for tracking and logging
   * @return ParallelSyncResult containing results for all entity syncs
   */
  public ParallelSyncResult syncAllEntitiesParallel(@NonNull String operationId) {
    log.info("🚀 Starting parallel synchronization with operation ID: {}", operationId);
    return parallelSyncOrchestrator.executeParallelSync(operationId);
  }

  // ==================== INDIVIDUAL ENTITY SYNC OPERATIONS ====================

  /**
   * Synchronizes a single show from Notion by its ID.
   *
   * @param showId The Notion ID of the show to sync.
   * @return SyncResult containing the outcome of the sync operation.
   */
  public SyncResult syncShow(@NonNull String showId) {
    return showSyncService.syncShow(showId);
  }

  /**
   * Gets all show IDs from the Notion database.
   *
   * @return List of all show IDs.
   */
  public java.util.List<String> getAllShowIds() {
    return showSyncService.getShowIds();
  }

  /**
   * Synchronizes a single segment from Notion by its ID.
   *
   * @param segmentId The Notion ID of the segment to sync.
   * @return SyncResult containing the outcome of the sync operation.
   */
  public SyncResult syncSegment(@NonNull String segmentId) {
    return segmentSyncService.syncSegment(segmentId);
  }

  /**
   * Gets all segment IDs from the Notion database.
   *
   * @return List of all segment IDs.
   */
  public java.util.List<String> getAllSegmentIds() {
    return segmentSyncService.getSegmentIds();
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
  public SyncResult syncTeams(@NonNull String operationId) {
    return teamSyncService.syncTeams(operationId);
  }

  /**
   * Synchronizes segments from Notion Segments database directly to the database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult containing the outcome of the sync operation
   */
  public SyncResult syncSegments(@NonNull String operationId) {
    return segmentSyncService.syncSegments(operationId + "-segments");
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
   * Synchronizes injury types from Notion Injuries database to the local database.
   *
   * @param operationId Operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncInjuryTypes(@NonNull String operationId) {
    return injurySyncService.syncInjuryTypes(operationId);
  }

  /**
   * Synchronizes NPCs from Notion to the local database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncNpcs(@NonNull String operationId) {
    return npcSyncService.syncNpcs(operationId);
  }

  /**
   * Synchronizes titles from Notion to the local database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncTitles(@NonNull String operationId) {
    return titleSyncService.syncTitles(operationId);
  }

  /**
   * Synchronizes title reigns from Notion to the local database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncTitleReigns(@NonNull String operationId) {
    return titleReignSyncService.syncTitleReigns(operationId);
  }

  /**
   * Synchronizes rivalries from Notion to the database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncRivalries(@NonNull String operationId) {
    return rivalrySyncService.syncRivalries(operationId);
  }

  /**
   * Synchronizes faction rivalries from Notion to the database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncFactionRivalries(@NonNull String operationId) {
    return factionRivalrySyncService.syncFactionRivalries(operationId);
  }
}
