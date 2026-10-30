-- 2.13.0 (follow-up to V135): the backfills are done — enforce the entity contract.
ALTER TABLE segment MODIFY COLUMN result VARCHAR(20) NOT NULL DEFAULT 'WINNERS';
ALTER TABLE segment MODIFY COLUMN narration_generated TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE league_roster MODIFY COLUMN no_contests INT NOT NULL DEFAULT 0;
