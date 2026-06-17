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
import com.github.javydreamercsw.base.ai.notion.NotionApiExecutor;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.SyncValidationService;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Enhanced base class for all sync services providing common functionality including rate limiting,
 * parallel processing, circuit breaking, and retry mechanisms.
 */
@Slf4j
public abstract class BaseSyncService {

  protected final ObjectMapper objectMapper;

  @Autowired protected SyncServiceDependencies syncServiceDependencies;

  protected final NotionApiExecutor notionApiExecutor;

  protected BaseSyncService(
      @NonNull final ObjectMapper objectMapper,
      @NonNull final SyncServiceDependencies syncServiceDependencies,
      @NonNull final NotionApiExecutor notionApiExecutor) {
    this.objectMapper = objectMapper;
    this.syncServiceDependencies = syncServiceDependencies;
    this.notionApiExecutor = notionApiExecutor;
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
   * Executes an API call with rate limiting.
   *
   * @param apiCall The API call to execute.
   * @return The result of the API call.
   */
  @SneakyThrows
  protected <T> T executeWithRateLimit(@NonNull final java.util.function.Supplier<T> apiCall) {
    syncServiceDependencies.getRateLimitService().acquirePermit();
    return apiCall.get();
  }

  /**
   * Validates that NOTION_TOKEN is available for sync operations.
   *
   * @param entityType The entity type being synced (for error messages)
   * @return true if token is available, false otherwise
   */
  public boolean validateNotionToken(@NonNull final String entityType) {
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
      final List<T> items,
      final Function<T, R> processor,
      final int batchSize,
      final String operationId,
      final int progressStep,
      final String description) {
    return processWithControlledParallelism(
        items, processor, batchSize, operationId, progressStep, description, null);
  }

  @SneakyThrows
  protected <T, R> List<R> processWithControlledParallelism(
      final List<T> items,
      final Function<T, R> processor,
      final int batchSize,
      final String operationId,
      final int progressStep,
      final String description,
      final java.util.function.Consumer<String> messageConsumer) {

    if (items.isEmpty()) {
      return List.of();
    }

    log.info(
        "Processing {} items with controlled parallelism (batch size: {})",
        items.size(),
        batchSize);
    List<R> allResults = new java.util.ArrayList<>();

    // Capture the current authentication to propagate to async threads
    // We capture Authentication instead of SecurityContext to avoid issues with context clearing
    // or thread-local storage differences in different environments.
    org.springframework.security.core.Authentication authentication =
        SecurityContextHolder.getContext().getAuthentication();

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
                            java.util.function.Supplier<R> task =
                                () -> {
                                  try {
                                    syncServiceDependencies.getRateLimitService().acquirePermit();
                                    return processor.apply(item);
                                  } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    String msg = "Interrupted while processing item";
                                    log.error(msg);
                                    if (messageConsumer != null) {
                                      messageConsumer.accept(msg);
                                    }
                                    throw new RuntimeException("Processing interrupted", e);
                                  } catch (Exception e) {
                                    String msg = "Error processing item: " + e.getMessage();
                                    log.error(msg);
                                    if (messageConsumer != null) {
                                      messageConsumer.accept(msg);
                                    }
                                    throw new RuntimeException("Processing failed", e);
                                  }
                                };

                            if (authentication != null) {
                              SecurityContext context = SecurityContextHolder.createEmptyContext();
                              context.setAuthentication(authentication);
                              SecurityContextHolder.setContext(context);
                              try {
                                return task.get();
                              } finally {
                                SecurityContextHolder.clearContext();
                              }
                            } else {
                              return com.github.javydreamercsw.base.security.GeneralSecurityUtils
                                  .runAsAdmin(task);
                            }
                          },
                          notionApiExecutor.getSyncExecutorService()))
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
                        if (messageConsumer != null) {
                          messageConsumer.accept(msg);
                        }
                        throw new RuntimeException("Future completion failed", e);
                      }
                    })
                .toList();

        allResults.addAll(batchResults);

        // Update progress
        int processedCount = Math.min(endIndex, items.size());
        double progressPercent = (double) processedCount / items.size() * 100;
        syncServiceDependencies
            .getProgressTracker()
            .updateProgress(
                operationId,
                progressStep,
                description.formatted(processedCount, items.size())
                    + " (%.1f%%)".formatted(progressPercent));

        // Small delay between batches to be nice to the API
        if (endIndex < items.size()) {
          CompletableFuture<Void> delay =
              CompletableFuture.runAsync(
                  () -> {},
                  CompletableFuture.delayedExecutor(
                      500, TimeUnit.MILLISECONDS, notionApiExecutor.getSyncExecutorService()));
          delay.get();
        }

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        String msg = "Interrupted while processing batch";
        log.error(msg);
        if (messageConsumer != null) {
          messageConsumer.accept(msg);
        }
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
        final boolean success,
        @NonNull final String entityType,
        final int createdCount,
        final int updatedCount,
        final int errorCount,
        final String errorMessage) {
      this.success = success;
      this.entityType = entityType;
      this.syncedCount = createdCount + updatedCount;
      this.createdCount = createdCount;
      this.updatedCount = updatedCount;
      this.errorCount = errorCount;
      this.errorMessage = errorMessage;
    }

    public static SyncResult success(
        @NonNull final String entityType,
        final int createdCount,
        final int updatedCount,
        final int errorCount) {
      return new SyncResult(true, entityType, createdCount, updatedCount, errorCount, null);
    }

    public static SyncResult failure(@NonNull final String entityType, final String errorMessage) {
      return new SyncResult(false, entityType, 0, 0, 0, errorMessage);
    }

    public String getSummary() {
      if (success) {
        return "%s: %d synced (%d created, %d updated), %d errors"
            .formatted(entityType, syncedCount, createdCount, updatedCount, errorCount);
      } else {
        return "%s: failed - %s".formatted(entityType, errorMessage);
      }
    }

    @Override
    public String toString() {
      if (success) {
        return """
        SyncResult{success=true, entityType='%s', syncedCount=%d, createdCount=%d,\
         updatedCount=%d, errorCount=%d}\
        """
            .formatted(entityType, syncedCount, createdCount, updatedCount, errorCount);
      } else {
        return "SyncResult{success=false, entityType='%s', errorMessage='%s'}"
            .formatted(entityType, errorMessage);
      }
    }
  }
}
