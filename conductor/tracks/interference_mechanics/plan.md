# Match Interference & Manager Mechanics - Plan

## Phase 1: Infrastructure & State
- [ ] Define `InterferenceType` enum (Distraction, Weapon, Trip, Strike).
- [ ] Create `InterferenceService` to handle the logic of interference attempts and results.
- [ ] Extend match state logic to include `refereeAwareness` (0-100).

## Phase 2: Core Logic
- [ ] Implement calculation logic for interference success vs. detection.
- [ ] Integrate Faction Affinity bonus into interference success rates.
- [ ] Add DQ and Ejection triggers based on awareness levels.

## Phase 3: Match View Integration
- [ ] Add "Ringside Actions" panel to `MatchView`.
- [ ] Implement buttons for active interference when conditions are met (manager present or faction member at ringside).
- [ ] Add visual feedback (notifications/banners) for interference results.

## Phase 4: NPC AI & Automated Matches
- [ ] Implement `InterferenceAiService` to decide when NPCs should interfere.
- [ ] Update `NPCSegmentResolutionService` to factor in potential interference during simulations.

## Phase 5: Testing & Documentation
- [ ] Unit tests for `InterferenceService` detection logic.
- [ ] Integration tests for DQ/Ejection scenarios.
- [ ] Document interference mechanics in the Game Guide.
