-- Gender division for the briefcase (ATW-hq8d): men's and women's divisions can each run their
-- own briefcase-deciding tournament and hold their own case. Copied from the granting
-- tournament's gender at grant time. NULL = all genders (ungendered lineage).
ALTER TABLE title_opportunity ADD COLUMN gender VARCHAR(16) NULL;
