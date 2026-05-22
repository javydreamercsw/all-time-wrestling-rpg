-- Make V42 idempotent: add column only if it doesn't exist (compatible with MySQL 8.0+)
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'segment_participant'
      AND column_name = 'team_number'
);
SET @add_sql = IF(@col_exists = 0,
    'ALTER TABLE segment_participant ADD COLUMN team_number INT NOT NULL DEFAULT 1',
    'SELECT 1'
);
PREPARE add_stmt FROM @add_sql;
EXECUTE add_stmt;
DEALLOCATE PREPARE add_stmt;
