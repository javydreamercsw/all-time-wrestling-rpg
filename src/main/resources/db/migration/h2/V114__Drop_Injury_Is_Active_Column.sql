-- is_active is now derived from healed_date (see Injury.java @Formula); drop the
-- independently-writable column so display and healing-eligibility can never disagree.
ALTER TABLE injury DROP COLUMN IF EXISTS is_active;
