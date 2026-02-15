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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

class PerformanceMonitoringServiceIT extends AbstractIntegrationTest {

  @Autowired private PerformanceMonitoringService performanceMonitoringService;

  @Test
  @WithMockUser(roles = "ADMIN")
  void testMetricCollectionAndHistory() {
    // Record some operations
    performanceMonitoringService.startOperation("TestOp");
    performanceMonitoringService.endOperation("TestOp");

    // Capture snapshot manually
    performanceMonitoringService.captureSnapshot();

    // Verify history
    List<PerformanceMonitoringService.PerformanceSnapshot> history =
        performanceMonitoringService.getHistory();
    assertNotNull(history);
    assertFalse(history.isEmpty());

    PerformanceMonitoringService.PerformanceSnapshot snapshot = history.getFirst();
    assertNotNull(snapshot.getTimestamp());
    assertTrue(snapshot.getHeapUsagePercent() >= 0);
    assertTrue(snapshot.getThreadCount() > 0);
  }
}
