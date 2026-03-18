# Implementation Plan: Content Set Grouping

## Phase 1: Data Model and Schema Updates [checkpoint: d599ce6]

* **Objective:** Ensure wrestlers have set data and clean up teams/factions.
* - [x] Task: Update JSON Schemas for Wrestlers
  - [x] Add `set` field to all objects in `src/main/resources/wrestlers.json`.
* - [x] Task: Clean up Teams and Factions
  - [x] Remove `set` field from `src/main/resources/teams.json` (Derived from members instead).
  - [x] Ensure `src/main/resources/factions.json` does not have a `set` field.
* - [x] Task: Database Migration for Set Preferences
  - [x] Verify `GameSetting` table structure.
  - [x] Initialize `GameSetting` entries for all expansions found in `expansions.json` (Default to Enabled).
* - [x] Task: Conductor - User Manual Verification 'Phase 1: Data Model' (Protocol in workflow.md)

## Phase 2: Backend Logic and Filtering [checkpoint: f977f58]

* **Objective:** Implement member-aware filtering for teams and factions.
* - [x] Task: Implement Expansion Service
  - [x] Write unit tests for `ExpansionService`.
  - [x] Implement `ExpansionService` to interact with `GameSetting` and `expansions.json`.
* - [x] Task: Update Content Loading Logic
  - [x] Implement filtering in `WrestlerService` based on `GameSetting`.
  - [x] Implement filtering in `TeamService`: Filter teams where any member's set is disabled.
  - [x] Implement filtering in `FactionService`: Filter factions where any member's set is disabled.
* - [x] Task: Conductor - User Manual Verification 'Phase 2: Backend Logic' (Protocol in workflow.md)

## Phase 3: Set Management UI

* **Objective:** Create the user interface for managing content sets.
* - [x] Task: Create Expansion Management View
  - [x] Implement the `ExpansionManagementView` component in Vaadin.
  - [x] Add toggles for each unique expansion found in `expansions.json`.
* - [x] Task: Integrate Expansion Management into Navigation
  - [x] Add an "Expansion Management" tab to the existing `AdminView`.
* - [ ] Task: Conductor - User Manual Verification 'Phase 3: Set Management UI' (Protocol in workflow.md)

## Phase 4: Final Integration and Verification

* **Objective:** Ensure end-to-end functionality and persistence.
* - [ ] Task: End-to-End Testing of Set Toggling
  - [ ] Write E2E tests: Disable a set -> Verify wrestler is hidden from Roster -> Enable set -> Verify wrestler reappears.
* - [ ] Task: Quality Gate Check
  - [ ] Verify 90% code coverage for new services.
  - [ ] Run full test suite (`./mvnw verify`).
* - [ ] Task: Conductor - User Manual Verification 'Phase 4: Final Integration' (Protocol in workflow.md)

