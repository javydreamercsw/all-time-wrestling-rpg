-- Remove season_number column from season table
-- This column was forcing an artificial numbering convention that isn't needed
-- Seasons can be named freely without requiring sequential numbers

ALTER TABLE season DROP COLUMN season_number;
