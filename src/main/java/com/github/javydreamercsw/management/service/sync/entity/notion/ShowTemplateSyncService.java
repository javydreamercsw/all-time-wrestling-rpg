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
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionPage;
import com.github.javydreamercsw.base.ai.notion.ShowTemplatePage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service responsible for synchronizing show templates from Notion to the database. */
@Service
@Slf4j
public class ShowTemplateSyncService extends BaseSyncService {

  @Autowired private ShowTemplateService showTemplateService;

  @Autowired
  private com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository
      showTypeRepository;

  @Autowired
  public ShowTemplateSyncService(
      ObjectMapper objectMapper, NotionSyncProperties syncProperties, NotionHandler notionHandler) {
    super(objectMapper, syncProperties, notionHandler);
  }

  public ShowTemplateSyncService(
      ObjectMapper objectMapper,
      NotionSyncProperties syncProperties,
      NotionHandler notionHandler,
      ShowTemplateService showTemplateService,
      com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository showTypeRepository) {
    super(objectMapper, syncProperties, notionHandler);
    this.showTemplateService = showTemplateService;
    this.showTypeRepository = showTypeRepository;
  }

  /**
   * Synchronizes show templates from Notion to the local JSON file and database.
   *
   * @param operationId Optional operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncShowTemplates(@NonNull String operationId) {
    // Check if already synced in current session
    if (isAlreadySyncedInSession("templates")) {
      log.info("⏭️ Show templates already synced in current session, skipping");
      return SyncResult.success("Show Templates", 0, 0, 0);
    }

    log.info("🎭 Starting show templates synchronization from Notion...");
    long startTime = System.currentTimeMillis();

    try {
      SyncResult result = performShowTemplatesSync(operationId, startTime);
      if (result.isSuccess()) {
        markAsSyncedInSession("Show Templates");
      }
      return result;
    } catch (Exception e) {
      log.error("Failed to sync show templates", e);
      return SyncResult.failure("Show Templates", e.getMessage());
    }
  }

  private SyncResult performShowTemplatesSync(@NonNull String operationId, long startTime) {
    try {
      // Check if entity is enabled
      if (!syncProperties.isEntityEnabled("templates")) {
        log.info("Show templates sync is disabled in configuration");
        return SyncResult.success("Show Templates", 0, 0, 0);
      }

      // Initialize progress tracking (3 steps: retrieve, convert, save to database)
      progressTracker.startOperation(operationId, "Sync Show Templates", 3);
      progressTracker.updateProgress(operationId, 1, "Retrieving show templates from Notion...");

      // Retrieve show templates from Notion
      log.info("📥 Retrieving show templates from Notion...");
      long retrieveStart = System.currentTimeMillis();

      // Check if NotionHandler is available
      if (!isNotionHandlerAvailable()) {
        log.warn("NotionHandler not available. Cannot sync show templates from Notion.");
        return SyncResult.failure(
            "Show Templates", "NotionHandler is not available for sync operations");
      }

      rateLimitService.acquirePermit();
      List<ShowTemplatePage> templatePages = notionHandler.loadAllShowTemplates();
      log.info(
          "✅ Retrieved {} show templates in {}ms",
          templatePages.size(),
          System.currentTimeMillis() - retrieveStart);

      if (templatePages.isEmpty()) {
        log.info("No show templates found in Notion database");
        progressTracker.completeOperation(operationId, true, "No show templates to sync", 0);
        return SyncResult.success("Show Templates", 0, 0, 0);
      }

      // Convert to DTOs
      progressTracker.updateProgress(
          operationId,
          2,
          String.format("Converting %d show templates to DTOs...", templatePages.size()));
      log.info("🔄 Converting show templates to DTOs...");
      long convertStart = System.currentTimeMillis();
      List<ShowTemplateDTO> templateDTOs =
          convertShowTemplatePagesToDTOs(templatePages, operationId);
      log.info(
          "✅ Converted {} show templates in {}ms",
          templateDTOs.size(),
          System.currentTimeMillis() - convertStart);

      // Save show templates to database
      progressTracker.updateProgress(
          operationId,
          3,
          String.format("Saving %d show templates to database...", templateDTOs.size()));
      log.info("💾 Saving show templates to database...");
      long dbStart = System.currentTimeMillis();
      int savedCount = saveShowTemplatesToDatabase(templateDTOs);
      long dbTime = System.currentTimeMillis() - dbStart;
      log.info("✅ Saved {} show templates to database in {}ms", savedCount, dbTime);

      long totalTime = System.currentTimeMillis() - startTime;
      log.info(
          "🎉 Successfully synchronized {} show templates in {}ms total", savedCount, totalTime);

      // Complete progress tracking
      progressTracker.completeOperation(
          operationId,
          true,
          String.format("Successfully synced %d show templates", savedCount),
          savedCount);

      // Record success in health monitor
      healthMonitor.recordSuccess("Show Templates", totalTime, savedCount);

      return SyncResult.success("Show Templates", savedCount, 0, 0);

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;
      log.error("❌ Failed to synchronize show templates from Notion after {}ms", totalTime, e);

      progressTracker.failOperation(operationId, "Sync failed: " + e.getMessage());

      // Record failure in health monitor
      healthMonitor.recordFailure("Show Templates", e.getMessage());

      return SyncResult.failure("Show Templates", e.getMessage());
    }
  }

  private List<ShowTemplateDTO> convertShowTemplatePagesToDTOs(
      @NonNull List<ShowTemplatePage> templatePages, String operationId) {
    return processWithControlledParallelism(
        templatePages,
        this::convertShowTemplatePageToDTO,
        10, // Batch size
        operationId,
        2, // Progress step
        "Converted %d/%d show templates");
  }

  /** Converts a single ShowTemplatePage to ShowTemplateDTO. */
  private ShowTemplateDTO convertShowTemplatePageToDTO(@NonNull ShowTemplatePage templatePage) {
    ShowTemplateDTO dto = new ShowTemplateDTO();
    dto.setName(extractNameFromNotionPage(templatePage));
    dto.setDescription(extractDescriptionFromNotionPage(templatePage));

    // Extract show type from Notion properties
    String showType = extractShowTypeFromNotionPage(templatePage);
    dto.setShowType(showType);
    dto.setExternalId(templatePage.getId());
    return dto;
  }

