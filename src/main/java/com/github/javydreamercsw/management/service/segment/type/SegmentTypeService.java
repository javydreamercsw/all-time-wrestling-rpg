/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.segment.type;

import com.github.javydreamercsw.management.config.CacheConfig;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.universe.UniverseSettingsService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SegmentTypeService {

  private final SegmentTypeRepository segmentTypeRepository;
  private final ExpansionService expansionService;
  private final UniverseContextService universeContextService;
  private final UniverseSettingsService universeSettingsService;

  public SegmentTypeService(
      @NonNull final SegmentTypeRepository segmentTypeRepository,
      @NonNull final ExpansionService expansionService,
      @NonNull final UniverseContextService universeContextService,
      @NonNull final UniverseSettingsService universeSettingsService) {
    this.segmentTypeRepository = segmentTypeRepository;
    this.expansionService = expansionService;
    this.universeContextService = universeContextService;
    this.universeSettingsService = universeSettingsService;
  }

  private Set<String> enabledExpansionCodes() {
    return universeContextService
        .getCurrentUniverse()
        .map(universeSettingsService::getEnabledExpansionCodesForUniverse)
        .orElseGet(() -> new HashSet<>(expansionService.getEnabledExpansionCodes()));
  }

  @PreAuthorize("isAuthenticated()")
  public Optional<SegmentType> findByName(@NonNull final String name) {
    return segmentTypeRepository.findByName(name);
  }

  @PreAuthorize("isAuthenticated()")
  @Cacheable(value = CacheConfig.SEGMENT_TYPES_CACHE, key = "'all'")
  public List<SegmentType> findAll() {
    Set<String> enabled = enabledExpansionCodes();
    return segmentTypeRepository.findAll().stream()
        .filter(st -> st.getExpansionCode() == null || enabled.contains(st.getExpansionCode()))
        .collect(Collectors.toList());
  }

  public long count() {
    return segmentTypeRepository.count();
  }

  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @CacheEvict(value = CacheConfig.SEGMENT_TYPES_CACHE, allEntries = true)
  public SegmentType createSegmentType(@NonNull final SegmentType segmentType) {
    log.info("Creating new segment type: {}", segmentType.getName());
    return segmentTypeRepository.save(segmentType);
  }

  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @CacheEvict(value = CacheConfig.SEGMENT_TYPES_CACHE, allEntries = true)
  public SegmentType createOrUpdateSegmentType(
      @NonNull final String name, @NonNull final String description) {
    return createOrUpdateSegmentType(name, description, "BASE_GAME");
  }

  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @CacheEvict(value = CacheConfig.SEGMENT_TYPES_CACHE, allEntries = true)
  public SegmentType createOrUpdateSegmentType(
      @NonNull final String name, @NonNull final String description, final String expansionCode) {
    Optional<SegmentType> existingOpt = segmentTypeRepository.findByName(name);
    if (existingOpt.isPresent()) {
      SegmentType st = existingOpt.get();
      if (st.getDescription() != null
          && st.getDescription().equals(description)
          && st.getExpansionCode().equals(expansionCode)) {
        return st;
      }
      st.setDescription(description);
      st.setExpansionCode(expansionCode);
      log.debug("Updating existing segment type: {}", name);
      return segmentTypeRepository.save(st);
    }

    SegmentType segmentType = new SegmentType();
    log.debug("Creating new segment type: {}", name);
    segmentType.setName(name);
    segmentType.setDescription(description);
    segmentType.setExpansionCode(expansionCode != null ? expansionCode : "BASE_GAME");
    return segmentTypeRepository.save(segmentType);
  }

  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @CacheEvict(value = CacheConfig.SEGMENT_TYPES_CACHE, allEntries = true)
  public void deleteSegmentType(@NonNull final Long id) {
    if (segmentTypeRepository.existsById(id)) {
      segmentTypeRepository.deleteById(id);
      log.debug("Deleted segment type with ID: {}", id);
    } else {
      log.debug("Segment type with ID {} not found for deletion.", id);
      throw new IllegalArgumentException("Segment type not found with ID: " + id);
    }
  }
}
