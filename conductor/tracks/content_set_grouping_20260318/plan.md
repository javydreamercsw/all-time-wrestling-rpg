# Implementation Plan: Content Set Grouping

## Phase 1: Data Model and Schema Updates

* **Objective:** Ensure wrestlers have set data and clean up teams/factions.
* - [x] Task: Update JSON Schemas for Wrestlers
  - [x] Add `set` field to all objects in `src/main/resources/wrestlers.json`.
* - [x] Task: Clean up Teams and Factions
  - [x] Remove `set` field from `src/main/resources/teams.json` (Derived from members instead).
  - [x] Ensure `src/main/resources/factions.json` does not have a `set` field.
* - [x] Task: Database Migration for Set Preferences
  - [x] Verify `GameSetting` table structure.
  - [x] Initialize `GameSetting` entries for all expansions found in `expansions.json` (Default to Enabled).
* - [ ] Task: Conductor - User Manual Verification 'Phase 1: Data Model' (Protocol in workflow.md)

## Phase 2: Backend Logic and Filtering

* **Objective:** Implement member-aware filtering for teams and factions.
* - [ ] Task: Implement Set Management Service
  - [ ] Write unit tests for `SetManagementService`.
  - [ ] Implement `SetManagementService` to interact with `GameSetting` and `expansions.json`.
* - [ ] Task: Update Content Loading Logic
  - [ ] Implement filtering in `WrestlerService` based on `GameSetting`.
  - [ ] Implement filtering in `TeamService`: Filter teams where any member's set is disabled.
  - [ ] Implement filtering in `FactionService`: Filter factions where any member's set is disabled.
* - [ ] Task: Conductor - User Manual Verification 'Phase 2: Backend Logic' (Protocol in workflow.md)

## Phase 3: Set Management UI

* **Objective:** Create the user interface for managing content sets.
* - [ ] Task: Create Set Management View
  - [ ] Write E2E/Integration tests for the Set Management view (displaying sets, toggling).
  - [ ] Implement the `SetManagementView` in Vaadin.
  - [ ] Add toggles for each unique set found in the system.
* - [ ] Task: Integrate Set Management into Navigation
  - [ ] Add a link to the Set Management view in the main navigation menu.
* - [ ] Task: Conductor - User Manual Verification 'Phase 3: Set Management UI' (Protocol in workflow.md)

## Phase 4: Final Integration and Verification

* **Objective:** Ensure end-to-end functionality and persistence.
* - [ ] Task: End-to-End Testing of Set Toggling
  - [ ] Write E2E tests: Disable a set -> Verify wrestler is hidden from Roster -> Enable set -> Verify wrestler reappears.
* - [ ] Task: Quality Gate Check
  - [ ] Verify 90% code coverage for new services.
  - [ ] Run full test suite (`./mvnw verify`).
* - [ ] Task: Conductor - User Manual Verification 'Phase 4: Final Integration' (Protocol in workflow.md)

