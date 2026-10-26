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
package com.github.javydreamercsw.management.sync;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamMember;
import com.github.javydreamercsw.management.domain.team.TeamOrigin;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamSize;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.dto.TeamImportDTO;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.team.TeamService;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(170)
public class TeamSync implements DataSyncContributor {

  private final boolean skipIfNotEmpty;
  private final TeamService teamService;
  private final TeamRepository teamRepository;
  private final WrestlerRepository wrestlerRepository;
  private final NpcService npcService;
  private final ObjectMapper objectMapper;
  private final UniverseRepository universeRepository;
  private final WrestlerStateRepository wrestlerStateRepository;

  @Autowired
  public TeamSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final boolean skipIfNotEmpty,
      final TeamService teamService,
      final TeamRepository teamRepository,
      final WrestlerRepository wrestlerRepository,
      final NpcService npcService,
      final ObjectMapper objectMapper,
      final UniverseRepository universeRepository,
      final WrestlerStateRepository wrestlerStateRepository) {
    this.skipIfNotEmpty = skipIfNotEmpty;
    this.teamService = teamService;
    this.teamRepository = teamRepository;
    this.wrestlerRepository = wrestlerRepository;
    this.npcService = npcService;
    this.objectMapper = objectMapper;
    this.universeRepository = universeRepository;
    this.wrestlerStateRepository = wrestlerStateRepository;
  }

  @Override
  public void sync() {
    if (skipIfNotEmpty && teamService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("teams.json");
    if (resource.exists()) {
      log.debug("Loading teams from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<TeamImportDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        Map<String, Wrestler> wrestlersByName =
            wrestlerRepository.findAll().stream()
                .collect(Collectors.toMap(Wrestler::getName, wr -> wr, (a, b) -> a));
        Map<String, Npc> npcByName =
            npcService.findAllUnfiltered().stream()
                .collect(Collectors.toMap(Npc::getName, n -> n, (a, b) -> a));
        List<Universe> universes = universeRepository.findAll();
        for (TeamImportDTO dto : dtos) {
          List<Wrestler> members = resolveMembers(dto, wrestlersByName);
          if (members.size() == expectedMemberCount(dto)) {
            Optional<Team> teamOpt = teamService.getTeamByName(dto.getName());
            if (teamOpt.isEmpty()) {
              teamOpt =
                  teamService.createTeam(
                      dto.getName(),
                      dto.getDescription(),
                      members.stream().map(Wrestler::getId).toList(),
                      teamSizeOf(dto),
                      null,
                      null,
                      TeamOrigin.REGISTRY);
            }
            if (teamOpt.isPresent()) {
              Team team = teamOpt.get();
              if (dto.getManager() != null) {
                Npc manager = npcByName.get(dto.getManager());
                if (manager != null) {
                  team.setManager(manager);
                  teamRepository.save(team);
                }
              }
              // Scope the seeded team to its members' shared universe (ATW-01g2 review):
              // tournament eligibility pools the registry through team.universe, so a
              // universe-less seed never reaches the wizard's registry grid. Runs after
              // WrestlerSync (@Order 120), so member states exist to agree on.
              assignUniverseFromMembers(team, universes);
            }
          } else {
            log.warn(
                "Skipping team '{}': expected {} member(s) resolvable by name, found {}",
                dto.getName(),
                expectedMemberCount(dto),
                members.size());
          }
          log.debug("Loaded team: {}", dto.getName());
        }
        log.debug("Team loading completed - {} teams processed", dtos.size());
      } catch (IOException e) {
        log.error("Error loading teams from file", e);
      }
    } else {
      log.warn("Teams file not found: {}", resource.getPath());
    }
  }

  /**
   * Scopes {@code team} to the universe all its members agree on: the universe where EVERY member
   * holds a wrestler_state. Ambiguous (members span universes) or stateless memberships leave the
   * team unassigned with a warning — the eligibility pool treats unassigned teams as never scoped,
   * never silently dropped into a wrong universe. A team already carrying a universe (e.g. resolved
   * from the session context at create) is left untouched.
   *
   * @param team the freshly seeded team
   * @param universes every universe in the install (seed installs hold one or a handful)
   */
  private void assignUniverseFromMembers(final Team team, final List<Universe> universes) {
    if (team.getUniverse() != null) {
      return;
    }
    Set<Long> shared = null;
    for (TeamMember member : team.getMembers()) {
      Set<Long> memberUniverseIds = new HashSet<>();
      for (Universe universe : universes) {
        if (wrestlerStateRepository
            .findByWrestlerAndUniverse(member.getWrestler(), universe)
            .isPresent()) {
          memberUniverseIds.add(universe.getId());
        }
      }
      if (shared == null) {
        shared = new HashSet<>(memberUniverseIds);
      } else {
        shared.retainAll(memberUniverseIds);
      }
    }
    if (shared == null || shared.isEmpty()) {
      log.warn(
          "Team '{}' members hold no wrestler states — team.universe left unset", team.getName());
      return;
    }
    if (shared.size() > 1) {
      log.warn(
          "Team '{}' members span {} universes — team.universe left unset (ambiguous)",
          team.getName(),
          shared.size());
      return;
    }
    universeRepository
        .findById(shared.iterator().next())
        .ifPresent(
            universe -> {
              team.setUniverse(universe);
              teamRepository.save(team);
              log.debug(
                  "Scoped seeded team '{}' to universe '{}' (member agreement)",
                  team.getName(),
                  universe.getName());
            });
  }

  /**
   * Resolves the entry's member list: the ordered {@code members} array when present, otherwise the
   * legacy {@code wrestler1}/{@code wrestler2} pair.
   *
   * @param dto the catalog entry
   * @param wrestlersByName wrestlers indexed by name
   * @return the resolved members, in slot order (missing names are skipped)
   */
  private List<Wrestler> resolveMembers(
      final TeamImportDTO dto, final Map<String, Wrestler> wrestlersByName) {
    List<String> names =
        dto.hasLegacyWrestlerFields()
            ? List.of(dto.getWrestler1(), dto.getWrestler2())
            : dto.getMembers();
    if (names == null) {
      return List.of();
    }
    return names.stream()
        .map(wrestlersByName::get)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  /**
   * Parses the entry's declared team size ({@link TeamSize#DUO} default for back-compat).
   *
   * @throws IllegalArgumentException when the code is not a {@link TeamSize} name
   */
  private TeamSize teamSizeOf(final TeamImportDTO dto) {
    return TeamSize.valueOf(dto.getTeamSize());
  }

  /** How many members the entry must resolve for the sync to create it. */
  private int expectedMemberCount(final TeamImportDTO dto) {
    return teamSizeOf(dto).getMemberCount();
  }
}
