-- ATW-etws: template assignment rows can carry a full tournament spec
-- (name, format, entrant count, final rule, linked title, allowed-rules pool).
-- The booking path creates ONE persistent tournament per row on first use and
-- stores it in tournament_id. Rows may reference either an existing tournament
-- (tournament_id) or a spec (spec_name + spec_format_id), never both.
ALTER TABLE show_template_segment_assignment
  ADD COLUMN spec_name VARCHAR(255) NULL,
  ADD COLUMN spec_format_id VARCHAR(64) NULL,
  ADD COLUMN spec_entrant_count INT NULL,
  ADD COLUMN spec_final_rule_id BIGINT NULL,
  ADD COLUMN spec_title_id BIGINT NULL;
ALTER TABLE show_template_segment_assignment
  ADD CONSTRAINT fk_sta_spec_final_rule FOREIGN KEY (spec_final_rule_id)
  REFERENCES segment_rule (segment_rule_id);
ALTER TABLE show_template_segment_assignment
  ADD CONSTRAINT fk_sta_spec_title FOREIGN KEY (spec_title_id) REFERENCES title (title_id);

-- Allowed-rules pool: SegmentRule is name-keyed (no code column), so the pool
-- is a join table of rule references rather than a name list.
CREATE TABLE IF NOT EXISTS show_template_assignment_rule (
  assignment_id BIGINT NOT NULL,
  segment_rule_id BIGINT NOT NULL,
  position INT NOT NULL DEFAULT 0,
  PRIMARY KEY (assignment_id, segment_rule_id),
  CONSTRAINT fk_star_assignment FOREIGN KEY (assignment_id)
    REFERENCES show_template_segment_assignment (assignment_id) ON DELETE CASCADE,
  CONSTRAINT fk_star_segment_rule FOREIGN KEY (segment_rule_id)
    REFERENCES segment_rule (segment_rule_id)
);
