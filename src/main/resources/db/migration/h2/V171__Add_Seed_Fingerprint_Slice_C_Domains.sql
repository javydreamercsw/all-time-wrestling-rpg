-- Seed-update review (2.13.0): per-row seed provenance for the five content domains that had no
-- stamp column (factions, arenas, locations, teams, commentators) — the same nullable fingerprint
-- the other fifteen seed domains carry since V166/V170. NULL = pre-fingerprint-era (legacy) or
-- user-created row.
ALTER TABLE faction ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
ALTER TABLE arena ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
ALTER TABLE location ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
ALTER TABLE team ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
ALTER TABLE commentator ADD COLUMN IF NOT EXISTS seed_fingerprint VARCHAR(64);
