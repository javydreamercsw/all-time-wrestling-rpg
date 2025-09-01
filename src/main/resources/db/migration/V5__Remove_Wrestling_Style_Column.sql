-- ATW RPG Database Migration V5: Remove Wrestling Style Column
-- Removes the wrestling_style column from wrestler table as narration will use description instead
-- H2 Database Compatible

-- Remove wrestling_style column from wrestler table
ALTER TABLE wrestler DROP COLUMN IF EXISTS wrestling_style;
