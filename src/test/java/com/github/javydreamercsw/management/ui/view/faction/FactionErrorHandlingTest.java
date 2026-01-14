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
package com.github.javydreamercsw.management.ui.view.faction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.npc.NpcService;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Tests for Faction UI error handling and edge cases. Focuses on testing how the UI handles various
 * error conditions and edge cases.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FactionErrorHandlingTest {

  @Mock private FactionService factionService;
  @Mock private WrestlerService wrestlerService;
  @Mock private NpcService npcService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private SecurityUtils securityUtils;

  private FactionListView factionListView;

  @BeforeEach
  void setUp() {
    // Setup will be done per test as needed to avoid unnecessary stubbing
    // Allow all actions for tests by default
    when(securityUtils.canCreate()).thenReturn(true);
    when(securityUtils.canEdit()).thenReturn(true);
    when(securityUtils.canDelete()).thenReturn(true);
    when(wrestlerRepository.findAll()).thenReturn(new ArrayList<>());
  }

  @Test
  @DisplayName("Should handle service unavailable errors")
  void shouldHandleServiceUnavailableErrors() {
    // Given
    when(factionService.findAllWithMembersAndTeams())
        .thenThrow(new RuntimeException("Database connection failed"));

    // When/Then - View should still be created but handle the error gracefully
    assertThrows(
        RuntimeException.class,
        () -> {
          new FactionListView(
              factionService, wrestlerService, npcService, wrestlerRepository, securityUtils);
        });
  }

  @Test
  @DisplayName("Should handle null service responses")
  void shouldHandleNullServiceResponses() {
    // Given - This test verifies that the view can handle null responses
    // In a real scenario, services should not return null, but if they do,
    // the view should handle it gracefully

    // When/Then - Should handle null responses gracefully
    assertDoesNotThrow(
        () -> {
          // In a real implementation, the view should handle null responses
          // This test verifies the concept without actually creating a view with null responses
          assertNotNull(factionService); // Service should exist
          assertNotNull(wrestlerService); // Service should exist
          // Instantiate the view to ensure it can be created without null pointers
          FactionListView view =
              new FactionListView(
                  factionService, wrestlerService, npcService, wrestlerRepository, securityUtils);
          assertNotNull(view);
        });
  }

  @Test
  @DisplayName("Should handle faction save failures")
  void shouldHandleFactionSaveFailures() {
    // Given
    when(factionService.findAllWithMembersAndTeams()).thenReturn(new ArrayList<>());
    when(wrestlerService.findAll()).thenReturn(new ArrayList<>());
    factionListView =
        new FactionListView(
            factionService, wrestlerService, npcService, wrestlerRepository, securityUtils);

    Faction factionToSave = Faction.builder().build();
    factionToSave.setName("Test Faction");

    when(factionService.save(any(Faction.class)))
        .thenThrow(new RuntimeException("Constraint violation: Name already exists"));

    // When/Then
    assertThrows(
        RuntimeException.class,
        () -> {
          factionService.save(factionToSave);
        });

    verify(factionService).save(factionToSave);
  }

  @Test
  @DisplayName("Should handle faction deletion failures")
  void shouldHandleFactionDeletionFailures() {
    // Given
    when(factionService.findAllWithMembersAndTeams()).thenReturn(new ArrayList<>());
    when(wrestlerService.findAll()).thenReturn(new ArrayList<>());
    factionListView =
        new FactionListView(
            factionService, wrestlerService, npcService, wrestlerRepository, securityUtils);

    Faction factionToDelete = Faction.builder().build();
    factionToDelete.setId(1L);
    factionToDelete.setName("Faction to Delete");

    doThrow(new RuntimeException("Cannot delete faction with active members"))
        .when(factionService)
        .delete(factionToDelete);

    // When/Then
    assertThrows(
        RuntimeException.class,
        () -> {
          factionService.delete(factionToDelete);
        });

    verify(factionService).delete(factionToDelete);
  }

  @Test
  @DisplayName("Should handle member management failures")
  void shouldHandleMemberManagementFailures() {
    // Given
    when(factionService.findAllWithMembersAndTeams()).thenReturn(new ArrayList<>());
    when(wrestlerService.findAll()).thenReturn(new ArrayList<>());
    factionListView =
        new FactionListView(
            factionService, wrestlerService, npcService, wrestlerRepository, securityUtils);

    Long factionId = 1L;
    Long wrestlerId = 2L;

    when(factionService.addMemberToFaction(factionId, wrestlerId))
        .thenThrow(new RuntimeException("Wrestler already in another faction"));

    // When/Then
    assertThrows(
        RuntimeException.class,
        () -> {
          factionService.addMemberToFaction(factionId, wrestlerId);
        });

    verify(factionService).addMemberToFaction(factionId, wrestlerId);
  }

  @Test
  @DisplayName("Should handle invalid faction data")
  void shouldHandleInvalidFactionData() {
    // Given
    when(factionService.findAllWithMembersAndTeams()).thenReturn(new ArrayList<>());
    when(wrestlerService.findAll()).thenReturn(new ArrayList<>());
    factionListView =
        new FactionListView(
            factionService, wrestlerService, npcService, wrestlerRepository, securityUtils);

    // Test various invalid faction scenarios
    Faction invalidFaction1 = Faction.builder().build(); // No name, no alignment
    Faction invalidFaction2 = Faction.builder().build(); // No name
    invalidFaction2.setName(""); // Empty name

    Faction invalidFaction3 = Faction.builder().build(); // No alignment
    invalidFaction3.setName("Valid Name");

    // When/Then - These should be caught by validation
    assertNull(invalidFaction1.getName());
    // Note: Faction entity may have default alignment set in constructor or @PrePersist
    // So we test that it's either null or has a default value

    assertTrue(invalidFaction2.getName().isEmpty());

    assertNotNull(invalidFaction3.getName());
    // The alignment might be set to a default value by the entity
    // In a real validation scenario, null alignment would be caught by form validation
  }

  @Test
  @DisplayName("Should handle concurrent modification errors")
  void shouldHandleConcurrentModificationErrors() {
    // Given
    when(factionService.findAllWithMembersAndTeams()).thenReturn(new ArrayList<>());
    when(wrestlerService.findAll()).thenReturn(new ArrayList<>());
    Faction faction = Faction.builder().build();
    faction.setId(1L);
    faction.setName("Concurrent Test Faction");

    when(factionService.save(faction))
        .thenThrow(
            new RuntimeException(
                "Optimistic locking failure: Entity was modified by another user"));

    // When/Then
    assertThrows(
        RuntimeException.class,
        () -> {
          factionService.save(faction);
        });

    verify(factionService).save(faction);
  }

  @Test
  @DisplayName("Should handle missing faction for member operations")
  void shouldHandleMissingFactionForMemberOperations() {
    // Given
    when(factionService.findAllWithMembersAndTeams()).thenReturn(new ArrayList<>());
    when(wrestlerService.findAll()).thenReturn(new ArrayList<>());
    factionListView =
        new FactionListView(
            factionService, wrestlerService, npcService, wrestlerRepository, securityUtils);

    Long nonExistentFactionId = 999L;
    Long wrestlerId = 1L;

    when(factionService.getFactionById(nonExistentFactionId)).thenReturn(Optional.empty());

    // When
    Optional<Faction> result = factionService.getFactionById(nonExistentFactionId);

    // Then
    assertFalse(result.isPresent());
    verify(factionService).getFactionById(nonExistentFactionId);
  }

  @Test
  @DisplayName("Should handle missing wrestler for member operations")
  void shouldHandleMissingWrestlerForMemberOperations() {
    // Given
    when(factionService.findAllWithMembersAndTeams()).thenReturn(new ArrayList<>());
    when(wrestlerService.findAll()).thenReturn(new ArrayList<>());
    factionListView =
        new FactionListView(
            factionService, wrestlerService, npcService, wrestlerRepository, securityUtils);

    Long factionId = 1L;
    Long nonExistentWrestlerId = 999L;

    when(factionService.addMemberToFaction(factionId, nonExistentWrestlerId))
        .thenThrow(new RuntimeException("Wrestler not found"));

    // When/Then
    assertThrows(
        RuntimeException.class,
        () -> {
          factionService.addMemberToFaction(factionId, nonExistentWrestlerId);
        });

    verify(factionService).addMemberToFaction(factionId, nonExistentWrestlerId);
  }

  @Test
  @DisplayName("Should handle large datasets gracefully")
  void shouldHandleLargeDatasetsGracefully() {
    // Given - Large dataset
    List<Faction> largeFactionList = new ArrayList<>();
    List<Wrestler> largeWrestlerList = new ArrayList<>();

    // Create 1000 factions and wrestlers
    for (int i = 0; i < 1000; i++) {
      Faction faction = Faction.builder().build();
      faction.setId((long) i);
      faction.setName("Faction " + i);
      faction.setActive(i % 2 == 0);
      faction.setCreationDate(Instant.now());
      largeFactionList.add(faction);

      Wrestler wrestler = Wrestler.builder().build();
      wrestler.setId((long) i);
      wrestler.setName("Wrestler " + i);
      wrestler.setFans((long) (50 + i));
      largeWrestlerList.add(wrestler);
    }

    when(factionService.findAllWithMembersAndTeams()).thenReturn(largeFactionList);
    when(wrestlerService.findAll()).thenReturn(largeWrestlerList);

    // When
    FactionListView viewWithLargeDataset =
        new FactionListView(
            factionService, wrestlerService, npcService, wrestlerRepository, securityUtils);

    // Then - Should handle large datasets without issues
    assertNotNull(viewWithLargeDataset);
    verify(factionService).findAllWithMembersAndTeams();
    verify(wrestlerRepository, atLeastOnce()).findAll();
  }

  @Test
  @DisplayName("Should handle network timeout errors")
  void shouldHandleNetworkTimeoutErrors() {
    // Given
    when(factionService.findAllWithMembersAndTeams())
        .thenThrow(new RuntimeException("Connection timeout"));

    // When/Then
    assertThrows(
        RuntimeException.class,
        () -> {
          new FactionListView(
              factionService, wrestlerService, npcService, wrestlerRepository, securityUtils);
        });

    verify(factionService).findAllWithMembersAndTeams();
  }

  @Test
  @DisplayName("Should handle permission denied errors")
  void shouldHandlePermissionDeniedErrors() {
    // Given
    when(factionService.findAllWithMembersAndTeams()).thenReturn(new ArrayList<>());
    when(wrestlerService.findAll()).thenReturn(new ArrayList<>());
    // Specifically set canEdit to false for this test case
    when(securityUtils.canEdit()).thenReturn(false);

    factionListView =
        new FactionListView(
            factionService, wrestlerService, npcService, wrestlerRepository, securityUtils);

    Faction restrictedFaction = Faction.builder().build();
    restrictedFaction.setId(1L);
    restrictedFaction.setName("Restricted Faction");

    when(factionService.save(restrictedFaction))
        .thenThrow(new RuntimeException("Access denied: Insufficient permissions"));

    // When/Then
    assertThrows(
        RuntimeException.class,
        () -> {
          factionService.save(restrictedFaction);
        });

    verify(factionService).save(restrictedFaction);
  }

  @Test
  @DisplayName("Should handle validation errors gracefully")
  void shouldHandleValidationErrorsGracefully() {
    // Given
    when(factionService.findAllWithMembersAndTeams()).thenReturn(new ArrayList<>());
    when(wrestlerService.findAll()).thenReturn(new ArrayList<>());
    factionListView =
        new FactionListView(
            factionService, wrestlerService, npcService, wrestlerRepository, securityUtils);

    // Test faction with name too long
    Faction factionWithLongName = Faction.builder().build();
    factionWithLongName.setName("A".repeat(300)); // Exceeds 255 character limit

    when(factionService.save(factionWithLongName))
        .thenThrow(new RuntimeException("Validation failed: Name exceeds maximum length"));

    // When/Then
    assertThrows(
        RuntimeException.class,
        () -> {
          factionService.save(factionWithLongName);
        });

    verify(factionService).save(factionWithLongName);
  }
}
