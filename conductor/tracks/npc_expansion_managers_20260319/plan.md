# Implementation Plan: NPC Expansion and Manager Support

## Phase 1: Data Model and Schema Updates [checkpoint: 6b1d2ad]

* **Objective:** Update the JSON data structures and database schema to support NPC expansions and manager assignments.
* - [x] Task: Update JSON Schemas for NPCs, Teams, and Factions
  - [x] Add `set` field to all objects in `src/main/resources/npcs.json` (Default to "BASE_GAME").
  - [x] Assign **MVP** to `HURT_BUSINESS` and **Colonel Mustafa** to `RUMBLE`.
  - [x] Add optional `manager` field to `src/main/resources/teams.json` and `src/main/resources/factions.json`.
* - [x] Task: Database Migrations
  - [x] Create a migration to add `expansion_code` column to the `npc` table.
  - [x] Create a migration to add `manager_id` column to the `team` and `faction` tables.
* - [x] Task: Conductor - User Manual Verification 'Phase 1: Data Model' (Protocol in workflow.md)

## Phase 2: Backend Logic and Filtering [checkpoint: 6b1d2ad]

* **Objective:** Implement the filtering logic for NPCs and manager assignments.
* - [x] Task: Update NPC and Content Loading Logic
  - [x] Update `NPC` entity and `NpcDTO` to support `expansionCode`.
  - [x] Update `DataInitializer` to map NPC `set` and Team/Faction `manager` assignments during sync.
  - [x] Implement expansion-aware filtering in `NPCService`.
  - [x] Add `ExpansionToggledEvent` listener to `NPCService` for cache eviction.
* - [x] Task: Update Team and Faction Domain and Services
  - [x] Update `Team` and `Faction` entities to include the `manager` relationship.
  - [x] Implement logic in `TeamService` and `FactionService` to hide assigned managers if their expansion is disabled.
* - [x] Task: Conductor - User Manual Verification 'Phase 2: Backend Logic' (Protocol in workflow.md)

## Phase 3: UI Integration [checkpoint: 67f894f]
*   **Objective:** Enhance the user interface to support manager assignments and filtered views.
*   - [x] Task: Update Management Dialogs
    - [x] Update `TeamFormDialog` to include an NPC dropdown for manager selection (filtered by expansion).
    - [x] Update Faction creation and edit dialogs to include a manager dropdown.
*   - [x] Task: Update Views and Components
    - [x] Display assigned managers in the Roster, Team list, and Faction views.
    - [x] Ensure all manager selection dropdowns across the app respect expansion enablement.
*   - [x] Task: Conductor - User Manual Verification 'Phase 3: UI Integration' (Protocol in workflow.md)

## Phase 4: Final Integration and Verification

* **Objective:** Ensure end-to-end functionality, test coverage, and documentation.
* - [ ] Task: Automated and E2E Testing
  - [ ] Write integration tests for NPC filtering.
  - [ ] Create E2E test: Disable `HURT_BUSINESS` -> Verify MVP is hidden.
  - [ ] Create E2E test: Verify Team manager is hidden if their expansion is disabled.
* - [ ] Task: Quality Gate Check
  - [ ] Verify >80% coverage for new logic.
  - [ ] Run full test suite (`./mvnw verify`).
* - [ ] Task: Documentation Update
  - [ ] Update `GAME_MECHANICS.md` to document the new manager support features.
* - [ ] Task: Conductor - User Manual Verification 'Phase 4: Final Integration' (Protocol in workflow.md)

