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
package com.github.javydreamercsw.management.service.sync.entity.notion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionApiExecutor;
import com.github.javydreamercsw.base.ai.notion.TitleReignPage;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.sync.SyncEntityType;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class TitleReignSyncService extends BaseSyncService {

  private final TitleReignNotionSyncService titleReignNotionSyncService;

  @Autowired
  public TitleReignSyncService(
      ObjectMapper objectMapper,
      SyncServiceDependencies syncServiceDependencies,
      TitleReignNotionSyncService titleReignNotionSyncService,
      NotionApiExecutor notionApiExecutor) {
    super(objectMapper, syncServiceDependencies, notionApiExecutor);
    this.titleReignNotionSyncService = titleReignNotionSyncService;
  }

  @Transactional
  public SyncResult syncTitleReigns(@NonNull String operationId) {
    if (syncServiceDependencies
        .getSyncSessionManager()
        .isAlreadySyncedInSession(SyncEntityType.TITLE_REIGN.getKey())) {
      return SyncResult.success(SyncEntityType.TITLE_REIGN.getKey(), 0, 0, 0);
    }

    log.info("👑 Starting title reigns synchronization from Notion...");
    long startTime = System.currentTimeMillis();

    try {
      SyncResult result = performTitleReignsSync(operationId, startTime);
      if (result.isSuccess()) {
        syncServiceDependencies
            .getSyncSessionManager()
            .markAsSyncedInSession(SyncEntityType.TITLE_REIGN.getKey());
      }
      return result;
    } catch (Exception e) {
      log.error("Failed to sync title reigns", e);
      return SyncResult.failure(SyncEntityType.TITLE_REIGN.getKey(), e.getMessage());
    }
  }

  private SyncResult performTitleReignsSync(@NonNull String operationId, long startTime) {
    if (!syncServiceDependencies
        .getNotionSyncProperties()
        .isEntityEnabled(SyncEntityType.TITLE_REIGN.getKey())) {
      log.info("Title Reigns sync is disabled in configuration");
      return SyncResult.success(SyncEntityType.TITLE_REIGN.getKey(), 0, 0, 0);
    }

    if (!isNotionHandlerAvailable()) {
      log.warn("NotionHandler not available. Cannot sync title reigns from Notion.");
      return SyncResult.failure(
          SyncEntityType.TITLE_REIGN.getKey(),
          "NotionHandler is not available for sync operations");
    }

    try {
      List<TitleReignPage> titleReignPages =
          executeWithRateLimit(
              () -> syncServiceDependencies.getNotionHandler().loadAllTitleReigns());
      log.info("✅ Retrieved {} title reigns from Notion", titleReignPages.size());

      for (TitleReignPage page : titleReignPages) {
        String titleExtId = page.getTitleRelationId();
        List<String> championExtIds = page.getChampionRelationIds();

        if (titleExtId == null || championExtIds.isEmpty()) {
          log.warn(
              "Skipping title reign with missing title or champion relation: {}", page.getId());
          continue;
        }

        Optional<Title> titleOpt =
            syncServiceDependencies.getTitleRepository().findByExternalId(titleExtId);

        if (titleOpt.isEmpty()) {
          log.warn("Skipping title reign: Title with ID '{}' not found locally.", titleExtId);
          continue;
        }

        Title title = titleOpt.get();
        List<Wrestler> champions =
            championExtIds.stream()
                .map(
                    id ->
                        syncServiceDependencies
                            .getWrestlerRepository()
                            .findByExternalId(id.trim())
                            .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();

        if (champions.isEmpty()) {
          log.warn("Skipping title reign: No champions found for IDs '{}'.", championExtIds);
          continue;
        }

        // Attempt to find existing reign
        TitleReign reign =
            syncServiceDependencies
                .getTitleReignRepository()
                .findByExternalId(page.getId())
                .or(
                    () ->
                        (page.getReignNumber() != null)
                            ? syncServiceDependencies
                                .getTitleReignRepository()
                                .findByTitleAndReignNumber(title, page.getReignNumber())
                            : Optional.empty())
                .orElseGet(TitleReign::new);

        reign.setExternalId(page.getId());
        reign.setTitle(title);
        reign.getChampions().clear();
        reign.getChampions().addAll(champions);
        reign.setReignNumber(page.getReignNumber() != null ? page.getReignNumber() : 1);
        reign.setNotes(page.getNotes());

        if (page.getWonAtSegmentRelationId() != null) {
          syncServiceDependencies
              .getSegmentRepository()
              .findByExternalId(page.getWonAtSegmentRelationId())
              .ifPresent(reign::setWonAtSegment);
        }

        // Parse dates
        try {
          if (page.getStartDate() != null) {
            reign.setStartDate(
                LocalDate.parse(page.getStartDate()).atStartOfDay(ZoneOffset.UTC).toInstant());
          }
          if (page.getEndDate() != null) {
            reign.setEndDate(
                LocalDate.parse(page.getEndDate()).atStartOfDay(ZoneOffset.UTC).toInstant());
          }
        } catch (DateTimeParseException e) {
          log.warn("Failed to parse date for title reign {}: {}", page.getId(), e.getMessage());
        }

        syncServiceDependencies.getTitleReignRepository().save(reign);
      }

      long totalTime = System.currentTimeMillis() - startTime;
      syncServiceDependencies
          .getHealthMonitor()
          .recordSuccess(SyncEntityType.TITLE_REIGN.getKey(), totalTime, titleReignPages.size());
      return SyncResult.success(SyncEntityType.TITLE_REIGN.getKey(), titleReignPages.size(), 0, 0);

    } catch (Exception e) {
      log.error("❌ Failed to synchronize title reigns from Notion", e);
      syncServiceDependencies
          .getHealthMonitor()
          .recordFailure(SyncEntityType.TITLE_REIGN.getKey(), e.getMessage());
      return SyncResult.failure(SyncEntityType.TITLE_REIGN.getKey(), e.getMessage());
    }
  }

  public SyncResult syncToNotion(@NonNull String operationId) {
    return titleReignNotionSyncService.syncToNotion(operationId);
  }
}
