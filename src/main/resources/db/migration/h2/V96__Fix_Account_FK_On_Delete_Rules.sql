-- Fix FK constraints on tables that reference account(id) so that deleting an account
-- does not leave orphaned rows or violate referential integrity.
--
-- universe_members: remove the membership row when the account is deleted.
--   The universe itself is NOT affected — only the linking row is removed.
ALTER TABLE universe_members DROP CONSTRAINT fk_um_account;
ALTER TABLE universe_members
    ADD CONSTRAINT fk_um_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE;

-- universe_join_request.resolved_by: null out the resolver reference when the admin
--   account is deleted. The request history is preserved.
EXECUTE IMMEDIATE (
    SELECT 'ALTER TABLE UNIVERSE_JOIN_REQUEST DROP CONSTRAINT "' || tc.CONSTRAINT_NAME || '"'
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
    JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
      ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME AND tc.TABLE_NAME = kcu.TABLE_NAME
    WHERE tc.TABLE_NAME = 'UNIVERSE_JOIN_REQUEST'
      AND tc.CONSTRAINT_TYPE = 'FOREIGN KEY'
      AND kcu.COLUMN_NAME = 'RESOLVED_BY'
    LIMIT 1
);
ALTER TABLE universe_join_request
    ADD CONSTRAINT fk_request_resolved_by FOREIGN KEY (resolved_by) REFERENCES account (id) ON DELETE SET NULL;

-- universe_invite.created_by: make nullable so existing invite links survive after the
--   admin who created them is deleted.
EXECUTE IMMEDIATE (
    SELECT 'ALTER TABLE UNIVERSE_INVITE DROP CONSTRAINT "' || tc.CONSTRAINT_NAME || '"'
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
    JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
      ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME AND tc.TABLE_NAME = kcu.TABLE_NAME
    WHERE tc.TABLE_NAME = 'UNIVERSE_INVITE'
      AND tc.CONSTRAINT_TYPE = 'FOREIGN KEY'
      AND kcu.COLUMN_NAME = 'CREATED_BY'
    LIMIT 1
);
ALTER TABLE universe_invite ALTER COLUMN created_by BIGINT NULL;
ALTER TABLE universe_invite
    ADD CONSTRAINT fk_invite_created_by FOREIGN KEY (created_by) REFERENCES account (id) ON DELETE SET NULL;
