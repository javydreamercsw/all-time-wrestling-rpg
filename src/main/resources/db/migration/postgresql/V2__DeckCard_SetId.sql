-- 1. Add set_id as nullable
ALTER TABLE deck_card ADD COLUMN set_id BIGINT;

-- 2. Populate set_id for all existing rows using the card table
UPDATE deck_card dc
SET set_id = (
  SELECT c.set_id FROM card c WHERE c.card_id = dc.card_id
);

-- 3. Now set set_id as NOT NULL
ALTER TABLE deck_card ALTER COLUMN set_id SET NOT NULL;

-- 4. Add foreign key constraint to card_set
ALTER TABLE deck_card ADD CONSTRAINT fk_deck_card_set FOREIGN KEY (set_id) REFERENCES card_set(set_id);

-- 5. Drop old unique constraint
-- Note: The constraint name might be different in your database.
-- Use '\d deck_card' in psql to find the correct constraint name.
ALTER TABLE deck_card DROP CONSTRAINT deck_card_deck_id_card_id_key;

-- 6. Add new unique constraint
ALTER TABLE deck_card ADD CONSTRAINT uq_deck_card_deck_card_set UNIQUE (deck_id, card_id, set_id);
