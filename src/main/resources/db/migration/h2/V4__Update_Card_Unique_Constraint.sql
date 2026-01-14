-- Update the unique constraint on the card table.
-- This script drops the old unique constraint (name, number, set_id)
-- and adds a new unique constraint (number, set_id).

ALTER TABLE card ADD CONSTRAINT UQ_card_number_set_id UNIQUE (number, set_id);