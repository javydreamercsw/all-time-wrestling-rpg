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

import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SegmentTypeService {

  private final SegmentTypeRepository segmentTypeRepository;

  public SegmentTypeService(@NonNull SegmentTypeRepository segmentTypeRepository) {
    this.segmentTypeRepository = segmentTypeRepository;
  }

  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.SEGMENT_TYPES_CACHE,
      key = "#name")
  public Optional<SegmentType> findByName(@NonNull String name) {
    return segmentTypeRepository.findByName(name);
  }

  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.SEGMENT_TYPES_CACHE,
      key = "'all'")
  public List<SegmentType> findAll() {
    return segmentTypeRepository.findAll();
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.SEGMENT_TYPES_CACHE,
      allEntries = true)
  public SegmentType createSegmentType(@NonNull SegmentType segmentType) {
    log.info("Creating new segment type: {}", segmentType.getName());
    return segmentTypeRepository.save(segmentType);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.SEGMENT_TYPES_CACHE,
      allEntries = true)
  public SegmentType createOrUpdateSegmentType(@NonNull String name, @NonNull String description) {
    Optional<SegmentType> existingOpt = segmentTypeRepository.findByName(name);
    SegmentType segmentType;
    if (existingOpt.isPresent()) {
      segmentType = existingOpt.get();
      log.debug("Updating existing segment type: {}", name);
    } else {
      segmentType = new SegmentType();
      log.debug("Creating new segment type: {}", name);
    }
    segmentType.setName(name);
    segmentType.setDescription(description);
    return segmentTypeRepository.save(segmentType);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.SEGMENT_TYPES_CACHE,
      allEntries = true)
  public void deleteSegmentType(@NonNull Long id) {
    if (segmentTypeRepository.existsById(id)) {
      segmentTypeRepository.deleteById(id);
      log.debug("Deleted segment type with ID: {}", id);
    } else {
      log.warn("Segment type with ID {} not found for deletion.", id);
      throw new IllegalArgumentException("Segment type not found with ID: " + id);
    }
  }
}
