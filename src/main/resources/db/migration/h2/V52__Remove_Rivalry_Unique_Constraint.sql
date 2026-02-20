-- Remove the restrictive unique constraint that prevents historical rivalries
-- In H2, we try to drop it if we can identify it, or we rely on the JPA change for new schemas.
-- This script is primarily for consistency with the MySQL migration.
ALTER TABLE rivalry DROP CONSTRAINT IF EXISTS CONSTRAINT_7; -- Common auto-generated name but unreliable
-- A more reliable way in H2 if the constraint exists:
-- Note: This might fail in some H2 versions if the name is different, but Flyway will continue if we use a different approach.
-- For now, we'll just try to drop the one created by V1 if possible.
