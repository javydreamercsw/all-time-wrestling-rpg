-- Default inbox retention: 90 days (global, applies to all universes unless overridden).
-- Set to -1 to disable automatic purge.
MERGE INTO game_setting (setting_key, setting_value, universe_id)
    KEY (setting_key, universe_id)
VALUES ('inbox.retention.days', '90', NULL);
