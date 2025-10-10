package com.github.javydreamercsw.management.service.faction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.base.test.AbstractIntegrationTest;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@DisplayName("Faction Service Integration Tests")
@EnabledIf("isNotionTokenAvailable")
@Transactional
class FactionServiceIntegrationTest extends AbstractIntegrationTest {

  @Autowired private FactionService factionService;

  @Autowired private WrestlerService wrestlerService;

  private Faction testFaction;
  private Wrestler testWrestler1;
  private Wrestler testWrestler2;

  @BeforeEach
  public void setUp() throws Exception {
    // Create test wrestlers with all required fields
    testWrestler1 = wrestlerService.createWrestler("John Cena", true, null);
    testWrestler2 = wrestlerService.createWrestler("The Rock", true, null);

    // Create test faction
    testFaction = new Faction();
    testFaction.setName("Test Faction");
    testFaction.setDescription("Test faction description");
    testFaction.setLeader(testWrestler1);
    testFaction.addMember(testWrestler1);
    testFaction.addMember(testWrestler2);
    testFaction = factionService.save(testFaction);
  }

  @Test
  @DisplayName("Should create new faction successfully")
  void shouldCreateNewFactionSuccessfully() {
    // Given
    Wrestler wrestler3 = wrestlerService.createWrestler("Stone Cold", true, null);

    // When
    Optional<Faction> result =
        factionService.createFaction(
            "The New Faction", "A newly created faction", wrestler3.getId());

    // Then
    assertThat(result).isPresent();
    Faction savedFaction = result.get();
    assertThat(savedFaction.getName()).isEqualTo("The New Faction");
    assertThat(savedFaction.getDescription()).isEqualTo("A newly created faction");
    assertThat(savedFaction.getLeader()).isEqualTo(wrestler3);
    assertThat(savedFaction.getMembers()).containsExactlyInAnyOrder(wrestler3);
  }

  @Test
  @DisplayName("Should not create duplicate faction names")
  void shouldNotCreateDuplicateFactionNames() {
    // Given - testFaction already exists from setUp()

    // When - try to create faction with same name
    Optional<Faction> result =
        factionService.createFaction(
            "Test Faction", // Duplicate name
            "Another description",
            testWrestler1.getId());

    // Then
    assertThat(result).isEmpty();
    // Verify only one faction with that name exists
    List<Faction> factions = factionService.findAll();
    assertThat(factions.stream().filter(f -> f.getName().equals("Test Faction"))).hasSize(1);
  }

  @Test
  @DisplayName("Should update existing faction successfully")
  void shouldUpdateExistingFactionSuccessfully() {
    // Given - testFaction already exists from setUp()
    Long factionId = testFaction.getId();

    // When - update faction
    Optional<Faction> result = factionService.getFactionById(factionId);
    assertThat(result).isPresent();
    Faction factionToUpdate = result.get();

    factionToUpdate.setName("Updated Faction Name");
    factionToUpdate.setDescription("Updated description");
    factionService.save(factionToUpdate);

    // Change leader
    assertNotNull(factionId);
    assertNotNull(testWrestler2.getId());
    result = factionService.changeFactionLeader(factionId, testWrestler2.getId());
    assertThat(result).isPresent();

    // Remove a member that is not the new leader
    assertNotNull(testWrestler1.getId());
    result =
        factionService.removeMemberFromFaction(factionId, testWrestler1.getId(), "Test removal");
    assertThat(result).isPresent();

    // Then
    Faction updatedFaction = result.get();
    assertThat(updatedFaction.getName()).isEqualTo("Updated Faction Name");
    assertThat(updatedFaction.getDescription()).isEqualTo("Updated description");
    assertThat(updatedFaction.getLeader()).isEqualTo(testWrestler2);
    assertThat(updatedFaction.getMembers()).containsExactlyInAnyOrder(testWrestler2);
  }

  @Test
  @DisplayName("Should delete faction successfully")
  void shouldDeleteFactionSuccessfully() {
    // Given - testFaction already exists from setUp()
    Long factionId = testFaction.getId();

    // When
    factionService.delete(testFaction);

    // Then
    Optional<Faction> deletedFaction = factionService.getFactionById(factionId);
    assertThat(deletedFaction).isEmpty();
  }

  @Test
  @DisplayName("Should add member to faction")
  void shouldAddMemberToFaction() {
    // Given - testFaction already exists from setUp()
    Wrestler newMember = wrestlerService.createWrestler("New Member", true, null);

    // When
    assertNotNull(testFaction.getId());
    assertNotNull(newMember.getId());
    Optional<Faction> result =
        factionService.addMemberToFaction(testFaction.getId(), newMember.getId());

    // Then
    assertThat(result).isPresent();
    Faction updatedFaction = result.get();
    assertThat(updatedFaction.getMembers()).contains(newMember);
  }

  @Test
  @DisplayName("Should remove member from faction")
  void shouldRemoveMemberFromFaction() {
    // Given - testFaction already exists from setUp() with testWrestler2 as member
    // Ensure testWrestler2 is a member
    testFaction.addMember(testWrestler2);
    factionService.save(testFaction);

    // When
    assertNotNull(testFaction.getId());
    assertNotNull(testWrestler2.getId());
    Optional<Faction> result =
        factionService.removeMemberFromFaction(
            testFaction.getId(), testWrestler2.getId(), "Removed for testing");

    // Then
    assertThat(result).isPresent();
    Faction updatedFaction = result.get();
    assertThat(updatedFaction.getMembers()).doesNotContain(testWrestler2);
  }

  @Test
  @DisplayName("Should find all factions with members")
  void shouldFindAllFactionsWithMembers() {
    // Given - testFaction already exists from setUp()

    // When
    List<Faction> factions = factionService.findAllWithMembers();

    // Then
    assertThat(factions).isNotEmpty();
    Optional<Faction> foundFaction =
        factions.stream().filter(f -> f.getName().equals("Test Faction")).findFirst();
    assertThat(foundFaction).isPresent();
    assertThat(foundFaction.get().getMembers()).isNotEmpty();
  }
}
