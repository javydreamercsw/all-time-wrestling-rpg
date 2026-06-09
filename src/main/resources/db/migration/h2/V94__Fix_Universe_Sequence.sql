-- H2: MERGE INTO with an explicit id does not advance the identity sequence,
-- so subsequent inserts generate id=1 and collide with the seeded Default Universe.
-- Reset the sequence to the current max so new universes get unique ids.
ALTER TABLE universe ALTER COLUMN id RESTART WITH (SELECT MAX(id) + 1 FROM universe);
