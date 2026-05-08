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

## Phase 4: Settings & Expansion Integration [checkpoint: c131681]

- [x] Task: Add a global system setting to enable/disable Status Cards.
  - [x] Write unit tests.
  - [x] Implement setting toggle in `GameSettingsService` or equivalent.
- [x] Task: Integrate with Expansion management (tie to "Women's Expansion").
  - [x] Write unit tests verifying statuses are disabled if the expansion is disabled.
  - [x] Implement logic to hook into expansion state changes.
- [x] Task: Conductor - User Manual Verification 'Phase 4: Settings & Expansion Integration' (Protocol in workflow.md)

## Phase 5: Match, Campaign, and Procedural Integration [checkpoint: 61c556a]

- [x] Task: Integrate Status Cards into Match Setup (Pre-Match Phase).
  - [x] Write unit tests for applying modifiers (momentum, hand size, etc.) during match initialization.
  - [x] Update Match engine / service to apply active status modifiers.
- [x] Task: Integrate Status Cards into Match Evaluation (Post-Match Phase).
  - [x] Write unit tests for trigger condition evaluation at the end of a match.
  - [x] Update Match resolution logic to call `WrestlerStatusService` to evaluate triggers.
- [x] Task: Update Campaign Chapter Configuration schema to support defining Status Card rewards/penalties on branches.
  - [x] Write unit tests.
  - [x] Implement schema updates and parsing logic.
- [x] Task: Document Status Card mechanics and scripting in `docs/`.
  - [x] Update `docs/GAME_MECHANICS.md` with double-sided card details and keys.
  - [x] Update `docs/CAMPAIGN_SCRIPTING.md` with instructions on referencing status keys in campaign branches.
- [x] Task: Implement Procedural Assignment hooks (framework for non-campaign assignments).
  - [x] Write unit tests.
  - [x] Implement event listeners or scheduled tasks for procedural assignment.
- [x] Task: Conductor - User Manual Verification 'Phase 5: Match, Campaign, and Procedural Integration' (Protocol in workflow.md)

## Phase 6: UI Integration [checkpoint: 8a82c3c]

- [x] Task: Add active status icons to the Wrestler Profile view.
  - [x] Write E2E/UI tests.
  - [x] Update Vaadin views to display icons/tooltips.
- [x] Task: Integrate Status visibility and interaction into the Match Setup UI.
  - [x] Write E2E/UI tests.
  - [x] Update Match Setup views.
- [x] Task: Implement Admin controls for manual status assignment/overrides. [8a82c3c]
  - [x] Write E2E/UI tests.
  - [x] Add controls to the Admin/GM dashboards.
- [x] Task: Conductor - User Manual Verification 'Phase 6: UI Integration' (Protocol in workflow.md)

## Summary of Implementation

- [x] Database entities and repositories for Status Cards.
- [x] StatusCardService for definition management.
- [x] Data initialization from JSON.
- [x] WrestlerStatusService for core logic (gain/flip/remove/evaluate).
- [x] Global toggle and Expansion integration.
- [x] Effective stat calculation (Momentum, Hand Size).
- [x] UI integration in Profile and Match views.
- [x] Campaign and Global Adjudication integration.
- [x] Documentation in GAME_MECHANICS.md and CAMPAIGN_SCRIPTING.md.

