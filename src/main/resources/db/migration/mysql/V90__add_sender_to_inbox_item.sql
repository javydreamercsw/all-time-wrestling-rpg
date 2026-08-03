ALTER TABLE inbox_item
  ADD COLUMN sender_account_id BIGINT NULL,
  ADD CONSTRAINT fk_inbox_sender FOREIGN KEY (sender_account_id) REFERENCES account(id) ON DELETE SET NULL;
