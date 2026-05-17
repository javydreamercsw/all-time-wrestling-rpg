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
package com.github.javydreamercsw.management.service.universe;

import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UniverseService {

  private final UniverseRepository universeRepository;
  private final ShowRepository showRepository;
  private final FactionRepository factionRepository;
  private final LeagueRepository leagueRepository;
  private final TitleRepository titleRepository;
  private final CampaignRepository campaignRepository;

  @PreAuthorize("isAuthenticated()")
  public List<Universe> findAll() {
    return universeRepository.findAll();
  }

  @PreAuthorize("isAuthenticated()")
  public Optional<Universe> findById(@NonNull final Long id) {
    return universeRepository.findById(id);
  }

  @PreAuthorize("isAuthenticated()")
  public Optional<Universe> findByName(@NonNull final String name) {
    return universeRepository.findByName(name);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public Universe save(@NonNull final Universe universe) {
    String name = universe.getName();
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Universe name must not be blank.");
    }
    universeRepository
        .findByName(name)
        .ifPresent(
            existing -> {
              if (!existing.getId().equals(universe.getId())) {
                throw new IllegalArgumentException(
                    "A universe named '" + name + "' already exists.");
              }
            });
    return universeRepository.save(universe);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public void delete(@NonNull final Long id) {
    Universe universe =
        universeRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Universe not found: " + id));

    if (showRepository.existsByUniverse(universe)) {
      throw new IllegalStateException(
          "Cannot delete universe '" + universe.getName() + "': it has associated shows.");
    }
    if (factionRepository.existsByUniverse(universe)) {
      throw new IllegalStateException(
          "Cannot delete universe '" + universe.getName() + "': it has associated factions.");
    }
    if (leagueRepository.existsByUniverse(universe)) {
      throw new IllegalStateException(
          "Cannot delete universe '" + universe.getName() + "': it has associated leagues.");
    }
    if (titleRepository.existsByUniverse(universe)) {
      throw new IllegalStateException(
          "Cannot delete universe '" + universe.getName() + "': it has associated titles.");
    }
    if (campaignRepository.existsByUniverse(universe)) {
      throw new IllegalStateException(
          "Cannot delete universe '" + universe.getName() + "': it has associated campaigns.");
    }

    universeRepository.delete(universe);
  }

  @PreAuthorize("isAuthenticated()")
  public long count() {
    return universeRepository.count();
  }
}
