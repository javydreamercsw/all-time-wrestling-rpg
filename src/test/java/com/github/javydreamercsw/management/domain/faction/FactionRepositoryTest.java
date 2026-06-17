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
package com.github.javydreamercsw.management.domain.faction;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.management.AbstractJpaTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for FactionRepository. Tests the new findByExternalId method and existing
 * functionality.
 */
class FactionRepositoryTest extends AbstractJpaTest {

  @Autowired private EntityManager entityManager;
  @Autowired private FactionRepository factionRepository;

  private Faction testFaction;

  @AfterEach
  public void tearDown() {
    entityManager.clear();
    super.tearDown();
  }

  @Override
  @BeforeEach
  public void baseSetUp() {
    super.baseSetUp();
    testFaction = Faction.builder().build();
    testFaction.setName("Test Faction");
    testFaction.setDescription("A test faction for unit tests");
    testFaction.setActive(true);
    testFaction.setCreationDate(Instant.now());
  }

  @Test
  @DisplayName("Should find faction by name")
  void shouldFindFactionByName() {
    // Given
    entityManager.persist(testFaction);
    entityManager.flush();

    // When
    Optional<Faction> foundFaction = factionRepository.findByName("Test Faction");

    // Then
    assertTrue(foundFaction.isPresent());
    assertEquals("Test Faction", foundFaction.get().getName());
  }

  @Test
  @DisplayName("Should check if faction name exists")
  void shouldCheckIfFactionNameExists() {
    // Given
    entityManager.persist(testFaction);
    entityManager.flush();

    // When & Then
    assertTrue(factionRepository.existsByName("Test Faction"));
    assertFalse(factionRepository.existsByName("Non-existent Faction"));
  }
}
