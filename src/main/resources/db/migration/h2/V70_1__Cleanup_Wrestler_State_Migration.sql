-- Cleanup after Wrestler State Migration (H2 version)
-- Drop old indexes that reference moved columns

-- Drop old wrestler table indexes that reference moved columns
DROP INDEX IF EXISTS idx_wrestler_tier;
DROP INDEX IF EXISTS idx_wrestler_faction;
DROP INDEX IF EXISTS idx_wrestler_availability;

-- Drop old columns from wrestler table (these have been moved to wrestler_state)
ALTER TABLE wrestler DROP COLUMN IF EXISTS fans;
ALTER TABLE wrestler DROP COLUMN IF EXISTS tier;
ALTER TABLE wrestler DROP COLUMN IF EXISTS bumps;
ALTER TABLE wrestler DROP COLUMN IF EXISTS current_health;
ALTER TABLE wrestler DROP COLUMN IF EXISTS physical_condition;
ALTER TABLE wrestler DROP COLUMN IF EXISTS morale;
ALTER TABLE wrestler DROP COLUMN IF EXISTS management_stamina;
ALTER TABLE wrestler DROP COLUMN IF EXISTS faction_id;
ALTER TABLE wrestler DROP COLUMN IF EXISTS manager_id;

