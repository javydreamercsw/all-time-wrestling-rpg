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
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link UniverseService}. Exercises real H2 round-trips to catch sequence /
 * identity bugs that unit tests with mocks cannot detect.
 */
class UniverseServiceIT extends ManagementIntegrationTest {

  @Autowired private UniverseService universeService;

  /** Remove any universes created during a test, restoring the seeded Default Universe only. */
  @AfterEach
  void cleanup() {
    universeService.findAll().stream()
        .filter(u -> u.getId() != null && u.getId() != 1L)
        .forEach(u -> universeService.delete(u.getId()));
  }

  // ── Sequence regression tests ────────────────────────────────────────────

  @Test
  @Transactional
  void createUniverse_afterSeededDefault_getsUniqueId() {
    // V75 seeds Default Universe with explicit id=1. Without V92 sequence reset
    // the next INSERT generates id=1 and throws a PK violation.
    Universe saved =
        universeService.save(
            Universe.builder().name("Test Universe Alpha").type(UniverseType.GLOBAL).build());

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getId()).isNotEqualTo(1L);
  }

  @Test
  @Transactional
  void createMultipleUniverses_eachGetsDistinctId() {
    Universe saved1 =
        universeService.save(
            Universe.builder().name("League Universe").type(UniverseType.LEAGUE).build());
    Universe saved2 =
        universeService.save(
            Universe.builder().name("Fantasy Universe").type(UniverseType.GLOBAL).build());

    assertThat(saved1.getId()).isNotNull();
    assertThat(saved2.getId()).isNotNull();
    assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
  }

  // ── Cache eviction tests ─────────────────────────────────────────────────

  @Test
  void save_newUniverse_appearsInFindAll() {
    // Prime the cache with the current list (Default Universe only).
    List<Universe> before = universeService.findAll();
    int sizeBefore = before.size();

    // save() must evict the cache so the next findAll() returns the new row.
    universeService.save(
        Universe.builder().name("Cache Test Universe").type(UniverseType.GLOBAL).build());

    List<Universe> after = universeService.findAll();
    assertThat(after).hasSize(sizeBefore + 1);
    assertThat(after).anyMatch(u -> "Cache Test Universe".equals(u.getName()));
  }

  @Test
  void delete_existingUniverse_disappearsFromFindAll() {
    Universe created =
        universeService.save(
            Universe.builder().name("To Be Deleted").type(UniverseType.GLOBAL).build());
    // Prime cache with the list that includes the new universe.
    assertThat(universeService.findAll()).anyMatch(u -> "To Be Deleted".equals(u.getName()));

    // delete() must evict the cache so the next findAll() no longer returns it.
    universeService.delete(created.getId());

    assertThat(universeService.findAll()).noneMatch(u -> "To Be Deleted".equals(u.getName()));
  }
}
