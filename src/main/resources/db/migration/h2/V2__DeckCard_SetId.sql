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

-- 5. Drop old unique constraint by actual name
ALTER TABLE deck_card DROP CONSTRAINT IF EXISTS CONSTRAINT_BF75B_INDEX_2;

-- 6. Add new unique constraint
ALTER TABLE deck_card ADD CONSTRAINT uq_deck_card_deck_card_set UNIQUE (deck_id, card_id, set_id);
