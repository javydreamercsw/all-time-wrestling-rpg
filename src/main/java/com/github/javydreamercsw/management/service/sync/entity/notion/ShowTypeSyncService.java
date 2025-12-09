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
import com.github.javydreamercsw.base.ai.notion.ShowPage;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Service responsible for synchronizing show types from Notion to the database. */
@Service
@Slf4j
public class ShowTypeSyncService extends BaseSyncService {

  private final ShowTypeService showTypeService;

  public ShowTypeSyncService(
      ObjectMapper objectMapper,
      SyncServiceDependencies syncServiceDependencies,
      ShowTypeService showTypeService,
      NotionApiExecutor notionApiExecutor) {
    super(objectMapper, syncServiceDependencies, notionApiExecutor);
    this.showTypeService = showTypeService;
  }

  /**
   * Synchronizes show types from Notion or creates default show types.
   *
   * @param operationId Operation ID for progress tracking
   * @return SyncResult indicating success or failure with details
   */
  public SyncResult syncShowTypes(@NonNull String operationId) {
    // Check if already synced in current session
    if (syncServiceDependencies.getSyncSessionManager().isAlreadySyncedInSession("show-types")) {
      log.info("⏭️ Show types already synced in current session, skipping");
      return SyncResult.success("Show Types", 0, 0, 0);
    }

    log.info("🎭 Starting show types synchronization with operation ID: {}", operationId);
    long startTime = System.currentTimeMillis();

    try {
      SyncResult result = performShowTypesSync(operationId, startTime);
      if (result.isSuccess()) {
        syncServiceDependencies.getSyncSessionManager().markAsSyncedInSession("show-types");
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
      syncServiceDependencies
          .getProgressTracker()
          .startOperation(operationId, "Sync Show Types", 4);
      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(operationId, 2, "Extracting show types from Notion...");

      // Extract show types from the Shows database in Notion
      Set<String> notionShowTypes = extractShowTypesFromNotionShows();

      if (notionShowTypes.isEmpty()) {
        log.info(
            "No show types found in Notion Shows database. Ensuring default show types exist...");
        // If no Notion types, create default show types
        SyncResult defaultResult = createDefaultShowTypesIfNeeded();
        createdCount += defaultResult.getCreatedCount();
        updatedCount += defaultResult.getUpdatedCount();

        syncServiceDependencies
            .getProgressTracker()
            .completeOperation(
                operationId,
                defaultResult.isSuccess(),
                defaultResult.isSuccess()
                    ? "Default show types ensured successfully"
                    : defaultResult.getErrorMessage(),
                createdCount + updatedCount);
        return SyncResult.success("Show Types", createdCount, updatedCount, 0);
      }

      syncServiceDependencies
          .getProgressTracker()
          .updateProgress(operationId, 3, "Processing Notion show types...");

      // Sync show types from Notion
      for (String showTypeName : notionShowTypes) {
        if (showTypeName == null || showTypeName.trim().isEmpty() || "N/A".equals(showTypeName)) {
          continue; // Skip invalid show types
        }

        // Check if show type already exists
        Optional<ShowType> existingShowType = showTypeService.findByName(showTypeName);

        if (existingShowType.isEmpty()) {
          // Create new show type
          showTypeService.createOrUpdateShowType(
              showTypeName, generateShowTypeDescription(showTypeName), 0, 0);
          createdCount++;
          log.info("Created show type from Notion: {}", showTypeName);
        } else {
          // Show type already exists, update it
          showTypeService.createOrUpdateShowType(
              showTypeName,
              existingShowType.get().getDescription(),
              existingShowType.get().getExpectedMatches(),
              existingShowType.get().getExpectedPromos());
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

      syncServiceDependencies
          .getProgressTracker()
          .completeOperation(
              operationId,
              true,
              String.format(
                  "Successfully synced %d show types (%d created, %d updated)",
                  createdCount + updatedCount, createdCount, updatedCount),
              createdCount + updatedCount);

      return SyncResult.success("Show Types", createdCount, updatedCount, 0);

    } catch (Exception e) {
      log.error("Failed to sync show types: {}", e.getMessage(), e);

      syncServiceDependencies
          .getProgressTracker()
          .completeOperation(operationId, false, "Failed to sync show types: " + e.getMessage(), 0);
      syncServiceDependencies.getHealthMonitor().recordFailure("Show Types", e.getMessage());

      return SyncResult.failure("Show Types", "Failed to sync show types: " + e.getMessage());
    }
  }

  private Set<String> extractShowTypesFromNotionShows() throws InterruptedException {
    Set<String> showTypes = new HashSet<>();

    // Check if NotionHandler is available
    if (!isNotionHandlerAvailable()) {
      log.warn("NotionHandler not available. Cannot extract show types from Notion.");
      return showTypes; // Return empty set
    }

    log.info("Extracting show types from Notion Shows database...");
    List<ShowPage> allShows =
        executeWithRateLimit(
            () -> syncServiceDependencies.getNotionHandler().loadAllShowsForSync());

    for (ShowPage showPage : allShows) {
      String showType =
          syncServiceDependencies
              .getNotionPageDataExtractor()
              .extractShowTypeFromNotionPage(showPage);
      if (showType != null && !showType.trim().isEmpty() && !"N/A".equals(showType)) {
        showTypes.add(showType.trim());
      }
    }

    log.info("Found {} unique show types in Notion: {}", showTypes.size(), showTypes);
    return showTypes;
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
      int createdCount = 0;

      // Create Weekly show type if it doesn't exist
      if (showTypeService.findByName("Weekly").isEmpty()) {
        showTypeService.createOrUpdateShowType("Weekly", "Weekly television show format", 4, 2);
        createdCount++;
        log.info("Created show type: Weekly");
      }

      // Create Premium Live Event (PLE) show type if it doesn't exist
      if (showTypeService.findByName("Premium Live Event (PLE)").isEmpty()) {
        showTypeService.createOrUpdateShowType(
            "Premium Live Event (PLE)", "Premium live event or pay-per-view format", 7, 3);
        createdCount++;
        log.info("Created show type: Premium Live Event (PLE)");
      }

      log.info("✅ Ensured {} show types exist", createdCount);
      return SyncResult.success("Show Types", createdCount, 0, 0);

    } catch (Exception e) {
      log.error("Failed to create default show types: {}", e.getMessage(), e);
      return SyncResult.failure(
          "Show Types", "Failed to create default show types: " + e.getMessage());
    }
  }
}
