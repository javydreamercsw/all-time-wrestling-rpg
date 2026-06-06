-- Step 1: Add nullable column so existing rows are not immediately rejected.
ALTER TABLE rivalry ADD COLUMN universe_id BIGINT;

-- Step 2: Backfill from league where the rivalry already has one.
UPDATE rivalry r
    INNER JOIN league l ON l.id = r.league_id
SET r.universe_id = l.universe_id
WHERE r.league_id IS NOT NULL;

-- Step 3: Default any remaining rows to the Default Universe (id=1).
UPDATE rivalry SET universe_id = 1 WHERE universe_id IS NULL;

-- Step 4: Tighten to NOT NULL and add FK in one statement.
ALTER TABLE rivalry
    MODIFY COLUMN universe_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_rivalry_universe
        FOREIGN KEY (universe_id) REFERENCES universe (id);
