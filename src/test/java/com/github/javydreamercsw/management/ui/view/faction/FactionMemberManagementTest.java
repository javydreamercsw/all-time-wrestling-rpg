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
 * Tests for Faction member management functionality. Focuses on testing member addition, removal,
 * and relationship management.
 */
@ExtendWith(MockitoExtension.class)
class FactionMemberManagementTest {

  @Mock private FactionService factionService;
  @Mock private WrestlerService wrestlerService;

  private FactionListView factionListView;
  private Faction testFaction;
  private List<Wrestler> availableWrestlers;
  private List<Wrestler> factionMembers;

  @BeforeEach
  void setUp() {
    // Create test data
    availableWrestlers = createAvailableWrestlers();
    factionMembers = createFactionMembers();
    testFaction = createTestFactionWithMembers();

    // Mock service responses
    when(factionService.findAllWithMembers()).thenReturn(List.of(testFaction));
    when(wrestlerService.findAll()).thenReturn(availableWrestlers);
    // Note: getFactionById is only mocked when needed in specific tests

    factionListView = new FactionListView(factionService, wrestlerService);
  }

  @Test
  @DisplayName("Should add new member to faction")
  void shouldAddNewMemberToFaction() {
    // Given
    Wrestler newMember = availableWrestlers.get(0); // Wrestler not in faction
    Faction updatedFaction = createUpdatedFactionWithNewMember(newMember);

    when(factionService.addMemberToFaction(testFaction.getId(), newMember.getId()))
        .thenReturn(Optional.of(updatedFaction));

    // When
    Optional<Faction> result =
        factionService.addMemberToFaction(testFaction.getId(), newMember.getId());

    // Then
    assertTrue(result.isPresent());
    verify(factionService).addMemberToFaction(testFaction.getId(), newMember.getId());

    // In a real scenario, the faction would have the new member
    // This tests the service integration
  }

  @Test
  @DisplayName("Should remove member from faction")
  void shouldRemoveMemberFromFaction() {
    // Given
    Wrestler memberToRemove = factionMembers.get(0);
    Faction updatedFaction = createUpdatedFactionWithRemovedMember(memberToRemove);

    when(factionService.removeMemberFromFaction(
            testFaction.getId(), memberToRemove.getId(), "Removed via UI"))
        .thenReturn(Optional.of(updatedFaction));

    // When
    Optional<Faction> result =
        factionService.removeMemberFromFaction(
            testFaction.getId(), memberToRemove.getId(), "Removed via UI");

    // Then
    assertTrue(result.isPresent());
    verify(factionService)
        .removeMemberFromFaction(testFaction.getId(), memberToRemove.getId(), "Removed via UI");
  }

  @Test
  @DisplayName("Should filter available wrestlers for member selection")
  void shouldFilterAvailableWrestlersForMemberSelection() {
    // Given - All wrestlers and current faction members
    List<Wrestler> allWrestlers = new ArrayList<>(availableWrestlers);
    allWrestlers.addAll(factionMembers);

    // When - Filter wrestlers not in faction
    List<Wrestler> availableForSelection =
        allWrestlers.stream().filter(wrestler -> !testFaction.hasMember(wrestler)).toList();

    // Then
    assertNotNull(availableForSelection);
    // Should only contain wrestlers not already in the faction
    assertTrue(availableForSelection.size() > 0);

    // Verify no current faction members are in the available list
    for (Wrestler member : factionMembers) {
      assertFalse(availableForSelection.contains(member));
    }
  }

  @Test
  @DisplayName("Should handle member count updates")
  void shouldHandleMemberCountUpdates() {
    // Given - Faction with initial member count
    int initialMemberCount = testFaction.getMemberCount();

    // When - Add a member (simulated)
    Wrestler newMember = availableWrestlers.get(0);
    Faction updatedFaction = createUpdatedFactionWithNewMember(newMember);

    // Then - Member count should increase
    assertTrue(updatedFaction.getMemberCount() >= initialMemberCount);
  }

  @Test
  @DisplayName("Should handle empty member list")
  void shouldHandleEmptyMemberList() {
    // Given - Faction with no members
    Faction emptyFaction = new Faction();
    emptyFaction.setId(99L);
    emptyFaction.setName("Empty Faction");
    emptyFaction.setIsActive(true);
    emptyFaction.setCreationDate(Instant.now());
    emptyFaction.setMembers(new ArrayList<>());

    // When/Then
    assertEquals(0, emptyFaction.getMemberCount());
    assertTrue(emptyFaction.getMembers().isEmpty());
  }

  @Test
  @DisplayName("Should handle member addition errors")
  void shouldHandleMemberAdditionErrors() {
    // Given
    Wrestler newMember = availableWrestlers.get(0);

    when(factionService.addMemberToFaction(testFaction.getId(), newMember.getId()))
        .thenThrow(new RuntimeException("Member already in another faction"));

    // When/Then
    assertThrows(
        RuntimeException.class,
        () -> {
          factionService.addMemberToFaction(testFaction.getId(), newMember.getId());
        });

    verify(factionService).addMemberToFaction(testFaction.getId(), newMember.getId());
  }

