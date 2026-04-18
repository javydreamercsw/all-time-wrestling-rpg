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
import com.github.javydreamercsw.management.service.sync.entity.notion.FactionSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.InjurySyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.InjuryTypeSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.NotionSyncServicesManager;
import com.github.javydreamercsw.management.service.sync.entity.notion.NpcSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.RivalrySyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.SeasonSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.SegmentSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.ShowSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.ShowTypeSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.TeamSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.TitleReignSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.TitleSyncService;
import com.github.javydreamercsw.management.service.sync.entity.notion.WrestlerSyncService;
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
      ObjectMapper objectMapper,
      SyncServiceDependencies syncServiceDependencies,
      NotionSyncServicesManager servicesManager,
      NotionApiExecutor notionApiExecutor,
      ParallelSyncOrchestrator parallelSyncOrchestrator) {
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
      syncShowTypes(operationId, SyncDirection.INBOUND);
      syncInjuryTypes(operationId, SyncDirection.INBOUND);

      // Phase 2: Core Entities (Can be parallelized)
      syncWrestlers(operationId, SyncDirection.INBOUND);
      syncNpcs(operationId, SyncDirection.INBOUND);
      syncSeasons(operationId, SyncDirection.INBOUND);

      // Phase 3: Dependent Entities
      syncFactions(operationId, SyncDirection.INBOUND);
      syncTeams(operationId, SyncDirection.INBOUND);
      syncTitles(operationId, SyncDirection.INBOUND);
      syncInjuries(operationId, SyncDirection.INBOUND);

      // Phase 4: Dynamic/Historical Data
      syncShows(operationId, SyncDirection.INBOUND);
      syncSegments(operationId, SyncDirection.INBOUND);
      syncRivalries(operationId, SyncDirection.INBOUND);
      syncTitleReigns(operationId);

      log.info("✅ Full Notion synchronization completed successfully.");
    } catch (Exception e) {
      log.error("❌ Full Notion synchronization failed: {}", e.getMessage(), e);
    }
  }

  public SyncResult syncWrestlers(@NonNull String operationId, @NonNull SyncDirection direction) {
    WrestlerSyncService service = servicesManager.getWrestlerSyncService();
    return service.syncWrestlers(operationId);
  }

  public SyncResult syncFactions(@NonNull String operationId, @NonNull SyncDirection direction) {
    FactionSyncService service = servicesManager.getFactionSyncService();
    return service.syncFactions(operationId);
  }

  public SyncResult syncTeams(@NonNull String operationId, @NonNull SyncDirection direction) {
    TeamSyncService service = servicesManager.getTeamSyncService();
    return service.syncTeams(operationId);
  }

  public SyncResult syncTitles(@NonNull String operationId, @NonNull SyncDirection direction) {
    TitleSyncService service = servicesManager.getTitleSyncService();
    return service.syncTitles(operationId);
  }

  public SyncResult syncTitleReigns(@NonNull String operationId) {
    TitleReignSyncService service = servicesManager.getTitleReignSyncService();
    return service.syncTitleReigns(operationId);
  }

  public SyncResult syncShowTypes(@NonNull String operationId, @NonNull SyncDirection direction) {
    ShowTypeSyncService service = servicesManager.getShowTypeSyncService();
    return service.syncShowTypes(operationId);
  }

  public SyncResult syncSeasons(@NonNull String operationId, @NonNull SyncDirection direction) {
    SeasonSyncService service = servicesManager.getSeasonSyncService();
    return service.syncSeasons(operationId);
  }

  public SyncResult syncShows(@NonNull String operationId, @NonNull SyncDirection direction) {
    ShowSyncService service = servicesManager.getShowSyncService();
    return service.syncShows(operationId);
  }

  public SyncResult syncSegments(@NonNull String operationId, @NonNull SyncDirection direction) {
    SegmentSyncService service = servicesManager.getSegmentSyncService();
    return service.syncSegments(operationId);
  }

  public SyncResult syncInjuryTypes(@NonNull String operationId, @NonNull SyncDirection direction) {
    InjuryTypeSyncService service = servicesManager.getInjuryTypeSyncService();
    return service.syncInjuryTypes(operationId);
  }

  public SyncResult syncInjuries(@NonNull String operationId, @NonNull SyncDirection direction) {
    InjurySyncService service = servicesManager.getInjurySyncService();
    return service.syncInjuries(operationId);
  }

  public SyncResult syncNpcs(@NonNull String operationId, @NonNull SyncDirection direction) {
    NpcSyncService service = servicesManager.getNpcSyncService();
    return service.syncNpcs(operationId, direction);
  }

  public SyncResult syncRivalries(@NonNull String operationId, @NonNull SyncDirection direction) {
    RivalrySyncService service = servicesManager.getRivalrySyncService();
    return service.syncRivalries(operationId);
  }

  public SyncResult syncFactionRivalries(
      @NonNull String operationId, @NonNull SyncDirection direction) {
    FactionRivalrySyncService service = servicesManager.getFactionRivalrySyncService();
    if (direction == SyncDirection.INBOUND) {
      return service.syncFactionRivalries(operationId);
    } else {
      return servicesManager.getFactionRivalryNotionSyncService().syncToNotion(operationId);
    }
  }

  public SyncResult syncShowTemplates(
      @NonNull String operationId, @NonNull SyncDirection direction) {
    if (direction == SyncDirection.INBOUND) {
      return servicesManager.getShowTemplateSyncService().syncShowTemplates(operationId);
    } else {
      return servicesManager.getShowTemplateNotionSyncService().syncToNotion(operationId);
    }
  }
}
