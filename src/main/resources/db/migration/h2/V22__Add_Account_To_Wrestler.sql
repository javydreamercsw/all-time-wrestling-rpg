ALTER TABLE wrestler ADD COLUMN account_id BIGINT;

ALTER TABLE wrestler
ADD CONSTRAINT fk_wrestler_account
FOREIGN KEY (account_id)
REFERENCES account(id);
