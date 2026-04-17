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
package com.github.javydreamercsw.management.service.league;

import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import java.io.Serializable;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@VaadinSessionScope
@Slf4j
@RequiredArgsConstructor
public class LeagueContextService implements Serializable {

  private final LeagueRepository leagueRepository;
  private Long currentLeagueId;

  public Optional<League> getCurrentLeague() {
    if (currentLeagueId == null) {
      // Default to the first league found if none selected
      return leagueRepository.findAll().stream().findFirst().map(l -> {
          this.currentLeagueId = l.getId();
          return l;
      });
    }
    return leagueRepository.findById(currentLeagueId);
  }

  public Long getCurrentLeagueId() {
    return getCurrentLeague().map(League::getId).orElse(1L); // Fallback to ID 1 (Global Universe)
  }

  public void setCurrentLeague(League league) {
    if (league != null) {
      this.currentLeagueId = league.getId();
      log.info("Current league context set to: {} (ID: {})", league.getName(), league.getId());
    }
  }
  
  public void setCurrentLeagueId(Long leagueId) {
      this.currentLeagueId = leagueId;
  }
}
