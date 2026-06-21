-- Backfill rivalry lifecycle defaults that changed in 2.4.1.
-- Only inserts the global row when no explicit configuration exists, so
-- admins who deliberately set a different value are not overwritten.

INSERT INTO game_setting (setting_key, setting_value, universe_id)
SELECT 'rivalry_heat_decay_enabled', 'true', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM game_setting
    WHERE setting_key = 'rivalry_heat_decay_enabled' AND universe_id IS NULL
);

INSERT INTO game_setting (setting_key, setting_value, universe_id)
SELECT 'rivalry_max_duration_days', '90', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM game_setting
    WHERE setting_key = 'rivalry_max_duration_days' AND universe_id IS NULL
);

INSERT INTO game_setting (setting_key, setting_value, universe_id)
SELECT 'rivalry_resolution_on_regular_shows', 'true', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM game_setting
    WHERE setting_key = 'rivalry_resolution_on_regular_shows' AND universe_id IS NULL
);

INSERT INTO game_setting (setting_key, setting_value, universe_id)
SELECT 'rivalry_resolution_threshold_regular', '25', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM game_setting
    WHERE setting_key = 'rivalry_resolution_threshold_regular' AND universe_id IS NULL
);

INSERT INTO game_setting (setting_key, setting_value, universe_id)
SELECT 'rivalry_resolution_min_heat', '10', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM game_setting
    WHERE setting_key = 'rivalry_resolution_min_heat' AND universe_id IS NULL
);
