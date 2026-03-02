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
package com.github.javydreamercsw.base.ai.notion;

import com.github.javydreamercsw.base.config.NotionSyncProperties;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
@Getter // Added to generate getters for all fields
public class NotionApiExecutor {

  private final NotionHandler notionHandler;
  private final NotionRateLimitService rateLimitService;
  private final NotionSyncProperties syncProperties; // Keep this field to use in newFixedThreadPool
  private final ExecutorService syncExecutorService;
  private final SyncProgressTracker syncProgressTracker;

  @Autowired
  public NotionApiExecutor(
      @NonNull NotionHandler notionHandler,
      @NonNull NotionRateLimitService rateLimitService,
      @NonNull NotionSyncProperties syncProperties,
      @NonNull SyncProgressTracker syncProgressTracker) {
    this.notionHandler = notionHandler;
    this.rateLimitService = rateLimitService;
    this.syncProperties = syncProperties;
    this.syncExecutorService =
        Executors.newFixedThreadPool(Math.max(1, this.syncProperties.getParallelThreads()));
    this.syncProgressTracker = syncProgressTracker;
  }

  /** Execute a Notion API call with proper rate limiting. */
  public <T> T executeWithRateLimit(@NonNull Supplier<T> apiCall) {
    return executeWithRateLimit(null, apiCall);
  }

  /** Execute a Notion API call with proper rate limiting and optional progress logging. */
  public <T> T executeWithRateLimit(@Nullable String operationId, @NonNull Supplier<T> apiCall) {
    try {
      if (operationId != null && rateLimitService.isRateLimited()) {
        syncProgressTracker.addLogMessage(operationId, "🚦 Rate limit active. Pausing...", "WARN");
      }
      rateLimitService.acquirePermit();
      return apiCall.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Rate limit service interrupted", e);
    }
  }

  @PreDestroy
  public void shutdownExecutor() {
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
}
