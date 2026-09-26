-- Optional gender restriction for tournament entrants (ATW-4g0j): narrows the eligible pool in
-- the creation wizard and auto-seeding on top of the linked title's own gender constraint.
-- NULL = all genders.
ALTER TABLE tournament ADD COLUMN gender VARCHAR(16) NULL;
