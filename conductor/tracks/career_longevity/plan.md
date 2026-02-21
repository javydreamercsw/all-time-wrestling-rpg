# Persistent "Wear & Tear" & Career Longevity - Plan

## Phase 1: Analysis

- [ ] Review `Wrestler` entity and `InjurySystem`.
- [ ] Check `MatchResult` processing for damage calculation.

## Phase 2: Design

- [ ] Define `PhysicalCondition` metrics (hidden vs. visible).
- [ ] Create degradation formulas based on match type.

## Phase 3: Implementation

- [ ] Add `physicalCondition` field to `Wrestler`.
- [ ] Update `MatchAdjudicationService` to apply wear and tear.
- [ ] Implement `RetirementService` for narrative triggers.

## Phase 4: Testing

- [ ] Verify cumulative damage over multiple matches.
- [ ] Test retirement event triggers.

