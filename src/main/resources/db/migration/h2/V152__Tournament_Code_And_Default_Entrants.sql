-- ATW-vg16: Stable tournament identity for the tournaments.json seed catalog.
-- `code` is the machine-readable handle (WellKnownTournament parity test guards it
-- against the JSON). `default_entrant_count` is the catalog's auto-seeding hint —
-- without it a SINGLE_ELIMINATION seed auto-starts a format-max (64) bracket.
ALTER TABLE tournament
  ADD COLUMN IF NOT EXISTS code VARCHAR(64);

ALTER TABLE tournament
  ADD COLUMN IF NOT EXISTS default_entrant_count INT;

CREATE UNIQUE INDEX IF NOT EXISTS ux_tournament_code ON tournament (code);
