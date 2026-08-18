CREATE TABLE wrestler_ability (
  wrestler_ability_id BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  wrestler_id         BIGINT       NOT NULL,
  name                VARCHAR(255) NOT NULL,
  description         TEXT,
  ability_type        VARCHAR(32)  NOT NULL,
  max_uses            INT,
  timing              VARCHAR(32),
  category            VARCHAR(32),
  is_default          BOOLEAN      NOT NULL DEFAULT TRUE,
  unlock_condition    TEXT,
  swap_condition      TEXT,
  cost_script         TEXT,
  effect_script       TEXT,
  CONSTRAINT fk_wrestler_ability_wrestler FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE,
  CONSTRAINT uq_wa_wrestler_name UNIQUE (wrestler_id, name)
);
ALTER TABLE segment_participant ADD COLUMN abilities_used VARCHAR(2000);
