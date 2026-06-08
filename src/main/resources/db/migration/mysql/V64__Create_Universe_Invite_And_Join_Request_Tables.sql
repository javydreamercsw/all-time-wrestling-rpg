-- Universe invite links: admins generate these to share with prospective members.
-- type: TARGETED (single-use, 7-day default expiry) or COMMUNITY (multi-use, no expiry)
CREATE TABLE universe_invite (
    id VARCHAR(36) NOT NULL,
    universe_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    revoked_at TIMESTAMP NULL,
    max_uses INT NULL,
    use_count INT NOT NULL DEFAULT 0,
    CONSTRAINT pk_universe_invite PRIMARY KEY (id),
    CONSTRAINT fk_invite_universe FOREIGN KEY (universe_id) REFERENCES universe(id) ON DELETE CASCADE,
    CONSTRAINT fk_invite_created_by FOREIGN KEY (created_by) REFERENCES account(id)
);

-- Join requests submitted by users who followed an invite link.
-- status: PENDING → APPROVED | REJECTED | BLOCKED
CREATE TABLE universe_join_request (
    id BIGINT NOT NULL AUTO_INCREMENT,
    universe_id BIGINT NOT NULL,
    invite_id VARCHAR(36) NULL,
    account_id BIGINT NULL,
    requester_name VARCHAR(255) NOT NULL,
    requester_email VARCHAR(255) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    resolved_by BIGINT NULL,
    notes VARCHAR(1000) NULL,
    CONSTRAINT pk_universe_join_request PRIMARY KEY (id),
    CONSTRAINT fk_request_universe FOREIGN KEY (universe_id) REFERENCES universe(id) ON DELETE CASCADE,
    CONSTRAINT fk_request_invite FOREIGN KEY (invite_id) REFERENCES universe_invite(id) ON DELETE SET NULL,
    CONSTRAINT fk_request_account FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE SET NULL,
    CONSTRAINT fk_request_resolved_by FOREIGN KEY (resolved_by) REFERENCES account(id)
);

CREATE INDEX idx_universe_invite_universe ON universe_invite(universe_id);
CREATE INDEX idx_universe_join_request_universe ON universe_join_request(universe_id);
CREATE INDEX idx_universe_join_request_status ON universe_join_request(status);
CREATE INDEX idx_universe_join_request_account ON universe_join_request(account_id);
