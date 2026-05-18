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
package com.github.javydreamercsw.management.controller.system;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.config.CacheConfig.CacheMonitor;
import com.github.javydreamercsw.management.config.DatabaseOptimizationConfig;
import com.github.javydreamercsw.management.controller.AbstractRestControllerIT;
import com.github.javydreamercsw.management.service.performance.PerformanceMonitoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for PerformanceController. Tests all REST endpoints for system performance
 * monitoring and management using the real Spring context.
 */
@DisplayName("PerformanceController Integration Tests")
@Transactional
class PerformanceControllerIT extends AbstractRestControllerIT {

  @Autowired private PerformanceMonitoringService performanceMonitoringService;
  @Autowired private CacheMonitor cacheMonitor;
  @Autowired private DatabaseOptimizationConfig databaseOptimizationConfig;

  @BeforeEach
  public void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new PerformanceController(
                    performanceMonitoringService, cacheMonitor, databaseOptimizationConfig))
            .build();
  }

  @Test
  @DisplayName("GET /api/system/performance/health should return 200 with status field")
  void shouldReturnHealthStatus() throws Exception {
    mockMvc
        .perform(get("/api/system/performance/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").exists())
        .andExpect(jsonPath("$.details").exists());
  }

  @Test
  @DisplayName("GET /api/system/performance/metrics should return 200 with metrics map")
  void shouldReturnPerformanceMetrics() throws Exception {
    mockMvc
        .perform(get("/api/system/performance/metrics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isMap());
  }

  @Test
  @DisplayName("GET /api/system/performance/recommendations should return 200")
  void shouldReturnPerformanceRecommendations() throws Exception {
    mockMvc
        .perform(get("/api/system/performance/recommendations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isMap());
  }

  @Test
  @DisplayName(
      "GET /api/system/performance/resources should return 200 with memory and thread info")
  void shouldReturnSystemResources() throws Exception {
    mockMvc
        .perform(get("/api/system/performance/resources"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.memory").exists())
        .andExpect(jsonPath("$.threads").exists())
        .andExpect(jsonPath("$.activeOperations").exists());
  }

  @Test
  @DisplayName("GET /api/system/performance/operations should return 200")
  void shouldReturnOperationStatistics() throws Exception {
    mockMvc
        .perform(get("/api/system/performance/operations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.counters").exists())
        .andExpect(jsonPath("$.timers").exists())
        .andExpect(jsonPath("$.activeOperations").exists());
  }

  @Test
  @DisplayName("GET /api/system/performance/cache/stats should return 200")
  void shouldReturnCacheStatistics() throws Exception {
    mockMvc
        .perform(get("/api/system/performance/cache/stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("active"))
        .andExpect(jsonPath("$.cacheCount").value(13));
  }

  @Test
  @DisplayName("DELETE /api/system/performance/metrics should return 200 and reset metrics")
  void shouldResetPerformanceMetrics() throws Exception {
    mockMvc
        .perform(delete("/api/system/performance/metrics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.message").value("Performance metrics reset successfully"));
  }

  @Test
  @DisplayName("DELETE /api/system/performance/cache should return 200 and clear caches")
  void shouldClearAllCaches() throws Exception {
    mockMvc
        .perform(delete("/api/system/performance/cache"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.message").value("All caches cleared successfully"));
  }

  @Test
  @DisplayName("POST /api/system/performance/cache/warmup should return 200 with success status")
  void shouldWarmUpCaches() throws Exception {
    mockMvc
        .perform(post("/api/system/performance/cache/warmup"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));
  }

  @Test
  @DisplayName(
      "POST /api/system/performance/database/analyze should return 200 with success status")
  void shouldAnalyzeDatabasePerformance() throws Exception {
    mockMvc
        .perform(post("/api/system/performance/database/analyze"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));
  }

  @Test
  @DisplayName("POST /api/system/performance/gc should return 200 with garbage collection result")
  void shouldForceGarbageCollection() throws Exception {
    mockMvc
        .perform(post("/api/system/performance/gc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.message").value("Garbage collection triggered"))
        .andExpect(jsonPath("$.freedMemoryMB").exists());
  }
}
