-- Default ended-rivalry retention: keep forever (0 disables the nightly purge, ATW-8nc8).
INSERT IGNORE INTO game_setting (setting_key, setting_value, universe_id)
VALUES ('rivalry.retention.days', '0', NULL);
