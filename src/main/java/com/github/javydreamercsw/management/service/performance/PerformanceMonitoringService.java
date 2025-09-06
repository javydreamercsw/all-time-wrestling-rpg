package com.github.javydreamercsw.management.service.performance;

import com.github.javydreamercsw.management.config.CacheConfig.CacheMonitor;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for monitoring application performance metrics. Tracks database queries, cache
 * performance, memory usage, and response times.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceMonitoringService {

  private final CacheMonitor cacheMonitor;

  // Performance metrics storage
  private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
  private final Map<String, AtomicLong> timers = new ConcurrentHashMap<>();
  private final Map<String, Instant> operationStartTimes = new ConcurrentHashMap<>();

  // JVM monitoring beans
  private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
  private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

  /** Records the start of a performance-critical operation. */
  public void startOperation(String operationName) {
    operationStartTimes.put(operationName, Instant.now());
    incrementCounter("operations.started." + operationName);
  }

  /** Records the end of a performance-critical operation and calculates duration. */
  public void endOperation(String operationName) {
    Instant startTime = operationStartTimes.remove(operationName);
    if (startTime != null) {
      long durationMs = Duration.between(startTime, Instant.now()).toMillis();
      recordTimer("operations.duration." + operationName, durationMs);
      incrementCounter("operations.completed." + operationName);

      // Log slow operations
      if (durationMs > 1000) { // More than 1 second
        log.warn("⚠️ Slow operation detected: {} took {}ms", operationName, durationMs);
      }
    }
  }

  /** Increments a counter metric. */
  public void incrementCounter(String counterName) {
    counters.computeIfAbsent(counterName, k -> new AtomicLong(0)).incrementAndGet();
  }

  /** Records a timer metric. */
  public void recordTimer(String timerName, long durationMs) {
    timers.computeIfAbsent(timerName, k -> new AtomicLong(0)).addAndGet(durationMs);
  }

  /** Gets the current value of a counter. */
  public long getCounter(String counterName) {
    AtomicLong counter = counters.get(counterName);
    return counter != null ? counter.get() : 0;
  }

  /** Gets the current value of a timer. */
  public long getTimer(String timerName) {
    AtomicLong timer = timers.get(timerName);
    return timer != null ? timer.get() : 0;
  }

  /** Gets comprehensive performance metrics. */
  public Map<String, Object> getPerformanceMetrics() {
    Map<String, Object> metrics = new HashMap<>();

    // Application metrics
    metrics.put("counters", new HashMap<>(counters));
    metrics.put("timers", new HashMap<>(timers));
    metrics.put("activeOperations", operationStartTimes.size());

    // JVM metrics
    metrics.put("memory", getMemoryMetrics());
    metrics.put("threads", getThreadMetrics());

    // Cache metrics
    metrics.put("caches", getCacheMetrics());

    return metrics;
  }

  /** Gets memory usage metrics. */
  private Map<String, Object> getMemoryMetrics() {
    Map<String, Object> memory = new HashMap<>();

    var heapMemory = memoryBean.getHeapMemoryUsage();
    var nonHeapMemory = memoryBean.getNonHeapMemoryUsage();

    memory.put(
        "heap",
        Map.of(
            "used",
            heapMemory.getUsed(),
            "max",
            heapMemory.getMax(),
            "committed",
            heapMemory.getCommitted(),
            "usagePercent",
            (double) heapMemory.getUsed() / heapMemory.getMax() * 100));

    memory.put(
        "nonHeap",
        Map.of(
            "used",
            nonHeapMemory.getUsed(),
            "max",
            nonHeapMemory.getMax(),
            "committed",
            nonHeapMemory.getCommitted()));

    return memory;
  }

  /** Gets thread metrics. */
  private Map<String, Object> getThreadMetrics() {
    Map<String, Object> threads = new HashMap<>();

    threads.put("count", threadBean.getThreadCount());
    threads.put("peak", threadBean.getPeakThreadCount());
    threads.put("daemon", threadBean.getDaemonThreadCount());
    threads.put("totalStarted", threadBean.getTotalStartedThreadCount());

    return threads;
  }

  /** Gets cache performance metrics. */
  private Map<String, Object> getCacheMetrics() {
    Map<String, Object> cacheMetrics = new HashMap<>();

    // This would be enhanced with actual cache statistics
    cacheMetrics.put("status", "active");
    cacheMetrics.put("cacheCount", 13); // Number of configured caches

    return cacheMetrics;
  }

  /** Scheduled method to log performance metrics periodically. */
  @Scheduled(fixedRate = 300000) // Every 5 minutes
  public void logPerformanceMetrics() {
    log.info("📊 Performance Metrics Summary:");

    // Log top counters
    counters.entrySet().stream()
        .sorted((e1, e2) -> Long.compare(e2.getValue().get(), e1.getValue().get()))
        .limit(5)
        .forEach(entry -> log.info("   Counter: {} = {}", entry.getKey(), entry.getValue().get()));

    // Log memory usage
    var heapMemory = memoryBean.getHeapMemoryUsage();
    double memoryUsagePercent = (double) heapMemory.getUsed() / heapMemory.getMax() * 100;
    log.info(
        "   Memory Usage: {}% ({} MB / {} MB)",
        String.format("%.1f", memoryUsagePercent),
        heapMemory.getUsed() / 1024 / 1024,
        heapMemory.getMax() / 1024 / 1024);

    // Log thread count
    log.info("   Active Threads: {}", threadBean.getThreadCount());

    // Log cache statistics
    cacheMonitor.logCacheStatistics();

    // Warn about performance issues
    if (memoryUsagePercent > 80) {
      log.warn("⚠️ High memory usage detected: {}%", String.format("%.1f", memoryUsagePercent));
    }

    if (threadBean.getThreadCount() > 100) {
      log.warn("⚠️ High thread count detected: {}", threadBean.getThreadCount());
    }
  }

  /** Gets health status information as a map. */
  public Map<String, Object> getHealthStatus() {
    var heapMemory = memoryBean.getHeapMemoryUsage();
    double memoryUsagePercent = (double) heapMemory.getUsed() / heapMemory.getMax() * 100;

    Map<String, Object> health = new HashMap<>();
    health.put("status", memoryUsagePercent > 95 ? "DOWN" : "UP");
    health.put("memoryUsage", String.format("%.1f%%", memoryUsagePercent));
    health.put("threadCount", threadBean.getThreadCount());
    health.put("activeOperations", operationStartTimes.size());

    if (memoryUsagePercent > 95) {
      health.put("reason", "Critical memory usage: " + String.format("%.1f%%", memoryUsagePercent));
    }

    return health;
  }

  /** Resets all performance metrics - useful for testing. */
  public void resetMetrics() {
    counters.clear();
    timers.clear();
    operationStartTimes.clear();
    log.info("🔄 Performance metrics reset");
  }

  /** Gets performance recommendations based on current metrics. */
  public Map<String, String> getPerformanceRecommendations() {
    Map<String, String> recommendations = new HashMap<>();

    var heapMemory = memoryBean.getHeapMemoryUsage();
    double memoryUsagePercent = (double) heapMemory.getUsed() / heapMemory.getMax() * 100;

    if (memoryUsagePercent > 80) {
      recommendations.put("memory", "Consider increasing heap size or optimizing memory usage");
    }

    if (threadBean.getThreadCount() > 50) {
      recommendations.put(
          "threads", "High thread count - consider using async processing or connection pooling");
    }

    if (operationStartTimes.size() > 10) {
      recommendations.put(
          "operations", "Many long-running operations detected - consider optimization");
    }

    // Check for slow operations
    long slowOperations =
        counters.entrySet().stream()
            .filter(entry -> entry.getKey().contains("slow"))
            .mapToLong(entry -> entry.getValue().get())
            .sum();

    if (slowOperations > 0) {
      recommendations.put(
          "slowQueries",
          "Slow operations detected - review database indexes and query optimization");
    }

    return recommendations;
  }
}
