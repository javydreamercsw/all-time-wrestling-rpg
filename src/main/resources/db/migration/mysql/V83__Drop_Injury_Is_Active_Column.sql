-- is_active is now derived from healed_date (see Injury.java @Formula); drop the
-- independently-writable column so display and healing-eligibility can never disagree.
SET @drop_is_active = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'injury' AND column_name = 'is_active') > 0, 'ALTER TABLE injury DROP COLUMN is_active', 'SELECT 1');
PREPARE stmt_drop_is_active FROM @drop_is_active;
EXECUTE stmt_drop_is_active;
DEALLOCATE PREPARE stmt_drop_is_active;
