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
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.FactionImportDTO;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
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
@Order(160)
public class FactionSync implements DataSyncContributor {

  private final boolean skipIfNotEmpty;
  private final FactionService factionService;
  private final WrestlerRepository wrestlerRepository;
  private final NpcService npcService;
  private final UniverseRepository universeRepository;
  private final ObjectMapper objectMapper;

  @Autowired
  public FactionSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final boolean skipIfNotEmpty,
      final FactionService factionService,
      final WrestlerRepository wrestlerRepository,
      final NpcService npcService,
      final UniverseRepository universeRepository,
      final ObjectMapper objectMapper) {
    this.skipIfNotEmpty = skipIfNotEmpty;
    this.factionService = factionService;
    this.wrestlerRepository = wrestlerRepository;
    this.npcService = npcService;
    this.universeRepository = universeRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    if (skipIfNotEmpty && factionService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("factions.json");
    if (resource.exists()) {
      log.debug("Loading factions from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<FactionImportDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        Long universeId =
            universeRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No universe found"))
                .getId();
        Map<String, Wrestler> wrestlersByName =
            wrestlerRepository.findAll().stream()
                .collect(Collectors.toMap(Wrestler::getName, wr -> wr, (a, b) -> a));
        Map<String, Npc> npcByName =
            npcService.findAllUnfiltered().stream()
                .collect(Collectors.toMap(Npc::getName, n -> n, (a, b) -> a));
        for (FactionImportDTO dto : dtos) {
          Optional<Wrestler> leaderOpt = Optional.ofNullable(wrestlersByName.get(dto.getLeader()));
          if (leaderOpt.isPresent()) {
            Optional<Faction> factionOpt = factionService.getFactionByName(dto.getName());
            if (factionOpt.isEmpty()) {
              factionOpt =
                  factionService.createFaction(
                      dto.getName(), dto.getDescription(), leaderOpt.get().getId(), universeId);
            }
            if (factionOpt.isPresent()) {
              Faction faction = factionOpt.get();
              for (String memberName : dto.getMembers()) {
                Optional.ofNullable(wrestlersByName.get(memberName))
                    .ifPresent(
                        wrestler ->
                            factionService.addMemberToFaction(faction.getId(), wrestler.getId()));
              }
              if (dto.getManager() != null) {
                Npc manager = npcByName.get(dto.getManager());
                if (manager != null) {
                  faction.setManager(manager);
                  factionService.save(faction);
                }
              }
            }
          }
          log.debug("Loaded faction: {}", dto.getName());
        }
        log.debug("Faction loading completed - {} factions processed", dtos.size());
      } catch (IOException e) {
        log.error("Error loading factions from file", e);
      }
    } else {
      log.warn("Factions file not found: {}", resource.getPath());
    }
  }
}
