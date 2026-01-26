-- Add theme_preference column to account table
ALTER TABLE account ADD COLUMN theme_preference VARCHAR(50);

INSERT INTO game_setting (setting_key, setting_value) VALUES ('default_theme', 'light');
