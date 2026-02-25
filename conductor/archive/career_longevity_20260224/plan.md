# Persistent "Wear & Tear" & Career Longevity - Plan

## Phase 1: Analysis

- [x] Review `Wrestler` entity and `InjurySystem`. (Bumps and injuries already exist, affecting effective starting health).
- [x] Check `MatchResult` processing for damage calculation. (Handled in `SegmentAdjudicationService` and `CampaignService`).

## Phase 2: Design

- [x] Define `PhysicalCondition` metrics (hidden vs. visible).
  - `physicalCondition`: 0-100 integer. 100 is perfect health.
  - Impacts `startingHealth` directly (e.g., -1 health for every 5% lost).
- [x] Create degradation formulas based on match type and duration.
  - Base loss: 1-3% per match.
  - Rules: "Extreme", "No DQ", "Cage" matches double the loss.
  - Main Event: +1% loss.
- [x] Design reset mechanism UI/API.

## Phase 3: Implementation

- [x] Add `physicalCondition` field to `Wrestler`.
- [x] Update `MatchAdjudicationService` to apply wear and tear. (Implemented `applyWearAndTear` in `SegmentAdjudicationService`).
- [x] Implement `RetirementService` for narrative triggers.
- [x] **Implement Admin/Debug mechanism to reset all wear and tear data.** (Added button to `AdminView`).

## Phase 4: Testing

- [x] Verify cumulative damage over multiple matches. (Tested in `WearAndTearAdjudicationTest`).
- [x] Test retirement event triggers. (Tested in `RetirementServiceTest`).

