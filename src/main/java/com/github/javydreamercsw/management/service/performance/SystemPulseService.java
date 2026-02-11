/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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

import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemPulseService {

  private final List<SegmentNarrationService> aiServices;
  private final Optional<SyncHealthMonitor> syncHealthMonitor;

  public Map<String, ServiceStatus> getPulse() {
    Map<String, ServiceStatus> pulse = new HashMap<>();

    for (SegmentNarrationService service : aiServices) {
      pulse.put(
          service.getProviderName(),
          new ServiceStatus(
              service.isAvailable() ? "UP" : "DOWN",
              service.isAvailable()
                  ? "Service configured and available."
                  : "Not configured or disabled."));
    }

    syncHealthMonitor.ifPresent(
        monitor -> {
          var health = monitor.health();
          pulse.put(
              "Notion Sync",
              new ServiceStatus(
                  health.getStatus().getCode(),
                  (String) health.getDetails().getOrDefault("message", "Sync status unknown.")));
        });

    return pulse;
  }

  public record ServiceStatus(String status, String message) {}
}
