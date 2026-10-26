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
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.dto.SegmentTypeDTO;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(50)
public class SegmentTypeSync implements DataSyncContributor {

  private final boolean skipIfNotEmpty;
  private final SegmentTypeService segmentTypeService;
  private final ObjectMapper objectMapper;

  @Autowired
  public SegmentTypeSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final boolean skipIfNotEmpty,
      final SegmentTypeService segmentTypeService,
      final ObjectMapper objectMapper) {
    this.skipIfNotEmpty = skipIfNotEmpty;
    this.segmentTypeService = segmentTypeService;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    if (skipIfNotEmpty && segmentTypeService.count() > 0) {
      topUpMissingSegmentTypes();
      return;
    }
    loadSegmentTypes(true);
  }

  /**
   * Skip-gate branch: the table is non-empty and the {@code skip-if-not-empty} flag protects
   * existing rows from seed-driven updates. Still create seed entries that don't exist yet, so
   * catalogs added in later releases reach flag-protected installs (e.g. the 2.12.0 Trios segment
   * type). Existing rows are never passed to the service.
   */
  private void topUpMissingSegmentTypes() {
    List<String> existingNames =
        segmentTypeService.findAll().stream().map(SegmentType::getName).toList();
    List<SegmentTypeDTO> dtos = parseSeedFile();
    for (SegmentTypeDTO dto : dtos) {
      if (!existingNames.contains(dto.getName())) {
        log.info("Segment type top-up: inserting missing seed entry '{}'.", dto.getName());
        segmentTypeService.createOrUpdateSegmentType(
            dto.getName(),
            dto.getDescription(),
            dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME",
            dto.getGuide(),
            dto.getCode(),
            dto.getEventOnly());
      }
    }
  }

  private void loadSegmentTypes(boolean updateExisting) {
    List<SegmentTypeDTO> dtos = parseSeedFile();
    for (SegmentTypeDTO dto : dtos) {
      SegmentType segmentType =
          segmentTypeService.createOrUpdateSegmentType(
              dto.getName(),
              dto.getDescription(),
              dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME",
              dto.getGuide(),
              dto.getCode(),
              dto.getEventOnly());
      log.debug(
          "Loaded segment type: {} (Players: {})",
          segmentType.getName(),
          dto.isUnlimited() ? "Unlimited" : dto.getPlayerAmount());
    }
    log.debug("Segment type loading completed");
  }

  private List<SegmentTypeDTO> parseSeedFile() {
    ClassPathResource resource = new ClassPathResource("segment_types.json");
    if (!resource.exists()) {
      log.warn("Segment types file not found: {}", resource.getPath());
      return List.of();
    }
    log.debug("Loading segment types from file: {}", resource.getPath());
    try (var is = resource.getInputStream()) {
      return objectMapper.readValue(is, new TypeReference<>() {});
    } catch (IOException e) {
      log.error("Error loading segment types from file", e);
      return List.of();
    }
  }
}
