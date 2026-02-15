# Plan: Data-Driven Achievement System

## Phase 1: Data Model Refactor

- [ ] Update `Achievement` entity to include `key` and `category`.
- [ ] Create `achievements.json` with initial definitions (ported from enum + new ones).
- [ ] Implement `AchievementRepository` methods to find by key.
- [ ] Update `DataInitializer` to load/sync achievements from JSON.

## Phase 2: Logic Integration

- [ ] Refactor `LegacyService` to use string-based keys for unlocking achievements.
- [ ] Integrate participation/win triggers in `SegmentAdjudicationService`.
- [ ] Integrate main event/PLE triggers in `ShowService`.
- [ ] Implement Rumble-specific victory achievement logic.

## Phase 3: UI & Verification

- [ ] Update `PlayerView` and `HallOfFameView` to support the new achievement model.
- [ ] Add category-based filtering or grouping in the Achievements tab (Optional).
- [ ] Verify with Unit and E2E tests.

