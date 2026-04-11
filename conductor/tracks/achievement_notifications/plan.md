# Plan: Achievement Notifications

## Phase 1: Infrastructure

- [x] Create `AchievementUnlockedEvent` in `com.github.javydreamercsw.management.event`.
- [x] Update `LegacyService` to inject `ApplicationEventPublisher`.
- [x] Publish `AchievementUnlockedEvent` in `LegacyService.unlockAchievement`.

## Phase 2: Inbox Integration

- [x] Create `AchievementInboxListener` in `com.github.javydreamercsw.management.event.inbox`.
- [x] Implement listener logic to call `inboxService.createInboxItem`.
- [x] Ensure `InboxItemTarget` correctly points to the `Account`.

## Phase 3: Verification

- [x] Add unit test to `AchievementSystemTest` to verify event publication.
- [x] Add integration test or verify manually that `InboxItem` is created.
- [x] Run full test suite.

