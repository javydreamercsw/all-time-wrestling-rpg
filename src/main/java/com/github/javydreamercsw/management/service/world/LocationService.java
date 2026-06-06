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

import com.github.javydreamercsw.base.image.DefaultImageService;
import com.github.javydreamercsw.base.image.ImageCategory;
import com.github.javydreamercsw.management.config.CacheConfig;
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.domain.world.LocationRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
  private final DefaultImageService imageService;

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  @CacheEvict(value = CacheConfig.LOCATIONS_CACHE, allEntries = true)
  public Location createLocation(
      final String name,
      final String description,
      final String imageUrl,
      final Set<String> culturalTags) {
    Location location =
        Location.builder()
            .name(name)
            .description(description)
            .imageUrl(imageUrl)
            .culturalTags(culturalTags)
            .build();
    return repository.save(location);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  @CacheEvict(value = CacheConfig.LOCATIONS_CACHE, allEntries = true)
  public Optional<Location> updateLocation(
      final Long id,
      final String name,
      final String description,
      final String imageUrl,
      final Set<String> culturalTags) {
    return repository
        .findById(id)
        .map(
            location -> {
              location.setName(name);
              location.setDescription(description);
              location.setImageUrl(imageUrl);
              location.setCulturalTags(culturalTags);
              return repository.save(location);
            });
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Optional<Location> findById(final Long id) {
    return repository.findById(id);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  @Cacheable(value = CacheConfig.LOCATIONS_CACHE, key = "'all'")
  public List<Location> findAll() {
    return repository.findAll();
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Page<Location> list(final Pageable pageable) {
    return repository.findAll(pageable);
  }

  public long count() {
    return repository.count();
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  @CacheEvict(value = CacheConfig.LOCATIONS_CACHE, allEntries = true)
  public void deleteLocation(final Long id) {
    repository.deleteById(id);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  public Optional<Location> findByName(final String name) {
    return repository.findByName(name);
  }

  /**
   * Resolves the image URL for a location, using the default image system if no specific URL is
   * set.
   *
   * @param location The location entity.
   * @return The resolved image URL.
   */
  public String resolveLocationImage(final Location location) {
    if (location.getImageUrl() != null && !location.getImageUrl().isBlank()) {
      return location.getImageUrl();
    }
    return imageService.resolveImage(location.getName(), ImageCategory.LOCATION).url();
  }
}
