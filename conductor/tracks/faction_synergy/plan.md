# Dynamic Faction Chemistry & Synergies - Plan

## Phase 1: Analysis [checkpoint: 20260217]

- [x] Review `factions.json` structure. 20260217
- [x] Analyze `CampaignService` and `MatchEngine` for hook points. 20260217

## Phase 2: Design

- [x] Define `FactionAffinity` data model. 20260217
- [x] Design `FactionBuff` interface. 20260217

## Phase 3: Implementation

- [~] Update `Faction` entity to track affinity.
- [ ] Implement affinity gain logic in `SegmentService`.
- [ ] Implement buff application in `MatchEngine`.
- [ ] Add UI indicators for active faction buffs.

## Phase 4: Testing

- [ ] Unit tests for affinity calculation.
- [ ] Integration tests for buff application in matches.

