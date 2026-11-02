-- Seed verify mode (fingerprint-gated reconciliation of seed-owned fields):
-- nullable fingerprint stamped at insert/reconcile; NULL = pre-fingerprint-era (legacy) row.
ALTER TABLE wrestler ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
ALTER TABLE card_set ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
ALTER TABLE card ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
ALTER TABLE deck ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
ALTER TABLE npc ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
ALTER TABLE segment_type ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
ALTER TABLE title ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
ALTER TABLE ringside_action ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
ALTER TABLE achievement ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
ALTER TABLE campaign_ability_card ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
ALTER TABLE segment_rule ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
