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
package com.github.javydreamercsw.management.service.drama;

import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.service.drama.DramaEventService.DramaEventCleanupResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled cleanup of old drama events. Enabled via {@code drama.events.cleanup.enabled=true}.
 * Runs weekly; thresholds are configurable via application properties.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "drama.events.cleanup.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class DramaEventCleanupScheduler {

  private final DramaEventService dramaEventService;

  @Value("${drama.events.retention.processed.days:90}")
  private int processedRetentionDays;

  @Value("${drama.events.retention.unprocessed.days:180}")
  private int unprocessedRetentionDays;

  @Scheduled(cron = "0 0 3 * * SUN")
  public void runCleanup() {
    GeneralSecurityUtils.runAsAdmin(
        () -> {
          try {
            log.info(
                "Starting DramaEvent cleanup (processed >{}d, unprocessed >{}d)",
                processedRetentionDays,
                unprocessedRetentionDays);
            DramaEventCleanupResult result =
                dramaEventService.purgeOldEvents(processedRetentionDays, unprocessedRetentionDays);
            log.info(
                "DramaEvent cleanup complete: {} processed, {} unprocessed deleted",
                result.processedDeleted(),
                result.unprocessedDeleted());
          } catch (Exception e) {
            log.error("Error during DramaEvent cleanup", e);
          }
        });
  }
}
