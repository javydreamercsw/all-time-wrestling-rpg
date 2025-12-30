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
import java.util.List;
import org.springframework.boot.actuate.health.HealthIndicator;

public interface ISyncHealthMonitor extends HealthIndicator {
  void recordSuccess(String entityType, long durationMs, int itemCount);

  void recordFailure(String entityType, String errorMessage);

  List<SyncMetric> getRecentMetrics();

  void resetMetrics();

  SyncHealthSummary getHealthSummary();
}
