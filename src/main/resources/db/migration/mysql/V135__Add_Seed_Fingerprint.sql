-- Seed verify mode (fingerprint-gated reconciliation of seed-owned fields):
-- nullable fingerprint stamped at insert/reconcile; NULL = pre-fingerprint-era (legacy) row.
-- Idempotent column adds (MySQL 8.0+ pattern from V50): prod installs may already
-- have these columns via Hibernate schema validation.
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'wrestler'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE wrestler ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'card_set'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE card_set ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'card'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE card ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'deck'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE deck ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'npc'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE npc ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'segment_type'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE segment_type ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'title'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE title ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'ringside_action'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE ringside_action ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'achievement'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE achievement ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'campaign_ability_card'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE campaign_ability_card ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'segment_rule'
      AND column_name = 'seed_fingerprint'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE segment_rule ADD COLUMN seed_fingerprint VARCHAR(64)',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;
