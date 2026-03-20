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
package com.github.javydreamercsw.management.service.npc;

import static com.github.javydreamercsw.management.config.CacheConfig.NPCS_CACHE;

import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.service.expansion.ExpansionToggledEvent;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NpcService {

  private final NpcRepository npcRepository;
  private final ExpansionService expansionService;

  public static final String ATTRIBUTE_AWARENESS = "awareness";

  public Npc findByName(String name) {
    return npcRepository.findByName(name).orElse(null);
  }

  public java.util.Optional<Npc> getByName(String name) {
    return npcRepository.findByName(name);
  }

  public Npc save(Npc npc) {
    return npcRepository.save(npc);
  }

  public java.util.Optional<Npc> findByExternalId(String externalId) {
    return npcRepository.findByExternalId(externalId);
  }

  public Npc findById(Long id) {
    return npcRepository.findById(id).orElse(null);
  }

  public List<Npc> findAllByType(String npcType) {
    List<String> enabledExpansions = expansionService.getEnabledExpansionCodes();
    return npcRepository.findAllByNpcType(npcType).stream()
        .filter(npc -> enabledExpansions.contains(npc.getExpansionCode()))
        .collect(Collectors.toList());
  }

  @Cacheable(value = NPCS_CACHE)
  public List<Npc> findAll() {
    List<String> enabledExpansions = expansionService.getEnabledExpansionCodes();
    return npcRepository.findAll().stream()
        .filter(npc -> enabledExpansions.contains(npc.getExpansionCode()))
        .collect(Collectors.toList());
  }

  public Page<Npc> findAll(Pageable pageable) {
    List<Npc> allFiltered = findAll();

    if (pageable.isUnpaged()) {
      return new PageImpl<>(allFiltered, pageable, allFiltered.size());
    }

    int start = (int) pageable.getOffset();
    int end = Math.min((start + pageable.getPageSize()), allFiltered.size());

    List<Npc> pageContent = new java.util.ArrayList<>();
    if (start < allFiltered.size()) {
      pageContent = allFiltered.subList(start, end);
    }

    return new PageImpl<>(pageContent, pageable, allFiltered.size());
  }

  @EventListener
  @CacheEvict(value = NPCS_CACHE, allEntries = true)
  public void onExpansionToggled(ExpansionToggledEvent event) {
    log.info("Expansion '{}' toggled, evicting NPC cache.", event.getExpansionCode());
  }

  public void delete(Npc npc) {
    npcRepository.delete(npc);
  }

  /**
   * Gets the awareness level of a referee NPC.
   *
   * @param npc The NPC to check.
   * @return The awareness level (0-100), defaults to 50 if not set.
   */
  public int getAwareness(Npc npc) {
    if (npc == null || npc.getAttributes() == null) {
      return 50;
    }
    Object val = npc.getAttributes().get(ATTRIBUTE_AWARENESS);
    if (val instanceof Number) {
      return ((Number) val).intValue();
    }
    return 50;
  }

  /**
   * Sets the awareness level of a referee NPC.
   *
   * @param npc The NPC to update.
   * @param awareness The awareness level (0-100).
   */
  @Transactional
  public void setAwareness(Npc npc, int awareness) {
    if (npc == null) {
      return;
    }
    Map<String, Object> attrs = npc.getAttributes();
    attrs.put(ATTRIBUTE_AWARENESS, Math.max(0, Math.min(100, awareness)));
    npc.setAttributes(attrs);
    npcRepository.save(npc);
  }
}
