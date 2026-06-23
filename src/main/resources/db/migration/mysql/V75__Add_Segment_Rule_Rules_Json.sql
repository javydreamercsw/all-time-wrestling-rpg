ALTER TABLE segment_rule ADD COLUMN rules_json LONGTEXT NULL;
ALTER TABLE segment_rule ADD COLUMN rules_hash VARCHAR(64) NULL;
