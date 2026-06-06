-- Step 1: Add nullable column so existing rows are not immediately rejected.
ALTER TABLE rivalry ADD COLUMN universe_id BIGINT;

-- Step 2: Backfill from league where the rivalry already has one.
UPDATE rivalry SET universe_id = (
    SELECT l.universe_id FROM league l WHERE l.id = rivalry.league_id
) WHERE rivalry.league_id IS NOT NULL;

-- Step 3: Default any remaining rows to the Default Universe (id=1).
UPDATE rivalry SET universe_id = 1 WHERE universe_id IS NULL;

-- Step 4: Tighten to NOT NULL.
ALTER TABLE rivalry ALTER COLUMN universe_id BIGINT NOT NULL;

-- Step 5: Add FK constraint.
ALTER TABLE rivalry ADD CONSTRAINT fk_rivalry_universe
    FOREIGN KEY (universe_id) REFERENCES universe(id);
