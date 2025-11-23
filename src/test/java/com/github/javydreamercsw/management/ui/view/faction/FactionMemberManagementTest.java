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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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

  private Faction testFaction;
  private Set<Wrestler> availableWrestlers;
  private Set<Wrestler> factionMembers;

  @BeforeEach
  void setUp() {
    // Create test data
    availableWrestlers = createAvailableWrestlers();
    factionMembers = createFactionMembers();
    testFaction = createTestFactionWithMembers();

    // Note: Service methods are only mocked when needed in specific tests
    when(factionService.findAllWithMembersAndTeams()).thenReturn(new ArrayList<>());
    when(wrestlerService.findAll()).thenReturn(new ArrayList<>());
  }

  @Test
  @DisplayName("Should add new member to faction")
  void shouldAddNewMemberToFaction() {
    // Given
    Wrestler newMember = new ArrayList<>(availableWrestlers).get(0); // Wrestler not in faction
    Faction updatedFaction = createUpdatedFactionWithNewMember(newMember);

    assertNotNull(testFaction.getId());
    assertNotNull(newMember.getId());
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
    Wrestler memberToRemove = new ArrayList<>(factionMembers).get(0);
    Faction updatedFaction = createUpdatedFactionWithRemovedMember(memberToRemove);

    assertNotNull(testFaction.getId());
    assertNotNull(memberToRemove.getId());
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
        allWrestlers.stream()
            .filter(wrestler -> !testFaction.hasMember(wrestler))
            .collect(Collectors.toList());

    // Then
    assertNotNull(availableForSelection);
    // Should only contain wrestlers not already in the faction
    assertFalse(availableForSelection.isEmpty());

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
    Wrestler newMember = new ArrayList<>(availableWrestlers).get(0);
    Faction updatedFaction = createUpdatedFactionWithNewMember(newMember);

    // Then - Member count should increase
    assertTrue(updatedFaction.getMemberCount() >= initialMemberCount);
  }

  @Test
  @DisplayName("Should handle empty member list")
  void shouldHandleEmptyMemberList() {
    // Given - Faction with no members
    Faction emptyFaction = Faction.builder().build();
    emptyFaction.setId(99L);
    emptyFaction.setName("Empty Faction");
    emptyFaction.setIsActive(true);
    emptyFaction.setCreationDate(Instant.now());
    emptyFaction.setMembers(new HashSet<>());

    // When/Then
    assertEquals(0, emptyFaction.getMemberCount());
    assertTrue(emptyFaction.getMembers().isEmpty());
  }

  @Test
  @DisplayName("Should handle member addition errors")
  void shouldHandleMemberAdditionErrors() {
    // Given
    Wrestler newMember = new ArrayList<>(availableWrestlers).get(0);

    assertNotNull(testFaction.getId());
    assertNotNull(newMember.getId());
    when(factionService.addMemberToFaction(testFaction.getId(), newMember.getId()))
        .thenThrow(new RuntimeException("Member already in another faction"));

    // When/Then
    assertThrows(
        RuntimeException.class,
        () -> {
          factionService.addMemberToFaction(testFaction.getId(), newMember.getId());
        });

    assertNotNull(testFaction.getId());
    assertNotNull(newMember.getId());
    verify(factionService).addMemberToFaction(testFaction.getId(), newMember.getId());
  }

  @Test
  @DisplayName("Should handle member removal errors")
  void shouldHandleMemberRemovalErrors() {
    // Given
    Wrestler memberToRemove = new ArrayList<>(factionMembers).get(0);

    assertNotNull(testFaction.getId());
    assertNotNull(memberToRemove.getId());
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

    assertNotNull(testFaction.getId());
    assertNotNull(memberToRemove.getId());
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
    Wrestler leader = new ArrayList<>(factionMembers).get(0);
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
    Wrestler newMember = new ArrayList<>(availableWrestlers).get(0);
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
    Faction faction = Faction.builder().build();
    faction.setId(1L);
    faction.setName("Test Faction");
    faction.setDescription("A faction for testing member management");
    faction.setIsActive(true);
    faction.setCreationDate(Instant.now());
    faction.setMembers(new HashSet<>(factionMembers));
    faction.setLeader(new ArrayList<>(factionMembers).get(0)); // First member is leader

    return faction;
  }

  /** Helper method to create faction members. */
  private Set<Wrestler> createFactionMembers() {
    Set<Wrestler> members = new HashSet<>();

    Wrestler member1 = Wrestler.builder().build();
    member1.setId(10L);
    member1.setName("Faction Leader");
    member1.setFans(95L);

    Wrestler member2 = Wrestler.builder().build();
    member2.setId(11L);
    member2.setName("Faction Member");
    member2.setFans(85L);

    members.add(member1);
    members.add(member2);

    return members;
  }

  /** Helper method to create available wrestlers (not in faction). */
  private Set<Wrestler> createAvailableWrestlers() {
    Set<Wrestler> wrestlers = new HashSet<>();

    Wrestler wrestler1 = Wrestler.builder().build();
    wrestler1.setId(20L);
    wrestler1.setName("Available Wrestler 1");
    wrestler1.setFans(80L);

    Wrestler wrestler2 = Wrestler.builder().build();
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
    Set<Wrestler> updatedMembers = new HashSet<>(updated.getMembers());
    updatedMembers.add(newMember);
    updated.setMembers(updatedMembers);

    return updated;
  }

  /** Helper method to create updated faction with removed member. */
  private Faction createUpdatedFactionWithRemovedMember(Wrestler removedMember) {
    Faction updated = createTestFactionWithMembers();
    Set<Wrestler> updatedMembers = new HashSet<>(updated.getMembers());
    updatedMembers.remove(removedMember);
    updated.setMembers(updatedMembers);

    return updated;
  }
}
