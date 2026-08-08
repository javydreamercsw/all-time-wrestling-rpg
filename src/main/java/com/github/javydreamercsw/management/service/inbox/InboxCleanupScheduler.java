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
package com.github.javydreamercsw.management.service.inbox;

import com.github.javydreamercsw.base.security.GeneralSecurityUtils;
import com.github.javydreamercsw.management.service.GameSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled purge of old inbox items. The retention window (in days) is read from the {@code
 * inbox.retention.days} game setting. Set the value to {@code -1} to disable the purge. Runs daily
 * at 03:00.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InboxCleanupScheduler {

  private final InboxService inboxService;
  private final GameSettingService gameSettingService;

  @Scheduled(cron = "0 0 3 * * *")
  public void runCleanup() {
    GeneralSecurityUtils.runAsAdmin(
        () -> {
          int retentionDays = gameSettingService.getInboxRetentionDays();
          if (retentionDays < 0) {
            log.debug("Inbox cleanup disabled (inbox.retention.days = {})", retentionDays);
            return;
          }
          try {
            log.info("Starting inbox cleanup (retention = {} days)", retentionDays);
            int deleted = inboxService.purgeOldItems(retentionDays);
            log.info("Inbox cleanup complete: {} items deleted", deleted);
          } catch (Exception e) {
            log.error("Error during inbox cleanup", e);
          }
        });
  }
}
