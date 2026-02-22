# Match Interference & Manager Mechanics - Plan

## Phase 1: Infrastructure & State

- [x] Define `InterferenceType` enum (Distraction, Weapon, Trip, Strike).
- [x] Create `InterferenceService` to handle the logic of interference attempts and results.
- [x] Extend match state logic to include `refereeAwareness` (0-100).
- [x] Add dynamic `attributes` JSON storage to `Npc` entity for stats like awareness.

## Phase 2: Core Logic

- [x] Implement calculation logic for interference success vs. detection.
- [x] Integrate Faction Affinity bonus into interference success rates.
- [x] Add DQ and Ejection triggers based on awareness levels.

## Phase 3: Match View Integration
- [x] Add "Ringside Actions" panel to `MatchView`.
- [x] Implement buttons for active interference when conditions are met.
- [x] Add visual feedback (notifications/banners) for interference results.
- [x] Allow bookers to assign referees to segments in `EditSegmentDialog`.

## Phase 4: NPC AI & Automated Matches
- [x] Implement `InterferenceAiService` to decide when NPCs should interfere.
- [x] Update `NPCSegmentResolutionService` to factor in potential interference during simulations.

## Phase 5: Testing & Documentation
- [x] Unit tests for `InterferenceService` detection logic.
- [x] Integration tests for DQ/Ejection scenarios.
- [x] Document interference mechanics in the Game Guide.
- [x] Automated E2E capture for documentation.

