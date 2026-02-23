-- Add referee and referee_awareness_level to segment table
ALTER TABLE segment ADD COLUMN referee_id BIGINT;
ALTER TABLE segment ADD COLUMN referee_awareness_level INT DEFAULT 0 NOT NULL;
ALTER TABLE segment ADD FOREIGN KEY (referee_id) REFERENCES npc(id);
