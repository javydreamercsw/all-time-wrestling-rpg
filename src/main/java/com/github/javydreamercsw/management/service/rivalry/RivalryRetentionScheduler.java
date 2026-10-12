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
import com.github.javydreamercsw.management.service.GameSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled retention purge of ENDED rivalries (ATW-8nc8). The retention window (in days) is read
 * from the {@code rivalry.retention.days} game setting; 0 (the default) keeps rivalries forever and
 * disables the purge. Runs daily at 03:10 — after {@code RivalryDecayService}'s 03:00 maintenance,
 * which may end rivalries whose fresh {@code endedDate}s then correctly fall outside any window.
 *
 * <p>A separate bean from the decay service so the call reaches {@code
 * RivalryService.purgeEndedRivalries} through the Spring proxy — a self-invocation from inside the
 * decay service would silently skip its @CacheEvict.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RivalryRetentionScheduler {

  private final RivalryService rivalryService;
  private final GameSettingService gameSettingService;

  @Scheduled(cron = "0 10 3 * * *")
  public void runRetentionPurge() {
    GeneralSecurityUtils.runAsAdmin(
        () -> {
          int retentionDays = gameSettingService.getRivalryRetentionDays();
          if (retentionDays <= 0) {
            log.debug(
                "Rivalry retention purge disabled (rivalry.retention.days = {})", retentionDays);
            return;
          }
          try {
            RivalryService.RivalryRetentionResult result =
                rivalryService.purgeEndedRivalries(retentionDays);
            log.info(
                "Rivalry retention purge complete: {} rivalries purged, {} segment references"
                    + " cleared",
                result.rivalriesPurged(),
                result.segmentsUnlinked());
          } catch (Exception e) {
            log.error("Error during rivalry retention purge", e);
          }
        });
  }
}
