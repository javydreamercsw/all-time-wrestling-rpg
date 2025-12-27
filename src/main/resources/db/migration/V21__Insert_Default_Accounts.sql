-- Insert default roles
INSERT INTO role (name, description) VALUES
    ('ADMIN', 'Full system access - can manage accounts and all content'),
    ('BOOKER', 'Can manage shows, wrestlers, and content but not system administration'),
    ('PLAYER', 'Can manage own content and view most data'),
    ('VIEWER', 'Read-only access to content');

-- Insert default accounts (passwords are BCrypt encoded: admin123, booker123, player123, viewer123)
-- BCrypt rounds: 10
-- Note: These passwords should be changed in production!
INSERT INTO account (username, password, email, enabled, account_non_expired, account_non_locked, credentials_non_expired)
VALUES
    ('admin', '$2a$10$wKGJ2IuP7HwMP66VaqSdYuqo3S1lcXpl9oqQkTGuLaDYHfbH57hD6', 'admin@atwrpg.local', TRUE, TRUE, TRUE, TRUE),
    ('booker', '$2a$10$OrFNvKFkH5s/DvDzd301Me4v9bpIulbPNasymqmaxCqaUM.kVXHEi', 'booker@atwrpg.local', TRUE, TRUE, TRUE, TRUE),
    ('player', '$2a$10$oHciydemMfshOLiGK7g4KO.Epu07svrzinu7PFvdJws5PYK3pIKx.', 'player@atwrpg.local', TRUE, TRUE, TRUE, TRUE),
    ('viewer', '$2a$10$no8XHshPMFd14eBxIs9e2uYW8bXm/pT6MOZsXnw.RHyhmRWgvok06', 'viewer@atwrpg.local', TRUE, TRUE, TRUE, TRUE);

-- Assign roles to accounts
INSERT INTO account_roles (account_id, role_id)
SELECT a.id, r.id FROM account a, role r WHERE a.username = 'admin' AND r.name = 'ADMIN';

INSERT INTO account_roles (account_id, role_id)
SELECT a.id, r.id FROM account a, role r WHERE a.username = 'booker' AND r.name = 'BOOKER';

INSERT INTO account_roles (account_id, role_id)
SELECT a.id, r.id FROM account a, role r WHERE a.username = 'player' AND r.name = 'PLAYER';

INSERT INTO account_roles (account_id, role_id)
SELECT a.id, r.id FROM account a, role r WHERE a.username = 'viewer' AND r.name = 'VIEWER';

