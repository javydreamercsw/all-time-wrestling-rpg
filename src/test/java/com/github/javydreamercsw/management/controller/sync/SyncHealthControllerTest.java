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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.base.service.ranking.RankingService;
import com.github.javydreamercsw.base.test.BaseControllerTest;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor.SyncHealthSummary;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor.SyncMetric;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = SyncHealthController.class,
    excludeAutoConfiguration = {DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class})
@TestPropertySource(properties = "notion.sync.enabled=true")
class SyncHealthControllerTest extends BaseControllerTest {

  @MockitoBean private CommandLineRunner commandLineRunner;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SyncHealthMonitor healthMonitor;
  @MockitoBean private RankingService rankingService;
  @MockitoBean private WrestlerRepository wrestlerRepository;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void shouldReturnHealthStatus() throws Exception {
    // Given
    Health health =
        Health.up()
            .withDetail("successfulSyncs", 10)
            .withDetail("failedSyncs", 2)
            .withDetail("successRate", 83.33)
            .build();
    when(healthMonitor.health()).thenReturn(health);

    // When & Then
    mockMvc
        .perform(get("/api/sync/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.details.successfulSyncs").value(10))
        .andExpect(jsonPath("$.details.failedSyncs").value(2));
  }

  @Test
  void shouldReturnHealthSummary() throws Exception {
    // Given
    LocalDateTime now = LocalDateTime.now();
    SyncHealthSummary summary = new SyncHealthSummary(15, 3, 83.33, 1500L, 0, now, null, null, 2);
    when(healthMonitor.getHealthSummary()).thenReturn(summary);

    // When & Then
    mockMvc
        .perform(get("/api/sync/health/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.successfulSyncs").value(15))
        .andExpect(jsonPath("$.failedSyncs").value(3))
        .andExpect(jsonPath("$.successRate").value(83.33))
        .andExpect(jsonPath("$.averageSyncTime").value(1500))
        .andExpect(jsonPath("$.consecutiveFailures").value(0))
        .andExpect(jsonPath("$.activeOperations").value(2));
  }

  @Test
  void shouldReturnMetrics() throws Exception {
    // Given
    List<SyncMetric> metrics =
        List.of(
            new SyncMetric("Shows", true, 1000L, 5, null),
            new SyncMetric("Wrestlers", false, 0L, 0, "Connection failed"));
    when(healthMonitor.getRecentMetrics()).thenReturn(metrics);

    // When & Then
    mockMvc
        .perform(get("/api/sync/health/metrics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].entityType").value("Shows"))
        .andExpect(jsonPath("$[0].success").value(true))
        .andExpect(jsonPath("$[0].durationMs").value(1000))
        .andExpect(jsonPath("$[0].itemCount").value(5))
        .andExpect(jsonPath("$[1].entityType").value("Wrestlers"))
        .andExpect(jsonPath("$[1].success").value(false))
        .andExpect(jsonPath("$[1].errorMessage").value("Connection failed"));
  }

  @Test
  void shouldReturnStatistics() throws Exception {
    // Given
    LocalDateTime now = LocalDateTime.now();
    SyncHealthSummary summary =
        new SyncHealthSummary(8, 2, 80.0, 1200L, 1, now, now.minusHours(1), "Last error", 0);
    List<SyncMetric> recentMetrics =
        List.of(
            new SyncMetric("Shows", true, 1000L, 5, null),
            new SyncMetric("Wrestlers", true, 1200L, 3, null));

    when(healthMonitor.getHealthSummary()).thenReturn(summary);
    when(healthMonitor.getRecentMetrics()).thenReturn(recentMetrics);

    // When & Then
    mockMvc
        .perform(get("/api/sync/health/stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalOperations").value(10))
        .andExpect(jsonPath("$.successRate").value(80.0))
        .andExpect(jsonPath("$.failureRate").value(20.0))
        .andExpect(jsonPath("$.averageSyncTime").value(1200))
        .andExpect(jsonPath("$.consecutiveFailures").value(1))
        .andExpect(jsonPath("$.recentSuccessRate").value(100.0))
        .andExpect(jsonPath("$.activeOperations").value(0));
  }

