-- V71: Add unique constraint to campaign_ability_card for name, alignmentType, and level
ALTER TABLE campaign_ability_card
ADD CONSTRAINT uk_name_alignment_level UNIQUE (name, alignment_type, level);
