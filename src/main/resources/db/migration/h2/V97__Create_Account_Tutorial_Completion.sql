CREATE TABLE account_tutorial_completion (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id    BIGINT      NOT NULL,
    universe_type VARCHAR(20) NOT NULL,
    current_step  INT         NOT NULL DEFAULT 0,
    completed_at  TIMESTAMP,
    CONSTRAINT fk_atc_account FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE CASCADE,
    CONSTRAINT uq_atc         UNIQUE (account_id, universe_type)
);
