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
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import com.github.javydreamercsw.management.service.sync.entity.notion.FactionRivalrySyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.NotionSyncServicesManager;
import com.github.javydreamercsw.management.service.sync.parallel.ParallelSyncOrchestrator;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Main service for orchestrating Notion synchronization. Coordinates individual entity sync
 * services.
 */
@Service
@Slf4j
public class NotionSyncService extends BaseSyncService {

  private final NotionSyncServicesManager servicesManager;
  private final ParallelSyncOrchestrator parallelSyncOrchestrator;

  @Autowired
  public NotionSyncService(
      final ObjectMapper objectMapper,
      final SyncServiceDependencies syncServiceDependencies,
      final NotionSyncServicesManager servicesManager,
      final NotionApiExecutor notionApiExecutor,
      final ParallelSyncOrchestrator parallelSyncOrchestrator) {
    super(objectMapper, syncServiceDependencies, notionApiExecutor);
    this.servicesManager = servicesManager;
    this.parallelSyncOrchestrator = parallelSyncOrchestrator;
  }

  /** Orchestrates a full synchronization of all enabled entities from Notion. */
  @Async
  public void fullSync() {
    String operationId = UUID.randomUUID().toString();
    log.info("🚀 Starting full Notion synchronization. Operation ID: {}", operationId);

    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      log.warn("Full sync aborted: Notion token not available.");
      return;
    }

    try {
      // Phase 1: Reference Data (Sequential due to dependencies)
      syncShowTypes(operationId);
      syncInjuryTypes(operationId);

      // Phase 2: Core Entities (Can be parallelized)
      syncWrestlers(operationId);
      syncNpcs(operationId, SyncDirection.INBOUND);
      syncSeasons(operationId);

      // Phase 3: Dependent Entities
      syncFactions(operationId);
      syncTeams(operationId);
      syncTitles(operationId);
      syncInjuries(operationId);

      // Phase 4: Dynamic/Historical Data
      syncShows(operationId);
      syncSegments(operationId);
      syncRivalries(operationId);
      syncTitleReigns(operationId);

      log.info("✅ Full Notion synchronization completed successfully.");
    } catch (Exception e) {
      log.error("❌ Full Notion synchronization failed: {}", e.getMessage(), e);
    }
  }

  public SyncResult syncWrestlers(@NonNull final String operationId) {
    return servicesManager.getWrestlerSyncService().syncWrestlers(operationId);
  }

  public SyncResult syncFactions(@NonNull final String operationId) {
    return servicesManager.getFactionSyncService().syncFactions(operationId);
  }

  public SyncResult syncTeams(@NonNull final String operationId) {
    return servicesManager.getTeamSyncService().syncTeams(operationId);
  }

  public SyncResult syncTitles(@NonNull final String operationId) {
    return servicesManager.getTitleSyncService().syncTitles(operationId);
  }

  public SyncResult syncTitleReigns(@NonNull final String operationId) {
    return servicesManager.getTitleReignSyncService().syncTitleReigns(operationId);
  }

  public SyncResult syncShowTypes(@NonNull final String operationId) {
    return servicesManager.getShowTypeSyncService().syncShowTypes(operationId);
  }

  public SyncResult syncSeasons(@NonNull final String operationId) {
    return servicesManager.getSeasonSyncService().syncSeasons(operationId);
  }

  public SyncResult syncShows(@NonNull final String operationId) {
    return servicesManager.getShowSyncService().syncShows(operationId);
  }

  public SyncResult syncSegments(@NonNull final String operationId) {
    return servicesManager.getSegmentSyncService().syncSegments(operationId);
  }

  public SyncResult syncInjuryTypes(@NonNull final String operationId) {
    return servicesManager.getInjuryTypeSyncService().syncInjuryTypes(operationId);
  }

  public SyncResult syncInjuries(@NonNull final String operationId) {
    return servicesManager.getInjurySyncService().syncInjuries(operationId);
  }

  public SyncResult syncNpcs(
      @NonNull final String operationId, @NonNull final SyncDirection direction) {
    return servicesManager.getNpcSyncService().syncNpcs(operationId, direction);
  }

  public SyncResult syncRivalries(@NonNull final String operationId) {
    return servicesManager.getRivalrySyncService().syncRivalries(operationId);
  }

  public SyncResult syncFactionRivalries(
      @NonNull final String operationId, @NonNull final SyncDirection direction) {
    FactionRivalrySyncService service = servicesManager.getFactionRivalrySyncService();
    if (direction == SyncDirection.INBOUND) {
      return service.syncFactionRivalries(operationId);
    } else {
      return servicesManager.getFactionRivalryNotionSyncService().syncToNotion(operationId);
    }
  }

  public SyncResult syncShowTemplates(
      @NonNull final String operationId, @NonNull final SyncDirection direction) {
    if (direction == SyncDirection.INBOUND) {
      return servicesManager.getShowTemplateSyncService().syncShowTemplates(operationId);
    } else {
      return servicesManager.getShowTemplateNotionSyncService().syncToNotion(operationId);
    }
  }
}
