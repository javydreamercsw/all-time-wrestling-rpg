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
      topUpMissingNpcs();
      return;
    }
    loadNpcs(true);
  }

  /**
   * Skip-gate branch: the table is non-empty and the {@code skip-if-not-empty} flag protects
   * existing rows from seed-driven updates. Still insert seed entries that don't exist yet, so
   * catalogs added in later releases reach flag-protected installs (e.g. the 2.12.0 referees).
   * Existing rows are never re-saved.
   */
  private void topUpMissingNpcs() {
    List<Npc> missing = loadNpcs(false);
    if (!missing.isEmpty()) {
      log.info("NPC top-up: inserting {} missing seed NPC(s).", missing.size());
      npcService.saveAll(missing);
    }
  }

  /**
   * Loads NPCs from {@code npcs.json}.
   *
   * @param updateExisting when true, updates existing rows to match the seed file (full sync); when
   *     false, only entries missing from the database are returned and existing rows are left
   *     untouched.
   * @return NPCs that were inserted or need saving.
   */
  private List<Npc> loadNpcs(boolean updateExisting) {
    ClassPathResource resource = new ClassPathResource("npcs.json");
    if (!resource.exists()) {
      log.warn("Npcs file not found: {}", resource.getPath());
      return List.of();
    }
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
          toSave.add(fromDto(dto));
        } else if (updateExisting) {
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
      if (!toSave.isEmpty() && updateExisting) {
        npcService.saveAll(toSave);
      }
      log.debug("Npc loading completed - {} npcs processed", dtos.size());
      return toSave;
    } catch (IOException e) {
      log.error("Error loading npcs from file", e);
      return List.of();
    }
  }

  private static Npc fromDto(NpcDTO dto) {
    Npc npc = new Npc();
    npc.setName(dto.getName());
    npc.setDescription(dto.getDescription());
    npc.setNpcType(dto.getType());
    if (dto.getExpansionCode() != null) {
      npc.setExpansionCode(dto.getExpansionCode());
    }
    if (dto.getAwareness() != null) {
      // NpcSync cannot call npcService here without coupling the mapping to the service;
      // awareness lands on the attributes map right after construction.
      Map<String, Object> attrs = npc.getAttributes();
      attrs.put(NpcService.ATTRIBUTE_AWARENESS, Math.max(0, Math.min(100, dto.getAwareness())));
      npc.setAttributes(attrs);
    }
    if (dto.getAlignment() != null) {
      try {
        npc.setAlignment(AlignmentType.valueOf(dto.getAlignment().toUpperCase()));
      } catch (IllegalArgumentException e) {
        log.warn("Invalid alignment '{}' for npc '{}'", dto.getAlignment(), dto.getName());
      }
    }
    return npc;
  }
}
