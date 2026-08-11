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
package com.github.javydreamercsw.management.service.rivalry;

import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.service.rivalry.RivalryService.HeatEventCleanupResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled cleanup of heat events for closed rivalries. Enabled via {@code
 * heat.events.cleanup.enabled=true}. Runs weekly; retention threshold is configurable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "heat.events.cleanup.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class HeatEventCleanupScheduler {

  private final RivalryService rivalryService;

  @Value("${heat.events.retention.days:30}")
  private int retentionDays;

  @Scheduled(cron = "0 0 3 * * MON")
  public void runCleanup() {
    GeneralSecurityUtils.runAsAdmin(
        () -> {
          try {
            log.info("Starting HeatEvent cleanup (closed rivalries older than {}d)", retentionDays);
            HeatEventCleanupResult result =
                rivalryService.purgeClosedRivalryHeatEvents(retentionDays);
            log.info(
                "HeatEvent cleanup complete: {} events deleted from {} closed rivalries",
                result.eventsDeleted(),
                result.rivalriesProcessed());
          } catch (Exception e) {
            log.error("Error during HeatEvent cleanup", e);
          }
        });
  }
}
