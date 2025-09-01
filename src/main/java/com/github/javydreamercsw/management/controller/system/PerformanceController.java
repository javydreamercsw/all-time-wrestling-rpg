package com.github.javydreamercsw.management.controller.system;

import com.github.javydreamercsw.management.config.CacheConfig.CacheMonitor;
import com.github.javydreamercsw.management.config.DatabaseOptimizationConfig;
import com.github.javydreamercsw.management.service.performance.PerformanceMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for performance monitoring and optimization endpoints. Provides access to performance
 * metrics, cache management, and optimization tools.
 */
@RestController
@RequestMapping("/api/system/performance")
@RequiredArgsConstructor
@Tag(name = "System", description = "System health, configuration, and administrative operations")
public class PerformanceController {

  private final PerformanceMonitoringService performanceMonitoringService;
  private final CacheMonitor cacheMonitor;
  private final DatabaseOptimizationConfig databaseOptimizationConfig;

  @Operation(
      summary = "Get performance metrics",
      description =
          "Returns comprehensive performance metrics including memory usage, thread counts, cache"
              + " statistics, and operation timings")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Performance metrics retrieved successfully")
      })
  @GetMapping("/metrics")
  public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
    Map<String, Object> metrics = performanceMonitoringService.getPerformanceMetrics();
    return ResponseEntity.ok(metrics);
  }

  @Operation(
      summary = "Get performance recommendations",
      description =
          "Returns performance optimization recommendations based on current system metrics")
  @GetMapping("/recommendations")
  public ResponseEntity<Map<String, String>> getPerformanceRecommendations() {
    Map<String, String> recommendations =
        performanceMonitoringService.getPerformanceRecommendations();
    return ResponseEntity.ok(recommendations);
  }

  @Operation(
      summary = "Reset performance metrics",
      description =
          "Resets all performance counters and timers - useful for testing or starting fresh"
              + " monitoring")
  @DeleteMapping("/metrics")
  public ResponseEntity<Map<String, String>> resetPerformanceMetrics() {
    performanceMonitoringService.resetMetrics();
    return ResponseEntity.ok(
        Map.of("status", "success", "message", "Performance metrics reset successfully"));
  }

  @Operation(
      summary = "Clear all caches",
      description = "Clears all application caches to free memory and force fresh data loading")
  @DeleteMapping("/cache")
  public ResponseEntity<Map<String, String>> clearAllCaches() {
    cacheMonitor.clearAllCaches();
    return ResponseEntity.ok(
        Map.of("status", "success", "message", "All caches cleared successfully"));
  }

  @Operation(
      summary = "Get cache statistics",
      description = "Returns detailed cache usage statistics and performance metrics")
  @GetMapping("/cache/stats")
  public ResponseEntity<Map<String, Object>> getCacheStatistics() {
    // This would return detailed cache statistics
    // For now, we'll return basic information
    Map<String, Object> cacheStats =
        Map.of(
            "status",
            "active",
            "cacheCount",
            13,
            "message",
            "Cache statistics logged to application logs");

    // Log cache statistics
    cacheMonitor.logCacheStatistics();

    return ResponseEntity.ok(cacheStats);
  }

  @Operation(
      summary = "Warm up caches",
      description = "Pre-loads frequently accessed data into caches to improve performance")
  @PostMapping("/cache/warmup")
  public ResponseEntity<Map<String, String>> warmUpCaches() {
    cacheMonitor.warmUpCaches();
    return ResponseEntity.ok(Map.of("status", "success", "message", "Cache warm-up initiated"));
  }

  @Operation(
      summary = "Analyze query performance",
      description = "Analyzes database query performance and suggests optimizations")
  @PostMapping("/database/analyze")
  public ResponseEntity<Map<String, String>> analyzeDatabasePerformance() {
    databaseOptimizationConfig.analyzeQueryPerformance();
    return ResponseEntity.ok(
        Map.of(
            "status",
            "success",
            "message",
            "Database performance analysis completed - check logs for details"));
  }

  @Operation(
      summary = "Get system resource usage",
      description =
          "Returns current system resource usage including memory, CPU, and thread information")
  @GetMapping("/resources")
  public ResponseEntity<Map<String, Object>> getSystemResources() {
    Map<String, Object> metrics = performanceMonitoringService.getPerformanceMetrics();

    // Extract just the resource-related metrics
    Map<String, Object> resources =
        Map.of(
            "memory",
            metrics.get("memory"),
            "threads",
            metrics.get("threads"),
            "activeOperations",
            metrics.get("activeOperations"));

    return ResponseEntity.ok(resources);
  }

  @Operation(
      summary = "Get operation statistics",
      description =
          "Returns statistics about application operations including counts, timings, and error"
              + " rates")
  @GetMapping("/operations")
  public ResponseEntity<Map<String, Object>> getOperationStatistics() {
    Map<String, Object> metrics = performanceMonitoringService.getPerformanceMetrics();

    // Extract operation-related metrics
    Map<String, Object> operations =
        Map.of(
            "counters",
            metrics.get("counters"),
            "timers",
            metrics.get("timers"),
            "activeOperations",
            metrics.get("activeOperations"));

    return ResponseEntity.ok(operations);
  }

  @Operation(
      summary = "Get performance health status",
      description = "Returns overall performance health status with key indicators")
  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> getPerformanceHealth() {
    // Get health status from the performance monitoring service
    Map<String, Object> health = performanceMonitoringService.getHealthStatus();

    Map<String, Object> healthStatus =
        Map.of(
            "status",
            health.get("status"),
            "details",
            health,
            "recommendations",
            performanceMonitoringService.getPerformanceRecommendations());

    return ResponseEntity.ok(healthStatus);
  }

  @Operation(
      summary = "Force garbage collection",
      description =
          "Triggers JVM garbage collection to free memory - use with caution in production")
  @PostMapping("/gc")
  public ResponseEntity<Map<String, String>> forceGarbageCollection() {
    long beforeMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

    System.gc();

    long afterMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    long freedMemory = beforeMemory - afterMemory;

    return ResponseEntity.ok(
        Map.of(
            "status",
            "success",
            "message",
            "Garbage collection triggered",
            "freedMemoryMB",
            String.valueOf(freedMemory / 1024 / 1024)));
  }
}
