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
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.domain.world.ArenaRepository;
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.domain.world.LocationRepository;
import com.github.javydreamercsw.management.dto.ArenaImportDTO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
@Order(110)
public class ArenaSync implements DataSyncContributor {

  private final boolean skipIfNotEmpty;
  private final ArenaRepository arenaRepository;
  private final LocationRepository locationRepository;
  private final ObjectMapper objectMapper;

  @Autowired
  public ArenaSync(
      @Value("${data.initializer.skip-if-not-empty:false}") final boolean skipIfNotEmpty,
      final ArenaRepository arenaRepository,
      final LocationRepository locationRepository,
      final ObjectMapper objectMapper) {
    this.skipIfNotEmpty = skipIfNotEmpty;
    this.arenaRepository = arenaRepository;
    this.locationRepository = locationRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public void sync() {
    if (skipIfNotEmpty && arenaRepository.count() > 0) {
      return;
    }
    ClassPathResource resource = new ClassPathResource("arenas.json");
    if (resource.exists()) {
      log.debug("Loading arenas from file: {}", resource.getPath());
      try (var is = resource.getInputStream()) {
        List<ArenaImportDTO> dtos = objectMapper.readValue(is, new TypeReference<>() {});
        if (dtos == null) {
          return;
        }
        Map<String, Arena> existingByName =
            arenaRepository.findAllWithLocation().stream()
                .collect(Collectors.toMap(Arena::getName, a -> a));
        List<Arena> toSave = new ArrayList<>();
        log.debug("Found {} arenas in JSON file", dtos.size());
        for (ArenaImportDTO dto : dtos) {
          Optional<Arena> existingArena = Optional.ofNullable(existingByName.get(dto.getName()));
          if (existingArena.isEmpty()) {
            Optional<Location> location = locationRepository.findByName(dto.getLocation());
            if (location.isPresent()) {
              toSave.add(
                  Arena.builder()
                      .name(dto.getName())
                      .description(dto.getDescription())
                      .location(location.get())
                      .capacity(dto.getCapacity())
                      .alignmentBias(dto.getAlignmentBias())
                      .imageUrl(dto.getImageUrl())
                      .environmentalTraits(dto.getEnvironmentalTraits())
                      .build());
            }
          } else {
            Arena existing = existingArena.get();
            boolean changed = false;
            if (!Objects.equals(dto.getDescription(), existing.getDescription())) {
              existing.setDescription(dto.getDescription());
              changed = true;
            }
            if (dto.getCapacity() != existing.getCapacity()) {
              existing.setCapacity(dto.getCapacity());
              changed = true;
            }
            if (dto.getAlignmentBias() != existing.getAlignmentBias()) {
              existing.setAlignmentBias(dto.getAlignmentBias());
              changed = true;
            }
            if (!Objects.equals(dto.getImageUrl(), existing.getImageUrl())) {
              existing.setImageUrl(dto.getImageUrl());
              changed = true;
            }
            if (!Objects.equals(dto.getEnvironmentalTraits(), existing.getEnvironmentalTraits())) {
              existing.setEnvironmentalTraits(dto.getEnvironmentalTraits());
              changed = true;
            }
            Optional<Location> locationOpt = locationRepository.findByName(dto.getLocation());
            if (locationOpt.isPresent() && !locationOpt.get().equals(existing.getLocation())) {
              existing.setLocation(locationOpt.get());
              changed = true;
            }
            if (changed) {
              toSave.add(existing);
            }
          }
        }
        arenaRepository.saveAll(toSave);
        log.debug("Arena loading completed - {} arenas processed", dtos.size());
      } catch (IOException e) {
        log.error("Error loading arenas from file", e);
      }
    } else {
      log.warn("Arenas file not found: {}", resource.getPath());
    }
  }
}
