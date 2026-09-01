ALTER TABLE segment_type ADD COLUMN code VARCHAR(64) NULL;
ALTER TABLE segment_type ADD CONSTRAINT uq_segment_type_code UNIQUE (code);

UPDATE segment_type SET code = 'one_on_one' WHERE name = 'One on One';
UPDATE segment_type SET code = 'tag_team' WHERE name = 'Tag Team';
UPDATE segment_type SET code = 'promo' WHERE name = 'Promo';
UPDATE segment_type SET code = 'abu_dhabi_rumble' WHERE name = 'Abu Dhabi Rumble';
UPDATE segment_type SET code = 'free_for_all' WHERE name = 'Free-for-All';
UPDATE segment_type SET code = 'handicap_match' WHERE name = 'Handicap Match';
