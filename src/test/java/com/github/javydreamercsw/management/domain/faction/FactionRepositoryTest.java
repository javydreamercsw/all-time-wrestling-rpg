package com.github.javydreamercsw.management.domain.faction;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/**
 * Integration tests for FactionRepository. Tests the new findByExternalId method and existing
 * functionality.
 */
@DataJpaTest
class FactionRepositoryTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private FactionRepository factionRepository;

  private Faction testFaction;

  @BeforeEach
  void setUp() {
    testFaction = new Faction();
    testFaction.setName("Test Faction");
    testFaction.setDescription("A test faction for unit tests");
    testFaction.setAlignment(FactionAlignment.NEUTRAL);
    testFaction.setIsActive(true);
    testFaction.setCreationDate(Instant.now());
    testFaction.setExternalId("notion-test-faction-123");
  }

  @Test
  @DisplayName("Should save and find faction by external ID")
  void shouldSaveAndFindFactionByExternalId() {
    // Given
    Faction savedFaction = entityManager.persistAndFlush(testFaction);

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
    entityManager.persistAndFlush(testFaction);

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
    entityManager.persistAndFlush(testFaction);

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
    entityManager.persistAndFlush(testFaction);

    // When & Then
    assertTrue(factionRepository.existsByName("Test Faction"));
    assertFalse(factionRepository.existsByName("Non-existent Faction"));
  }

  @Test
  @DisplayName("Should handle faction with external ID but no name conflicts")
  void shouldHandleFactionWithExternalIdButNoNameConflicts() {
    // Given
    Faction faction1 = new Faction();
    faction1.setName("Faction One");
    faction1.setAlignment(FactionAlignment.FACE);
    faction1.setIsActive(true);
    faction1.setCreationDate(Instant.now());
    faction1.setExternalId("external-id-1");

    Faction faction2 = new Faction();
    faction2.setName("Faction Two");
    faction2.setAlignment(FactionAlignment.HEEL);
    faction2.setIsActive(true);
    faction2.setCreationDate(Instant.now());
    faction2.setExternalId("external-id-2");

    entityManager.persistAndFlush(faction1);
    entityManager.persistAndFlush(faction2);

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
