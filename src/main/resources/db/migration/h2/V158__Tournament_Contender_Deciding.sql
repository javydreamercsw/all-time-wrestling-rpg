-- Contender-deciding tournament payoff (ATW-ewrp): the winner becomes the #1 contender
-- for the linked title instead of challenging — the payoff books as a contender match
-- (title attached, NOT on the line). FALSE = classic title-on-the-line semantics.
ALTER TABLE tournament ADD COLUMN contender_deciding BOOLEAN NOT NULL DEFAULT FALSE;
