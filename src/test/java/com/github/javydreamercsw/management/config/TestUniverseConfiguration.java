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
package com.github.javydreamercsw.management.config;

import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import java.util.Optional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile({"test", "e2e"})
public class TestUniverseConfiguration {

  /**
   * Provide a test-specific UniverseContextService that doesn't require a Vaadin session. This is a
   * singleton bean that will be used in tests instead of the session-scoped one.
   */
  @Bean
  @Primary
  public UniverseContextService testUniverseContextService(UniverseRepository universeRepository) {
    return new UniverseContextService(universeRepository) {
      private Long testUniverseId = 1L;

      @Override
      public Optional<Universe> getCurrentUniverse() {
        if (testUniverseId == null) {
          return universeRepository.findAll().stream()
              .findFirst()
              .map(
                  u -> {
                    this.testUniverseId = u.getId();
                    return u;
                  });
        }
        return universeRepository.findById(testUniverseId);
      }

      @Override
      public Long getCurrentUniverseId() {
        return getCurrentUniverse().map(Universe::getId).orElse(1L);
      }

      @Override
      public void setCurrentUniverse(Universe universe) {
        if (universe != null) {
          this.testUniverseId = universe.getId();
        }
      }

      @Override
      public void setCurrentUniverseId(Long universeId) {
        this.testUniverseId = universeId;
      }
    };
  }
}
