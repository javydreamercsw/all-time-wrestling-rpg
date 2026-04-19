-- Cleanup: Drop old columns from wrestler table that have been moved to wrestler_state
-- These columns are no longer in the Wrestler entity

-- Drop old columns from wrestler table (MySQL)
-- First drop indexes safely if they exist
SET @drop_index_stmt1 = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'wrestler' AND index_name = 'idx_wrestler_tier') > 0, 'DROP INDEX idx_wrestler_tier ON wrestler', 'SELECT 1');
PREPARE stmt1 FROM @drop_index_stmt1;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

SET @drop_index_stmt2 = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'wrestler' AND index_name = 'idx_wrestler_faction') > 0, 'DROP INDEX idx_wrestler_faction ON wrestler', 'SELECT 1');
PREPARE stmt2 FROM @drop_index_stmt2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

SET @drop_index_stmt3 = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'wrestler' AND index_name = 'idx_wrestler_availability') > 0, 'DROP INDEX idx_wrestler_availability ON wrestler', 'SELECT 1');
PREPARE stmt3 FROM @drop_index_stmt3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

-- Then drop foreign keys if they exist
SET @drop_fk_stmt1 = IF((SELECT COUNT(*) FROM information_schema.key_column_usage WHERE table_schema = DATABASE() AND table_name = 'wrestler' AND constraint_name = 'wrestler_ibfk_1') > 0, 'ALTER TABLE wrestler DROP FOREIGN KEY wrestler_ibfk_1', 'SELECT 1');
PREPARE fk_stmt1 FROM @drop_fk_stmt1;
EXECUTE fk_stmt1;
DEALLOCATE PREPARE fk_stmt1;

SET @drop_fk_stmt2 = IF((SELECT COUNT(*) FROM information_schema.key_column_usage WHERE table_schema = DATABASE() AND table_name = 'wrestler' AND constraint_name = 'wrestler_ibfk_2') > 0, 'ALTER TABLE wrestler DROP FOREIGN KEY wrestler_ibfk_2', 'SELECT 1');
PREPARE fk_stmt2 FROM @drop_fk_stmt2;
EXECUTE fk_stmt2;
DEALLOCATE PREPARE fk_stmt2;

SET @drop_fk_stmt3 = IF((SELECT COUNT(*) FROM information_schema.key_column_usage WHERE table_schema = DATABASE() AND table_name = 'wrestler' AND constraint_name = 'wrestler_ibfk_3') > 0, 'ALTER TABLE wrestler DROP FOREIGN KEY wrestler_ibfk_3', 'SELECT 1');
PREPARE fk_stmt3 FROM @drop_fk_stmt3;
EXECUTE fk_stmt3;
DEALLOCATE PREPARE fk_stmt3;

ALTER TABLE wrestler DROP COLUMN IF EXISTS fans;
ALTER TABLE wrestler DROP COLUMN IF EXISTS tier;
ALTER TABLE wrestler DROP COLUMN IF EXISTS bumps;
ALTER TABLE wrestler DROP COLUMN IF EXISTS current_health;
ALTER TABLE wrestler DROP COLUMN IF EXISTS physical_condition;
ALTER TABLE wrestler DROP COLUMN IF EXISTS morale;
ALTER TABLE wrestler DROP COLUMN IF EXISTS management_stamina;
ALTER TABLE wrestler DROP COLUMN IF EXISTS faction_id;
ALTER TABLE wrestler DROP COLUMN IF EXISTS manager_id;
