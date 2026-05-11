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
package com.github.javydreamercsw.management.service.show.type;

import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class ShowTypeService {
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private Clock clock;

  @PreAuthorize("isAuthenticated()")
  public List<ShowType> list(@NonNull final Pageable pageable) {
    return showTypeRepository.findAllBy(pageable).toList();
  }

  public long count() {
    return showTypeRepository.count();
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.SHOW_TYPES_CACHE,
      allEntries = true)
  public ShowType save(@NonNull final ShowType showType) {
    showType.setCreationDate(clock.instant());
    return showTypeRepository.saveAndFlush(showType);
  }

  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.SHOW_TYPES_CACHE,
      key = "'all'")
  public List<ShowType> findAll() {
    return showTypeRepository.findAll();
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.SHOW_TYPES_CACHE,
      allEntries = true)
  public void delete(@NonNull final ShowType showType) {
    showTypeRepository.delete(showType);
  }

  @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_BOOKER')")
  @org.springframework.cache.annotation.CacheEvict(
      value = com.github.javydreamercsw.management.config.CacheConfig.SHOW_TYPES_CACHE,
      allEntries = true)
  public ShowType createOrUpdateShowType(
      @NonNull final String name,
      @NonNull final String description,
      final int expectedMatches,
      final int expectedPromos) {
    Optional<ShowType> existingShowType = findByName(name);
    if (existingShowType.isPresent()) {
      ShowType st = existingShowType.get();
      if (st.getName().equals(name)
          && st.getDescription().equals(description)
          && st.getExpectedMatches() == expectedMatches
          && st.getExpectedPromos() == expectedPromos) {
        return st;
      }
    }
    ShowType showType = existingShowType.orElseGet(ShowType::new);
    showType.setName(name);
    showType.setDescription(description);
    showType.setExpectedMatches(expectedMatches);
    showType.setExpectedPromos(expectedPromos);
    return save(showType);
  }

  /**
   * Find a show type by name.
   *
   * @param name The name of the show type
   * @return Optional containing the show type if found
   */
  @PreAuthorize("isAuthenticated()")
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.SHOW_TYPES_CACHE,
      key = "#name")
  public Optional<ShowType> findByName(@NonNull final String name) {
    return showTypeRepository.findByName(name);
  }

  /**
   * Check if a show type exists by name.
   *
   * @param name The name to check
   * @return true if a show type with this name exists
   */
  @PreAuthorize("isAuthenticated()")
  public boolean existsByName(@NonNull final String name) {
    return showTypeRepository.existsByName(name);
  }
}
