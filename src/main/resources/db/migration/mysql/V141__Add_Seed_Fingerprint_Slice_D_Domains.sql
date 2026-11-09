-- Seed-update review (2.13.0): per-row seed provenance for the final four content domains that
-- had no stamp column (commentary teams, campaign upgrades, outcome matrices + their entries,
-- wrestler relationships) — the same nullable fingerprint the other twenty seed domains carry
-- since V135/V139/V140. NULL = pre-fingerprint-era (legacy) or user-created row.
-- Idempotent column adds (MySQL 8.0+ pattern from V50): prod installs may already
-- have these columns via Hibernate schema validation.
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'commentary_team'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE commentary_team ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'campaign_upgrade'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE campaign_upgrade ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'outcome_matrix'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE outcome_matrix ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'outcome_matrix_entry'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE outcome_matrix_entry ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'wrestler_relationship'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE wrestler_relationship ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;
