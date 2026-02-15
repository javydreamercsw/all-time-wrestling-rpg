# Plan: Player Career Legacy & Hall of Fame

## Phase 1: Legacy Engine [checkpoint: 76af5732]

- [x] Add `legacyScore` and `prestige` fields to `Account` entity.
- [x] Create `Achievement` entity and `AccountAchievement` link table.
- [x] Implement `LegacyService` to calculate score updates based on game events.

## Phase 2: Milestones & Badges [checkpoint: Phase 2]

- [x] Define `AchievementType` enum (e.g., FIRST_TITLE, WIN_STREAK, BOOKER_OF_THE_YEAR).
- [x] Implement logic to trigger achievements upon relevant events.
- [x] Update `Account` to track `milestones` list (mapped to Achievements).

## Phase 3: Hall of Fame UI [checkpoint: Phase 3]

- [x] Create `HallOfFameView` to list top Accounts by Legacy Score.
- [x] Update `PlayerDashboard` to show current Legacy Score and Badges.
- [ ] Implement "Induction Ceremony" notification (Bonus).

## Phase 4: Verification

- [ ] Unit tests for Legacy Score formula and achievement triggers.
- [ ] E2E test for Hall of Fame display and Player Profile updates.

