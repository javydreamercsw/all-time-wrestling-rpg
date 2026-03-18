# Implementation Plan: Content Set Grouping

## Phase 1: Data Model and Schema Updates
*   **Objective:** Update the JSON data structures and database schema to support content sets.
*   - [ ] **Task: Update JSON Schemas for Wrestlers and Teams**
    - [ ] Add `set` field to all objects in `src/main/resources/wrestlers.json` (Default to "BASE_GAME").
    - [ ] Add `set` field to all objects in `src/main/resources/teams.json` (Default to "BASE_GAME").
*   - [ ] **Task: Database Migration for Set Preferences**
    - [ ] Verify `GameSetting` table structure for key-value pair support.
    - [ ] Create a migration/setup script to initialize set enablement settings.
*   - [ ] **Task: Conductor - User Manual Verification 'Phase 1: Data Model' (Protocol in workflow.md)**

## Phase 2: Backend Logic and Filtering
*   **Objective:** Implement the service-level logic to filter content based on enabled sets.
*   - [ ] **Task: Implement Set Management Service**
    - [ ] Write unit tests for `SetManagementService` (retrieving sets, toggling state).
    - [ ] Implement `SetManagementService` to interact with `GameSetting`.
*   - [ ] **Task: Update Content Loading Logic**
    - [ ] Write unit tests for `WrestlerService` and `TeamService` filtering by enabled sets.
    - [ ] Implement filtering logic in `WrestlerService` to exclude content from disabled sets.
    - [ ] Implement filtering logic in `TeamService` to exclude content from disabled sets.
*   - [ ] **Task: Conductor - User Manual Verification 'Phase 2: Backend Logic' (Protocol in workflow.md)**

## Phase 3: Set Management UI
*   **Objective:** Create the user interface for managing content sets.
*   - [ ] **Task: Create Set Management View**
    - [ ] Write E2E/Integration tests for the Set Management view (displaying sets, toggling).
    - [ ] Implement the `SetManagementView` in Vaadin.
    - [ ] Add toggles for each unique set found in the system.
*   - [ ] **Task: Integrate Set Management into Navigation**
    - [ ] Add a link to the Set Management view in the main navigation menu.
*   - [ ] **Task: Conductor - User Manual Verification 'Phase 3: Set Management UI' (Protocol in workflow.md)**

## Phase 4: Final Integration and Verification
*   **Objective:** Ensure end-to-end functionality and persistence.
*   - [ ] **Task: End-to-End Testing of Set Toggling**
    - [ ] Write E2E tests: Disable a set -> Verify wrestler is hidden from Roster -> Enable set -> Verify wrestler reappears.
*   - [ ] **Task: Quality Gate Check**
    - [ ] Verify 90% code coverage for new services.
    - [ ] Run full test suite (`./mvnw verify`).
*   - [ ] **Task: Conductor - User Manual Verification 'Phase 4: Final Integration' (Protocol in workflow.md)**