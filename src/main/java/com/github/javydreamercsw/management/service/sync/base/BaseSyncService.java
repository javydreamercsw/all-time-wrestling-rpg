package com.github.javydreamercsw.management.service.sync.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionPage;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.service.sync.CircuitBreakerService;
import com.github.javydreamercsw.management.service.sync.DataIntegrityChecker;
import com.github.javydreamercsw.management.service.sync.NotionRateLimitService;
import com.github.javydreamercsw.management.service.sync.RetryService;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.SyncTransactionManager;
import com.github.javydreamercsw.management.service.sync.SyncValidationService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Enhanced base class for all sync services providing common functionality including rate limiting,
 * parallel processing, circuit breaking, and retry mechanisms.
 */
@Slf4j
public abstract class BaseSyncService {

  protected final ObjectMapper objectMapper;
  protected final NotionSyncProperties syncProperties;

  // Session-based tracking to prevent duplicate syncing during batch operations
  private final ThreadLocal<Set<String>> currentSyncSession = ThreadLocal.withInitial(HashSet::new);

  // Controlled parallelism executor for all sync operations
  private final ExecutorService syncExecutorService = Executors.newFixedThreadPool(3);

  // Optional NotionHandler for integration tests
  @Autowired(required = false)
  public NotionHandler notionHandler;

  // Enhanced sync infrastructure services - autowired
  @Autowired public SyncProgressTracker progressTracker;
  @Autowired public SyncHealthMonitor healthMonitor;
  @Autowired public RetryService retryService;
  @Autowired public CircuitBreakerService circuitBreakerService;
  @Autowired public SyncValidationService validationService;
  @Autowired public SyncTransactionManager syncTransactionManager;
  @Autowired public DataIntegrityChecker integrityChecker;
  @Autowired public NotionRateLimitService rateLimitService;

  protected BaseSyncService(ObjectMapper objectMapper, NotionSyncProperties syncProperties) {
    this.objectMapper = objectMapper;
    this.syncProperties = syncProperties;
  }

  /**
   * Helper method to check if NotionHandler is available for operations.
   *
   * @return true if NotionHandler is available, false otherwise
   */
  protected boolean isNotionHandlerAvailable() {
    return notionHandler != null;
  }

  /**
   * Checks if an entity has already been synced in the current session.
   *
   * @param entityName The name of the entity to check
   * @return true if already synced, false otherwise
   */
  protected boolean isAlreadySyncedInSession(@NonNull String entityName) {
    return currentSyncSession.get().contains(entityName.toLowerCase());
  }

  /**
   * Marks an entity as synced in the current session.
   *
   * @param entityName The name of the entity to mark as synced
   */
  protected void markAsSyncedInSession(@NonNull String entityName) {
    currentSyncSession.get().add(entityName.toLowerCase());
    log.debug("🏷️ Marked '{}' as synced in current session", entityName);
  }

  /** Clears the current sync session (should be called at the start of batch operations). */
  public void clearSyncSession() {
    currentSyncSession.get().clear();
    log.debug("🧹 Cleared sync session");
  }

  /** Cleans up the sync session thread local (should be called at the end of operations). */
  public void cleanupSyncSession() {
    currentSyncSession.remove();
    log.debug("🗑️ Cleaned up sync session thread local");
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
    Path backupDir = Paths.get(syncProperties.getBackup().getDirectory());
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
      Path backupDir = Paths.get(syncProperties.getBackup().getDirectory());
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

      int maxFiles = syncProperties.getBackup().getMaxFiles();
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
  protected boolean validateNotionToken(@NonNull String entityType) {
    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      log.warn("NOTION_TOKEN not available. Cannot sync {} from Notion.", entityType);
      return false;
    }
    return true;
  }

  /**
   * Execute a sync operation with full resilience patterns: circuit breaker, retry, and rate
   * limiting.
   */
  protected <T> T executeResilientSync(
      String entityType, String operationId, Function<Integer, T> syncOperation) throws Exception {
    return circuitBreakerService.execute(
        entityType,
        () ->
            retryService.executeWithRetry(
                entityType,
                (int attemptNumber) -> {
                  log.debug("🔄 {} sync attempt {} starting", entityType, attemptNumber);
                  rateLimitService.acquirePermit();
                  return syncOperation.apply(attemptNumber);
                }));
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
                              rateLimitService.acquirePermit();
                              return processor.apply(item);
                            } catch (InterruptedException e) {
                              Thread.currentThread().interrupt();
                              log.error("Interrupted while processing item");
                              throw new RuntimeException("Processing interrupted", e);
                            } catch (Exception e) {
                              log.error("Error processing item: {}", e.getMessage());
                              throw new RuntimeException("Processing failed", e);
                            }
                          },
                          syncExecutorService))
              .toList();

      // Wait for batch completion with timeout
      try {
        List<R> batchResults =
            futures.stream()
                .map(
                    future -> {
                      try {
                        return future.get(30, TimeUnit.SECONDS);
                      } catch (Exception e) {
                        log.error("Failed to complete processing future: {}", e.getMessage());
                        throw new RuntimeException("Future completion failed", e);
                      }
                    })
                .toList();

        allResults.addAll(batchResults);

        // Update progress
        int processedCount = Math.min(endIndex, items.size());
        double progressPercent = (double) processedCount / items.size() * 100;
        progressTracker.updateProgress(
            operationId,
            progressStep,
            String.format(
                "%s %d/%d items (%.1f%%)",
                description, processedCount, items.size(), progressPercent));

        // Small delay between batches to be nice to the API
        if (endIndex < items.size()) {
          Thread.sleep(500);
        }

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.error("Interrupted while processing batch");
        throw new RuntimeException("Batch processing interrupted", e);
      }
    }

    return allResults;
  }

  /** Execute a Notion API call with proper rate limiting. */
  protected <T> T executeWithRateLimit(Supplier<T> apiCall) throws InterruptedException {
    rateLimitService.acquirePermit();
    return apiCall.get();
  }

  /** Shutdown the executor service (should be called during application shutdown). */
  public void shutdown() {
    syncExecutorService.shutdown();
    try {
      if (!syncExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
        syncExecutorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      syncExecutorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /** Represents the result of a synchronization operation. */
  @Getter
  public static class SyncResult {
    // Getters
    private final boolean success;
    private final String entityType;
    private final int syncedCount;
    private final int errorCount;
    private final String errorMessage;

    private SyncResult(
        boolean success,
        @NonNull String entityType,
        int syncedCount,
        int errorCount,
        String errorMessage) {
      this.success = success;
      this.entityType = entityType;
      this.syncedCount = syncedCount;
      this.errorCount = errorCount;
      this.errorMessage = errorMessage;
    }

    public static SyncResult success(@NonNull String entityType, int syncedCount, int errorCount) {
      return new SyncResult(true, entityType, syncedCount, errorCount, null);
    }

    public static SyncResult failure(@NonNull String entityType, String errorMessage) {
      return new SyncResult(false, entityType, 0, 0, errorMessage);
    }

    public String getSummary() {
      if (success) {
        return String.format("%s: %d synced, %d errors", entityType, syncedCount, errorCount);
      } else {
        return String.format("%s: failed - %s", entityType, errorMessage);
      }
    }

    @Override
    public String toString() {
      if (success) {
        return String.format(
            "SyncResult{success=true, entityType='%s', syncedCount=%d, errorCount=%d}",
            entityType, syncedCount, errorCount);
      } else {
        return String.format(
            "SyncResult{success=false, entityType='%s', errorMessage='%s'}",
            entityType, errorMessage);
      }
    }
  }
}
