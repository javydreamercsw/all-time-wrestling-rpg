-- Remove the restrictive unique constraint that prevents historical rivalries
-- In MySQL, the index name for a UNIQUE (col1, col2) constraint often defaults to the first column name.
-- We use ALTER TABLE ... DROP INDEX to remove it.
ALTER TABLE rivalry DROP INDEX wrestler1_id;

-- Add a non-unique index for performance
CREATE INDEX idx_rivalry_wrestlers ON rivalry (wrestler1_id, wrestler2_id);
