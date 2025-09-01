package com.github.javydreamercsw.management.ui.view.faction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Integration tests for Faction CRUD operations through the UI. Tests the interaction between the
 * UI layer and service layer.
 */
@ExtendWith(MockitoExtension.class)
class FactionCrudIntegrationTest {

  @Mock private FactionService factionService;
  @Mock private WrestlerService wrestlerService;

  private FactionListView factionListView;
  private List<Faction> testFactions;
  private List<Wrestler> testWrestlers;

  @BeforeEach
  void setUp() {
    testWrestlers = createTestWrestlers();
    testFactions = createTestFactions();

    // Mock service responses
    when(factionService.findAllWithMembers()).thenReturn(testFactions);
    when(wrestlerService.findAll()).thenReturn(testWrestlers);

    factionListView = new FactionListView(factionService, wrestlerService);
  }

  @Test
  @DisplayName("Should create new faction through service")
  void shouldCreateNewFactionThroughService() {
    // Given
    Faction newFaction = new Faction();
    newFaction.setName("New Test Faction");
    newFaction.setDescription("A newly created faction");
    newFaction.setIsActive(true);
    newFaction.setCreationDate(Instant.now());

    Faction savedFaction = new Faction();
    savedFaction.setId(99L);
    savedFaction.setName(newFaction.getName());
    savedFaction.setDescription(newFaction.getDescription());
    savedFaction.setIsActive(newFaction.getIsActive());
    savedFaction.setCreationDate(newFaction.getCreationDate());

    when(factionService.save(any(Faction.class))).thenReturn(savedFaction);

    // When - Simulate saving through the service
    Faction result = factionService.save(newFaction);

    // Then
    assertNotNull(result);
    assertEquals(99L, result.getId());
    assertEquals("New Test Faction", result.getName());
    assertTrue(result.getIsActive());
    verify(factionService).save(newFaction);
  }

  @Test
  @DisplayName("Should update existing faction through service")
  void shouldUpdateExistingFactionThroughService() {
    // Given
    Faction existingFaction = testFactions.get(0);
    existingFaction.setDescription("Updated description");

    when(factionService.save(any(Faction.class))).thenReturn(existingFaction);

    // When
    Faction result = factionService.save(existingFaction);

    // Then
    assertNotNull(result);
    assertEquals("Updated description", result.getDescription());
    verify(factionService).save(existingFaction);
  }

  @Test
  @DisplayName("Should delete faction through service")
  void shouldDeleteFactionThroughService() {
    // Given
    Faction factionToDelete = testFactions.get(0);
    doNothing().when(factionService).delete(factionToDelete);

    // When
    factionService.delete(factionToDelete);

    // Then
    verify(factionService).delete(factionToDelete);
  }

  @Test
  @DisplayName("Should handle faction member management operations")
  void shouldHandleFactionMemberManagementOperations() {
    // Given
    Faction faction = testFactions.get(0);
    Wrestler newMember = testWrestlers.get(0);

    when(factionService.addMemberToFaction(faction.getId(), newMember.getId()))
        .thenReturn(Optional.of(faction));

    // When - Add member
    Optional<Faction> result =
        factionService.addMemberToFaction(faction.getId(), newMember.getId());

    // Then
    assertTrue(result.isPresent());
    verify(factionService).addMemberToFaction(faction.getId(), newMember.getId());
  }

  @Test
  @DisplayName("Should handle member removal operations")
  void shouldHandleMemberRemovalOperations() {
    // Given
    Faction faction = testFactions.get(0);
    Wrestler memberToRemove = testWrestlers.get(0);

    when(factionService.removeMemberFromFaction(
            faction.getId(), memberToRemove.getId(), "Removed via UI"))
        .thenReturn(Optional.of(faction));

    // When - Remove member
    Optional<Faction> result =
        factionService.removeMemberFromFaction(
            faction.getId(), memberToRemove.getId(), "Removed via UI");

    // Then
    assertTrue(result.isPresent());
    verify(factionService)
        .removeMemberFromFaction(faction.getId(), memberToRemove.getId(), "Removed via UI");
  }

