# Plan: Title Reign Visibility and History Tracking

## Phase 1: Foundation & Data Layer
- [x] Task 1.1: Enhance DTOs (`ChampionshipDTO`, `RankedWrestlerDTO`) and create `TitleReignDTO` to include historical data fields. [448295e]
- [x] Task 1.2: Update `RankingService` to implement logic for fetching full historical data and calculating durations (including dynamic updates for active reigns). [c3611a3]
- [x] Task 1.3: Write Unit Tests for duration calculation logic, ensuring "Ongoing" status is correctly identified and timed relative to the game date. [c3611a3]
- [x] Task 1.4: Write Unit Tests for service-layer history retrieval (filtering by wrestler and by championship). [c3611a3]
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Foundation & Data Layer' (Protocol in workflow.md)

## Phase 2: Reusable UI Components
- [x] Task 2.1: Develop a `HistoryTimelineComponent` in Vaadin to provide a visual graphical sequence of holders/titles. [9164cb6]
- [x] Task 2.2: Develop a `ReignCardComponent` for detailed, expandable display of individual reigns, including match references. [9164cb6]
- [x] Task 2.3: Implement the Tooltip and Navigation logic to allow match references to show summaries on hover and redirect on click. [9164cb6]
- [x] Task 2.4: Write Component Tests (using Karibu-Testing) to verify UI behavior and data binding. [9164cb6]
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Reusable UI Components' (Protocol in workflow.md)

## Phase 3: Wrestler Profile Integration
- [ ] Task 3.1: Update `WrestlerProfileView` to include the new Title History section with the timeline and cards.
- [ ] Task 3.2: Ensure responsive layout for the history section on the profile page.
- [ ] Task 3.3: Write an End-to-End (E2E) test verifying that a wrestler's title history is visible and match links work.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Wrestler Profile Integration' (Protocol in workflow.md)

## Phase 4: Championship Legacy View Integration
- [ ] Task 4.1: Update `RankingView` (or a dedicated detail view) to display the complete lineal history of a selected championship.
- [ ] Task 4.2: Implement visual highlighting for the "Current Champion" in the legacy timeline.
- [ ] Task 4.3: Write an End-to-End (E2E) test verifying the lineal history navigation and correctness of the legacy display.
- [ ] Task: Conductor - User Manual Verification 'Phase 4: Championship Legacy View Integration' (Protocol in workflow.md)
