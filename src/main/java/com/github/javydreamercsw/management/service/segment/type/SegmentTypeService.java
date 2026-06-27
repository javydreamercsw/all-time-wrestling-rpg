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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.config.CacheConfig;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRulePlayGuide;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.universe.UniverseSettingsService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
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

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    Map<String, Integer> priorities = expansionService.buildPriorityMap();
    return segmentTypeRepository.findAll().stream()
        .filter(st -> st.getExpansionCode() == null || enabled.contains(st.getExpansionCode()))
        .collect(
            Collectors.toMap(
                SegmentType::getName,
                st -> st,
                (a, b) ->
                    priorities.getOrDefault(a.getExpansionCode(), 0)
                            >= priorities.getOrDefault(b.getExpansionCode(), 0)
                        ? a
                        : b))
        .values()
        .stream()
        .sorted(Comparator.comparing(SegmentType::getName))
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
    return createOrUpdateSegmentType(name, description, "BASE_GAME", null);
  }

  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @CacheEvict(value = CacheConfig.SEGMENT_TYPES_CACHE, allEntries = true)
  public SegmentType createOrUpdateSegmentType(
      @NonNull final String name, @NonNull final String description, final String expansionCode) {
    return createOrUpdateSegmentType(name, description, expansionCode, null);
  }

  @Transactional
  @PreAuthorize(
      "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER') or hasAuthority('ROLE_SYSTEM')")
  @CacheEvict(value = CacheConfig.SEGMENT_TYPES_CACHE, allEntries = true)
  public SegmentType createOrUpdateSegmentType(
      @NonNull final String name,
      @NonNull final String description,
      final String expansionCode,
      final SegmentRulePlayGuide guide) {
    String incomingHash = computeGuideHash(guide);
    Optional<SegmentType> existingOpt = segmentTypeRepository.findByName(name);
    if (existingOpt.isPresent()) {
      SegmentType st = existingOpt.get();
      boolean guideChanged = !java.util.Objects.equals(st.getGuideHash(), incomingHash);
      if (java.util.Objects.equals(st.getDescription(), description)
          && java.util.Objects.equals(st.getExpansionCode(), expansionCode)
          && !guideChanged) {
        return st;
      }
      st.setDescription(description);
      st.setExpansionCode(expansionCode != null ? expansionCode : "BASE_GAME");
      if (guideChanged) {
        st.setGuide(guide);
        st.setGuideHash(incomingHash);
      }
      log.debug("Updating existing segment type: {}", name);
      return segmentTypeRepository.save(st);
    }

    SegmentType segmentType = new SegmentType();
    log.debug("Creating new segment type: {}", name);
    segmentType.setName(name);
    segmentType.setDescription(description);
    segmentType.setExpansionCode(expansionCode != null ? expansionCode : "BASE_GAME");
    segmentType.setGuide(guide);
    segmentType.setGuideHash(incomingHash);
    return segmentTypeRepository.save(segmentType);
  }

  private String computeGuideHash(final SegmentRulePlayGuide guide) {
    if (guide == null) {
      return null;
    }
    try {
      String json = OBJECT_MAPPER.writeValueAsString(guide);
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (JsonProcessingException | NoSuchAlgorithmException e) {
      log.warn("Failed to compute guide hash for segment type", e);
      return null;
    }
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
