-- 2.13.0 (follow-up to V166): the backfills are done — enforce the entity contract.
ALTER TABLE segment ALTER COLUMN result SET NOT NULL;
ALTER TABLE segment ALTER COLUMN narration_generated SET NOT NULL;
ALTER TABLE league_roster ALTER COLUMN no_contests SET NOT NULL;
