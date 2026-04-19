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

-- Drop old columns from wrestler table (MySQL 8.0.26 compatible approach)
SET @drop_fans = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'wrestler' AND column_name = 'fans') > 0, 'ALTER TABLE wrestler DROP COLUMN fans', 'SELECT 1');
PREPARE stmt_fans FROM @drop_fans;
EXECUTE stmt_fans;
DEALLOCATE PREPARE stmt_fans;

SET @drop_tier = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'wrestler' AND column_name = 'tier') > 0, 'ALTER TABLE wrestler DROP COLUMN tier', 'SELECT 1');
PREPARE stmt_tier FROM @drop_tier;
EXECUTE stmt_tier;
DEALLOCATE PREPARE stmt_tier;

SET @drop_bumps = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'wrestler' AND column_name = 'bumps') > 0, 'ALTER TABLE wrestler DROP COLUMN bumps', 'SELECT 1');
PREPARE stmt_bumps FROM @drop_bumps;
EXECUTE stmt_bumps;
DEALLOCATE PREPARE stmt_bumps;

SET @drop_ch = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'wrestler' AND column_name = 'current_health') > 0, 'ALTER TABLE wrestler DROP COLUMN current_health', 'SELECT 1');
PREPARE stmt_ch FROM @drop_ch;
EXECUTE stmt_ch;
DEALLOCATE PREPARE stmt_ch;

SET @drop_pc = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'wrestler' AND column_name = 'physical_condition') > 0, 'ALTER TABLE wrestler DROP COLUMN physical_condition', 'SELECT 1');
PREPARE stmt_pc FROM @drop_pc;
EXECUTE stmt_pc;
DEALLOCATE PREPARE stmt_pc;

SET @drop_morale = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'wrestler' AND column_name = 'morale') > 0, 'ALTER TABLE wrestler DROP COLUMN morale', 'SELECT 1');
PREPARE stmt_morale FROM @drop_morale;
EXECUTE stmt_morale;
DEALLOCATE PREPARE stmt_morale;

SET @drop_ms = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'wrestler' AND column_name = 'management_stamina') > 0, 'ALTER TABLE wrestler DROP COLUMN management_stamina', 'SELECT 1');
PREPARE stmt_ms FROM @drop_ms;
EXECUTE stmt_ms;
DEALLOCATE PREPARE stmt_ms;

SET @drop_fi = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'wrestler' AND column_name = 'faction_id') > 0, 'ALTER TABLE wrestler DROP COLUMN faction_id', 'SELECT 1');
PREPARE stmt_fi FROM @drop_fi;
EXECUTE stmt_fi;
DEALLOCATE PREPARE stmt_fi;

SET @drop_mi = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'wrestler' AND column_name = 'manager_id') > 0, 'ALTER TABLE wrestler DROP COLUMN manager_id', 'SELECT 1');
PREPARE stmt_mi FROM @drop_mi;
EXECUTE stmt_mi;
DEALLOCATE PREPARE stmt_mi;
