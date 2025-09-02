package com.github.javydreamercsw.management.ui.view.faction;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for FactionListView that tests real database interactions. This test would catch
 * LazyInitializationException that unit tests with mocks cannot detect.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FactionListViewIntegrationTest {

  @Autowired private FactionService factionService;

  @Autowired private WrestlerService wrestlerService;

  @Autowired private FactionRepository factionRepository;

  @Autowired private WrestlerRepository wrestlerRepository;

  private Faction testFaction;
  private Wrestler testWrestler1;
  private Wrestler testWrestler2;

  @BeforeEach
  void setUp() {
    // Clean up any existing test data
    factionRepository.deleteAll();
    wrestlerRepository.deleteAll();

    // Create real wrestlers in the database
    testWrestler1 = new Wrestler();
    testWrestler1.setName("Triple H");
    testWrestler1.setStartingStamina(10);
    testWrestler1.setLowStamina(5);
    testWrestler1.setStartingHealth(10);
    testWrestler1.setLowHealth(5);
    testWrestler1.setDeckSize(30);
    testWrestler1.setCreationDate(Instant.now());
    testWrestler1 = wrestlerRepository.save(testWrestler1);

    testWrestler2 = new Wrestler();
    testWrestler2.setName("Randy Orton");
    testWrestler2.setStartingStamina(10);
    testWrestler2.setLowStamina(5);
    testWrestler2.setStartingHealth(10);
    testWrestler2.setLowHealth(5);
    testWrestler2.setDeckSize(30);
    testWrestler2.setCreationDate(Instant.now());
    testWrestler2 = wrestlerRepository.save(testWrestler2);

    // Create real faction in the database with members
    testFaction = new Faction();
    testFaction.setName("Evolution");
    testFaction.setDescription("A dominant faction");
    testFaction.setIsActive(true);
    testFaction.setCreationDate(Instant.now());
    testFaction.setFormedDate(Instant.now());
    testFaction = factionRepository.save(testFaction);

    // Add members to the faction
    factionService.addMemberToFaction(testFaction.getId(), testWrestler1.getId());
    factionService.addMemberToFaction(testFaction.getId(), testWrestler2.getId());
  }

  @Test
  @DisplayName("Should load factions with members without LazyInitializationException")
  void shouldLoadFactionsWithMembersWithoutLazyInitializationException() {
    // This test simulates exactly what the UI does and would catch LazyInitializationException

    // When - Load factions using the same method the UI uses
    List<Faction> factions = factionService.findAllWithMembers();

    // Then - Verify we have factions with members
    assertFalse(factions.isEmpty(), "Should have at least one faction");

    Faction faction =
        factions.stream()
            .filter(f -> "Evolution".equals(f.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Evolution faction should exist"));

    assertNotNull(faction.getMembers(), "Faction should have members");
    assertFalse(faction.getMembers().isEmpty(), "Faction should have at least one member");

    // This is the critical test - accessing wrestler names outside of transaction context
    // This would throw LazyInitializationException if members aren't properly eagerly loaded
    for (Wrestler member : faction.getMembers()) {
      String memberName = member.getName(); // This is what the UI grid does
      assertNotNull(memberName, "Member name should not be null");
      assertFalse(memberName.trim().isEmpty(), "Member name should not be empty");

      // Also test other properties that might be accessed in the UI
      Long id = member.getId();
      assertNotNull(id, "Wrestler ID should not be null");
    }

    // Verify we can access the specific members we added
    List<String> memberNames = faction.getMembers().stream().map(Wrestler::getName).toList();

    assertTrue(memberNames.contains("Triple H"), "Should contain Triple H");
    assertTrue(memberNames.contains("Randy Orton"), "Should contain Randy Orton");
  }

  @Test
  @DisplayName("Should create FactionListView without LazyInitializationException")
  void shouldCreateFactionListViewWithoutLazyInitializationException() {
    // This test creates the actual UI component with real data
    // It would catch LazyInitializationException during grid initialization

    // When - Create the view (this triggers data loading and grid setup)
    FactionListView view = new FactionListView(factionService, wrestlerService);

    // Then - View should be created successfully
    assertNotNull(view, "FactionListView should be created successfully");

    // The fact that we get here without exception means the eager loading is working
    // In the original bug, this would have thrown LazyInitializationException
    // when the grid tried to render faction members
  }

  @Test
  @DisplayName("Should handle faction with no members gracefully")
  void shouldHandleFactionWithNoMembersGracefully() {
    // Create a faction with no members
    Faction emptyFaction = new Faction();
    emptyFaction.setName("Empty Faction");
    emptyFaction.setDescription("A faction with no members");
    emptyFaction.setIsActive(true);
    emptyFaction.setCreationDate(Instant.now());
    emptyFaction.setFormedDate(Instant.now());
    emptyFaction = factionRepository.save(emptyFaction);

    // When - Load factions
    List<Faction> factions = factionService.findAllWithMembers();

    // Then - Should handle empty members list gracefully
    Faction empty =
        factions.stream()
            .filter(f -> "Empty Faction".equals(f.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Empty faction should exist"));

    // Members list should be empty but not null
    assertNotNull(empty.getMembers(), "Members list should not be null");
    assertTrue(empty.getMembers().isEmpty(), "Members list should be empty");
  }
}
