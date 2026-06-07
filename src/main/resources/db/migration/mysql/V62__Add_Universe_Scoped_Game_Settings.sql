-- Add nullable universe_id FK to game_setting so settings can be scoped
-- per-universe. NULL universe_id = global default (existing behaviour).

-- Drop the existing PK.
ALTER TABLE game_setting DROP PRIMARY KEY;

-- Add surrogate PK column.
ALTER TABLE game_setting
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- Add the universe FK column.
ALTER TABLE game_setting
    ADD COLUMN universe_id BIGINT NULL
    REFERENCES universe(id) ON DELETE CASCADE;

-- Enforce uniqueness of (key, universe).
ALTER TABLE game_setting
    ADD CONSTRAINT uq_game_setting_key_universe
    UNIQUE (setting_key, universe_id);