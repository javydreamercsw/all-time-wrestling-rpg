-- Qualifier group size for QUALIFIER_GROUPS tournaments (ATW-o4ad follow-up): the number of
-- wrestlers per qualifier Free-for-All group. NULL = format default (3-wrestler groups).
ALTER TABLE tournament ADD COLUMN qualifier_group_size INT NULL;
