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
  void tearDown() {
    entityManager.clear();
  }

  @Override
  @BeforeEach
  public void baseSetUp() throws Exception {
    super.baseSetUp();
    testFaction = Faction.builder().build();
    testFaction.setName("Test Faction");
    testFaction.setDescription("A test faction for unit tests");
    testFaction.setActive(true);
    testFaction.setCreationDate(Instant.now());
    testFaction.setExternalId("notion-test-faction-123");
  }

  @Test
  @DisplayName("Should save and find faction by external ID")
  void shouldSaveAndFindFactionByExternalId() {
    // Given
    entityManager.persist(testFaction);
    entityManager.flush();
    Faction savedFaction = testFaction;

    // When
    Optional<Faction> foundFaction = factionRepository.findByExternalId("notion-test-faction-123");

    // Then
    assertTrue(foundFaction.isPresent());
    assertEquals(savedFaction.getId(), foundFaction.get().getId());
    assertEquals("Test Faction", foundFaction.get().getName());
    assertEquals("notion-test-faction-123", foundFaction.get().getExternalId());
  }

  @Test
  @DisplayName("Should return empty when external ID not found")
  void shouldReturnEmptyWhenExternalIdNotFound() {
    // Given
    entityManager.persist(testFaction);
    entityManager.flush();

    // When
    Optional<Faction> foundFaction = factionRepository.findByExternalId("non-existent-id");

    // Then
    assertFalse(foundFaction.isPresent());
  }

  @Test
  @DisplayName("Should handle null external ID")
  void shouldHandleNullExternalId() {
    // When
    Optional<Faction> foundFaction = factionRepository.findByExternalId(null);

    // Then
    assertFalse(foundFaction.isPresent());
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
    assertEquals("notion-test-faction-123", foundFaction.get().getExternalId());
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

  @Test
  @DisplayName("Should handle faction with external ID but no name conflicts")
  void shouldHandleFactionWithExternalIdButNoNameConflicts() {
    // Given
    Faction faction1 = Faction.builder().build();
    faction1.setName("Faction One");
    faction1.setActive(true);
    faction1.setCreationDate(Instant.now());
    faction1.setExternalId("external-id-1");

    Faction faction2 = Faction.builder().build();
    faction2.setName("Faction Two");
    faction2.setActive(true);
    faction2.setCreationDate(Instant.now());
    faction2.setExternalId("external-id-2");

    entityManager.persist(faction1);
    entityManager.flush();
    entityManager.persist(faction2);
    entityManager.flush();

    // When
    Optional<Faction> found1 = factionRepository.findByExternalId("external-id-1");
    Optional<Faction> found2 = factionRepository.findByExternalId("external-id-2");

    // Then
    assertTrue(found1.isPresent());
    assertTrue(found2.isPresent());
    assertEquals("Faction One", found1.get().getName());
    assertEquals("Faction Two", found2.get().getName());
    assertNotEquals(found1.get().getId(), found2.get().getId());
  }

  @Test
  @DisplayName("Should handle empty external ID string")
  void shouldHandleEmptyExternalIdString() {
    // When
    Optional<Faction> foundFaction = factionRepository.findByExternalId("");

    // Then
    assertFalse(foundFaction.isPresent());
  }
}
