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
package com.github.javydreamercsw.management.service.sync.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionPage;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.config.EntitySyncConfiguration;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.SyncValidationService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Enhanced base class for all sync services providing common functionality including rate limiting,
 * parallel processing, circuit breaking, and retry mechanisms.
 */
@Slf4j
public abstract class BaseSyncService {

  protected final ObjectMapper objectMapper;

  @Autowired protected SyncServiceDependencies syncServiceDependencies;

  protected BaseSyncService(
      @NonNull ObjectMapper objectMapper,
      @NonNull SyncServiceDependencies syncServiceDependencies) {
    this.objectMapper = objectMapper;
    this.syncServiceDependencies = syncServiceDependencies;
  }



  /**
   * Helper method to check if NotionHandler is available for operations.
   *
   * @return true if NotionHandler is available, false otherwise
   */
  public boolean isNotionHandlerAvailable() {
    return syncServiceDependencies.getNotionHandler() != null
        && EnvironmentVariableUtil.isNotionTokenAvailable();
  }

  /**
   * Creates a backup of the specified JSON file before sync operation.
   *
   * @param fileName The name of the JSON file to backup
   * @throws IOException if backup creation fails
   */
  protected void createBackup(@NonNull String fileName) throws IOException {
    Path originalFile = Paths.get("src/main/resources/" + fileName);

    if (!Files.exists(originalFile)) {
      log.debug("Original file {} does not exist, skipping backup", fileName);
      return;
    }

    // Create backup directory
    Path backupDir = Paths.get(syncServiceDependencies.getNotionSyncProperties().getBackup().getDirectory());
    Files.createDirectories(backupDir);

    // Create backup file with timestamp
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    String backupFileName = fileName.replace(".json", "_" + timestamp + ".json");
    Path backupFile = backupDir.resolve(backupFileName);

    // Copy original file to backup location
    Files.copy(originalFile, backupFile);
    log.info("Created backup: {}", backupFile);

    // Clean up old backups
    cleanupOldBackups(fileName);
  }

  /**
   * Removes old backup files, keeping only the configured maximum number.
   *
   * @param fileName The base file name to clean up backups for
   */
  private void cleanupOldBackups(@NonNull String fileName) {
    try {
      Path backupDir = Paths.get(syncServiceDependencies.getNotionSyncProperties().getBackup().getDirectory());
      if (!Files.exists(backupDir)) {
        return;
      }

      String baseFileName = fileName.replace(".json", "");
      List<Path> backupFiles =
          Files.list(backupDir)
              .filter(path -> path.getFileName().toString().startsWith(baseFileName + "_"))
              .sorted(
                  (p1, p2) ->
                      p2.getFileName()
                          .toString()
                          .compareTo(p1.getFileName().toString())) // Newest first
              .toList();

      int maxFiles = syncServiceDependencies.getNotionSyncProperties().getBackup().getMaxFiles();
      if (backupFiles.size() > maxFiles) {
        List<Path> filesToDelete = backupFiles.subList(maxFiles, backupFiles.size());
        for (Path fileToDelete : filesToDelete) {
          Files.delete(fileToDelete);
          log.debug("Deleted old backup: {}", fileToDelete);
        }
        log.info("Cleaned up {} old backup files for {}", filesToDelete.size(), fileName);
      }

    } catch (IOException e) {
      log.warn("Failed to cleanup old backups for {}: {}", fileName, e.getMessage());
    }
  }

  /** Extracts name from any NotionPage type using raw properties. */
  protected String extractNameFromNotionPage(@NonNull NotionPage page) {
    if (page.getRawProperties() != null) {
      // Try different possible property names for name
      Object name = page.getRawProperties().get("Name");
      if (name == null) {
        name = page.getRawProperties().get("Title"); // Alternative name property
      }

      if (name != null) {
        String nameStr = extractTextFromProperty(name);
        if (nameStr != null && !nameStr.trim().isEmpty() && !"N/A".equals(nameStr)) {
          return nameStr.trim();
        }
      }

      log.debug("Name property not found or empty for page: {}", page.getId());
    }
    return "Unknown";
  }

  /** Extracts description from any NotionPage type using raw properties. */
  protected String extractDescriptionFromNotionPage(@NonNull NotionPage page) {
    if (page.getRawProperties() != null) {
      Object description = page.getRawProperties().get("Description");
      return description != null ? description.toString() : "";
    }
    return "";
  }