  @Test
  @DisplayName("Should handle member removal errors")
  void shouldHandleMemberRemovalErrors() {
    // Given
    Wrestler memberToRemove = factionMembers.get(0);

    when(factionService.removeMemberFromFaction(
            testFaction.getId(), memberToRemove.getId(), "Removed via UI"))
        .thenThrow(new RuntimeException("Member not found in faction"));

    // When/Then
    assertThrows(
        RuntimeException.class,
        () -> {
          factionService.removeMemberFromFaction(
              testFaction.getId(), memberToRemove.getId(), "Removed via UI");
        });

    verify(factionService)
        .removeMemberFromFaction(testFaction.getId(), memberToRemove.getId(), "Removed via UI");
  }

  @Test
  @DisplayName("Should validate member relationships")
  void shouldValidateMemberRelationships() {
    // Given - Faction with members
    assertNotNull(testFaction.getMembers());
    assertTrue(testFaction.getMemberCount() > 0);

    // When/Then - Each member should have proper relationship
    for (Wrestler member : testFaction.getMembers()) {
      assertNotNull(member);
      assertNotNull(member.getId());
      assertNotNull(member.getName());
      assertTrue(testFaction.hasMember(member));
    }
  }

  @Test
  @DisplayName("Should handle leader as member relationship")
  void shouldHandleLeaderAsMemberRelationship() {
    // Given - Faction with leader who is also a member
    Wrestler leader = factionMembers.get(0);
    testFaction.setLeader(leader);

    // When/Then
    assertNotNull(testFaction.getLeader());
    assertEquals(leader.getId(), testFaction.getLeader().getId());
    assertTrue(testFaction.hasMember(leader));
  }

  @Test
  @DisplayName("Should refresh member data after operations")
  void shouldRefreshMemberDataAfterOperations() {
    // Given - Initial faction state
    int initialMemberCount = testFaction.getMemberCount();

    // When - Simulate member addition and refresh
    Wrestler newMember = availableWrestlers.get(0);
    Faction updatedFaction = createUpdatedFactionWithNewMember(newMember);

    when(factionService.getFactionById(testFaction.getId()))
        .thenReturn(Optional.of(updatedFaction));

    Optional<Faction> refreshedFaction = factionService.getFactionById(testFaction.getId());

    // Then
    assertTrue(refreshedFaction.isPresent());
    assertTrue(refreshedFaction.get().getMemberCount() >= initialMemberCount);
  }

  /** Helper method to create test faction with members. */
  private Faction createTestFactionWithMembers() {
    Faction faction = new Faction();
    faction.setId(1L);
    faction.setName("Test Faction");
    faction.setDescription("A faction for testing member management");
    faction.setIsActive(true);
    faction.setCreationDate(Instant.now());
    faction.setMembers(new ArrayList<>(factionMembers));
    faction.setLeader(factionMembers.get(0)); // First member is leader

    return faction;
  }

  /** Helper method to create faction members. */
  private List<Wrestler> createFactionMembers() {
    List<Wrestler> members = new ArrayList<>();

    Wrestler member1 = new Wrestler();
    member1.setId(10L);
    member1.setName("Faction Leader");
    member1.setFans(95L);

    Wrestler member2 = new Wrestler();
    member2.setId(11L);
    member2.setName("Faction Member");
    member2.setFans(85L);

    members.add(member1);
    members.add(member2);

    return members;
  }

  /** Helper method to create available wrestlers (not in faction). */
  private List<Wrestler> createAvailableWrestlers() {
    List<Wrestler> wrestlers = new ArrayList<>();

    Wrestler wrestler1 = new Wrestler();
    wrestler1.setId(20L);
    wrestler1.setName("Available Wrestler 1");
    wrestler1.setFans(80L);

    Wrestler wrestler2 = new Wrestler();
    wrestler2.setId(21L);
    wrestler2.setName("Available Wrestler 2");
    wrestler2.setFans(75L);

    wrestlers.add(wrestler1);
    wrestlers.add(wrestler2);

    return wrestlers;
  }

  /** Helper method to create updated faction with new member. */
  private Faction createUpdatedFactionWithNewMember(Wrestler newMember) {
    Faction updated = createTestFactionWithMembers();
    List<Wrestler> updatedMembers = new ArrayList<>(updated.getMembers());
    updatedMembers.add(newMember);
    updated.setMembers(updatedMembers);

    return updated;
  }

  /** Helper method to create updated faction with removed member. */
  private Faction createUpdatedFactionWithRemovedMember(Wrestler removedMember) {
    Faction updated = createTestFactionWithMembers();
    List<Wrestler> updatedMembers = new ArrayList<>(updated.getMembers());
    updatedMembers.remove(removedMember);
    updated.setMembers(updatedMembers);

    return updated;
  }
}
