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
import com.vaadin.flow.server.VaadinSession;
import java.io.Serializable;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to manage the current universe context for the user session. This is a singleton service
 * that uses VaadinSession attributes for session-specific storage and a ThreadLocal fallback for
 * non-web environments (like some integration tests).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UniverseContextService implements Serializable {

  private final UniverseRepository universeRepository;
  private static final String UNIVERSE_ID_SESSION_KEY = "currentUniverseId";
  private static final ThreadLocal<Long> threadLocalUniverseId = ThreadLocal.withInitial(() -> 1L);

  /**
   * Get the current universe.
   *
   * @return Optional of current universe
   */
  public Optional<Universe> getCurrentUniverse() {
    return universeRepository.findById(getCurrentUniverseId());
  }

  private Long getInternalUniverseId() {
    try {
      VaadinSession session = VaadinSession.getCurrent();
      if (session != null) {
        Long id = (Long) session.getAttribute(UNIVERSE_ID_SESSION_KEY);
        if (id != null) {
          return id;
        }
      }
    } catch (Exception e) {
      // Ignore session access errors in non-web contexts
    }
    return threadLocalUniverseId.get();
  }

  private void setInternalUniverseId(final Long id) {
    try {
      VaadinSession session = VaadinSession.getCurrent();
      if (session != null) {
        session.setAttribute(UNIVERSE_ID_SESSION_KEY, id);
        return;
      }
    } catch (Exception e) {
      // Ignore session access errors in non-web contexts
    }
    threadLocalUniverseId.set(id);
  }

  /**
   * Get the current universe ID, falling back to 1L (Default Universe) if none.
   *
   * @return Current universe ID
   */
  public Long getCurrentUniverseId() {
    return getInternalUniverseId();
  }

  /**
   * Set the current universe context.
   *
   * @param universe The universe to set
   */
  public void setCurrentUniverse(final Universe universe) {
    if (universe != null) {
      setCurrentUniverseId(universe.getId());
      log.info(
          "Current universe context set to: {} (ID: {})", universe.getName(), universe.getId());
    }
  }

  /**
   * Set the current universe ID.
   *
   * @param universeId The universe ID to set
   */
  public void setCurrentUniverseId(@NonNull final Long universeId) {
    setInternalUniverseId(universeId);
  }
}
