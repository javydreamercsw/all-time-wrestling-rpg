-- Remove the restrictive unique constraint that prevents historical rivalries
-- Add a non-unique index first to satisfy foreign key requirements for wrestler1_id
CREATE INDEX idx_rivalry_wrestlers ON rivalry (wrestler1_id, wrestler2_id);

-- Now we can drop the unique index that was previously covering the foreign key
ALTER TABLE rivalry DROP INDEX wrestler1_id;
