-- Cleanup: Drop old columns from wrestler table that have been moved to wrestler_state
-- These columns are no longer in the Wrestler entity

-- Drop old columns from wrestler table (H2)
ALTER TABLE wrestler DROP COLUMN IF EXISTS fans;
ALTER TABLE wrestler DROP COLUMN IF EXISTS tier;
ALTER TABLE wrestler DROP COLUMN IF EXISTS bumps;
ALTER TABLE wrestler DROP COLUMN IF EXISTS current_health;
ALTER TABLE wrestler DROP COLUMN IF EXISTS physical_condition;
ALTER TABLE wrestler DROP COLUMN IF EXISTS morale;
ALTER TABLE wrestler DROP COLUMN IF EXISTS management_stamina;
ALTER TABLE wrestler DROP COLUMN IF EXISTS faction_id;
ALTER TABLE wrestler DROP COLUMN IF EXISTS manager_id;
