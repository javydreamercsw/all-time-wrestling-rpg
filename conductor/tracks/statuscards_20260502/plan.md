# Implementation Plan: Status Cards Mechanic

## Phase 1: Database Entities and Repositories [checkpoint: 7563867]

- [x] Task: Create `StatusCard` entity to define the cards (name, description, level 1/2 effects, trigger conditions). [72e7a94]
  - [x] Write unit tests for `StatusCard` entity.
  - [x] Implement `StatusCard` entity.
- [x] Task: Create `WrestlerStatus` entity to map a `Wrestler` to a `StatusCard` and track the current level. [de56ae7]
  - [x] Write unit tests for `WrestlerStatus` entity.
  - [x] Implement `WrestlerStatus` entity.
- [x] Task: Create `WrestlerStatusHistory` entity to log all status changes (gains, flips, losses). [bcfdb42]
  - [x] Write unit tests for `WrestlerStatusHistory` entity.
  - [x] Implement `WrestlerStatusHistory` entity.
- [x] Task: Create Spring Data JPA repositories for the new entities. [452fec5]
  - [x] Write integration tests for repositories.
  - [x] Implement repositories.
- [x] Task: Create Flyway migration script for the new tables. [da939f2]
- [x] Task: Conductor - User Manual Verification 'Phase 1: Database Entities and Repositories' (Protocol in workflow.md)

## Phase 2: Status Card Management and Data Initialization [checkpoint: 7736217]

- [x] Task: Refactor `StatusCard` to use `key`, `level1Name`, and `level2Name` instead of `name`. [7d5f39d]
  - [x] Update `StatusCard` entity, `StatusCardDTO`, and tests.
  - [x] Update Flyway migration scripts (H2 and MySQL).
- [x] Task: Create `StatusCardService` to manage `StatusCard` entities. [2b1a05b]
  - [x] Write unit tests for `StatusCardService`.
  - [x] Implement `StatusCardService`.
- [x] Task: Create `src/main/resources/status_cards.json` with initial statuses. [525ab53]
- [x] Task: Update `DataInitializer` to sync status cards from JSON on startup. [449e5cd]
  - [x] Write integration tests for `DataInitializer` sync.
  - [x] Implement `syncStatusCardsFromFile` in `DataInitializer`.
- [x] Task: Conductor - User Manual Verification 'Phase 2: Status Card Management and Data Initialization' (Protocol in workflow.md)

## Phase 3: Core Service Logic [checkpoint: 24ca649]

- [x] Task: Implement `WrestlerStatusService` to handle core logic (assign, flip, remove, evaluate conditions).
  - [x] Write unit tests for assigning a new status (Level I).
  - [x] Write unit tests for flipping an existing status (Level I -> Level II).
  - [x] Write unit tests for ignoring/handling duplicate assignments (already Level II).
  - [x] Write unit tests for removing a status.
  - [x] Write unit tests for evaluating trigger conditions (e.g., match outcomes).
  - [x] Implement `WrestlerStatusService` methods to pass all tests.
- [x] Task: Conductor - User Manual Verification 'Phase 3: Core Service Logic' (Protocol in workflow.md)

## Phase 4: Settings & Expansion Integration
- [x] Task: Add a global system setting to enable/disable Status Cards.
    - [x] Write unit tests.
    - [x] Implement setting toggle in `GameSettingsService` or equivalent.
- [x] Task: Integrate with Expansion management (tie to "Women's Expansion").
    - [x] Write unit tests verifying statuses are disabled if the expansion is disabled.
    - [x] Implement logic to hook into expansion state changes.
- [~] Task: Conductor - User Manual Verification 'Phase 4: Settings & Expansion Integration' (Protocol in workflow.md)

## Phase 5: Match, Campaign, and Procedural Integration

- [ ] Task: Integrate Status Cards into Match Setup (Pre-Match Phase).
  - [ ] Write unit tests for applying modifiers (momentum, hand size, etc.) during match initialization.
  - [ ] Update Match engine / service to apply active status modifiers.
- [ ] Task: Integrate Status Cards into Match Evaluation (Post-Match Phase).
  - [ ] Write unit tests for trigger condition evaluation at the end of a match.
  - [ ] Update Match resolution logic to call `WrestlerStatusService` to evaluate triggers.
- [ ] Task: Update Campaign Chapter Configuration schema to support defining Status Card rewards/penalties on branches.
  - [ ] Write unit tests.
  - [ ] Implement schema updates and parsing logic.
- [ ] Task: Document Status Card mechanics and scripting in `docs/`.
  - [ ] Update `docs/GAME_MECHANICS.md` with double-sided card details and keys.
  - [ ] Update `docs/CAMPAIGN_SCRIPTING.md` with instructions on referencing status keys in campaign branches.
- [ ] Task: Implement Procedural Assignment hooks (framework for non-campaign assignments).
  - [ ] Write unit tests.
  - [ ] Implement event listeners or scheduled tasks for procedural assignment.
- [ ] Task: Conductor - User Manual Verification 'Phase 5: Match, Campaign, and Procedural Integration' (Protocol in workflow.md)

## Phase 6: UI Integration

- [ ] Task: Add active status icons to the Wrestler Profile view.
  - [ ] Write E2E/UI tests.
  - [ ] Update Vaadin views to display status cards. Should somehow indicate level (I vs II), which one is active and allow flip to the other side.
- [ ] Task: Integrate Status visibility and interaction into the Match Setup UI.
  - [ ] Write E2E/UI tests.
  - [ ] Update Match Setup views.
- [ ] Task: Implement Admin controls for manual status assignment/overrides.
  - [ ] Write E2E/UI tests.
  - [ ] Add controls to the Admin/GM dashboards.
- [ ] Task: Conductor - User Manual Verification 'Phase 6: UI Integration' (Protocol in workflow.md)

