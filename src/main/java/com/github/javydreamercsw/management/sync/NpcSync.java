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
import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.dto.NpcDTO;
import com.github.javydreamercsw.management.service.npc.NpcService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(90)
public class NpcSync implements DataSyncContributor {

  private final boolean skipIfNotEmpty;
  private final NpcService npcService;
  private final ObjectMapper objectMapper;

  @Autowired
  public NpcSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final boolean skipIfNotEmpty,
      final NpcService npcService,
      final ObjectMapper objectMapper) {
    this.skipIfNotEmpty = skipIfNotEmpty;
    this.npcService = npcService;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    if (skipIfNotEmpty && npcService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("npcs.json");
    if (resource.exists()) {
      log.debug("Loading npcs from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<NpcDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        Map<String, Npc> existingNpcs =
            npcService.findAllUnfiltered().stream()
                .collect(Collectors.toMap(Npc::getName, n -> n, (a, b) -> a));
        List<Npc> toSave = new ArrayList<>();
        for (NpcDTO dto : dtos) {
          Npc npc = existingNpcs.get(dto.getName());
          if (npc == null) {
            npc = new Npc();
            npc.setName(dto.getName());
            npc.setDescription(dto.getDescription());
            npc.setNpcType(dto.getType());
            if (dto.getExpansionCode() != null) {
              npc.setExpansionCode(dto.getExpansionCode());
            }
            if (dto.getAwareness() != null) {
              npcService.setAwareness(npc, dto.getAwareness());
            }
            if (dto.getAlignment() != null) {
              try {
                npc.setAlignment(AlignmentType.valueOf(dto.getAlignment().toUpperCase()));
              } catch (IllegalArgumentException e) {
                log.warn("Invalid alignment '{}' for npc '{}'", dto.getAlignment(), dto.getName());
              }
            }
            toSave.add(npc);
          } else {
            boolean changed = false;
            if (!Objects.equals(npc.getDescription(), dto.getDescription())) {
              npc.setDescription(dto.getDescription());
              changed = true;
            }
            if (!Objects.equals(npc.getNpcType(), dto.getType())) {
              npc.setNpcType(dto.getType());
              changed = true;
            }
            if (dto.getExpansionCode() != null
                && !Objects.equals(npc.getExpansionCode(), dto.getExpansionCode())) {
              npc.setExpansionCode(dto.getExpansionCode());
              changed = true;
            }
            if (dto.getAlignment() != null) {
              try {
                AlignmentType at = AlignmentType.valueOf(dto.getAlignment().toUpperCase());
                if (!Objects.equals(npc.getAlignment(), at)) {
                  npc.setAlignment(at);
                  changed = true;
                }
              } catch (IllegalArgumentException e) {
                log.warn("Invalid alignment '{}' for npc '{}'", dto.getAlignment(), dto.getName());
              }
            }
            if (dto.getAwareness() != null) {
              npcService.setAwareness(npc, dto.getAwareness());
            }
            if (changed) {
              toSave.add(npc);
            }
          }
        }
        if (!toSave.isEmpty()) {
          npcService.saveAll(toSave);
        }
        log.debug("Npc loading completed - {} npcs processed", dtos.size());
      } catch (IOException e) {
        log.error("Error loading npcs from file", e);
      }
    } else {
      log.warn("Npcs file not found: {}", resource.getPath());
    }
  }
}
