# Interactive Backstage "Dialogue Tree" Events - Plan

## Phase 1: Analysis

- [x] Review `CampaignService` and `CampaignPhase` flow.
- [x] Analyze existing `Event` system.

## Phase 2: Design

- [x] Define `BackstageEncounter` structure (trigger, options, outcomes).
- [x] Create AI prompts for generating dialogue.

## Phase 3: Implementation

- [x] Implement `EncounterGenerationService`. (Implemented as `BackstageEncounterService`)
- [x] Create `BackstageView` for interactive dialogue. (Implemented as `BackstageEncounterView`)
- [x] Integrate outcomes with `CampaignState`.

## Phase 4: Testing

- [x] Test random trigger logic.
- [x] Verify attribute updates from choices.