  /** Extracts show type from any NotionPage type using raw properties. */
  private String extractShowTypeFromNotionPage(@NonNull NotionPage page) {
    if (page.getRawProperties() != null) {
      Object showType = page.getRawProperties().get("Show Type");
      if (showType == null) {
        showType = page.getRawProperties().get("ShowType");
      }
      if (showType == null) {
        showType = page.getRawProperties().get("Type");
      }

      if (showType != null) {
        // Handle different property types
        String showTypeStr = extractShowTypeValue(showType);
        if (showTypeStr != null && !showTypeStr.trim().isEmpty() && !"N/A".equals(showTypeStr)) {
          return showTypeStr.trim();
        }
      }

      log.debug("Show type not found or empty for page: {}", page.getId());
      return null;
    }
    return null;
  }

  /**
   * Extracts show type value from different Notion property types. Handles text, select, and
   * relation properties.
   */
  private String extractShowTypeValue(Object property) {
    if (property == null) {
      return null;
    }

    try {
      // Handle PageProperty objects (from Notion API)
      if (property instanceof notion.api.v1.model.pages.PageProperty pageProperty) {

        // Handle relation properties
        if (pageProperty.getRelation() != null && !pageProperty.getRelation().isEmpty()) {
          // For relation properties, we need to resolve the referenced page
          // The relation contains PageReference objects with IDs
          var relation = pageProperty.getRelation().get(0); // Get first relation
          String relationId = relation.getId();

          // Try to resolve the relation by fetching the referenced page title
          // For now, we'll use a mapping based on known show type page IDs
          return resolveShowTypeFromRelationId(relationId);
        }

        // Handle select properties
        if (pageProperty.getSelect() != null) {
          return pageProperty.getSelect().getName();
        }

        // Handle title properties
        if (pageProperty.getTitle() != null && !pageProperty.getTitle().isEmpty()) {
          return pageProperty.getTitle().get(0).getPlainText();
        }

        // Handle rich text properties
        if (pageProperty.getRichText() != null && !pageProperty.getRichText().isEmpty()) {
          return pageProperty.getRichText().get(0).getPlainText();
        }
      }

      // Fallback: try to extract as string
      String fallbackStr = property.toString().trim();
      if (!fallbackStr.isEmpty() && !"N/A".equals(fallbackStr)) {
        // Check if it looks like a relation string
        if (fallbackStr.contains("PageReference") || fallbackStr.contains("relation=")) {
          log.warn("Show type appears to be a relation but could not be resolved: {}", fallbackStr);
          return null;
        }
        return fallbackStr;
      }

    } catch (Exception e) {
      log.error("Failed to extract show type from property: {}", property, e);
    }

    return null;
  }

  /**
   * Resolves show type name from relation ID. This is a temporary solution until we can implement
   * proper relation resolution.
   */
  private String resolveShowTypeFromRelationId(@NonNull String relationId) {
    // For now, we'll map known relation IDs to show types
    // In a full implementation, you would fetch the referenced page from Notion

    // You can add mappings here based on your Notion show type page IDs
    // Example: if ("1fe90edc-c30f-800b-bbd0-d6e0cba01c9b".equals(relationId)) return "Weekly";

    log.warn(
        "Show type relation ID '{}' could not be resolved to a show type name. Please check your"
            + " Notion database configuration.",
        relationId);
    return null;
  }

  /**
   * Saves the list of ShowTemplateDTO objects to the database.
   *
   * @param templateDTOs List of ShowTemplateDTO objects to save
   * @return Number of show templates successfully saved/updated
   */
  private int saveShowTemplatesToDatabase(@NonNull List<ShowTemplateDTO> templateDTOs) {
    log.info("💾 Saving {} show templates to database...", templateDTOs.size());
    int savedCount = 0;
    int updatedCount = 0;
    int skippedCount = 0;

    for (ShowTemplateDTO dto : templateDTOs) {
      try {
        if (dto.getShowType() == null || dto.getShowType().trim().isEmpty()) {
          log.warn("Skipping template '{}' - no show type could be determined.", dto.getName());
          skippedCount++;
          continue;
        }

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

        Optional<com.github.javydreamercsw.management.domain.show.type.ShowType> showTypeOpt =
            showTypeRepository.findByName(dto.getShowType());
        if (showTypeOpt.isEmpty()) {
          log.warn("Show type not found: {}", dto.getShowType());
          skippedCount++;
          continue;
        }

        template.setName(dto.getName());
        template.setDescription(dto.getDescription());
        template.setShowType(showTypeOpt.get());
        template.setExternalId(dto.getExternalId());

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
    return savedCount;
  }

  /** DTO for Show Template data from Notion. */
  @Setter
  @Getter
  public static class ShowTemplateDTO {
    // Getters and setters
    private String name;
    private String description;
    private String showType;
    private String externalId; // Notion page ID
  }
}
