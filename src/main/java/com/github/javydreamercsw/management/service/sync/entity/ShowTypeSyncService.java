package com.github.javydreamercsw.management.service.sync.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.ShowPage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service responsible for synchronizing show types from Notion to the database. */
@Service
@Slf4j
public class ShowTypeSyncService extends BaseSyncService {

  @Autowired private ShowTypeService showTypeService;

  public ShowTypeSyncService(ObjectMapper objectMapper, NotionSyncProperties syncProperties) {
    super(objectMapper, syncProperties);
  }

  /**
   * Synchronizes show types from Notion or creates default show types.
   *
   * @param operationId Operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncShowTypes(@NonNull String operationId) {
    // Check if already synced in current session
    if (isAlreadySyncedInSession("show-types")) {
      log.info("⏭️ Show types already synced in current session, skipping");
      return SyncResult.success("Show Types", 0, 0);
    }

    log.info("🎭 Starting show types synchronization with operation ID: {}", operationId);
    long startTime = System.currentTimeMillis();

    try {
      SyncResult result = performShowTypesSync(operationId, startTime);
      if (result.isSuccess()) {
        markAsSyncedInSession("show-types");
      }
      return result;
    } catch (Exception e) {
      log.error("Failed to sync show types", e);
      return SyncResult.failure("Show Types", e.getMessage());
    }
  }

  private SyncResult performShowTypesSync(@NonNull String operationId, long startTime) {
    try {
      // Initialize progress tracking for show types sync
      progressTracker.startOperation(operationId, "Sync Show Types", 4);
      progressTracker.updateProgress(operationId, 1, "Extracting show types from Notion...");

      // Extract show types from the Shows database in Notion
      Set<String> notionShowTypes = extractShowTypesFromNotionShows();

      progressTracker.updateProgress(operationId, 2, "Processing show types...");

      if (notionShowTypes.isEmpty()) {
        log.info(
            "No show types found in Notion Shows database. Creating default show types if needed.");
        progressTracker.updateProgress(operationId, 3, "Creating default show types...");
        SyncResult result = createDefaultShowTypesIfNeeded();

        progressTracker.completeOperation(
            operationId,
            result.isSuccess(),
            result.isSuccess()
                ? "Default show types created successfully"
                : result.getErrorMessage(),
            result.getSyncedCount());

        return result;
      }

      // Sync show types from Notion
      int createdCount = 0;
      int updatedCount = 0;

      for (String showTypeName : notionShowTypes) {
        if (showTypeName == null || showTypeName.trim().isEmpty() || "N/A".equals(showTypeName)) {
          continue; // Skip invalid show types
        }

        // Check if show type already exists
        Optional<ShowType> existingShowType = showTypeService.findByName(showTypeName);

        if (existingShowType.isEmpty()) {
          // Create new show type
          ShowType newShowType = new ShowType();
          newShowType.setName(showTypeName);
          newShowType.setDescription(generateShowTypeDescription(showTypeName));
          showTypeService.save(newShowType);
          createdCount++;
          log.info("Created show type from Notion: {}", showTypeName);
        } else {
          // Show type already exists
          updatedCount++;
          log.debug("Show type already exists: {}", showTypeName);
        }
      }

      progressTracker.updateProgress(operationId, 3, "Creating default show types if needed...");

      // Also ensure default show types exist for backward compatibility
      SyncResult defaultResult = createDefaultShowTypesIfNeeded();
      if (defaultResult.isSuccess()) {
        createdCount += defaultResult.getSyncedCount();
      }

      long duration = System.currentTimeMillis() - startTime;
      log.info(
          "✅ Show types sync completed in {}ms. Created: {}, Updated: {}",
          duration,
          createdCount,
          updatedCount);

      progressTracker.completeOperation(
          operationId,
          true,
          String.format(
              "Successfully synced %d show types (%d created, %d updated)",
              createdCount + updatedCount, createdCount, updatedCount),
          createdCount + updatedCount);

      return SyncResult.success("Show Types", createdCount, updatedCount);

    } catch (Exception e) {
      log.error("Failed to sync show types: {}", e.getMessage(), e);

      progressTracker.completeOperation(
          operationId, false, "Failed to sync show types: " + e.getMessage(), 0);

      return SyncResult.failure("Show Types", "Failed to sync show types: " + e.getMessage());
    }
  }

  /** Extracts all unique show types from the Shows database in Notion. */
  private Set<String> extractShowTypesFromNotionShows() {
    Set<String> showTypes = new HashSet<>();

    try {
      // Check if NotionHandler is available
      if (!isNotionHandlerAvailable()) {
        log.warn("NotionHandler not available. Cannot extract show types from Notion.");
        return showTypes; // Return empty set
      }

      log.info("Extracting show types from Notion Shows database...");
      List<ShowPage> allShows = executeWithRateLimit(notionHandler::loadAllShowsForSync);

      for (ShowPage showPage : allShows) {
        String showType = extractShowType(showPage);
        if (showType != null && !showType.trim().isEmpty() && !"N/A".equals(showType)) {
          showTypes.add(showType.trim());
        }
      }

      log.info("Found {} unique show types in Notion: {}", showTypes.size(), showTypes);
      return showTypes;

    } catch (Exception e) {
      log.error("Failed to extract show types from Notion: {}", e.getMessage(), e);
      return new HashSet<>(); // Return empty set on error
    }
  }

