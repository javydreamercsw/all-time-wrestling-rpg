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
import com.github.javydreamercsw.management.dto.SegmentRuleDTO;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(30)
public class SegmentRuleSync implements DataSyncContributor {

  private final SegmentRuleService segmentRuleService;
  private final ObjectMapper objectMapper;

  @Autowired
  public SegmentRuleSync(
      final SegmentRuleService segmentRuleService, final ObjectMapper objectMapper) {
    this.segmentRuleService = segmentRuleService;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    ClassPathResource resource = new ClassPathResource("segment_rules.json");
    if (resource.exists()) {
      log.debug("Loading segment rules from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<SegmentRuleDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        for (SegmentRuleDTO dto : dtos) {
          segmentRuleService.createOrUpdateRule(
              dto.getName(),
              dto.getDescription(),
              dto.isRequiresHighHeat(),
              dto.isNoDq(),
              dto.getBumpAddition(),
              dto.getExpansionCode() != null ? dto.getExpansionCode() : "BASE_GAME",
              dto.getGuide(),
              dto.isAllowsRefereeStopage());
        }
        log.debug("Segment rule loading completed - {} rules loaded", dtos.size());
      } catch (IOException e) {
        log.error("Error loading segment rules from file", e);
      }
    } else {
      log.warn("Segment rules file not found: {}", resource.getPath());
    }
  }
}