  /** Extracts text content from a Notion property object. */
  protected String extractTextFromProperty(Object property) {
    if (property == null) {
      return null;
    }

    // Handle Map objects that mimic Notion's structure
    if (property instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) property;
      if (map.containsKey("title")) {
        Object titleObj = map.get("title");
        if (titleObj instanceof List) {
          @SuppressWarnings("unchecked")
          List<Object> titleList = (List<Object>) titleObj;
          if (!titleList.isEmpty() && titleList.get(0) instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> titleMap = (Map<String, Object>) titleList.get(0);
            if (titleMap.containsKey("text") && titleMap.get("text") instanceof Map) {
              @SuppressWarnings("unchecked")
              Map<String, Object> textMap = (Map<String, Object>) titleMap.get("text");
              if (textMap.containsKey("content")) {
                return textMap.get("content").toString();
              }
            }
          }
        }
      }
    }

    // Handle PageProperty objects (from Notion API)
    if (property instanceof notion.api.v1.model.pages.PageProperty pageProperty) {

      // Handle title properties
      if (pageProperty.getTitle() != null && !pageProperty.getTitle().isEmpty()) {
        return pageProperty.getTitle().get(0).getPlainText();
      }

      // Handle rich text properties
      if (pageProperty.getRichText() != null && !pageProperty.getRichText().isEmpty()) {
        return pageProperty.getRichText().get(0).getPlainText();
      }

      // Handle select properties
      if (pageProperty.getSelect() != null) {
        return pageProperty.getSelect().getName();
      }

      // Handle other property types as needed
      log.debug("Unhandled PageProperty type: {}", pageProperty.getType());
      return null;
    }

    // Handle simple string values
    String str = property.toString().trim();

    // Avoid returning the entire PageProperty string representation
    if (str.startsWith("PageProperty(")) {
      log.warn("Property extraction returned PageProperty object string - this indicates a bug");
      return null;
    }

