CREATE TABLE password_reset_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL,
    account_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    created_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_password_reset_token_account FOREIGN KEY (account_id) REFERENCES account (id)
);

CREATE UNIQUE INDEX idx_password_reset_token_token ON password_reset_token(token);
