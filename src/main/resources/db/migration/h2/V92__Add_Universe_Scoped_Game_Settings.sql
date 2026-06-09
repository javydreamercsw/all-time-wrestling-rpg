-- Add nullable universe_id FK to game_setting so settings can be scoped
-- per-universe. NULL universe_id = global default (existing behaviour).
-- A composite UNIQUE(setting_key, universe_id) constraint allows the same key
-- to exist once globally (NULL) and once per universe.
-- H2: rename the old PK column, add surrogate PK + universe_id, then rebuild
-- the unique constraint.

-- Step 1: Rename existing PK column to setting_key (it was called setting_key already, but drop the PK constraint first via recreating the table structure).
-- H2 does not support DROP PRIMARY KEY directly; use ALTER TABLE to add a new
-- surrogate column, then drop the old constraint via ADD CONSTRAINT approach.

-- Add the surrogate id column as the new PK.
ALTER TABLE game_setting ADD COLUMN id BIGINT AUTO_INCREMENT;

-- Drop the old PRIMARY KEY constraint (H2 uses the constraint name).
ALTER TABLE game_setting DROP CONSTRAINT IF EXISTS pk_gamesetting;

-- Make the new surrogate column the PK.
ALTER TABLE game_setting ADD PRIMARY KEY (id);

-- Add the universe FK column (nullable — existing rows keep NULL = global).
ALTER TABLE game_setting
    ADD COLUMN universe_id BIGINT NULL
    REFERENCES universe(id) ON DELETE CASCADE;

-- Enforce uniqueness of (key, universe) — NULL universe counted once per key.
ALTER TABLE game_setting
    ADD CONSTRAINT uq_game_setting_key_universe
    UNIQUE (setting_key, universe_id);
