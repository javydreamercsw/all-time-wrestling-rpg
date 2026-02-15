# Specification: Achievement Notifications

## Goal

Notify players via an Inbox Item whenever they unlock a new career achievement.

## Requirements

1. **Event Publication:** `LegacyService` should publish a `NewAchievementEvent` when an achievement is successfully unlocked.
2. **Inbox Integration:** Create an `AchievementInboxListener` that listens for `NewAchievementEvent` and creates a corresponding `InboxItem`.
3. **Information Richness:** The inbox item should include the achievement name and the XP (Prestige) awarded.
4. **Targeting:** The inbox item should be targeted at the `Account` that earned the achievement.

## Success Criteria

- Unlocking an achievement creates a new record in the `inbox_item` table.
- The player sees a notification in their dashboard inbox.
- Existing tests still pass, and new tests verify notification creation.

