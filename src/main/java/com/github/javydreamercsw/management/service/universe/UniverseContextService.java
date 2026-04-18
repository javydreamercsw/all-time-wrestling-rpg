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
package com.github.javydreamercsw.management.service.universe;

import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import java.io.Serializable;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@VaadinSessionScope
@Slf4j
@RequiredArgsConstructor
public class UniverseContextService implements Serializable {

  private final UniverseRepository universeRepository;
  private Long currentUniverseId;

  public Optional<Universe> getCurrentUniverse() {
    if (currentUniverseId == null) {
      // Default to the first universe found if none selected
      return universeRepository.findAll().stream()
          .findFirst()
          .map(
              u -> {
                this.currentUniverseId = u.getId();
                return u;
              });
    }
    return universeRepository.findById(currentUniverseId);
  }

  public Long getCurrentUniverseId() {
    return getCurrentUniverse()
        .map(Universe::getId)
        .orElse(1L); // Fallback to ID 1 (Default Universe)
  }

  public void setCurrentUniverse(Universe universe) {
    if (universe != null) {
      this.currentUniverseId = universe.getId();
      log.info(
          "Current universe context set to: {} (ID: {})", universe.getName(), universe.getId());
    }
  }

  public void setCurrentUniverseId(Long universeId) {
    this.currentUniverseId = universeId;
  }
}
