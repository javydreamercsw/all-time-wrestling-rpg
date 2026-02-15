# Plan: Player Career Legacy & Hall of Fame

## Phase 1: Legacy Engine

- [ ] Add `legacyScore` and `prestige` fields to `Account` entity.
- [ ] Create `Achievement` entity and `AccountAchievement` link table.
- [ ] Implement `LegacyService` to calculate score updates based on game events.

## Phase 2: Milestones & Badges

- [ ] Define `AchievementType` enum (e.g., FIRST_TITLE, WIN_STREAK, BOOKER_OF_THE_YEAR).
- [ ] Implement logic to trigger achievements upon relevant events.
- [ ] Update `Account` to track `milestones` list.

## Phase 3: Hall of Fame UI

- [ ] Create `HallOfFameView` to list top Accounts by Legacy Score.
- [ ] Update `PlayerDashboard` to show current Legacy Score and Badges.
- [ ] Implement "Induction Ceremony" notification.

## Phase 4: Verification

- [ ] Unit tests for Legacy Score formula and achievement triggers.
- [ ] E2E test for Hall of Fame display and Player Profile updates.

