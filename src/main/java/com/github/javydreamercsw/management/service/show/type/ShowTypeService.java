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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class ShowTypeService {
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private Clock clock;

  public List<ShowType> list(@NonNull Pageable pageable) {
    return showTypeRepository.findAllBy(pageable).toList();
  }

  public long count() {
    return showTypeRepository.count();
  }

  public ShowType save(@NonNull ShowType showType) {
    showType.setCreationDate(clock.instant());
    return showTypeRepository.saveAndFlush(showType);
  }

  public List<ShowType> findAll() {
    return showTypeRepository.findAll();
  }

  public void delete(@NonNull ShowType showType) {
    showTypeRepository.delete(showType);
  }

  public ShowType createOrUpdateShowType(
      @NonNull String name, @NonNull String description, int expectedMatches, int expectedPromos) {
    Optional<ShowType> existingShowType = findByName(name);
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
  public Optional<ShowType> findByName(@NonNull String name) {
    return showTypeRepository.findByName(name);
  }

  /**
   * Check if a show type exists by name.
   *
   * @param name The name to check
   * @return true if a show type with this name exists
   */
  public boolean existsByName(@NonNull String name) {
    return showTypeRepository.existsByName(name);
  }
}
