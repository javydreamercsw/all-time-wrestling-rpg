-- Insert sentinel type for injuries that predate this FK.
-- The specialEffects field documents the purpose.
INSERT INTO injury_type (injury_name, health_effect, stamina_effect, card_effect, special_effects)
VALUES (
    'Legacy Injury', 0, 0, 0,
    'Placeholder for injuries that existed before injury types were introduced. Update to the correct type when known.'
);

-- Step 1: Add nullable so existing rows are not immediately rejected.
ALTER TABLE injury ADD COLUMN injury_type_id BIGINT;

-- Step 2: Backfill all existing rows with the sentinel.
UPDATE injury SET injury_type_id = (
    SELECT injury_type_id FROM injury_type WHERE injury_name = 'Legacy Injury'
);

-- Step 3: Tighten to NOT NULL.
ALTER TABLE injury ALTER COLUMN injury_type_id BIGINT NOT NULL;

-- Step 4: Add FK constraint.
ALTER TABLE injury ADD CONSTRAINT fk_injury_injury_type
    FOREIGN KEY (injury_type_id) REFERENCES injury_type(injury_type_id);