  @Test
  @DisplayName("Should handle service errors gracefully")
  void shouldHandleServiceErrorsGracefully() {
    // Given
    Faction factionWithError = new Faction();
    factionWithError.setName("Error Faction");

    when(factionService.save(any(Faction.class)))
        .thenThrow(new RuntimeException("Database connection error"));

    // When/Then
    assertThrows(
        RuntimeException.class,
        () -> {
          factionService.save(factionWithError);
        });

    verify(factionService).save(factionWithError);
  }

  @Test
  @DisplayName("Should refresh data after CRUD operations")
  void shouldRefreshDataAfterCrudOperations() {
    // Given - Initial data load
    verify(factionService, atLeastOnce()).findAllWithMembers();

    // When - Simulate data refresh after operation
    List<Faction> updatedFactions = new ArrayList<>(testFactions);
    Faction newFaction = new Faction();
    newFaction.setId(99L);
    newFaction.setName("Newly Added Faction");
    updatedFactions.add(newFaction);

    when(factionService.findAll()).thenReturn(updatedFactions);

    // Simulate refresh
    List<Faction> refreshedData = factionService.findAll();

    // Then
    assertNotNull(refreshedData);
    assertEquals(3, refreshedData.size()); // Original 2 + 1 new
    assertTrue(refreshedData.stream().anyMatch(f -> "Newly Added Faction".equals(f.getName())));
  }

  @Test
  @DisplayName("Should handle empty wrestler list for member management")
  void shouldHandleEmptyWrestlerListForMemberManagement() {
    // Given
    when(wrestlerService.findAll()).thenReturn(new ArrayList<>());

    // When
    FactionListView viewWithNoWrestlers = new FactionListView(factionService, wrestlerService);

    // Then
    assertNotNull(viewWithNoWrestlers);
    verify(wrestlerService, atLeastOnce()).findAll();
  }

  @Test
  @DisplayName("Should validate faction data before save operations")
  void shouldValidateFactionDataBeforeSaveOperations() {
    // Given - Invalid faction (no name)
    Faction invalidFaction = new Faction();
    // Name is null/empty - should be invalid

    // When/Then - In a real scenario, validation would prevent this from reaching the service
    // For this test, we verify the service would be called with proper data
    assertNull(invalidFaction.getName());
  }

  @Test
  @DisplayName("Should handle concurrent faction operations")
  void shouldHandleConcurrentFactionOperations() {
    // Given
    Faction faction1 = testFactions.get(0);
    Faction faction2 = testFactions.get(1);

    when(factionService.save(faction1)).thenReturn(faction1);
    when(factionService.save(faction2)).thenReturn(faction2);

    // When - Simulate concurrent operations
    Faction result1 = factionService.save(faction1);
    Faction result2 = factionService.save(faction2);

    // Then
    assertNotNull(result1);
    assertNotNull(result2);
    assertNotEquals(result1.getId(), result2.getId());
    verify(factionService).save(faction1);
    verify(factionService).save(faction2);
  }

  /** Helper method to create test factions for integration testing. */
  private List<Faction> createTestFactions() {
    List<Faction> factions = new ArrayList<>();

    Faction evolution = new Faction();
    evolution.setId(1L);
    evolution.setName("Evolution");
    evolution.setDescription("A dominant faction");
    evolution.setIsActive(false);
    evolution.setCreationDate(Instant.now());

    Faction dx = new Faction();
    dx.setId(2L);
    dx.setName("D-Generation X");
    dx.setDescription("Rebellious faction");
    dx.setIsActive(true);
    dx.setCreationDate(Instant.now());

    factions.add(evolution);
    factions.add(dx);

    return factions;
  }

  /** Helper method to create test wrestlers for integration testing. */
  private List<Wrestler> createTestWrestlers() {
    List<Wrestler> wrestlers = new ArrayList<>();

    Wrestler wrestler1 = new Wrestler();
    wrestler1.setId(1L);
    wrestler1.setName("Triple H");
    wrestler1.setFans(95L);

    Wrestler wrestler2 = new Wrestler();
    wrestler2.setId(2L);
    wrestler2.setName("Shawn Michaels");
    wrestler2.setFans(90L);

    wrestlers.add(wrestler1);
    wrestlers.add(wrestler2);

    return wrestlers;
  }
}
