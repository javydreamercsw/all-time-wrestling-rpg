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
package com.github.javydreamercsw.management.event.listener;

import com.github.javydreamercsw.management.event.ChampionshipChangeEvent;
import com.github.javydreamercsw.management.event.ChampionshipDefendedEvent;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LegacyEventListener {

  private final LegacyService legacyService;

  @EventListener
  public void onChampionshipChange(ChampionshipChangeEvent event) {
    log.debug(
        "LegacyEventListener: Handling ChampionshipChangeEvent for title ID {}",
        event.getTitleId());
    event
        .getNewChampions()
        .forEach(
            w -> {
              if (w.getAccount() != null) {
                legacyService.updateLegacyScore(w.getAccount());
              }
            });
    event
        .getOldChampions()
        .forEach(
            w -> {
              if (w.getAccount() != null) {
                legacyService.updateLegacyScore(w.getAccount());
              }
            });
  }

  @EventListener
  public void onChampionshipDefended(ChampionshipDefendedEvent event) {
    log.debug(
        "LegacyEventListener: Handling ChampionshipDefendedEvent for {}", event.getTitleName());
    event
        .getChampions()
        .forEach(
            w -> {
              if (w.getAccount() != null) {
                legacyService.updateLegacyScore(w.getAccount());
              }
            });
  }
}
