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
package com.github.javydreamercsw.management.service.sync;

import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor.SyncHealthSummary;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor.SyncMetric;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;

@Slf4j
public class NoOpSyncHealthMonitor implements ISyncHealthMonitor {

  @Override
  public Health health() {
    return Health.up().withDetail("message", "Notion Sync is disabled (No-Op)").build();
  }

  @Override
  public void recordSuccess(String entityType, long durationMs, int itemCount) {
    log.debug("No-Op: recordSuccess for {} (sync disabled)", entityType);
  }

  @Override
  public void recordFailure(String entityType, String errorMessage) {
    log.debug("No-Op: recordFailure for {} (sync disabled)", entityType);
  }

  @Override
  public List<SyncMetric> getRecentMetrics() {
    return Collections.emptyList();
  }

  @Override
  public void resetMetrics() {
    log.debug("No-Op: resetMetrics (sync disabled)");
  }

  @Override
  public SyncHealthSummary getHealthSummary() {
    return new SyncHealthSummary(0, 0, 0.0, 0L, 0, null, null, "Notion Sync disabled", 0);
  }
}
