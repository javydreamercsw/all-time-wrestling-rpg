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

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.Universe.UniverseType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link UniverseService}. Exercises real H2 round-trips to catch sequence /
 * identity bugs that unit tests with mocks cannot detect.
 */
@Transactional
class UniverseServiceIT extends ManagementIntegrationTest {

  @Autowired private UniverseService universeService;

  @Test
  void createUniverse_afterSeededDefault_getsUniqueId() {
    // The "Default Universe" is seeded in V75 with explicit id=1 via MERGE INTO.
    // If the H2 identity sequence was not reset (V92), this second INSERT would
    // also generate id=1 and throw a PK violation.
    Universe second =
        Universe.builder().name("Test Universe Alpha").type(UniverseType.GLOBAL).build();
    Universe saved = universeService.save(second);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getId()).isNotEqualTo(1L);
  }

  @Test
  void createMultipleUniverses_eachGetsDistinctId() {
    Universe u1 = Universe.builder().name("League Universe").type(UniverseType.LEAGUE).build();
    Universe u2 = Universe.builder().name("Fantasy Universe").type(UniverseType.GLOBAL).build();

    Universe saved1 = universeService.save(u1);
    Universe saved2 = universeService.save(u2);

    assertThat(saved1.getId()).isNotNull();
    assertThat(saved2.getId()).isNotNull();
    assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
  }
}
