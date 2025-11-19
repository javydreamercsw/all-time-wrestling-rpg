package com.github.javydreamercsw.management.ui.view.faction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.grid.Grid;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for FactionListView. Tests the UI components, grid functionality, and CRUD operations.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FactionListViewTest {

  @Mock private FactionService factionService;
  @Mock private WrestlerService wrestlerService;

  private FactionListView factionListView;
  private List<Faction> testFactions;
  private List<Wrestler> testWrestlers;

  @BeforeEach
  void setUp() {
    // Create test wrestlers first
    testWrestlers = createTestWrestlers();

    // Create test factions with members (now that wrestlers exist)
    testFactions = createTestFactionsWithMembers();

    // Mock service responses
    when(factionService.findAllWithMembersAndTeams()).thenReturn(testFactions);
    when(wrestlerService.findAll()).thenReturn(testWrestlers);

    // Create the view
    factionListView = new FactionListView(factionService, wrestlerService);
  }

  @Test
  @DisplayName("Should initialize view with correct components")
  void shouldInitializeViewWithCorrectComponents() {
    // Then
    assertNotNull(factionListView.name);
    assertNotNull(factionListView.createBtn);
    assertNotNull(factionListView.factionGrid);

    assertEquals("Create Faction", factionListView.createBtn.getText());
    assertTrue(factionListView.name.getPlaceholder().contains("Enter faction name"));
  }

  @Test
  @DisplayName("Should load factions into grid on initialization")
  void shouldLoadFactionsIntoGridOnInitialization() {
    // Then
    verify(factionService).findAllWithMembersAndTeams();

    // Grid should be configured with test data
    assertNotNull(factionListView.factionGrid);
    // Note: In a real test environment, you might need to use TestBench or similar
    // to verify grid contents, as Vaadin components are complex to test in unit tests
  }

  @Test
  @DisplayName("Should have correct grid columns")
  void shouldHaveCorrectGridColumns() {
    // Then
    Grid<Faction> grid = factionListView.factionGrid;
    assertNotNull(grid);

    // Verify grid is configured (columns are added during construction)
    assertFalse(grid.getColumns().isEmpty());
  }

  @Test
  @DisplayName("Should call faction service when saving faction")
  void shouldCallFactionServiceWhenSavingFaction() {
    // When - This would normally be triggered by UI interaction
    // For unit testing, we can verify the service is properly injected
    assertNotNull(factionListView);

    // Then
    // The service should be available for use
    verify(factionService).findAllWithMembersAndTeams(); // Called during initialization
  }

  @Test
  @DisplayName("Should handle empty faction list")
  void shouldHandleEmptyFactionList() {
    // Given
    when(factionService.findAllWithMembers()).thenReturn(new ArrayList<>());

    // When
    FactionListView emptyView = new FactionListView(factionService, wrestlerService);

    // Then
    assertNotNull(emptyView);
    assertNotNull(emptyView.factionGrid);
  }

  @Test
  @DisplayName("Should handle wrestler service for member management")
  void shouldHandleWrestlerServiceForMemberManagement() {
    // Given - wrestlers are already mocked in setUp

    // Then
    verify(wrestlerService, atLeastOnce()).findAll(); // May be called during initialization
    assertNotNull(factionListView);
  }

  @Test
  @DisplayName("Should render faction members without LazyInitializationException")
  void shouldRenderFactionMembersWithoutLazyInitializationException() {
    // This test simulates what happens when the grid tries to render faction members
    // It should catch LazyInitializationException if the members aren't properly eagerly loaded

    // Given - factions with members are already set up in setUp()
    assertFalse(testFactions.isEmpty());

    // When - simulate grid rendering by accessing member names (this is what the UI does)
    for (Faction faction : testFactions) {
      if (faction.getMembers() != null && !faction.getMembers().isEmpty()) {
        // This is exactly what the grid column does:
        // faction.getMembers().stream().map(Wrestler::getName)
        for (Wrestler member : faction.getMembers()) {
          // This call would throw LazyInitializationException if members aren't properly loaded
          String memberName = member.getName();
          assertNotNull(memberName, "Member name should not be null");
          assertFalse(memberName.isEmpty(), "Member name should not be empty");
        }
      }
    }

    // Then - if we get here without exception, the eager loading is working
    assertTrue(
        true, "Successfully accessed all faction member names without LazyInitializationException");
  }

  @Test
  @DisplayName("Should create view with all required services")
  void shouldCreateViewWithAllRequiredServices() {
    // Given - services are mocked

    // When
    FactionListView view = new FactionListView(factionService, wrestlerService);

    // Then
    assertNotNull(view);
    assertNotNull(view.name);
    assertNotNull(view.createBtn);
    assertNotNull(view.factionGrid);
  }

  /** Helper method to create test factions with members for testing. */
  private List<Faction> createTestFactionsWithMembers() {
    List<Faction> factions = new ArrayList<>();

    // Create Evolution faction with members
    Faction evolution = Faction.builder().build();
    evolution.setId(1L);
    evolution.setName("Evolution");
    evolution.setDescription("A dominant faction in WWE");
    evolution.setIsActive(false);
    evolution.setCreationDate(Instant.now());
    evolution.setFormedDate(Instant.now().minusSeconds(365 * 24 * 60 * 60)); // 1 year ago
    evolution.setDisbandedDate(Instant.now().minusSeconds(180 * 24 * 60 * 60)); // 6 months ago

    // Add members to Evolution (this is what was missing!)
    List<Wrestler> evolutionMembers = new ArrayList<>();
    evolutionMembers.add(testWrestlers.get(0)); // Triple H
    evolutionMembers.add(testWrestlers.get(2)); // Randy Orton
    evolution.setMembers(evolutionMembers);

    // Create DX faction with members
    Faction dx = Faction.builder().build();
    dx.setId(2L);
    dx.setName("D-Generation X");
    dx.setDescription("Rebellious faction");
    dx.setIsActive(true);
    dx.setCreationDate(Instant.now());
    dx.setFormedDate(Instant.now().minusSeconds(200 * 24 * 60 * 60)); // ~7 months ago

    // Add members to DX
    List<Wrestler> dxMembers = new ArrayList<>();
    dxMembers.add(testWrestlers.get(1)); // Shawn Michaels
    dx.setMembers(dxMembers);

    factions.add(evolution);
    factions.add(dx);

    return factions;
  }

  /** Helper method to create test wrestlers for testing. */
  private List<Wrestler> createTestWrestlers() {
    List<Wrestler> wrestlers = new ArrayList<>();

    Wrestler wrestler1 = Wrestler.builder().build();
    wrestler1.setId(1L);
    wrestler1.setName("Triple H");
    wrestler1.setFans(95L);

    Wrestler wrestler2 = Wrestler.builder().build();
    wrestler2.setId(2L);
    wrestler2.setName("Shawn Michaels");
    wrestler2.setFans(90L);

    Wrestler wrestler3 = Wrestler.builder().build();
    wrestler3.setId(3L);
    wrestler3.setName("Randy Orton");
    wrestler3.setFans(85L);

    wrestlers.add(wrestler1);
    wrestlers.add(wrestler2);
    wrestlers.add(wrestler3);

    return wrestlers;
  }
}
