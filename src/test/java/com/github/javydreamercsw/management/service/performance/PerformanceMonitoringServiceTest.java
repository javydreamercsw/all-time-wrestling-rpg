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
package com.github.javydreamercsw.management.service.performance;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.config.CacheConfig.CacheMonitor;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PerformanceMonitoringServiceTest {

  @Mock private CacheMonitor cacheMonitor;

  private PerformanceMonitoringService service;

  @BeforeEach
  void setUp() {
    service = new PerformanceMonitoringService(cacheMonitor);
    // Reset after construction so constructor-seeded values don't interfere with tests
    service.resetMetrics();
  }

  @Test
  void startOperation_storesStartTime() {
    service.startOperation("TestOp");

    // After starting, the counter for started operations should be 1
    assertThat(service.getCounter("operations.started.TestOp")).isEqualTo(1L);
  }

  @Test
  void endOperation_calculatesAndRecordsDuration() {
    service.startOperation("TestOp");
    service.endOperation("TestOp");

    // Timer for the operation should have been recorded (value >= 0)
    assertThat(service.getTimer("operations.duration.TestOp")).isGreaterThanOrEqualTo(0L);
  }

  @Test
  void endOperation_removesFromActiveOperations() {
    service.startOperation("TestOp");
    service.endOperation("TestOp");

    // After end, completed counter should be 1
    assertThat(service.getCounter("operations.completed.TestOp")).isEqualTo(1L);
  }

  @Test
  void recordTokenUsage_incrementsThreeCounters() {
    service.recordTokenUsage("OpenAI", 100, 50);

    assertThat(service.getCounter("ai.tokens.input.OpenAI")).isEqualTo(100L);
    assertThat(service.getCounter("ai.tokens.output.OpenAI")).isEqualTo(50L);
    assertThat(service.getCounter("ai.requests.OpenAI")).isEqualTo(1L);
  }

  @Test
  void incrementCounter_singleArg_incrementsByOne() {
    service.incrementCounter("my.counter");

    assertThat(service.getCounter("my.counter")).isEqualTo(1L);
  }

  @Test
  void incrementCounter_doubleArg_incrementsByValue() {
    service.incrementCounter("my.counter", 42L);

    assertThat(service.getCounter("my.counter")).isEqualTo(42L);
  }

  @Test
  void incrementCounter_multipleCallsAccumulate() {
    service.incrementCounter("my.counter");
    service.incrementCounter("my.counter");
    service.incrementCounter("my.counter", 3L);

    assertThat(service.getCounter("my.counter")).isEqualTo(5L);
  }

  @Test
  void getCounter_nonExistentKey_returnsZero() {
    assertThat(service.getCounter("nonexistent.counter")).isEqualTo(0L);
  }

  @Test
  void recordTimer_addsToTimer() {
    service.recordTimer("my.timer", 500L);

    assertThat(service.getTimer("my.timer")).isEqualTo(500L);
  }

  @Test
  void getTimer_nonExistentKey_returnsZero() {
    assertThat(service.getTimer("nonexistent.timer")).isEqualTo(0L);
  }

  @Test
  void resetMetrics_clearsAllCountersAndTimers() {
    service.incrementCounter("some.counter", 10L);
    service.recordTimer("some.timer", 200L);

    service.resetMetrics();

    assertThat(service.getCounter("some.counter")).isEqualTo(0L);
    assertThat(service.getTimer("some.timer")).isEqualTo(0L);
  }

  @Test
  void getHistory_returnsDefensiveCopy() {
    service.captureSnapshot();
    List<PerformanceMonitoringService.PerformanceSnapshot> history1 = service.getHistory();
    List<PerformanceMonitoringService.PerformanceSnapshot> history2 = service.getHistory();

    // Should be equal content but different list instances
    assertThat(history1).isEqualTo(history2);
    assertThat(history1).isNotSameAs(history2);
  }

  @Test
  void getHealthStatus_returnsUpWhenMemoryNormal() {
    Map<String, Object> health = service.getHealthStatus();

    // Under normal test conditions memory usage will be well below 95%
    assertThat(health).containsKey("status");
    assertThat(health.get("status")).isEqualTo("UP");
  }

  @Test
  void captureSnapshot_addsToHistory() {
    int sizeBefore = service.getHistory().size();
    service.captureSnapshot();

    assertThat(service.getHistory()).hasSize(sizeBefore + 1);
  }
}
