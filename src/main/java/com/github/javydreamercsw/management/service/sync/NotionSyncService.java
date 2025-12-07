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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionApiExecutor;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import com.github.javydreamercsw.management.service.sync.entity.notion.NotionSyncServicesManager;
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

  private final NotionSyncServicesManager notionSyncServicesManager;
  private final ParallelSyncOrchestrator parallelSyncOrchestrator;
  private final NotionApiExecutor notionApiExecutor;

  /** Constructor for NotionSyncService. */
  @Autowired // Add @Autowired for constructor injection
  public NotionSyncService(
      ObjectMapper objectMapper,
      SyncServiceDependencies syncServiceDependencies,
      NotionSyncServicesManager notionSyncServicesManager,
      NotionApiExecutor notionApiExecutor,
      ParallelSyncOrchestrator parallelSyncOrchestrator) { // Add SyncServiceDependencies here
    super(objectMapper, syncServiceDependencies); // Pass it to super, remove syncProperties
    this.notionSyncServicesManager = notionSyncServicesManager;
    this.notionApiExecutor = notionApiExecutor;
    this.parallelSyncOrchestrator = parallelSyncOrchestrator;
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
    return notionSyncServicesManager.getShowSyncService().syncShow(showId);
  }

  /**
   * Gets all show IDs from the Notion database.
   *
   * @return List of all show IDs.
   */
  public java.util.List<String> getAllShowIds() {
    return notionSyncServicesManager.getShowSyncService().getShowIds();
  }

  /**
   * Synchronizes a single segment from Notion by its ID.
   *
   * @param segmentId The Notion ID of the segment to sync.
   * @return SyncResult containing the outcome of the sync operation.
   */
  public SyncResult syncSegment(@NonNull String segmentId) {
    return notionSyncServicesManager.getSegmentSyncService().syncSegment(segmentId);
  }

  /**
   * Gets all segment IDs from the Notion database.
   *
   * @return List of all segment IDs.
   */
  public java.util.List<String> getAllSegmentIds() {
    return notionSyncServicesManager.getSegmentSyncService().getSegmentIds();
  }

  /**
   * Synchronizes shows from Notion Shows database directly to the database.
   *
   * @return SyncResult containing the outcome of the sync operation
   */
  public SyncResult syncShows(@NonNull String operationId, @NonNull SyncDirection direction) {
    return direction.equals(SyncDirection.INBOUND)
        ? notionSyncServicesManager.getShowSyncService().syncShows(operationId)
        : notionSyncServicesManager.getShowNotionSyncService().syncToNotion(operationId);
  }

  /**
   * Synchronizes wrestlers from Notion to the local JSON file and database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncWrestlers(@NonNull String operationId, @NonNull SyncDirection direction) {
    return direction.equals(SyncDirection.INBOUND)
        ? notionSyncServicesManager.getWrestlerSyncService().syncWrestlers(operationId)
        : notionSyncServicesManager.getWrestlerNotionSyncService().syncToNotion(operationId);
  }

  /**
   * Synchronizes factions from Notion to the database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncFactions(@NonNull String operationId, @NonNull SyncDirection direction) {
    return direction.equals(SyncDirection.INBOUND)
        ? notionSyncServicesManager.getFactionSyncService().syncFactions(operationId)
        : notionSyncServicesManager.getFactionNotionSyncService().syncToNotion(operationId);
  }

  /**
   * Synchronizes teams from Notion to the local database.
   *
   * @return SyncResult containing the operation status and details
   */
  public SyncResult syncTeams(@NonNull String operationId, @NonNull SyncDirection direction) {
    return direction.equals(SyncDirection.INBOUND)
        ? notionSyncServicesManager.getTeamSyncService().syncTeams(operationId)
        : notionSyncServicesManager.getTeamNotionSyncService().syncToNotion(operationId);
  }

  /**
   * Synchronizes show template data between the application and Notion based on the specified
   * direction.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncShowTemplates(
      @NonNull String operationId, @NonNull SyncDirection direction) {
    return direction.equals(SyncDirection.INBOUND)
        ? notionSyncServicesManager.getShowTemplateSyncService().syncShowTemplates(operationId)
        : notionSyncServicesManager
            .getShowTemplateNotionSyncService()
            .syncToNotion(operationId);
  }

  /**
   * Synchronizes seasons from Notion to the database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncSeasons(@NonNull String operationId, @NonNull SyncDirection direction) {
    return direction.equals(SyncDirection.INBOUND)
        ? notionSyncServicesManager.getSeasonSyncService().syncSeasons(operationId)
        : notionSyncServicesManager.getSeasonNotionSyncService().syncToNotion(operationId);
  }

  /**
   * Synchronizes show types from Notion or creates default show types.
   *
   * @param operationId Operation ID for progress tracking
   * @param direction The direction of the synchronization (inbound or outbound)
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncShowTypes(@NonNull String operationId, @NonNull SyncDirection direction) {
    return direction.equals(SyncDirection.INBOUND)
        ? notionSyncServicesManager.getShowTypeSyncService().syncShowTypes(operationId)
        : notionSyncServicesManager.getShowTypeNotionSyncService().syncToNotion(operationId);
  }

  /**
   * Synchronizes injury types from Notion Injuries database to the local database.
   *
   * @param operationId Operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncInjuryTypes(@NonNull String operationId, @NonNull SyncDirection direction) {
    return direction.equals(SyncDirection.INBOUND)
        ? notionSyncServicesManager.getInjurySyncService().syncInjuryTypes(operationId)
        : notionSyncServicesManager.getInjuryNotionSyncService().syncToNotion(operationId);
  }

  /**
   * Synchronizes NPCs from Notion to the local database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncNpcs(@NonNull String operationId, @NonNull SyncDirection direction) {
    return direction.equals(SyncDirection.INBOUND)
        ? notionSyncServicesManager.getNpcSyncService().syncNpcs(operationId, direction)
        : notionSyncServicesManager.getNpcNotionSyncService().syncToNotion(operationId);
  }

  /**
   * Synchronizes titles from Notion to the local database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncTitles(@NonNull String operationId, @NonNull SyncDirection direction) {
    return direction.equals(SyncDirection.INBOUND)
        ? notionSyncServicesManager.getTitleSyncService().syncTitles(operationId)
        : notionSyncServicesManager.getTitleNotionSyncService().syncToNotion(operationId);
  }

  /**
   * Synchronizes title reigns from Notion to the local database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncTitleReigns(@NonNull String operationId) {
    return notionSyncServicesManager.getTitleReignSyncService().syncTitleReigns(operationId);
  }

  /**
   * Synchronizes rivalries from Notion to the database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncRivalries(@NonNull String operationId, @NonNull SyncDirection direction) {
    return direction.equals(SyncDirection.INBOUND)
        ? notionSyncServicesManager.getRivalrySyncService().syncRivalries(operationId)
        : notionSyncServicesManager.getRivalryNotionSyncService().syncToNotion(operationId);
  }

  /**
   * Synchronizes faction rivalries from Notion to the database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncFactionRivalries(
      @NonNull String operationId, @NonNull SyncDirection direction) {
    return direction.equals(SyncDirection.INBOUND)
        ? notionSyncServicesManager
            .getFactionRivalrySyncService()
            .syncFactionRivalries(operationId)
        : notionSyncServicesManager
            .getFactionRivalryNotionSyncService()
            .syncToNotion(operationId);
  }

  /**
   * Synchronizes segment data between the application and Notion based on the specified direction.
   *
   * @param operationId Optional operation ID for progress tracking
   * @param direction The direction of the synchronization (inbound or outbound)
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncSegments(@NonNull String operationId, @NonNull SyncDirection direction) {
    return direction.equals(SyncDirection.INBOUND)
        ? notionSyncServicesManager
            .getSegmentSyncService()
            .syncSegments(operationId + "-segments")
        : notionSyncServicesManager.getSegmentNotionSyncService().syncToNotion(operationId);
  }
}
