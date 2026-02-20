-- Remove the restrictive unique constraint that prevents historical rivalries
-- The name 'wrestler1_id' is the default name given by MySQL for the UNIQUE (wrestler1_id, wrestler2_id) constraint
ALTER TABLE rivalry DROP INDEX wrestler1_id;

-- Add a non-unique index for performance
CREATE INDEX idx_rivalry_wrestlers ON rivalry (wrestler1_id, wrestler2_id);
