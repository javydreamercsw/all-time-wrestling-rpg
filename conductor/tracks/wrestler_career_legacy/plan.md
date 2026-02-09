# Plan: Wrestler Career Legacy & Hall of Fame

## Phase 1: Legacy Engine

- [ ] Add `legacyScore` and `careerMilestones` fields to `Wrestler` entity.
- [ ] Implement `LegacyService` to calculate score updates after each show.
- [ ] Create `Achievement` enum and persistence logic.

## Phase 2: Legend Status

- [ ] Add `isLegend` and `retirementDate` to `Wrestler`.
- [ ] Implement retirement workflow (manual or age-based).
- [ ] Add "Legendary Perk" logic (bonus points for faction members).

## Phase 3: Hall of Fame UI

- [ ] Create `HallOfFameView`.
- [ ] Build a "Career Highlight" component for the Wrestler Profile.
- [ ] Implement filtering for "All-Time Greats."

## Phase 4: Verification

- [ ] Unit tests for Legacy Score formula.
- [ ] E2E test for the retirement transition.

