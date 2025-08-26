package com.github.javydreamercsw.management.service.sync.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.ShowTemplatePage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.dto.ShowTemplateDTO;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service responsible for synchronizing show templates from Notion to the database. */
@Service
@Slf4j
public class ShowTemplateSyncService extends BaseSyncService {

  @Autowired private ShowTemplateService showTemplateService;

  public ShowTemplateSyncService(ObjectMapper objectMapper, NotionSyncProperties syncProperties) {
    super(objectMapper, syncProperties);
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
      return SyncResult.success("Show Templates", 0, 0);
    }

    log.info("🎭 Starting show templates synchronization from Notion...");
    long startTime = System.currentTimeMillis();

    try {
      SyncResult result = performShowTemplatesSync(operationId, startTime);
      if (result.isSuccess()) {
        markAsSyncedInSession("templates");
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
        return SyncResult.success("Show Templates", 0, 0);
      }

      // Initialize progress tracking (3 steps: retrieve, convert, save to database)
      if (operationId != null) {
        progressTracker.startOperation(operationId, "Sync Show Templates", 3);
        progressTracker.updateProgress(operationId, 1, "Retrieving show templates from Notion...");
      }

      // Retrieve show templates from Notion
      log.info("📥 Retrieving show templates from Notion...");
      long retrieveStart = System.currentTimeMillis();

      // Check if NotionHandler is available
      if (!isNotionHandlerAvailable()) {
        log.warn("NotionHandler not available. Cannot sync show templates from Notion.");
        return SyncResult.failure(
            "ShowTemplates", "NotionHandler is not available for sync operations");
      }

      List<ShowTemplatePage> templatePages = notionHandler.loadAllShowTemplates();
      log.info(
          "✅ Retrieved {} show templates in {}ms",
          templatePages.size(),
          System.currentTimeMillis() - retrieveStart);

      // Convert to DTOs
      if (operationId != null) {
        progressTracker.updateProgress(
            operationId,
            2,
            String.format("Converting %d show templates to DTOs...", templatePages.size()));
      }
      log.info("🔄 Converting show templates to DTOs...");
      long convertStart = System.currentTimeMillis();
      List<ShowTemplateDTO> templateDTOs = convertShowTemplatePagesToDTOs(templatePages);
      log.info(
          "✅ Converted {} show templates in {}ms",
          templateDTOs.size(),
          System.currentTimeMillis() - convertStart);

      // Save show templates to database
      if (operationId != null) {
        progressTracker.updateProgress(
            operationId,
            3,
            String.format("Saving %d show templates to database...", templateDTOs.size()));
      }
      log.info("💾 Saving show templates to database...");
      long dbStart = System.currentTimeMillis();
      int savedCount = saveShowTemplatesToDatabase(templateDTOs);
      long dbTime = System.currentTimeMillis() - dbStart;
      log.info("✅ Saved {} show templates to database in {}ms", savedCount, dbTime);

      long totalTime = System.currentTimeMillis() - startTime;
      log.info(
          "🎉 Successfully synchronized {} show templates in {}ms total", savedCount, totalTime);

      // Complete progress tracking
      if (operationId != null) {
        progressTracker.completeOperation(
            operationId,
            true,
            String.format("Successfully synced %d show templates", savedCount),
            savedCount);
      }

      // Record success in health monitor
      healthMonitor.recordSuccess("Show Templates", totalTime, savedCount);

      return SyncResult.success("Show Templates", savedCount, 0);

    } catch (Exception e) {
      long totalTime = System.currentTimeMillis() - startTime;
      log.error("❌ Failed to synchronize show templates from Notion after {}ms", totalTime, e);

      if (operationId != null) {
        progressTracker.failOperation(operationId, "Sync failed: " + e.getMessage());
      }

      // Record failure in health monitor
      healthMonitor.recordFailure("Show Templates", e.getMessage());

      return SyncResult.failure("Show Templates", e.getMessage());
    }
  }

  /** Converts ShowTemplatePage objects to ShowTemplateDTO objects. */
  private List<ShowTemplateDTO> convertShowTemplatePagesToDTOs(
      @NonNull List<ShowTemplatePage> templatePages) {
    return templatePages.parallelStream()
        .map(this::convertShowTemplatePageToDTO)
        .collect(Collectors.toList());
  }

  /** Converts a single ShowTemplatePage to ShowTemplateDTO. */
  private ShowTemplateDTO convertShowTemplatePageToDTO(@NonNull ShowTemplatePage templatePage) {
    ShowTemplateDTO dto = new ShowTemplateDTO();
    dto.setName(extractNameFromNotionPage(templatePage));
    dto.setDescription(extractDescriptionFromNotionPage(templatePage));

    // Extract show type and provide intelligent defaults
    String showType = extractShowTypeFromNotionPage(templatePage);
    if (showType == null || showType.trim().isEmpty()) {
      // Smart mapping based on template name patterns
      String templateName = dto.getName().toLowerCase();
      if (templateName.contains("weekly")
          || templateName.contains("continuum")
          || templateName.contains("timeless")) {
        showType = "Weekly";
        log.debug("Mapped template '{}' to Weekly show type based on name pattern", dto.getName());
      } else if (templateName.contains("ple")
          || templateName.contains("premium")
          || templateName.contains("ppv")) {
        showType = "PLE";
        log.debug("Mapped template '{}' to PLE show type based on name pattern", dto.getName());
      } else {
        // Default to Weekly for unknown templates
        showType = "Weekly";
        log.debug("Using default Weekly show type for template: {}", dto.getName());
      }
    }

    dto.setShowType(showType);
    dto.setExternalId(templatePage.getId());
    return dto;
  }

  /** Extracts show type from any NotionPage type using raw properties. */
  private String extractShowTypeFromNotionPage(
      @NonNull com.github.javydreamercsw.base.ai.notion.NotionPage page) {
    if (page.getRawProperties() != null) {
      Object showType = page.getRawProperties().get("Show Type");
      if (showType == null) {
        showType = page.getRawProperties().get("ShowType");
      }
      if (showType == null) {
        showType = page.getRawProperties().get("Type");
      }

      if (showType != null) {
        String showTypeStr = showType.toString().trim();
        if (!showTypeStr.isEmpty() && !"N/A".equals(showTypeStr)) {
          return showTypeStr;
        }
      }

      log.debug("Show type not found or empty for page: {}", page.getId());
      return null;
    }
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

    for (ShowTemplateDTO dto : templateDTOs) {
      try {
        // Use the ShowTemplateService to create or update the template
        ShowTemplate savedTemplate =
            showTemplateService.createOrUpdateTemplate(
                dto.getName(),
                dto.getDescription(),
                dto.getShowType(), // This maps to showTypeName in the service
                null // notionUrl - we don't have this in the DTO currently
                );

        if (savedTemplate != null) {
          // Set external ID if available and save only once
          if (dto.getExternalId() != null && !dto.getExternalId().trim().isEmpty()) {
            savedTemplate.setExternalId(dto.getExternalId());
            // Save again only if we modified the external ID
            savedTemplate = showTemplateService.save(savedTemplate);
          }
          savedCount++;
          log.debug("Saved show template: {}", dto.getName());
        } else {
          log.warn(
              "Failed to save show template: {} (show type not found: {})",
              dto.getName(),
              dto.getShowType());
        }
      } catch (Exception e) {
        log.error("Failed to save show template '{}': {}", dto.getName(), e.getMessage());
      }
    }

    log.info(
        "Successfully saved {} out of {} show templates to database",
        savedCount,
        templateDTOs.size());
    return savedCount;
  }

  /** DTO for Show Template data from Notion. */
  public static class ShowTemplateDTO {
    private String name;
    private String description;
    private String showType;
    private String externalId; // Notion page ID

    // Getters and setters
    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getShowType() {
      return showType;
    }

    public void setShowType(String showType) {
      this.showType = showType;
    }

    public String getExternalId() {
      return externalId;
    }

    public void setExternalId(String externalId) {
      this.externalId = externalId;
    }
  }
}
