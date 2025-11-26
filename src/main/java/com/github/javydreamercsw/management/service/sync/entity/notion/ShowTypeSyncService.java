package com.github.javydreamercsw.management.service.sync.entity.notion;

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
      return SyncResult.success("Show Types", 0, 0, 0);
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
    int createdCount = 0;
    int updatedCount = 0;

    try {
      // Initialize progress tracking for show types sync
      progressTracker.startOperation(operationId, "Sync Show Types", 4);
      progressTracker.updateProgress(operationId, 2, "Extracting show types from Notion...");

      // Extract show types from the Shows database in Notion
      Set<String> notionShowTypes = extractShowTypesFromNotionShows();

      if (notionShowTypes.isEmpty()) {
        log.info(
            "No show types found in Notion Shows database. Ensuring default show types exist...");
        // If no Notion types, create default show types
        SyncResult defaultResult = createDefaultShowTypesIfNeeded();
        createdCount += defaultResult.getCreatedCount();
        updatedCount += defaultResult.getUpdatedCount();

        progressTracker.completeOperation(
            operationId,
            defaultResult.isSuccess(),
            defaultResult.isSuccess()
                ? "Default show types ensured successfully"
                : defaultResult.getErrorMessage(),
            createdCount + updatedCount);
        return SyncResult.success("Show Types", createdCount, updatedCount, 0);
      }

      progressTracker.updateProgress(operationId, 3, "Processing Notion show types...");

      // Sync show types from Notion
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
          // Show type already exists, update it
          // Only update if necessary (e.g., description changed)
          // For now, just increment updatedCount if it exists
          showTypeService.save(existingShowType.get()); // Persist changes to existing entity
          updatedCount++;
          log.debug("Show type already exists: {}", showTypeName);
        }
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

      return SyncResult.success("Show Types", createdCount, updatedCount, 0);

    } catch (Exception e) {
      log.error("Failed to sync show types: {}", e.getMessage(), e);

      progressTracker.completeOperation(
          operationId, false, "Failed to sync show types: " + e.getMessage(), 0);
      healthMonitor.recordFailure("Show Types", e.getMessage());

      return SyncResult.failure("Show Types", "Failed to sync show types: " + e.getMessage());
    }
  }

  /** Extracts all unique show types from the Shows database in Notion. */
  private Set<String> extractShowTypesFromNotionShows() throws InterruptedException {
    Set<String> showTypes = new HashSet<>();

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
      log.info("createDefaultShowTypesIfNeeded called");
      // Check if any show types exist
      List<ShowType> existingShowTypes = showTypeService.findAll();
      log.info("existingShowTypes: {}", existingShowTypes);
      if (existingShowTypes.isEmpty()) {
        log.info("No show types found in database. Creating default show types...");

        int createdCount = 0;

        // Create Weekly show type
        log.info("Checking for Weekly show type");
        if (showTypeService.findByName("Weekly").isEmpty()) {
          log.info("Creating Weekly show type");
          ShowType weeklyType = new ShowType();
          weeklyType.setName("Weekly");
          weeklyType.setDescription("Weekly television show format");
          showTypeService.save(weeklyType);
          createdCount++;
          log.info("Created show type: Weekly");
        }

        // Create Premium Live Event (PLE) show type
        log.info("Checking for Premium Live Event (PLE) show type");
        if (showTypeService.findByName("Premium Live Event (PLE)").isEmpty()) {
          log.info("Creating Premium Live Event (PLE) show type");
          ShowType pleType = new ShowType();
          pleType.setName("Premium Live Event (PLE)");
          pleType.setDescription("Premium live event or pay-per-view format");
          showTypeService.save(pleType);
          createdCount++;
          log.info("Created show type: Premium Live Event (PLE)");
        }

        log.info("✅ Created {} default show types", createdCount);
        return SyncResult.success("Show Types", createdCount, 0, 0);

      } else {
        log.info(
            "Show types already exist in database: {}",
            existingShowTypes.stream().map(ShowType::getName).toList());
        return SyncResult.success("Show Types", 0, 0, 0);
      }

    } catch (Exception e) {
      log.error("Failed to create default show types: {}", e.getMessage(), e);
      return SyncResult.failure(
          "Show Types", "Failed to create default show types: " + e.getMessage());
    }
  }
}
