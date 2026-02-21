# Implementation Plan: Add Season Summary to Player Dashboard

## Phase 1: Data Access and Logic

This phase focuses on creating the backend services and repositories needed to aggregate season statistics for a player.

- [ ] Task: Create `SeasonStatsDTO` to hold summary data
  - [ ] Define fields for wins, losses, draws, starting fans, ending fans, and accolades.
- [ ] Task: Implement `SeasonStatsService` to calculate statistics
  - [ ] Create `SeasonStatsService.java`
  - [ ] Write failing test: `testCalculateSeasonStatsForPlayer` in `SeasonStatsServiceTest.java`
  - [ ] Implement `calculateStats(Player player, Season season)` logic
  - [ ] Verify test passes
- [ ] Task: Add method to `SeasonRepository` to retrieve all seasons for a player
  - [ ] Write failing test in `SeasonRepositoryTest.java`
  - [ ] Implement query logic
  - [ ] Verify test passes
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Data Access and Logic' (Protocol in workflow.md)

## Phase 2: Sidebar Component Implementation

This phase focuses on building the Vaadin component for the Season Summary and integrating it into the sidebar.

- [ ] Task: Create `SeasonSummaryComponent` using Vaadin
  - [ ] Write failing test: `testSeasonSummaryComponentRendering` in `SeasonSummaryComponentTest.java` (using Karibu Testing)
  - [ ] Implement basic layout with placeholders for record and fan growth
  - [ ] Integrate **Progress Bars** for fan growth visualization
  - [ ] Verify test passes
- [ ] Task: Implement Season Selection Dropdown
  - [ ] Add `ComboBox` to `SeasonSummaryComponent` to list available seasons
  - [ ] Write failing test: `testSeasonSelectionUpdatesData`
  - [ ] Implement listener to refresh statistics when a new season is selected
  - [ ] Verify test passes
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Sidebar Component Implementation' (Protocol in workflow.md)

## Phase 3: Dashboard Integration and Polishing

This phase focuses on placing the component into the Player Dashboard and adding final visual touches.

- [ ] Task: Integrate `SeasonSummaryComponent` into `PlayerDashboardView`
  - [ ] Locate sidebar/drawer in `PlayerDashboardView.java`
  - [ ] Add the new component to the layout
  - [ ] Write failing E2E test: `testSeasonSummaryVisibleOnDashboard`
  - [ ] Verify test passes
- [ ] Task: Add Icons/Badges for Accolades
  - [ ] Implement logic to display badges based on championships held during the season
  - [ ] Write failing test: `testAccoladeBadgesDisplayed`
  - [ ] Verify test passes
- [ ] Task: Final UI Polish and Responsiveness
  - [ ] Ensure Lumo theme styling is consistent
  - [ ] Verify layout on mobile screen sizes
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Dashboard Integration and Polishing' (Protocol in workflow.md)

