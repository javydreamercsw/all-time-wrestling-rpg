-- Seed-update review (2.13.0): per-row seed provenance for the four content domains that had no
-- stamp column (show types, show templates, status cards, tournaments) — the same nullable
-- fingerprint the other eleven seed domains carry since V166. NULL = pre-fingerprint-era
-- (legacy) or user-created row.
ALTER TABLE show_type ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
ALTER TABLE show_template ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
ALTER TABLE status_card ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
ALTER TABLE tournament ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
