-- ATW-o4ad: recurring tournament editions. A tournament chain links editions
-- via parent_tournament_id (self-reference) with edition_ordinal numbering
-- (1 = first edition). recurrence=NONE keeps today's one-shot semantics
-- (payoff books, pairing consumed); ANNUAL auto-creates the next edition when
-- the payoff books and re-points the PLE template pairing instead of
-- consuming it. NULL parent/ordinal/recurrence = legacy one-shot, so no
-- backfill is needed and existing COMPLETE tournaments never re-fire.
ALTER TABLE tournament ADD COLUMN IF NOT EXISTS parent_tournament_id BIGINT;
ALTER TABLE tournament ADD COLUMN IF NOT EXISTS edition_ordinal INT;
ALTER TABLE tournament ADD COLUMN IF NOT EXISTS recurrence VARCHAR(16) DEFAULT 'NONE' NOT NULL;

ALTER TABLE tournament
  ADD CONSTRAINT fk_tournament_parent FOREIGN KEY (parent_tournament_id)
  REFERENCES tournament (tournament_id);
CREATE INDEX IF NOT EXISTS idx_tournament_parent ON tournament (parent_tournament_id);
