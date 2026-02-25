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
package com.github.javydreamercsw.management.service.world;

import com.github.javydreamercsw.base.ai.image.ImageGenerationService;
import com.github.javydreamercsw.base.ai.image.ImageGenerationService.ImageRequest;
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.domain.world.Arena.AlignmentBias;
import com.github.javydreamercsw.management.domain.world.ArenaRepository;
import com.github.javydreamercsw.management.domain.world.Location;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ArenaService {

  private final ArenaRepository repository;
  private final LocationService locationService;
  private final ImageGenerationService imageGenerationService;

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Arena createArena(
      String name,
      String description,
      Long locationId,
      Integer capacity,
      AlignmentBias alignmentBias,
      Set<String> environmentalTraits) {
    Location location =
        locationService
            .findById(locationId)
            .orElseThrow(() -> new IllegalArgumentException("Location not found"));
    Arena arena =
        Arena.builder()
            .name(name)
            .description(description)
            .location(location)
            .capacity(capacity)
            .alignmentBias(alignmentBias)
            .environmentalTraits(environmentalTraits)
            .build();
    return repository.save(arena);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<Arena> updateArena(
      Long id,
      String name,
      String description,
      Long locationId,
      Integer capacity,
      AlignmentBias alignmentBias,
      String imageUrl,
      Set<String> environmentalTraits) {
    return repository
        .findById(id)
        .map(
            arena -> {
              Location location =
                  locationService
                      .findById(locationId)
                      .orElseThrow(() -> new IllegalArgumentException("Location not found"));
              arena.setName(name);
              arena.setDescription(description);
              arena.setLocation(location);
              arena.setCapacity(capacity);
              arena.setAlignmentBias(alignmentBias);
              arena.setImageUrl(imageUrl);
              arena.setEnvironmentalTraits(environmentalTraits);
              return repository.save(arena);
            });
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<Arena> findById(Long id) {
    return repository.findById(id);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public List<Arena> findAll() {
    return repository.findAll();
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Page<Arena> list(Pageable pageable) {
    return repository.findAll(pageable);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public long count() {
    return repository.count();
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public void deleteArena(Long id) {
    repository.deleteById(id);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<Arena> findByName(String name) {
    return repository.findByName(name);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<String> generateArenaImage(Long arenaId) {
    return repository
        .findById(arenaId)
        .map(
            arena -> {
              String prompt =
                  String.format(
                      "A futuristic wrestling arena named '%s' located in '%s'. Description: '%s'."
                          + " Capacity: %d. Alignment Bias: %s. Environmental Traits: %s. Focus on"
                          + " the unique visual style implied by these characteristics. Science"
                          + " fiction, fantasy art, digital painting.",
                      arena.getName(),
                      arena.getLocation().getName(),
                      arena.getDescription(),
                      arena.getCapacity(),
                      arena.getAlignmentBias().getDisplayName(),
                      String.join(", ", arena.getEnvironmentalTraits()));

              ImageRequest request =
                  ImageRequest.builder()
                      .prompt(prompt)
                      .size("1024x1024") // Standard size
                      .responseFormat("url")
                      .build();

              String imageUrl = imageGenerationService.generateImage(request);
              if (imageUrl != null && !imageUrl.isBlank()) {
                arena.setImageUrl(imageUrl);
                repository.save(arena);
                return imageUrl;
              }
              return null;
            });
  }
}
