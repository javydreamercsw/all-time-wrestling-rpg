# Implementation Plan: Status Cards Mechanic

## Phase 1: Database Entities and Repositories

- [x] Task: Create `StatusCard` entity to define the cards (name, description, level 1/2 effects, trigger conditions). [72e7a94]
    - [x] Write unit tests for `StatusCard` entity.
    - [x] Implement `StatusCard` entity.- [x] Task: Create `WrestlerStatus` entity to map a `Wrestler` to a `StatusCard` and track the current level.
  - [x] Write unit tests for `WrestlerStatus` entity.
  - [x] Implement `WrestlerStatus` entity.
- [x] Task: Create `WrestlerStatusHistory` entity to log all status changes (gains, flips, losses). [bcfdb42]
    - [x] Write unit tests for `WrestlerStatusHistory` entity.
    - [x] Implement `WrestlerStatusHistory` entity.
- [x] Task: Create Spring Data JPA repositories for the new entities. [452fec5]
    - [x] Write integration tests for repositories.
    - [x] Implement repositories.
- [x] Task: Create Flyway migration script for the new tables. [da939f2]
- [~] Task: Conductor - User Manual Verification 'Phase 1: Database Entities and Repositories' (Protocol in workflow.md)

## Phase 2: Core Service Logic

- [ ] Task: Implement `WrestlerStatusService` to handle core logic (assign, flip, remove, evaluate conditions).
  - [ ] Write unit tests for assigning a new status (Level I).
  - [ ] Write unit tests for flipping an existing status (Level I -> Level II).
  - [ ] Write unit tests for ignoring/handling duplicate assignments (already Level II).
  - [ ] Write unit tests for removing a status.
  - [ ] Write unit tests for evaluating trigger conditions (e.g., match outcomes).
  - [ ] Implement `WrestlerStatusService` methods to pass all tests.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Core Service Logic' (Protocol in workflow.md)

## Phase 3: Settings & Expansion Integration

- [ ] Task: Add a global system setting to enable/disable Status Cards.
  - [ ] Write unit tests.
  - [ ] Implement setting toggle in `GameSettingsService` or equivalent.
- [ ] Task: Integrate with Expansion management (tie to "Women's Expansion").
  - [ ] Write unit tests verifying statuses are disabled if the expansion is disabled.
  - [ ] Implement logic to hook into expansion state changes.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Settings & Expansion Integration' (Protocol in workflow.md)

## Phase 4: Match, Campaign, and Procedural Integration

- [ ] Task: Integrate Status Cards into Match Setup (Pre-Match Phase).
  - [ ] Write unit tests for applying modifiers (momentum, hand size, etc.) during match initialization.
  - [ ] Update Match engine / service to apply active status modifiers.
- [ ] Task: Integrate Status Cards into Match Evaluation (Post-Match Phase).
  - [ ] Write unit tests for trigger condition evaluation at the end of a match.
  - [ ] Update Match resolution logic to call `WrestlerStatusService` to evaluate triggers.
- [ ] Task: Update Campaign Chapter Configuration schema to support defining Status Card rewards/penalties on branches.
  - [ ] Write unit tests.
  - [ ] Implement schema updates and parsing logic.
- [ ] Task: Implement Procedural Assignment hooks (framework for non-campaign assignments).
  - [ ] Write unit tests.
  - [ ] Implement event listeners or scheduled tasks for procedural assignment.
- [ ] Task: Conductor - User Manual Verification 'Phase 4: Match, Campaign, and Procedural Integration' (Protocol in workflow.md)

## Phase 5: UI Integration

- [ ] Task: Add active status icons to the Wrestler Profile view.
  - [ ] Write E2E/UI tests.
  - [ ] Update Vaadin views to display icons/tooltips.
- [ ] Task: Integrate Status visibility and interaction into the Match Setup UI.
  - [ ] Write E2E/UI tests.
  - [ ] Update Match Setup views.
- [ ] Task: Implement Admin controls for manual status assignment/overrides.
  - [ ] Write E2E/UI tests.
  - [ ] Add controls to the Admin/GM dashboards.
- [ ] Task: Conductor - User Manual Verification 'Phase 5: UI Integration' (Protocol in workflow.md)

