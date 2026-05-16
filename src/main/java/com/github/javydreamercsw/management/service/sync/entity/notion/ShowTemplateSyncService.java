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
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.ShowTemplatePage;
import com.github.javydreamercsw.management.domain.show.template.RecurrenceType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.SyncSessionManager;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.time.DayOfWeek;
import java.time.Month;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to synchronize Show Templates from Notion. This service identifies new or updated
 * templates in Notion and synchronizes them to the local database.
 */
@Service
@Slf4j
public class ShowTemplateSyncService extends BaseSyncService implements NotionEntitySyncService {

  private final ShowTemplateService showTemplateService;
  private final ShowTypeRepository showTypeRepository;

  @Autowired @Lazy private ShowTemplateSyncService self;

  public ShowTemplateSyncService(
      final ObjectMapper objectMapper,
      final SyncServiceDependencies syncServiceDependencies,
      final ShowTemplateService showTemplateService,
      final ShowTypeRepository showTypeRepository,
      final NotionApiExecutor notionApiExecutor) {
    super(objectMapper, syncServiceDependencies, notionApiExecutor);
    this.showTemplateService = showTemplateService;
    this.showTypeRepository = showTypeRepository;
    this.self = this;
  }

  @Override
  @Transactional
  public SyncResult syncToNotion(@NonNull final String operationId) {
    log.warn("syncToNotion not implemented for ShowTemplateSyncService");
    return SyncResult.success("Show Templates", 0, 0, 0);
  }

  @Override
  @Transactional
  public SyncResult syncToNotion(@NonNull final String operationId, final Collection<Long> ids) {
    return syncToNotion(operationId);
  }

  /**
   * Synchronizes all show templates from Notion.
   *
   * @param operationId The unique ID for this sync operation
   * @return SyncResult containing the synchronization metrics
   */
  public SyncResult syncShowTemplates(final String operationId) {
    SyncSessionManager sessionManager = syncServiceDependencies.getSyncSessionManager();

    if (sessionManager.isAlreadySyncedInSession("Show Templates")) {
      log.info("Show templates synchronization already in progress. Skipping.");
      return SyncResult.success("Show Templates", 0, 0, 0);
    }

    try {
      sessionManager.markAsSyncedInSession("Show Templates");
      return performShowTemplatesSync(operationId);
    } finally {
      sessionManager.resetSyncStatus("Show Templates");
    }
  }

  private SyncResult performShowTemplatesSync(final String operationId) {
    long startTime = System.currentTimeMillis();
    log.info("🎭 Starting show templates synchronization from Notion...");

    try {
      if (!isNotionHandlerAvailable()) {
        log.warn("NotionHandler not available. Cannot sync show templates from Notion.");
        return SyncResult.failure(
            "Show Templates", "NotionHandler is not available for sync operations");
      }

      // Check if sync is enabled for this entity
      if (!syncServiceDependencies.getNotionSyncProperties().isEntityEnabled("templates")) {
        log.info("Show templates sync is disabled in configuration");
        return SyncResult.success("Show Templates", 0, 0, 0);
      }

      // Start progress tracking
      syncServiceDependencies
          .getProgressTracker()
          .startOperation(operationId, "Sync Show Templates", 3);

      NotionHandler notionHandler = syncServiceDependencies.getNotionHandler();

      // 1. Load from Notion
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(operationId, 1, "Retrieving show templates from Notion...");
      log.info("📥 Retrieving show templates from Notion...");
      List<ShowTemplatePage> templatePages = notionHandler.loadAllShowTemplates();
      log.info("✅ Retrieved {} show templates in {}ms", templatePages.size(), 0);

      if (templatePages.isEmpty()) {
        log.info("No show templates found in Notion database");
        syncServiceDependencies
            .getProgressTracker()
            .completeOperation(operationId, true, "No show templates to sync", 0);
        return SyncResult.success("Show Templates", 0, 0, 0);
      }

      // Convert to DTOs
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(
              operationId,
              2,
              "Converting %d show templates to DTOs...".formatted(templatePages.size()));
      log.info("🔄 Converting show templates to DTOs...");
      long convertStart = System.currentTimeMillis();
      List<ShowTemplateDTO> templateDTOs =
          convertShowTemplatePagesToDTOs(templatePages, operationId);
      log.info(
          "✅ Converted {} show templates in {}ms",
          templateDTOs.size(),
          System.currentTimeMillis() - convertStart);

      List<ShowTemplateDTO> filteredDTOs =
          templateDTOs.stream()
              .filter(dto -> dto.getShowType() != null && !dto.getShowType().isBlank())
              .collect(Collectors.toList());
      log.info("Found {} templates with show types.", filteredDTOs.size());

      // Save show templates to database
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(
              operationId,
              3,
              "Saving %d show templates to database...".formatted(filteredDTOs.size()));
      log.info("💾 Saving show templates to database...");
      long dbStart = System.currentTimeMillis();
      int successCount = saveShowTemplatesToDatabase(filteredDTOs);
      long dbTime = System.currentTimeMillis() - dbStart;
      log.info("✅ Saved {} show templates to database in {}ms", successCount, dbTime);

      long totalTime = System.currentTimeMillis() - startTime;
      log.info(
          "🎉 Successfully synchronized {} show templates in {}ms total", successCount, totalTime);

      // Complete progress tracking
      syncServiceDependencies
          .getProgressTracker()
          .completeOperation(
              operationId,
              true,
              "Successfully synced %d show templates".formatted(successCount),
              successCount);

      // Record success in health monitor
      syncServiceDependencies
          .getHealthMonitor()
          .recordSuccess("Show Templates", totalTime, successCount);

      return SyncResult.success("Show Templates", successCount, 0, 0);

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;
      log.error("❌ Failed to synchronize show templates from Notion after {}ms", totalTime, e);

      syncServiceDependencies
          .getProgressTracker()
          .failOperation(operationId, "Sync failed: " + e.getMessage());

      // Record failure in health monitor
      syncServiceDependencies.getHealthMonitor().recordFailure("Show Templates", e.getMessage());

      return SyncResult.failure("Show Templates", e.getMessage());
    }
  }

