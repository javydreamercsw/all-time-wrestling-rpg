package com.github.javydreamercsw.management.service.sync;

import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Monitors the health and performance of Notion sync operations. Provides health checks,
 * performance metrics, and proactive issue detection.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notion.sync.enabled", havingValue = "true")
public class SyncHealthMonitor implements HealthIndicator {

  private final NotionSyncProperties syncProperties;
  private final SyncProgressTracker progressTracker;

  // Health metrics
  private final AtomicInteger successfulSyncs = new AtomicInteger(0);
  private final AtomicInteger failedSyncs = new AtomicInteger(0);
  private final AtomicLong totalSyncTime = new AtomicLong(0);
  private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

  private LocalDateTime lastSuccessfulSync;
  private LocalDateTime lastFailedSync;
  private String lastErrorMessage;
  private final List<SyncMetric> recentMetrics = new ArrayList<>();

  @Override
  public Health health() {
    if (!syncProperties.isEnabled()) {
      // If sync is explicitly disabled, report UP with a specific detail
      log.info("Notion sync is disabled, reporting health as UP.");
      return Health.up().withDetail("message", "Notion Sync is disabled").build();
    }

    Health.Builder builder = new Health.Builder();

    try {
      // Check basic configuration
      if (!isConfigurationValid()) {
        return builder
            .down()
            .withDetail("error", "Invalid configuration")
            .withDetail(
                "notionToken",
                EnvironmentVariableUtil.isNotionTokenAvailable() ? "Available" : "Missing")
            .withDetail("syncEnabled", syncProperties.isEnabled())
            .build();
      }

      // Check recent sync performance
      if (hasRecentFailures()) {
        builder.down().withDetail("consecutiveFailures", consecutiveFailures.get());
        if (lastErrorMessage != null) {
          builder.withDetail("lastError", lastErrorMessage);
        }
        if (lastFailedSync != null) {
          builder.withDetail("lastFailedSync", lastFailedSync);
        }
      } else if (hasStaleSync()) {
        builder.down().withDetail("warning", "No recent successful sync");
        if (lastSuccessfulSync != null) {
          builder.withDetail("lastSuccessfulSync", lastSuccessfulSync);
        }
      } else {
        builder.up();
      }

      // Add performance metrics
      builder
          .withDetail("successfulSyncs", successfulSyncs.get())
          .withDetail("failedSyncs", failedSyncs.get())
          .withDetail("successRate", calculateSuccessRate())
          .withDetail("averageSyncTime", calculateAverageSyncTime())
          .withDetail("activeOperations", progressTracker.getActiveOperations().size());

      if (lastSuccessfulSync != null) {
        builder.withDetail("lastSuccessfulSync", lastSuccessfulSync);
      }

      return builder.build();
    } catch (Exception e) {
      log.error("Error checking sync health", e);
      return builder.down().withDetail("error", "Health check failed: " + e.getMessage()).build();
    }
  }

  /** Record a successful sync operation. */
  public void recordSuccess(String entityType, long durationMs, int itemCount) {
    successfulSyncs.incrementAndGet();
    totalSyncTime.addAndGet(durationMs);
    consecutiveFailures.set(0);
    lastSuccessfulSync = LocalDateTime.now();

    // Add to recent metrics
    synchronized (recentMetrics) {
      recentMetrics.add(new SyncMetric(entityType, true, durationMs, itemCount, null));

      // Keep only last 50 metrics
      if (recentMetrics.size() > 50) {
        recentMetrics.remove(0);
      }
    }

    log.debug("Recorded successful sync: {} ({} items, {}ms)", entityType, itemCount, durationMs);
  }

  /** Record a failed sync operation. */
  public void recordFailure(String entityType, String errorMessage) {
    failedSyncs.incrementAndGet();
    consecutiveFailures.incrementAndGet();
    lastFailedSync = LocalDateTime.now();
    lastErrorMessage = errorMessage;

    // Add to recent metrics
    synchronized (recentMetrics) {
      recentMetrics.add(new SyncMetric(entityType, false, 0, 0, errorMessage));

      // Keep only last 50 metrics
      if (recentMetrics.size() > 50) {
        recentMetrics.remove(0);
      }
    }

    log.warn("Recorded failed sync: {} - {}", entityType, errorMessage);
  }

