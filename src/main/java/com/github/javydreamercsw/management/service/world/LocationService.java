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

import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.domain.world.LocationRepository;
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
public class LocationService {

  private final LocationRepository repository;

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Location createLocation(
      String name, String description, Set<String> culturalTags) {
    Location location =
        Location.builder()
            .name(name)
            .description(description)
            .culturalTags(culturalTags)
            .build();
    return repository.save(location);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<Location> updateLocation(
      Long id, String name, String description, Set<String> culturalTags) {
    return repository
        .findById(id)
        .map(
            location -> {
              location.setName(name);
              location.setDescription(description);
              location.setCulturalTags(culturalTags);
              return repository.save(location);
            });
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<Location> findById(Long id) {
    return repository.findById(id);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public List<Location> findAll() {
    return repository.findAll();
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Page<Location> list(Pageable pageable) {
    return repository.findAll(pageable);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public long count() {
    return repository.count();
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public void deleteLocation(Long id) {
    repository.deleteById(id);
  }
  
  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public Optional<Location> findByName(String name) {
    return repository.findByName(name);
  }
}
