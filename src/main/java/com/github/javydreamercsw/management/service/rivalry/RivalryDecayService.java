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
package com.github.javydreamercsw.management.service.rivalry;

import com.github.javydreamercsw.management.domain.rivalry.HeatEvent;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.service.GameSettingService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RivalryDecayService {

  private final RivalryRepository rivalryRepository;
  private final GameSettingService gameSettingService;
  private final Clock clock;

  /** Runs nightly to apply heat decay and auto-close stale rivalries. */
  @Scheduled(cron = "0 0 3 * * *")
  @Transactional
  public void runDailyMaintenance() {
    elevateToSystem();
    try {
      applyHeatDecay();
      closeExpiredRivalries();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  /** Decays heat on rivalries that have had no activity for the configured interval. */
  @Transactional
  public void applyHeatDecay() {
    if (!gameSettingService.isRivalryHeatDecayEnabled()) {
      return;
    }

    int decayAmount = gameSettingService.getRivalryHeatDecayPerInterval();
    int intervalDays = gameSettingService.getRivalryHeatDecayIntervalDays();
    Instant cutoff = Instant.now(clock).minus(intervalDays, ChronoUnit.DAYS);

    List<Rivalry> candidates = rivalryRepository.findByIsActiveTrue();
    int decayed = 0;
    for (Rivalry rivalry : candidates) {
      if (rivalry.getHeat() <= 0) {
        continue;
      }
      Instant lastActivity = latestHeatEventDate(rivalry);
      if (lastActivity == null || lastActivity.isBefore(cutoff)) {
        int oldHeat = rivalry.getHeat();
        int newHeat = Math.max(0, oldHeat - decayAmount);
        rivalry.setHeat(newHeat);

        HeatEvent event = new HeatEvent();
        event.setRivalry(rivalry);
        event.setHeatChange(-decayAmount);
        event.setReason("Automatic heat decay");
        event.setEventDate(Instant.now(clock));
        event.setHeatAfterEvent(newHeat);
        rivalry.getHeatEvents().add(event);

        rivalryRepository.save(rivalry);
        decayed++;
        log.debug(
            "Decayed rivalry {} heat {} → {} (last activity: {})",
            rivalry.getId(),
            oldHeat,
            newHeat,
            lastActivity);
      }
    }
    if (decayed > 0) {
      log.info(
          "Heat decay applied to {} rivalries (amount: {}, interval: {}d)",
          decayed,
          decayAmount,
          intervalDays);
    }
  }

  /** Auto-closes rivalries that have exceeded the configured maximum duration. */
  @Transactional
  public void closeExpiredRivalries() {
    int maxDays = gameSettingService.getRivalryMaxDurationDays();
    if (maxDays <= 0) {
      return;
    }

    Instant expiryDate = Instant.now(clock).minus(maxDays, ChronoUnit.DAYS);
    List<Rivalry> candidates = rivalryRepository.findByIsActiveTrue();
    int closed = 0;
    for (Rivalry rivalry : candidates) {
      if (rivalry.getStartedDate() != null && rivalry.getStartedDate().isBefore(expiryDate)) {
        rivalry.endRivalry("Exceeded maximum rivalry duration (%dd)".formatted(maxDays));
        rivalryRepository.save(rivalry);
        closed++;
        log.info(
            "Auto-closed rivalry {} (started: {}, max duration: {}d)",
            rivalry.getId(),
            rivalry.getStartedDate(),
            maxDays);
      }
    }
    if (closed > 0) {
      log.info("Auto-closed {} expired rivalries (max duration: {}d)", closed, maxDays);
    }
  }

  private Instant latestHeatEventDate(final Rivalry rivalry) {
    return rivalry.getHeatEvents().stream()
        .map(HeatEvent::getEventDate)
        .max(Instant::compareTo)
        .orElse(null);
  }

  private void elevateToSystem() {
    var auth =
        new UsernamePasswordAuthenticationToken(
            "system", null, List.of(new SimpleGrantedAuthority("ROLE_SYSTEM")));
    SecurityContextHolder.getContext().setAuthentication(auth);
  }
}