  /** Get recent sync metrics for analysis. */
  public List<SyncMetric> getRecentMetrics() {
    synchronized (recentMetrics) {
      return new ArrayList<>(recentMetrics);
    }
  }

  /** Periodic health check and cleanup. */
  @Scheduled(fixedRate = 300000) // Every 5 minutes
  public void performHealthCheck() {
    try {
      // Log health status only if sync is enabled, otherwise the UP status with disabled message is
      // sufficient
      if (syncProperties.isEnabled()) {
        if (hasRecentFailures()) {
          log.warn("Sync health degraded: {} consecutive failures", consecutiveFailures.get());
        } else if (hasStaleSync()) {
          log.warn("Sync health warning: No recent successful sync (last: {})", lastSuccessfulSync);
        } else {
          log.debug("Sync health: OK (Success rate: {}%)", calculateSuccessRate());
        }
      }

      // Clean up old metrics
      cleanupOldMetrics();

    } catch (Exception e) {
      log.error("Error during health check", e);
    }
  }

  private boolean isConfigurationValid() {
    // This check now only matters if sync is enabled.
    // If disabled, the health check returns UP, not down for config.
    return !syncProperties.isEnabled() || EnvironmentVariableUtil.isNotionTokenAvailable();
  }

  private boolean hasRecentFailures() {
    return consecutiveFailures.get() >= 3;
  }

  private boolean hasStaleSync() {
    if (lastSuccessfulSync == null) {
      return true;
    }

    // Consider sync stale if no success in last 24 hours
    return ChronoUnit.HOURS.between(lastSuccessfulSync, LocalDateTime.now()) > 24;
  }

  private double calculateSuccessRate() {
    int total = successfulSyncs.get() + failedSyncs.get();
    if (total == 0) {
      return 100.0;
    }
    return (double) successfulSyncs.get() / total * 100.0;
  }

  private long calculateAverageSyncTime() {
    int successful = successfulSyncs.get();
    if (successful == 0) {
      return 0;
    }
    return totalSyncTime.get() / successful;
  }

  private void cleanupOldMetrics() {
    synchronized (recentMetrics) {
      // Remove metrics older than 24 hours
      LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
      recentMetrics.removeIf(metric -> metric.timestamp().isBefore(cutoff));
    }
  }

  /** Reset all metrics (useful for testing or maintenance). */
  public void resetMetrics() {
    successfulSyncs.set(0);
    failedSyncs.set(0);
    totalSyncTime.set(0);
    consecutiveFailures.set(0);
    lastSuccessfulSync = null;
    lastFailedSync = null;
    lastErrorMessage = null;

    synchronized (recentMetrics) {
      recentMetrics.clear();
    }

    log.info("Sync health metrics reset");
  }

  /** Get current health summary. */
  public SyncHealthSummary getHealthSummary() {
    return new SyncHealthSummary(
        successfulSyncs.get(),
        failedSyncs.get(),
        calculateSuccessRate(),
        calculateAverageSyncTime(),
        consecutiveFailures.get(),
        lastSuccessfulSync,
        lastFailedSync,
        lastErrorMessage,
        progressTracker.getActiveOperations().size());
  }

  /** Data class for sync metrics. */
  public record SyncMetric(
      String entityType,
      boolean success,
      long durationMs,
      int itemCount,
      String errorMessage,
      LocalDateTime timestamp) {
    public SyncMetric(
        String entityType, boolean success, long durationMs, int itemCount, String errorMessage) {
      this(entityType, success, durationMs, itemCount, errorMessage, LocalDateTime.now());
    }
  }

  /** Data class for health summary. */
  public record SyncHealthSummary(
      int successfulSyncs,
      int failedSyncs,
      double successRate,
      long averageSyncTime,
      int consecutiveFailures,
      LocalDateTime lastSuccessfulSync,
      LocalDateTime lastFailedSync,
      String lastErrorMessage,
      int activeOperations) {}
}
