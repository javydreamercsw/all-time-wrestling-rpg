-- Drop the unique constraint on wrestler(account_id) if it exists
-- In H2, we can use a simpler SQL approach to drop the constraint by name if we're sure of it, 
-- or use a script that is correctly formatted for H2's Java bridge.

-- Try to drop by known name first (most likely case if Flyway applied it)
ALTER TABLE WRESTLER DROP CONSTRAINT IF EXISTS UNIQUE_WRESTLER_ACCOUNT;

-- Also check for system-generated names just in case
-- We'll use a safer approach using H2's INFORMATION_SCHEMA in a single statement if possible
-- or just rely on the above which covers the explicit name used in V36.
