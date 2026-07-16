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

import com.github.javydreamercsw.base.image.DefaultImageService;
import com.github.javydreamercsw.base.image.ImageCategory;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.service.expansion.ExpansionToggledEvent;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.universe.UniverseExpansionToggledEvent;
import com.github.javydreamercsw.management.service.universe.UniverseSettingsService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class NpcService {

  private final NpcRepository npcRepository;
  private final ExpansionService expansionService;
  private final UniverseContextService universeContextService;
  private final UniverseSettingsService universeSettingsService;
  private final DefaultImageService imageService;

  @Autowired
  public NpcService(
      final NpcRepository npcRepository,
      final ExpansionService expansionService,
      final UniverseContextService universeContextService,
      final UniverseSettingsService universeSettingsService,
      final DefaultImageService imageService) {
    this.npcRepository = npcRepository;
    this.expansionService = expansionService;
    this.universeContextService = universeContextService;
    this.universeSettingsService = universeSettingsService;
    this.imageService = imageService;
  }

  private Set<String> enabledExpansionCodes() {
    return universeContextService
        .getCurrentUniverse()
        .map(universeSettingsService::getEnabledExpansionCodesForUniverse)
        .orElseGet(() -> new java.util.HashSet<>(expansionService.getEnabledExpansionCodes()));
  }

  public static final String ATTRIBUTE_AWARENESS = "awareness";

  public Npc findByName(final String name) {
    return npcRepository.findByName(name).orElse(null);
  }

  public java.util.Optional<Npc> getByName(final String name) {
    return npcRepository.findByName(name);
  }

  public Npc save(final Npc npc) {
    return npcRepository.save(npc);
  }

  @Transactional
  public List<Npc> saveAll(final List<Npc> npcs) {
    return npcRepository.saveAll(npcs);
  }

  public Npc findById(final Long id) {
    return npcRepository.findById(id).orElse(null);
  }

  public List<Npc> findAllByType(final String npcType) {
    Set<String> enabledExpansions = enabledExpansionCodes();
    return npcRepository.findAllByNpcType(npcType).stream()
        .filter(Npc::isActive)
        .filter(
            npc ->
                npc.getExpansionCode() == null
                    || enabledExpansions.contains(npc.getExpansionCode()))
        .collect(Collectors.toList());
  }

  /**
   * Returns active NPCs of a given type without expansion filtering. Safe to call from background
   * threads (e.g. NarrationDialog async load).
   */
  public List<Npc> findAllByTypeUnfiltered(final String npcType) {
    return npcRepository.findAllByNpcType(npcType).stream()
        .filter(Npc::isActive)
        .sorted(Comparator.comparing(Npc::getName))
        .collect(Collectors.toList());
  }

  /**
   * Returns all NPCs without any filtering. Used by DataInitializer for seeding dirty-checks — must
   * include inactive so disabled NPCs are not re-created.
   */
  public List<Npc> findAllUnfiltered() {
    return npcRepository.findAll().stream()
        .sorted(Comparator.comparing(Npc::getName))
        .collect(Collectors.toList());
  }

  /**
   * Returns all NPCs including inactive ones, filtered only by enabled expansions. Use this in
   * admin list views where managers need to see and re-enable disabled NPCs.
   */
  public List<Npc> findAllIncludingInactive() {
    Set<String> enabledExpansions = enabledExpansionCodes();
    return npcRepository.findAll().stream()
        .filter(
            npc ->
                npc.getExpansionCode() == null
                    || enabledExpansions.contains(npc.getExpansionCode()))
        .sorted(Comparator.comparing(Npc::getName))
        .collect(Collectors.toList());
  }

  @Cacheable(value = NPCS_CACHE)
  public List<Npc> findAll() {
    return findAll(enabledExpansionCodes());
  }

  /**
   * Expansion-filtered lookup using caller-supplied codes. Use this from async contexts or request
   * threads where the thread-local universe context is unavailable (e.g. cache warm-up), so the
   * result isn't cached under the universe-agnostic {@link #findAll()} key and served back to
   * unrelated universes for the cache TTL.
   */
  public List<Npc> findAll(final Set<String> enabledExpansionCodes) {
    return npcRepository.findAll().stream()
        .filter(Npc::isActive)
        .filter(
            npc ->
                npc.getExpansionCode() == null
                    || enabledExpansionCodes.contains(npc.getExpansionCode()))
        .collect(Collectors.toList());
  }

  public Page<Npc> findAll(final Pageable pageable) {
    List<Npc> allFiltered = findAll();

    if (pageable.isUnpaged()) {
      return new PageImpl<>(allFiltered, pageable, allFiltered.size());
    }

    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), allFiltered.size());

    List<Npc> pageContent = new java.util.ArrayList<>();
    if (start < allFiltered.size()) {
      pageContent = allFiltered.subList(start, end);
    }

    return new PageImpl<>(pageContent, pageable, allFiltered.size());
  }

  @EventListener
  @CacheEvict(value = NPCS_CACHE, allEntries = true)
  public void onExpansionToggled(final ExpansionToggledEvent event) {
    log.info("Expansion '{}' toggled, evicting NPC cache.", event.getExpansionCode());
  }

  @EventListener
  @CacheEvict(value = NPCS_CACHE, allEntries = true)
  public void onUniverseExpansionToggled(final UniverseExpansionToggledEvent event) {
    log.info(
        "Universe {} expansion '{}' toggled, evicting NPC cache.",
        event.getUniverseId(),
        event.getExpansionCode());
  }

  @Transactional
  @CacheEvict(value = NPCS_CACHE, allEntries = true)
  public void setActive(final Long id, final boolean active) {
    npcRepository
        .findById(id)
        .ifPresent(
            npc -> {
              npc.setActive(active);
              npcRepository.save(npc);
            });
  }

  public void delete(final Npc npc) {
    npcRepository.delete(npc);
  }

  /**
   * Resolves the image URL for an NPC, using the default image system if no specific URL is set.
   *
   * @param npc The NPC entity.
   * @return The resolved image URL.
   */
  public String resolveNpcImage(final Npc npc) {
    if (npc.getImageUrl() != null && !npc.getImageUrl().isBlank()) {
      return npc.getImageUrl();
    }
    return imageService.resolveImage(npc.getName(), ImageCategory.NPC).url();
  }

  /**
   * Gets the awareness level of a referee NPC.
   *
   * @param npc The NPC to check.
   * @return The awareness level (0-100), defaults to 50 if not set.
   */
  public int getAwareness(final Npc npc) {
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
  public void setAwareness(final Npc npc, final int awareness) {
    if (npc == null) {
      return;
    }
    Map<String, Object> attrs = npc.getAttributes();
    attrs.put(ATTRIBUTE_AWARENESS, Math.max(0, Math.min(100, awareness)));
    npc.setAttributes(attrs);
    npcRepository.save(npc);
  }

  public long count() {
    return npcRepository.count();
  }
}
