-- Seed default game_settings for rivalry lifecycle management.
-- INSERT IGNORE is a no-op if the key already exists.
INSERT IGNORE INTO game_setting (setting_key, setting_value) VALUES
    ('rivalry_resolution_threshold_ple',      '30'),
    ('rivalry_resolution_threshold_regular',  '35'),
    ('rivalry_resolution_on_regular_shows',   'false'),
    ('rivalry_max_duration_days',             '0'),
    ('rivalry_heat_decay_enabled',            'false'),
    ('rivalry_heat_decay_per_interval',       '1'),
    ('rivalry_heat_decay_interval_days',      '7');