  @Test
  void shouldResetMetrics() throws Exception {
    // Given
    doNothing().when(healthMonitor).resetMetrics();

    // When & Then
    mockMvc
        .perform(post("/api/sync/health/reset"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("success"))
        .andExpect(jsonPath("$.message").value("Health metrics reset successfully"));

    verify(healthMonitor).resetMetrics();
  }

  @Test
  void shouldReturnRecommendations() throws Exception {
    // Given
    LocalDateTime now = LocalDateTime.now();
    SyncHealthSummary summary =
        new SyncHealthSummary(5, 5, 50.0, 35000L, 2, now.minusHours(25), now, "Recent error", 0);
    when(healthMonitor.getHealthSummary()).thenReturn(summary);

    // When & Then
    mockMvc
        .perform(get("/api/sync/health/recommendations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(
            jsonPath("$[0]")
                .value("Success rate is below 90%. Check error logs and network connectivity."))
        .andExpect(
            jsonPath("$[1]")
                .value(
                    "Average sync time is high. Consider optimizing sync operations or checking"
                        + " network performance."))
        .andExpect(
            jsonPath("$[2]")
                .value(
                    "No successful sync in over 24 hours. Check sync scheduler and"
                        + " configuration."));
  }

  @Test
  void shouldReturnGoodRecommendationWhenHealthy() throws Exception {
    // Given
    LocalDateTime now = LocalDateTime.now();
    SyncHealthSummary summary = new SyncHealthSummary(95, 5, 95.0, 2000L, 0, now, null, null, 1);
    when(healthMonitor.getHealthSummary()).thenReturn(summary);

    // When & Then
    mockMvc
        .perform(get("/api/sync/health/recommendations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(
            jsonPath("$[0]").value("Sync health is good. Continue monitoring for any changes."));
  }

  @Test
  void shouldReturnSimpleStatus() throws Exception {
    // Given
    Health health = Health.up().withDetail("successfulSyncs", 10).build();
    LocalDateTime now = LocalDateTime.now();
    SyncHealthSummary summary = new SyncHealthSummary(10, 2, 83.33, 1500L, 0, now, null, null, 1);

    when(healthMonitor.health()).thenReturn(health);
    when(healthMonitor.getHealthSummary()).thenReturn(summary);

    // When & Then
    mockMvc
        .perform(get("/api/sync/health/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("HEALTHY"))
        .andExpect(jsonPath("$.successRate").value(83.33))
        .andExpect(jsonPath("$.activeOperations").value(1))
        .andExpect(jsonPath("$.message").value("OK"));
  }

  @Test
  void shouldReturnUnhealthyStatus() throws Exception {
    // Given
    Health health = Health.down().withDetail("error", "Multiple failures detected").build();
    LocalDateTime now = LocalDateTime.now();
    SyncHealthSummary summary =
        new SyncHealthSummary(
            5, 10, 33.33, 2000L, 5, now.minusHours(2), now, "Connection failed", 0);

    when(healthMonitor.health()).thenReturn(health);
    when(healthMonitor.getHealthSummary()).thenReturn(summary);

    // When & Then
    mockMvc
        .perform(get("/api/sync/health/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UNHEALTHY"))
        .andExpect(jsonPath("$.successRate").value(33.33))
        .andExpect(jsonPath("$.message").value("Multiple failures detected"));
  }

  @Test
  void shouldHandleHealthMonitorException() throws Exception {
    // Given
    when(healthMonitor.health()).thenThrow(new RuntimeException("Health check failed"));

    // When & Then
    mockMvc
        .perform(get("/api/sync/health"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value("DOWN"))
        .andExpect(
            jsonPath("$.details.error").value("Failed to get health status: Health check failed"));
  }

  @Test
  void shouldHandleResetException() throws Exception {
    // Given
    doThrow(new RuntimeException("Reset failed")).when(healthMonitor).resetMetrics();

    // When & Then
    mockMvc
        .perform(post("/api/sync/health/reset"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value("error"))
        .andExpect(jsonPath("$.message").value("Failed to reset metrics: Reset failed"));
  }
}
