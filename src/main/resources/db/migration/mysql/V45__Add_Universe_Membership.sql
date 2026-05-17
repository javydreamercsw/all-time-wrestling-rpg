CREATE TABLE universe_members
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    universe_id BIGINT       NOT NULL,
    account_id  BIGINT       NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'MEMBER',
    joined_date TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_um_universe FOREIGN KEY (universe_id) REFERENCES universe (id),
    CONSTRAINT fk_um_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT uk_universe_member UNIQUE (universe_id, account_id)
);

-- Seed all existing accounts as MEMBER of the Default Universe (ID=1)
INSERT INTO universe_members (universe_id, account_id, role)
SELECT 1, id, 'MEMBER'
FROM account;
