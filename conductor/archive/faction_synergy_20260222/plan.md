# Dynamic Faction Chemistry & Synergies - Plan

## Phase 1: Analysis [checkpoint: 20260217]

- [x] Review `factions.json` structure. 20260217
- [x] Analyze `CampaignService` and `MatchEngine` for hook points. 20260217

## Phase 2: Design

- [x] Define `FactionAffinity` data model. 20260217
- [x] Design `FactionBuff` interface. 20260217

## Phase 3: Implementation

- [x] Update `Faction` entity to track affinity.
- [x] Implement affinity bonus in `NPCSegmentResolutionService` (win probability boost).
- [x] Cap faction affinity at 100 in `FactionService`.
- [x] Implement affinity gain logic in `SegmentAdjudicationService`.
- [x] Add UI indicators for active faction buffs in `FactionListView`.

## Phase 4: Testing

- [x] Unit tests for synergy bonus calculation in `NPCSegmentResolutionService`.
- [x] Unit tests for affinity gain in `SegmentAdjudicationService`.
- [x] Integration tests for synergy bonus impact in `NPCSegmentResolutionServiceTest`.

