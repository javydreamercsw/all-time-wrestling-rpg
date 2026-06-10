-- Fix FK constraints on tables that reference account(id) so that deleting an account
-- does not leave orphaned rows or violate referential integrity.
--
-- universe_members: remove the membership row when the account is deleted.
--   The universe itself is NOT affected — only the linking row is removed.
ALTER TABLE universe_members DROP FOREIGN KEY fk_um_account;
ALTER TABLE universe_members
    ADD CONSTRAINT fk_um_account FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE CASCADE;

-- universe_join_request.resolved_by: null out the resolver reference when the admin
--   account is deleted. The request history is preserved.
ALTER TABLE universe_join_request DROP FOREIGN KEY fk_request_resolved_by;
ALTER TABLE universe_join_request
    ADD CONSTRAINT fk_request_resolved_by FOREIGN KEY (resolved_by) REFERENCES account (id) ON DELETE SET NULL;

-- universe_invite.created_by: make the column nullable so existing invite links survive
--   even after the admin who created them is deleted.
ALTER TABLE universe_invite MODIFY COLUMN created_by BIGINT NULL;
ALTER TABLE universe_invite DROP FOREIGN KEY fk_invite_created_by;
ALTER TABLE universe_invite
    ADD CONSTRAINT fk_invite_created_by FOREIGN KEY (created_by) REFERENCES account (id) ON DELETE SET NULL;
