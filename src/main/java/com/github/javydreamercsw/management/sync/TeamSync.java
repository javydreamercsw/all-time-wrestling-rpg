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
import com.github.javydreamercsw.management.domain.team.TeamOrigin;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamSize;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.TeamImportDTO;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.team.TeamService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

  @Autowired
  public TeamSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final boolean skipIfNotEmpty,
      final TeamService teamService,
      final TeamRepository teamRepository,
      final WrestlerRepository wrestlerRepository,
      final NpcService npcService,
      final ObjectMapper objectMapper) {
    this.skipIfNotEmpty = skipIfNotEmpty;
    this.teamService = teamService;
    this.teamRepository = teamRepository;
    this.wrestlerRepository = wrestlerRepository;
    this.npcService = npcService;
    this.objectMapper = objectMapper;
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

  /** Declared team size for the entry ({@link TeamSize#DUO} default for back-compat). */
  private TeamSize teamSizeOf(final TeamImportDTO dto) {
    return dto.getTeamSize() != null ? dto.getTeamSize() : TeamSize.DUO;
  }

  /** How many members the entry must resolve for the sync to create it. */
  private int expectedMemberCount(final TeamImportDTO dto) {
    return teamSizeOf(dto).getMemberCount();
  }
}
