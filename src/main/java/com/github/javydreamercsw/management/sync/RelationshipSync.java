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
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.RelationshipImportDTO;
import com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService;
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
@Order(130)
public class RelationshipSync implements DataSyncContributor {

  private final boolean skipIfNotEmpty;
  private final WrestlerRepository wrestlerRepository;
  private final WrestlerRelationshipService relationshipService;
  private final ObjectMapper objectMapper;

  @Autowired
  public RelationshipSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final boolean skipIfNotEmpty,
      final WrestlerRepository wrestlerRepository,
      final WrestlerRelationshipService relationshipService,
      final ObjectMapper objectMapper) {
    this.skipIfNotEmpty = skipIfNotEmpty;
    this.wrestlerRepository = wrestlerRepository;
    this.relationshipService = relationshipService;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    if (skipIfNotEmpty && relationshipService.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("relationships.json");
    if (resource.exists()) {
      log.debug("Loading relationships from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<RelationshipImportDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        Map<String, Wrestler> wrestlersByName =
            wrestlerRepository.findAll().stream()
                .collect(Collectors.toMap(Wrestler::getName, wr -> wr, (a, b) -> a));
        for (RelationshipImportDTO dto : dtos) {
          Optional<Wrestler> w1 = Optional.ofNullable(wrestlersByName.get(dto.getWrestler1()));
          Optional<Wrestler> w2 = Optional.ofNullable(wrestlersByName.get(dto.getWrestler2()));
          if (w1.isPresent() && w2.isPresent()) {
            relationshipService.createOrUpdateRelationship(
                w1.get().getId(),
                w2.get().getId(),
                dto.getType(),
                dto.getLevel(),
                dto.getIsStoryline(),
                dto.getNotes());
          }
        }
        log.debug("Relationship loading completed - {} relationships processed", dtos.size());
      } catch (IOException e) {
        log.error("Error loading relationships from file", e);
      }
    } else {
      log.warn("Relationships file not found: {}", resource.getPath());
    }
  }
}
