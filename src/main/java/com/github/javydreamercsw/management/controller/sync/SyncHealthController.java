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
package com.github.javydreamercsw.management.controller.sync;

import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor.SyncHealthSummary;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor.SyncMetric;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for monitoring and managing sync health. Provides endpoints for health checks,
 * metrics, and diagnostics.
 */
@RestController
@RequestMapping("/api/sync/health")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "notion.sync.enabled", havingValue = "true")
public class SyncHealthController {

  private final SyncHealthMonitor healthMonitor;

  /** Get overall sync health status. */
  @GetMapping
  public ResponseEntity<Health> getHealth() {
    try {
      Health health = healthMonitor.health();
      return ResponseEntity.ok(health);
    } catch (Exception e) {
      log.error("Error getting sync health", e);
      return ResponseEntity.internalServerError()
          .body(
              Health.down()
                  .withDetail("error", "Failed to get health status: " + e.getMessage())
                  .build());
    }
  }

  /** Get detailed health summary with metrics. */
  @GetMapping("/summary")
  public ResponseEntity<SyncHealthSummary> getHealthSummary() {
    try {
      SyncHealthSummary summary = healthMonitor.getHealthSummary();
      return ResponseEntity.ok(summary);
    } catch (Exception e) {
      log.error("Error getting sync health summary", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** Get recent sync metrics for analysis. */
  @GetMapping("/metrics")
  public ResponseEntity<List<SyncMetric>> getMetrics() {
    try {
      List<SyncMetric> metrics = healthMonitor.getRecentMetrics();
      return ResponseEntity.ok(metrics);
    } catch (Exception e) {
      log.error("Error getting sync metrics", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** Get sync performance statistics. */
  @GetMapping("/stats")
  public ResponseEntity<Map<String, Object>> getStats() {
    try {
      SyncHealthSummary summary = healthMonitor.getHealthSummary();
      List<SyncMetric> metrics = healthMonitor.getRecentMetrics();

      // Calculate additional statistics
      long totalOperations = summary.successfulSyncs() + summary.failedSyncs();
      double failureRate =
          totalOperations > 0 ? (double) summary.failedSyncs() / totalOperations * 100.0 : 0.0;

      // Recent performance (last 10 operations)
      List<SyncMetric> recentMetrics = metrics.stream().limit(10).toList();

      long recentSuccesses = recentMetrics.stream().mapToLong(m -> m.success() ? 1 : 0).sum();

      double recentSuccessRate =
          recentMetrics.isEmpty() ? 100.0 : (double) recentSuccesses / recentMetrics.size() * 100.0;

      Map<String, Object> stats =
          Map.of(
              "totalOperations",
              totalOperations,
              "successRate",
              summary.successRate(),
              "failureRate",
              failureRate,
              "averageSyncTime",
              summary.averageSyncTime(),
              "consecutiveFailures",
              summary.consecutiveFailures(),
              "recentSuccessRate",
              recentSuccessRate,
              "activeOperations",
              summary.activeOperations(),
              "lastSuccessfulSync",
              summary.lastSuccessfulSync(),
              "lastFailedSync",
              summary.lastFailedSync());

      return ResponseEntity.ok(stats);
    } catch (Exception e) {
      log.error("Error getting sync statistics", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** Reset health metrics (useful for testing or maintenance). */
  @PostMapping("/reset")
  public ResponseEntity<Map<String, String>> resetMetrics() {
    try {
      healthMonitor.resetMetrics();
      log.info("Sync health metrics reset via API");

      return ResponseEntity.ok(
          Map.of("status", "success", "message", "Health metrics reset successfully"));
    } catch (Exception e) {
      log.error("Error resetting sync metrics", e);
      return ResponseEntity.internalServerError()
          .body(Map.of("status", "error", "message", "Failed to reset metrics: " + e.getMessage()));
    }
  }

  /** Get sync health recommendations based on current metrics. */
  @GetMapping("/recommendations")
  public ResponseEntity<List<String>> getRecommendations() {
    try {
      SyncHealthSummary summary = healthMonitor.getHealthSummary();
      List<String> recommendations = generateRecommendations(summary);

      return ResponseEntity.ok(recommendations);
    } catch (Exception e) {
      log.error("Error generating sync recommendations", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** Generate health recommendations based on current metrics. */
  private List<String> generateRecommendations(SyncHealthSummary summary) {
    List<String> recommendations = new java.util.ArrayList<>();

    // Check success rate
    if (summary.successRate() < 90.0) {
      recommendations.add("Success rate is below 90%. Check error logs and network connectivity.");
    }

    // Check consecutive failures
    if (summary.consecutiveFailures() >= 3) {
      recommendations.add(
          "Multiple consecutive failures detected. Check NOTION_TOKEN and API connectivity.");
    }

    // Check average sync time
    if (summary.averageSyncTime() > 30000) { // 30 seconds
      recommendations.add(
          "Average sync time is high. Consider optimizing sync operations or checking network"
              + " performance.");
    }

    // Check stale syncs
    if (summary.lastSuccessfulSync() != null) {
      long hoursSinceLastSuccess =
          java.time.temporal.ChronoUnit.HOURS.between(
              summary.lastSuccessfulSync(), java.time.LocalDateTime.now());

      if (hoursSinceLastSuccess > 24) {
        recommendations.add(
            "No successful sync in over 24 hours. Check sync scheduler and configuration.");
      } else if (hoursSinceLastSuccess > 6) {
        recommendations.add("No recent successful sync. Monitor sync operations closely.");
      }
    }

    // Check active operations
    if (summary.activeOperations() > 5) {
      recommendations.add(
          "Many active sync operations. Check for stuck operations or reduce sync frequency.");
    }

    // Default recommendation if all is well
    if (recommendations.isEmpty()) {
      recommendations.add("Sync health is good. Continue monitoring for any changes.");
    }

    return recommendations;
  }

  /** Perform a health check and return simple status. */
  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getStatus() {
    try {
      Health health = healthMonitor.health();
      SyncHealthSummary summary = healthMonitor.getHealthSummary();

      String status =
          switch (health.getStatus().getCode()) {
            case "UP" -> "HEALTHY";
            case "DOWN" -> "UNHEALTHY";
            case "OUT_OF_SERVICE" -> "OUT_OF_SERVICE";
            default -> "DEGRADED";
          };

      Map<String, Object> statusResponse =
          Map.of(
              "status",
              status,
              "successRate",
              summary.successRate(),
              "lastSync",
              summary.lastSuccessfulSync() != null ? summary.lastSuccessfulSync() : "Never",
              "activeOperations",
              summary.activeOperations(),
              "message",
              health.getDetails().getOrDefault("error", "OK"));

      return ResponseEntity.ok(statusResponse);
    } catch (Exception e) {
      log.error("Error getting sync status", e);
      return ResponseEntity.internalServerError()
          .body(Map.of("status", "ERROR", "message", "Failed to get status: " + e.getMessage()));
    }
  }
}
