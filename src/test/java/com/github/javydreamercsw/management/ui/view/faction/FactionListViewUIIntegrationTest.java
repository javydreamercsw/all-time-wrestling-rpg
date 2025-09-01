package com.github.javydreamercsw.management.ui.view.faction;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.base.test.BaseTest;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.sync.entity.WrestlerSyncService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.DataProvider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * UI Integration test for FactionListView that tests real UI components with real database
 * interactions. This test verifies that the UI correctly displays faction members that are stored
 * in the database, bridging the gap between data layer tests and actual user experience.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FactionListViewUIIntegrationTest extends BaseTest {

  @Autowired private FactionService factionService;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private FactionRepository factionRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private WrestlerSyncService wrestlerSyncService;

  private FactionListView factionListView;
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
    testWrestler1.setStartingStamina(15);
    testWrestler1.setLowStamina(4);
    testWrestler1.setStartingHealth(18);
    testWrestler1.setLowHealth(4);
    testWrestler1.setDeckSize(15);
    testWrestler1.setCreationDate(Instant.now());
    testWrestler1 = wrestlerRepository.save(testWrestler1);

    testWrestler2 = new Wrestler();
    testWrestler2.setName("Randy Orton");
    testWrestler2.setStartingStamina(16);
    testWrestler2.setLowStamina(2);
    testWrestler2.setStartingHealth(16);
    testWrestler2.setLowHealth(2);
    testWrestler2.setDeckSize(15);
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

    // Initialize the UI view with real services - this is the key UI integration test
    factionListView = new FactionListView(factionService, wrestlerService);
  }

  @Test
  @DisplayName("Should instantiate UI components without errors")
  void shouldInstantiateUIComponentsWithoutErrors() {
    // When - UI components are instantiated in setUp()

    // Then - All UI components should be properly initialized
    assertNotNull(factionListView, "FactionListView should be instantiated");
    assertNotNull(factionListView.factionGrid, "Faction grid should be initialized");
    assertNotNull(factionListView.name, "Name field should be initialized");
    assertNotNull(factionListView.createBtn, "Create button should be initialized");
  }

  @Test
  @DisplayName("Should display faction members in UI grid without LazyInitializationException")
  void shouldDisplayFactionMembersInUIGridWithoutLazyInitializationException() {
    // When - The UI loads data (this happens during view instantiation via refreshGrid())
    Grid<Faction> factionGrid = factionListView.factionGrid;

    // Then - Verify the grid is populated with faction data
    assertNotNull(factionGrid, "Faction grid should be initialized");

    // Get the data provider and items - this tests the actual UI data loading
    DataProvider<Faction, ?> dataProvider = factionGrid.getDataProvider();
    assertNotNull(dataProvider, "Data provider should not be null");

    // Fetch all items from the grid using the correct Vaadin API
    List<Faction> gridItems =
        dataProvider.fetch(new com.vaadin.flow.data.provider.Query<>()).toList();
    assertFalse(gridItems.isEmpty(), "Grid should contain faction data");

    // Find our test faction in the grid
    Optional<Faction> evolutionFaction =
        gridItems.stream().filter(f -> "Evolution".equals(f.getName())).findFirst();

    assertTrue(evolutionFaction.isPresent(), "Evolution faction should be present in grid");

    Faction faction = evolutionFaction.get();

    // This is the critical test - accessing members should not throw LazyInitializationException
    // because the UI's refreshGrid() method is now properly transactional
    assertNotNull(faction.getMembers(), "Faction members should not be null");
    assertFalse(faction.getMembers().isEmpty(), "Faction should have members");
    assertEquals(2, faction.getMembers().size(), "Faction should have 2 members");

    // Verify specific members are present - this tests the complete data flow
    List<String> memberNames = faction.getMembers().stream().map(Wrestler::getName).toList();
    assertTrue(memberNames.contains("Triple H"), "Should contain Triple H");
    assertTrue(memberNames.contains("Randy Orton"), "Should contain Randy Orton");
  }

  @Test
  @DisplayName("Should render member names in grid column without errors")
  void shouldRenderMemberNamesInGridColumnWithoutErrors() {
    // When - We simulate the grid column rendering logic
    Grid<Faction> factionGrid = factionListView.factionGrid;

    // Get the faction data from the grid
    List<Faction> gridItems =
        factionGrid.getDataProvider().fetch(new com.vaadin.flow.data.provider.Query<>()).toList();

    Optional<Faction> evolutionFaction =
        gridItems.stream().filter(f -> "Evolution".equals(f.getName())).findFirst();

    assertTrue(evolutionFaction.isPresent(), "Evolution faction should be present");

    Faction faction = evolutionFaction.get();

    // This simulates exactly what the grid column renderer does - it should not throw an exception
    String membersColumnValue;
    if (faction.getMembers() == null || faction.getMembers().isEmpty()) {
      membersColumnValue = "No members";
    } else {
      membersColumnValue =
          faction.getMembers().stream().map(Wrestler::getName).collect(Collectors.joining(", "));
    }

    // Verify the rendered value matches what users should see
    assertNotNull(membersColumnValue, "Members column value should not be null");
    assertFalse(membersColumnValue.equals("No members"), "Should not show 'No members'");
    assertTrue(membersColumnValue.contains("Triple H"), "Should display Triple H");
    assertTrue(membersColumnValue.contains("Randy Orton"), "Should display Randy Orton");
    assertTrue(membersColumnValue.contains(", "), "Should join names with comma");
    assertEquals(
        "Triple H, Randy Orton", membersColumnValue, "Should display members in expected format");
  }

  @Test
  @DisplayName("Should handle empty faction list gracefully in UI")
  void shouldHandleEmptyFactionListGracefullyInUI() {
    // Given - Remove all factions
    factionRepository.deleteAll();

    // When - Create a new view instance (this triggers refreshGrid)
    FactionListView emptyView = new FactionListView(factionService, wrestlerService);

    // Then - Grid should be empty but not throw exceptions
    Grid<Faction> factionGrid = emptyView.factionGrid;
    assertNotNull(factionGrid, "Grid should be initialized even when empty");

    List<Faction> gridItems =
        factionGrid.getDataProvider().fetch(new com.vaadin.flow.data.provider.Query<>()).toList();

    assertTrue(gridItems.isEmpty(), "Grid should be empty when no factions exist");
  }

  @Test
  @DisplayName("Should display faction leader information correctly in UI")
  void shouldDisplayFactionLeaderInformationCorrectlyInUI() {
    // Given - Set a leader for the faction
    testFaction.setLeader(testWrestler1);
    factionRepository.save(testFaction);

    // When - Create a new view to pick up the leader change
    FactionListView viewWithLeader = new FactionListView(factionService, wrestlerService);

    Grid<Faction> factionGrid = viewWithLeader.factionGrid;
    List<Faction> gridItems =
        factionGrid.getDataProvider().fetch(new com.vaadin.flow.data.provider.Query<>()).toList();

    Optional<Faction> evolutionFaction =
        gridItems.stream().filter(f -> "Evolution".equals(f.getName())).findFirst();

    assertTrue(evolutionFaction.isPresent(), "Evolution faction should be present");

    Faction faction = evolutionFaction.get();

    // Test leader column rendering (simulates what the UI column renderer does)
    String leaderColumnValue =
        faction.getLeader() != null ? faction.getLeader().getName() : "No Leader";

    assertEquals("Triple H", leaderColumnValue, "Should display leader name correctly");
    assertNotEquals(
        "No Leader", leaderColumnValue, "Should not show 'No Leader' when leader exists");
  }

  @Test
  @DisplayName("Should maintain transaction boundary during grid refresh")
  void shouldMaintainTransactionBoundaryDuringGridRefresh() {
    // This test specifically validates that the @Transactional annotation fix works

    // When - Directly call the refreshGrid method (simulates what happens on view load)
    // This should not throw LazyInitializationException due to the @Transactional annotation
    assertDoesNotThrow(
        () -> {
          // Note: We can't directly access refreshGrid since it's private, but instantiation calls
          // it
          new FactionListView(factionService, wrestlerService);
        },
        "View instantiation should not throw LazyInitializationException");

    // Verify the grid is properly populated
    Grid<Faction> factionGrid = factionListView.factionGrid;
    List<Faction> gridItems =
        factionGrid.getDataProvider().fetch(new com.vaadin.flow.data.provider.Query<>()).toList();

    assertFalse(gridItems.isEmpty(), "Grid should be populated after refresh");

    // Verify we can access faction members without transaction issues
    for (Faction faction : gridItems) {
      assertDoesNotThrow(
          () -> {
            List<Wrestler> members = faction.getMembers();
            if (members != null) {
              for (Wrestler member : members) {
                String memberName = member.getName(); // This would fail without proper transaction
                assertNotNull(memberName, "Member name should be accessible");
              }
            }
          },
          "Should be able to access faction members without LazyInitializationException");
    }
  }

  @Test
  @DisplayName("Should test actual UI form field initialization")
  void shouldTestActualUIFormFieldInitialization() {
    // When - UI components are created

    // Then - Form fields should be properly configured
    assertNotNull(factionListView.name, "Name field should exist");
    assertEquals(
        "Enter faction name...",
        factionListView.name.getPlaceholder(),
        "Should have correct placeholder");
    assertEquals(255, factionListView.name.getMaxLength(), "Should have correct max length");

    assertNotNull(factionListView.createBtn, "Create button should exist");
    assertEquals(
        "Create Faction", factionListView.createBtn.getText(), "Should have correct button text");
  }

  @Test
  @DisplayName("Should display synced factions with members in UI grid")
  @EnabledIf("isNotionTokenAvailable")
  void shouldDisplaySyncedFactionsWithMembersInUIGrid() {
    // Given - Clean slate and sync wrestlers/factions from Notion
    factionRepository.deleteAll();
    wrestlerRepository.deleteAll();

    // When - Sync wrestlers first (this will also sync related factions)
    wrestlerSyncService.syncWrestlers("ui-integration-test");

    // Then - Verify that synced data is present in the database
    List<Wrestler> syncedWrestlers = wrestlerRepository.findAll();
    List<Faction> syncedFactions = factionRepository.findAll();

    // Skip the test if no data was synced (no NOTION_TOKEN or empty Notion database)
    if (syncedFactions.isEmpty()) {
      org.junit.jupiter.api.Assumptions.assumeTrue(
          false, "No factions were synced from Notion - skipping UI test");
      return;
    }

    // Create UI view with the synced data
    FactionListView syncedDataView = new FactionListView(factionService, wrestlerService);

    // Verify the grid displays synced faction data correctly
    Grid<Faction> factionGrid = syncedDataView.factionGrid;
    assertNotNull(factionGrid, "Faction grid should be initialized");

    List<Faction> gridItems =
        factionGrid.getDataProvider().fetch(new com.vaadin.flow.data.provider.Query<>()).toList();

    assertFalse(gridItems.isEmpty(), "Should display synced factions from Notion");
    assertEquals(
        syncedFactions.size(), gridItems.size(), "Grid should display all synced factions");

    // Test that we can access faction data without LazyInitializationException
    for (Faction faction : gridItems) {
      assertDoesNotThrow(
          () -> {
            // Access all faction properties that the UI displays
            String name = faction.getName();
            String description = faction.getDescription();
            Boolean isActive = faction.getIsActive();
            String externalId = faction.getExternalId();

            assertNotNull(name, "Faction name should be accessible");
            assertNotNull(externalId, "External ID should be present for synced factions");

            // Test status column rendering (simulates UI column rendering)
            String statusDisplay = isActive ? "Active" : "Disbanded";
            assertNotNull(statusDisplay, "Status should be displayable");

            // Access leader properties if present
            if (faction.getLeader() != null) {
              String leaderName = faction.getLeader().getName();
              String leaderExternalId = faction.getLeader().getExternalId();
              assertNotNull(leaderName, "Leader name should be accessible");
              assertNotNull(leaderExternalId, "Leader should have external ID from sync");

              // Test leader column rendering
              String leaderDisplay = leaderName;
              assertNotNull(leaderDisplay, "Leader display should work");
            }

            // Access member properties
            if (faction.getMembers() != null && !faction.getMembers().isEmpty()) {
              // Test members column rendering (simulates UI column rendering)
              String membersDisplay =
                  faction.getMembers().stream()
                      .map(Wrestler::getName)
                      .collect(Collectors.joining(", "));
              assertNotNull(membersDisplay, "Members display should work");
              assertFalse(
                  membersDisplay.equals("No members"),
                  "Should not show 'No members' when members exist");

              // Verify each member is accessible
              for (Wrestler member : faction.getMembers()) {
                String memberName = member.getName();
                String memberExternalId = member.getExternalId();
                assertNotNull(memberName, "Member name should be accessible");
                assertNotNull(memberExternalId, "Member should have external ID from sync");
              }
            }
          },
          "Should access all synced faction data without LazyInitializationException");
    }

    // Test specific faction if we know what to expect from sync
    // Look for any faction with members to test the complete data flow
    Optional<Faction> factionWithMembers =
        gridItems.stream()
            .filter(f -> f.getMembers() != null && !f.getMembers().isEmpty())
            .findFirst();

    if (factionWithMembers.isPresent()) {
      Faction testFaction = factionWithMembers.get();

      // Test the exact UI column rendering logic
      String membersColumnValue;
      if (testFaction.getMembers() == null || testFaction.getMembers().isEmpty()) {
        membersColumnValue = "No members";
      } else {
        membersColumnValue =
            testFaction.getMembers().stream()
                .map(Wrestler::getName)
                .collect(Collectors.joining(", "));
      }

      String leaderColumnValue =
          testFaction.getLeader() != null ? testFaction.getLeader().getName() : "No Leader";
      String statusColumnValue = testFaction.getIsActive() ? "Active" : "Disbanded";

      // Verify the rendered values are valid
      assertNotNull(membersColumnValue, "Members column value should not be null");
      assertNotNull(leaderColumnValue, "Leader column value should not be null");
      assertNotNull(statusColumnValue, "Status column value should not be null");

      // Log the synced faction for debugging
      System.out.println(
          "✅ Successfully tested synced faction: "
              + testFaction.getName()
              + " with "
              + testFaction.getMembers().size()
              + " members");
    }

    // Final verification: ensure the grid shows real synced data
    assertTrue(
        gridItems.stream().allMatch(f -> f.getExternalId() != null),
        "All factions in grid should have external IDs from sync");

    System.out.println(
        "✅ UI Integration Test completed with "
            + gridItems.size()
            + " synced factions displayed correctly");
  }
}
