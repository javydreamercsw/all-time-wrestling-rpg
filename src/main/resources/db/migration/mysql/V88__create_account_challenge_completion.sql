CREATE TABLE account_challenge_completion (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  account_id BIGINT NOT NULL,
  challenge_id VARCHAR(255) NOT NULL,
  status VARCHAR(50) NOT NULL,
  completed_at DATETIME NULL,
  player_notes TEXT NULL,
  proof_image_url VARCHAR(500) NULL,
  CONSTRAINT uq_acc_challenge UNIQUE (account_id, challenge_id),
  CONSTRAINT fk_acc_challenge_account FOREIGN KEY (account_id) REFERENCES account(id)
);
