-- Add no_dq to segment_rule table
ALTER TABLE segment_rule ADD COLUMN no_dq BOOLEAN DEFAULT FALSE NOT NULL;
