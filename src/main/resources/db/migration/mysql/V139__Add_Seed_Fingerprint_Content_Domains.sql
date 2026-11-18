-- Seed-update review (2.13.0): per-row seed provenance for the four content domains that had no
-- stamp column (show types, show templates, status cards, tournaments) — the same nullable
-- fingerprint the other eleven seed domains carry since V135. NULL = pre-fingerprint-era
-- (legacy) or user-created row.
-- Idempotent column adds (MySQL 8.0+ pattern from V50): prod installs may already
-- have these columns via Hibernate schema validation.
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'show_type'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE show_type ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'show_template'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE show_template ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'status_card'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE status_card ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'tournament'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE tournament ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;
