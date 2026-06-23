-- Add expansion_code to feature entities so they can be filtered by enabled set.
-- Defaults to 'BASE_GAME' to preserve existing data.
ALTER TABLE segment_type ADD COLUMN expansion_code VARCHAR(50) NOT NULL DEFAULT 'BASE_GAME';
ALTER TABLE segment_rule ADD COLUMN expansion_code VARCHAR(50) NOT NULL DEFAULT 'BASE_GAME';
ALTER TABLE achievement ADD COLUMN expansion_code VARCHAR(50) NOT NULL DEFAULT 'BASE_GAME';
ALTER TABLE title ADD COLUMN expansion_code VARCHAR(50) NOT NULL DEFAULT 'BASE_GAME';
ALTER TABLE ringside_action ADD COLUMN expansion_code VARCHAR(50) NOT NULL DEFAULT 'BASE_GAME';
