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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javydreamercsw.management.config.CacheConfig.CacheMonitor;
import com.github.javydreamercsw.management.config.DatabaseOptimizationConfig;
import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.service.performance.PerformanceMonitoringService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WithMockUser(authorities = {"ADMIN", "ROLE_ADMIN"})
class PerformanceControllerTest extends AbstractControllerTest {

  @MockitoBean private PerformanceMonitoringService performanceMonitoringService;
  @MockitoBean private CacheMonitor cacheMonitor;
  @MockitoBean private DatabaseOptimizationConfig databaseOptimizationConfig;

  private Map<String, Object> metricsMap;

  @BeforeEach
  void setUp() {
    metricsMap = new HashMap<>();
    metricsMap.put("memory", Map.of("used", 100L));
    metricsMap.put("threads", Map.of("count", 10));
    metricsMap.put("activeOperations", 2);
    metricsMap.put("counters", Map.of());
    metricsMap.put("timers", Map.of());
  }

  @Test
  void getPerformanceMetrics_returnsMetrics() throws Exception {
    when(performanceMonitoringService.getPerformanceMetrics()).thenReturn(metricsMap);

    mockMvc.perform(get("/api/system/performance/metrics").with(csrf())).andExpect(status().isOk());
  }

  @Test
  void getPerformanceRecommendations_returnsMap() throws Exception {
    Map<String, String> recommendations = Map.of("memory", "Optimize heap usage");
    when(performanceMonitoringService.getPerformanceRecommendations()).thenReturn(recommendations);

    mockMvc
        .perform(get("/api/system/performance/recommendations").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.memory").value("Optimize heap usage"));
  }

  @Test
  void resetPerformanceMetrics_returnsSuccess() throws Exception {
    doNothing().when(performanceMonitoringService).resetMetrics();

    mockMvc
        .perform(delete("/api/system/performance/metrics").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));
  }

  @Test
  void clearAllCaches_returnsSuccess() throws Exception {
    doNothing().when(cacheMonitor).clearAllCaches();

    mockMvc
        .perform(delete("/api/system/performance/cache").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));
  }

  @Test
  void getCacheStatistics_returnsStats() throws Exception {
    doNothing().when(cacheMonitor).logCacheStatistics();

    mockMvc
        .perform(get("/api/system/performance/cache/stats").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("active"))
        .andExpect(jsonPath("$.cacheCount").value(13));
  }

  @Test
  void warmUpCaches_returnsSuccess() throws Exception {
    doNothing().when(cacheMonitor).warmUpCaches();

    mockMvc
        .perform(post("/api/system/performance/cache/warmup").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));
  }

  @Test
  void analyzeDatabasePerformance_returnsSuccess() throws Exception {
    when(databaseOptimizationConfig.analyzeQueryPerformance()).thenReturn(List.of());

    mockMvc
        .perform(post("/api/system/performance/database/analyze").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"));
  }

  @Test
  void getSystemResources_returnsSubsetOfMetrics() throws Exception {
    when(performanceMonitoringService.getPerformanceMetrics()).thenReturn(metricsMap);

    mockMvc
        .perform(get("/api/system/performance/resources").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.memory").exists())
        .andExpect(jsonPath("$.threads").exists());
  }

  @Test
  void getOperationStatistics_returnsOperations() throws Exception {
    when(performanceMonitoringService.getPerformanceMetrics()).thenReturn(metricsMap);

    mockMvc
        .perform(get("/api/system/performance/operations").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.counters").exists())
        .andExpect(jsonPath("$.timers").exists());
  }

  @Test
  void getPerformanceHealth_returnsHealthStatus() throws Exception {
    Map<String, Object> healthMap = new HashMap<>();
    healthMap.put("status", "healthy");
    healthMap.put("details", Map.of());

    when(performanceMonitoringService.getHealthStatus()).thenReturn(healthMap);
    when(performanceMonitoringService.getPerformanceRecommendations()).thenReturn(Map.of());

    mockMvc
        .perform(get("/api/system/performance/health").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("healthy"));
  }

  @Test
  void forceGarbageCollection_returnsSuccess() throws Exception {
    mockMvc
        .perform(post("/api/system/performance/gc").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.message").value("Garbage collection triggered"));
  }
}
