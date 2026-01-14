ALTER TABLE title_reign ADD COLUMN won_at_segment_id BIGINT;
ALTER TABLE title_reign ADD CONSTRAINT fk_title_reign_won_at_segment FOREIGN KEY (won_at_segment_id) REFERENCES segment(segment_id);
