-- Add unique constraint to campaign_ability_card for name, alignmentType, and level.
-- IF EXISTS for DROP INDEX requires MySQL 8.0.29+; use a prepared statement for compatibility.
SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'campaign_ability_card'
      AND index_name = 'uk_name_alignment_level'
);
SET @drop_sql = IF(@idx_exists > 0,
    'ALTER TABLE campaign_ability_card DROP INDEX uk_name_alignment_level',
    'SELECT 1'
);
PREPARE drop_stmt FROM @drop_sql;
EXECUTE drop_stmt;
DEALLOCATE PREPARE drop_stmt;

ALTER TABLE campaign_ability_card
    ADD CONSTRAINT uk_name_alignment_level UNIQUE (name, alignment_type, level);
