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
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.domain.world.LocationRepository;
import com.github.javydreamercsw.management.dto.LocationImportDTO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(100)
public class LocationSync implements DataSyncContributor {

  private final boolean skipIfNotEmpty;
  private final LocationRepository locationRepository;
  private final ObjectMapper objectMapper;

  @Autowired
  public LocationSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final boolean skipIfNotEmpty,
      final LocationRepository locationRepository,
      final ObjectMapper objectMapper) {
    this.skipIfNotEmpty = skipIfNotEmpty;
    this.locationRepository = locationRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    if (skipIfNotEmpty && locationRepository.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("locations.json");
    if (resource.exists()) {
      log.debug("Loading locations from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<LocationImportDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        if (dtos == null) {
          return;
        }
        List<Location> toSave = new ArrayList<>();
        log.debug("Found {} locations in JSON file", dtos.size());
        for (LocationImportDTO dto : dtos) {
          Optional<Location> existing = locationRepository.findByName(dto.getName());
          if (existing.isEmpty()) {
            toSave.add(
                Location.builder()
                    .name(dto.getName())
                    .description(dto.getDescription())
                    .imageUrl(dto.getImageUrl())
                    .culturalTags(dto.getCulturalTags())
                    .build());
          } else {
            Location loc = existing.get();
            boolean changed = false;
            if (!dto.getDescription().equals(loc.getDescription())) {
              loc.setDescription(dto.getDescription());
              changed = true;
            }
            if (dto.getImageUrl() != null && !dto.getImageUrl().equals(loc.getImageUrl())) {
              loc.setImageUrl(dto.getImageUrl());
              changed = true;
            }
            if (dto.getCulturalTags() != null
                && !dto.getCulturalTags().equals(loc.getCulturalTags())) {
              loc.setCulturalTags(dto.getCulturalTags());
              changed = true;
            }
            if (changed) {
              toSave.add(loc);
            }
          }
        }
        locationRepository.saveAll(toSave);
        locationRepository.flush();
        log.debug("Location loading completed - {} locations processed", dtos.size());
      } catch (IOException e) {
        log.error("Error loading locations from file", e);
      }
    } else {
      log.warn("Locations file not found: {}", resource.getPath());
    }
  }
}
