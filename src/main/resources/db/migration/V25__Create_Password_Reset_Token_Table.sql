CREATE TABLE password_reset_token (
  id BIGINT NOT NULL,
  token VARCHAR(255),
  account_id BIGINT NOT NULL,
  expiry_date TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_password_reset_token_account
    FOREIGN KEY (account_id)
    REFERENCES account(id)
);