  private List<ShowTemplateDTO> convertShowTemplatePagesToDTOs(
      @NonNull final List<ShowTemplatePage> templatePages, final String operationId) {
    return processWithControlledParallelism(
        templatePages,
        this::convertShowTemplatePageToDTO,
        10, // Batch size
        operationId,
        2, // Progress step
        "Converted %d/%d show templates");
  }

  /** Converts a single ShowTemplatePage to ShowTemplateDTO. */
  private ShowTemplateDTO convertShowTemplatePageToDTO(
      @NonNull final ShowTemplatePage templatePage) {
    ShowTemplateDTO dto = new ShowTemplateDTO();
    dto.setName(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractNameFromNotionPage(templatePage));
    dto.setDescription(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractDescriptionFromNotionPage(templatePage));

    // Extract show type from Notion properties
    String showType =
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractShowTypeFromNotionPage(templatePage);
    dto.setShowType(showType);
    dto.setExternalId(templatePage.getId());

    dto.setDurationDays(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractDurationDaysFromNotionPage(templatePage));
    dto.setRecurrenceType(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractRecurrenceTypeFromNotionPage(templatePage));
    dto.setDayOfWeek(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractDayOfWeekFromNotionPage(templatePage));
    dto.setDayOfMonth(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractDayOfMonthFromNotionPage(templatePage));
    dto.setWeekOfMonth(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractWeekOfMonthFromNotionPage(templatePage));
    dto.setMonth(
        syncServiceDependencies
            .getNotionPageDataExtractor()
            .extractMonthFromNotionPage(templatePage));

    return dto;
  }

  private int saveShowTemplatesToDatabase(@NonNull final List<ShowTemplateDTO> templateDTOs) {
    int savedCount = 0;
    int updatedCount = 0;
    int skippedCount = 0;

    for (ShowTemplateDTO dto : templateDTOs) {
      try {
        ShowTemplate template = null;
        if (dto.getExternalId() != null && !dto.getExternalId().isBlank()) {
          template = showTemplateService.findByExternalId(dto.getExternalId()).orElse(null);
        }

        if (template == null) {
          template = showTemplateService.findByName(dto.getName()).orElse(null);
        }

        boolean isNew = template == null;

        if (isNew) {
          template = new ShowTemplate();
        }

        Optional<ShowType> showTypeOpt = showTypeRepository.findByName(dto.getShowType());
        if (showTypeOpt.isEmpty()) {
          log.warn("Show type not found: {}", dto.getShowType());
          skippedCount++;
          continue;
        }

        template.setName(dto.getName());
        template.setDescription(dto.getDescription());
        template.setShowType(showTypeOpt.get());
        template.setExternalId(dto.getExternalId());

        template.setDurationDays(dto.getDurationDays() != null ? dto.getDurationDays() : 1);
        if (dto.getRecurrenceType() != null) {
          try {
            template.setRecurrenceType(
                RecurrenceType.valueOf(dto.getRecurrenceType().toUpperCase()));
          } catch (IllegalArgumentException e) {
            template.setRecurrenceType(RecurrenceType.NONE);
          }
        }
        if (dto.getDayOfWeek() != null) {
          try {
            template.setDayOfWeek(DayOfWeek.valueOf(dto.getDayOfWeek().toUpperCase()));
          } catch (IllegalArgumentException ignored) {
          }
        }
        template.setDayOfMonth(dto.getDayOfMonth());
        template.setWeekOfMonth(dto.getWeekOfMonth());
        if (dto.getMonth() != null) {
          try {
            template.setMonth(Month.valueOf(dto.getMonth().toUpperCase()));
          } catch (IllegalArgumentException ignored) {
          }
        }

        showTemplateService.save(template);

        if (isNew) {
          savedCount++;
        } else {
          updatedCount++;
        }
      } catch (Exception e) {
        log.error("Failed to save show template '{}': {}", dto.getName(), e.getMessage());
        skippedCount++;
      }
    }
    log.info(
        "Successfully saved/updated {} show templates ({} new, {} updated, {} skipped)",
        savedCount + updatedCount,
        savedCount,
        updatedCount,
        skippedCount);
    return savedCount + updatedCount;
  }

  /** DTO for Show Template data from Notion. */
  @Setter
  @Getter
  public static class ShowTemplateDTO {
    private String name;
    private String description;
    private String showType;
    private String externalId; // Notion page ID
    private Integer durationDays;
    private String recurrenceType;
    private String dayOfWeek;
    private Integer dayOfMonth;
    private Integer weekOfMonth;
    private String month;
  }
}
