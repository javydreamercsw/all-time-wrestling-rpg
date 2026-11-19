-- Seed-update review (2.13.0): per-row seed provenance for the final four content domains that
-- had no stamp column (commentary teams, campaign upgrades, outcome matrices + their entries,
-- wrestler relationships) — the same nullable fingerprint the other twenty seed domains carry
-- since V166/V170/V171. NULL = pre-fingerprint-era (legacy) or user-created row.
ALTER TABLE commentary_team ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
ALTER TABLE campaign_upgrade ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
ALTER TABLE outcome_matrix ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
ALTER TABLE outcome_matrix_entry ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
ALTER TABLE wrestler_relationship ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