    return str.isEmpty() ? null : str;
  }

  /** Extracts a property value as a string from raw properties map. */
  protected String extractPropertyAsString(
      @NonNull java.util.Map<String, Object> rawProperties, @NonNull String propertyName) {

    Object property = rawProperties.get(propertyName);
    return extractTextFromProperty(property);
  }

  /**
   * Validates that NOTION_TOKEN is available for sync operations.
   *
   * @param entityType The entity type being synced (for error messages)
   * @return true if token is available, false otherwise
   */
  public boolean validateNotionToken(@NonNull String entityType) {
    SyncValidationService.ValidationResult result =
        syncServiceDependencies.getValidationService().validateSyncPrerequisites();
    if (!result.isValid()) {
      log.warn("Notion token validation failed for {}: {}", entityType, result.getErrors());
      return false;
    }
    return true;
  }

  /**
   * Process a list of items with controlled parallel processing and rate limiting.
   *
   * @param items List of items to process
   * @param processor Function to process each item
   * @param batchSize Number of items to process in each batch
   * @param operationId Operation ID for progress tracking
   * @param progressStep Current progress step
   * @param description Description for progress updates
   * @return List of processed results
   */
  protected <T, R> List<R> processWithControlledParallelism(
      List<T> items,
      Function<T, R> processor,
      int batchSize,
      String operationId,
      int progressStep,
      String description) {
    return processWithControlledParallelism(
        items, processor, batchSize, operationId, progressStep, description, null);
  }

  @SneakyThrows
  protected <T, R> List<R> processWithControlledParallelism(
      List<T> items,
      Function<T, R> processor,
      int batchSize,
      String operationId,
      int progressStep,
      String description,
      java.util.function.Consumer<String> messageConsumer) {

    if (items.isEmpty()) {
      return List.of();
    }

    log.info(
        "Processing {} items with controlled parallelism (batch size: {})",
        items.size(),
        batchSize);
    List<R> allResults = new java.util.ArrayList<>();

    for (int i = 0; i < items.size(); i += batchSize) {
      int endIndex = Math.min(i + batchSize, items.size());
      List<T> batch = items.subList(i, endIndex);

      log.debug("Processing batch {}-{} of {}", i + 1, endIndex, items.size());

      // Process batch with parallel execution and rate limiting
      List<CompletableFuture<R>> futures =
          batch.stream()
              .map(
                  item ->
                      CompletableFuture.supplyAsync(
                          () -> {
                            try {
                              syncServiceDependencies.getRateLimitService().acquirePermit();
                              return processor.apply(item);
                            } catch (InterruptedException e) {
                              Thread.currentThread().interrupt();
                              String msg = "Interrupted while processing item";
                              log.error(msg);
                              if (messageConsumer != null) messageConsumer.accept(msg);
                              throw new RuntimeException("Processing interrupted", e);
                            } catch (Exception e) {
                              String msg = "Error processing item: " + e.getMessage();
                              log.error(msg);
                              if (messageConsumer != null) messageConsumer.accept(msg);
                              throw new RuntimeException("Processing failed", e);
                            }
                          },
                          syncServiceDependencies.getNotionApiExecutor().getSyncExecutorService()))
              .toList();

      // Wait for batch completion with timeout
      try {
        List<R> batchResults =
            futures.stream()
                .map(
                    future -> {
                      try {
                        return future.get(2, TimeUnit.MINUTES);
                      } catch (Exception e) {
                        String msg = "Failed to complete processing future: " + e.getMessage();
                        log.error(msg);
                        if (messageConsumer != null) messageConsumer.accept(msg);
                        throw new RuntimeException("Future completion failed", e);
                      }
                    })
                .toList();

        allResults.addAll(batchResults);

        // Update progress
        int processedCount = Math.min(endIndex, items.size());
        double progressPercent = (double) processedCount / items.size() * 100;
        syncServiceDependencies.getProgressTracker().updateProgress(
            operationId,
            progressStep,
            String.format(
                "%s %d/%d items (%.1f%%)",
                description, processedCount, items.size(), progressPercent));

        // Small delay between batches to be nice to the API
        if (endIndex < items.size()) {
          CompletableFuture<Void> delay =
              CompletableFuture.runAsync(
                  () -> {},
                  CompletableFuture.delayedExecutor(
                      500,
                      TimeUnit.MILLISECONDS,
                      syncServiceDependencies.getNotionApiExecutor().getSyncExecutorService()));
          delay.get();
        }

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        String msg = "Interrupted while processing batch";
        log.error(msg);
        if (messageConsumer != null) messageConsumer.accept(msg);
        throw new RuntimeException("Batch processing interrupted", e);
      }
    }

    return allResults;
  }

  /** Represents the result of a synchronization operation. */
  @Getter
  public static class SyncResult {
    // Getters
    private final boolean success;
    private final String entityType;
    private final int syncedCount;
    private final int createdCount;
    private final int updatedCount;
    private final int errorCount;
    private final String errorMessage;
    private final List<String> messages = new java.util.concurrent.CopyOnWriteArrayList<>();

    private SyncResult(
        boolean success,
        @NonNull String entityType,
        int createdCount,
        int updatedCount,
        int errorCount,
        String errorMessage) {
      this.success = success;
      this.entityType = entityType;
      this.syncedCount = createdCount + updatedCount;
      this.createdCount = createdCount;
      this.updatedCount = updatedCount;
      this.errorCount = errorCount;
      this.errorMessage = errorMessage;
    }

    public static SyncResult success(
        @NonNull String entityType, int createdCount, int updatedCount, int errorCount) {
      return new SyncResult(true, entityType, createdCount, updatedCount, errorCount, null);
    }

    public static SyncResult failure(@NonNull String entityType, String errorMessage) {
      return new SyncResult(false, entityType, 0, 0, 0, errorMessage);
    }

    public static SyncResult unsupported(@NonNull String entityType, String errorMessage) {
      return new SyncResult(false, entityType, 0, 0, 0, errorMessage);
    }

    public String getSummary() {
      if (success) {
        return String.format(
            "%s: %d synced (%d created, %d updated), %d errors",
            entityType, syncedCount, createdCount, updatedCount, errorCount);
      } else {
        return String.format("%s: failed - %s", entityType, errorMessage);
      }
    }

    @Override
    public String toString() {
      if (success) {
        return String.format(
            "SyncResult{success=true, entityType='%s', syncedCount=%d, createdCount=%d,"
                + " updatedCount=%d, errorCount=%d}",
            entityType, syncedCount, createdCount, updatedCount, errorCount);
      } else {
        return String.format(
            "SyncResult{success=false, entityType='%s', errorMessage='%s'}",
            entityType, errorMessage);
      }
    }
  }
}
