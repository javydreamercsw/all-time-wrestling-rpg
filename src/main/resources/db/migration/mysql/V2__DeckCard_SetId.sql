SET FOREIGN_KEY_CHECKS = 0;

-- 1. Add set_id as nullable
ALTER TABLE deck_card ADD COLUMN set_id BIGINT;

-- 2. Populate set_id for all existing rows using the card table
UPDATE deck_card dc
SET set_id = (
  SELECT c.set_id FROM card c WHERE c.card_id = dc.card_id
);

-- 3. Now set set_id as NOT NULL
ALTER TABLE deck_card MODIFY COLUMN set_id BIGINT NOT NULL;

-- Drop foreign keys first
ALTER TABLE deck_card DROP FOREIGN KEY deck_card_ibfk_1;
ALTER TABLE deck_card DROP FOREIGN KEY deck_card_ibfk_2;

-- 5. Drop old unique constraint
ALTER TABLE deck_card DROP INDEX uq_deck_card_deck_id_card_id;

-- 6. Add foreign key constraint to card_set
ALTER TABLE deck_card ADD CONSTRAINT fk_deck_card_set FOREIGN KEY (set_id) REFERENCES card_set(set_id);

-- 7. Add new unique constraint
ALTER TABLE deck_card ADD CONSTRAINT uq_deck_card_deck_card_set UNIQUE (deck_id, card_id, set_id);

-- Add foreign keys back (without explicit names, let MySQL generate them)
ALTER TABLE deck_card ADD FOREIGN KEY (deck_id) REFERENCES deck(deck_id) ON DELETE CASCADE;
ALTER TABLE deck_card ADD FOREIGN KEY (card_id) REFERENCES card(card_id) ON DELETE CASCADE;

SET FOREIGN_KEY_CHECKS = 1;