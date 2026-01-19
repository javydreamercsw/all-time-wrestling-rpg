ALTER TABLE campaign_ability_card ADD COLUMN secondary_effect_script VARCHAR(255);
ALTER TABLE campaign_ability_card ADD COLUMN secondary_timing VARCHAR(50);
ALTER TABLE campaign_ability_card ADD COLUMN secondary_one_time_use BOOLEAN DEFAULT FALSE;