  /** Extracts show type from a ShowPage. */
  private String extractShowType(@NonNull ShowPage showPage) {
    if (showPage.getRawProperties() != null) {
      Object showType = showPage.getRawProperties().get("Show Type");
      return showType != null ? showType.toString() : null;
    }
    return null;
  }

  /** Generates a description for a show type based on its name. */
  private String generateShowTypeDescription(String showTypeName) {
    if (showTypeName == null) {
      return "Show type";
    }

    String lowerName = showTypeName.toLowerCase();

    if (lowerName.contains("weekly")) {
      return "Weekly television show format";
    } else if (lowerName.contains("ple") || lowerName.contains("premium")) {
      return "Premium live event or pay-per-view format";
    } else if (lowerName.contains("ppv") || lowerName.contains("pay-per-view")) {
      return "Pay-per-view event format";
    } else if (lowerName.contains("special")) {
      return "Special event show format";
    } else if (lowerName.contains("house")) {
      return "House show or live event format";
    } else if (lowerName.contains("tournament")) {
      return "Tournament-style show format";
    } else {
      return showTypeName + " show format";
    }
  }

  /** Creates default show types if none exist in the database. */
  private SyncResult createDefaultShowTypesIfNeeded() {
    try {
      // Check if any show types exist
      List<ShowType> existingShowTypes = showTypeService.findAll();
      if (existingShowTypes.isEmpty()) {
        log.info("No show types found in database. Creating default show types...");

        int createdCount = 0;

        // Create Weekly show type
        ShowType weeklyType = new ShowType();
        weeklyType.setName("Weekly");
        weeklyType.setDescription("Weekly television show format");
        showTypeService.save(weeklyType);
        createdCount++;
        log.info("Created show type: Weekly");

        // Create Premium Live Event (PLE) show type
        ShowType pleType = new ShowType();
        pleType.setName("Premium Live Event (PLE)");
        pleType.setDescription("Premium live event or pay-per-view format");
        showTypeService.save(pleType);
        createdCount++;
        log.info("Created show type: Premium Live Event (PLE)");

        log.info("✅ Created {} default show types", createdCount);
        return SyncResult.success("Show Types", createdCount, 0);

      } else {
        log.info(
            "Show types already exist in database: {}",
            existingShowTypes.stream().map(ShowType::getName).toList());
        return SyncResult.success("Show Types", 0, 0);
      }

    } catch (Exception e) {
      log.error("Failed to create default show types: {}", e.getMessage(), e);
      return SyncResult.failure(
          "Show Types", "Failed to create default show types: " + e.getMessage());
    }
  }
}
