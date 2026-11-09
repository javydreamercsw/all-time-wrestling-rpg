-- Seed-update review (2.13.0): per-row seed provenance for the five content domains that had no
-- stamp column (factions, arenas, locations, teams, commentators) — the same nullable fingerprint
-- the other fifteen seed domains carry since V135/V139. NULL = pre-fingerprint-era (legacy) or
-- user-created row.
-- Idempotent column adds (MySQL 8.0+ pattern from V50): prod installs may already
-- have these columns via Hibernate schema validation.
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'faction'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE faction ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'arena'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE arena ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'location'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE location ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'team'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE team ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'commentator'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE commentator ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;
