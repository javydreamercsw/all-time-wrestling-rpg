# Implementation Plan: NPC Expansion and Manager Support

## Phase 1: Data Model and Schema Updates
*   **Objective:** Update the JSON data structures and database schema to support NPC expansions and manager assignments.
*   - [ ] Task: Update JSON Schemas for NPCs, Teams, and Factions
    - [ ] Add `set` field to all objects in `src/main/resources/npcs.json` (Default to "BASE_GAME").
    - [ ] Assign **MVP** to `HURT_BUSINESS` and **Colonel Mustafa** to `RUMBLE`.
    - [ ] Add optional `manager` field to `src/main/resources/teams.json` and `src/main/resources/factions.json`.
*   - [ ] Task: Database Migrations
    - [ ] Create a migration to add `expansion_code` column to the `npc` table.
    - [ ] Create a migration to add `manager_id` column to the `team` and `faction` tables.
*   - [ ] Task: Conductor - User Manual Verification 'Phase 1: Data Model' (Protocol in workflow.md)

## Phase 2: Backend Logic and Filtering
*   **Objective:** Implement the filtering logic for NPCs and manager assignments.
*   - [ ] Task: Update NPC and Content Loading Logic
    - [ ] Update `NPC` entity and `NpcImportDTO` to support `expansionCode`.
    - [ ] Update `DataInitializer` to map NPC `set` and Team/Faction `manager` assignments during sync.
    - [ ] Implement expansion-aware filtering in `NPCService`.
    - [ ] Add `ExpansionToggledEvent` listener to `NPCService` for cache eviction.
*   - [ ] Task: Update Team and Faction Domain and Services
    - [ ] Update `Team` and `Faction` entities to include the `manager` relationship.
    - [ ] Implement logic in `TeamService` and `FactionService` to hide assigned managers if their expansion is disabled.
*   - [ ] Task: Conductor - User Manual Verification 'Phase 2: Backend Logic' (Protocol in workflow.md)

## Phase 3: UI Integration
*   **Objective:** Enhance the user interface to support manager assignments and filtered views.
*   - [ ] Task: Update Management Dialogs
    - [ ] Update `TeamFormDialog` to include an NPC dropdown for manager selection (filtered by expansion).
    - [ ] Update Faction creation and edit dialogs to include a manager dropdown.
*   - [ ] Task: Update Views and Components
    - [ ] Display assigned managers in the Roster, Team list, and Faction views.
    - [ ] Ensure all manager selection dropdowns across the app respect expansion enablement.
*   - [ ] Task: Conductor - User Manual Verification 'Phase 3: UI Integration' (Protocol in workflow.md)

## Phase 4: Final Integration and Verification
*   **Objective:** Ensure end-to-end functionality, test coverage, and documentation.
*   - [ ] Task: Automated and E2E Testing
    - [ ] Write integration tests for NPC filtering.
    - [ ] Create E2E test: Disable `HURT_BUSINESS` -> Verify MVP is hidden.
    - [ ] Create E2E test: Verify Team manager is hidden if their expansion is disabled.
*   - [ ] Task: Quality Gate Check
    - [ ] Verify >80% coverage for new logic.
    - [ ] Run full test suite (`./mvnw verify`).
*   - [ ] Task: Documentation Update
    - [ ] Update `GAME_MECHANICS.md` to document the new manager support features.
*   - [ ] Task: Conductor - User Manual Verification 'Phase 4: Final Integration' (Protocol in workflow.md)
