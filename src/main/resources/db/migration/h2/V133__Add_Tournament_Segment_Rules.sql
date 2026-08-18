-- Pool of allowed segment rules for a tournament (random selection when booking)
CREATE TABLE tournament_segment_rule (
  tournament_id BIGINT NOT NULL,
  segment_rule_id BIGINT NOT NULL,
  PRIMARY KEY (tournament_id, segment_rule_id),
  CONSTRAINT fk_tsr_tournament FOREIGN KEY (tournament_id)
    REFERENCES tournament(tournament_id) ON DELETE CASCADE,
  CONSTRAINT fk_tsr_segment_rule FOREIGN KEY (segment_rule_id)
    REFERENCES segment_rule(segment_rule_id)
);

-- Optional fixed rule per round (overrides the tournament pool when booking)
ALTER TABLE tournament_round
  ADD COLUMN segment_rule_id BIGINT NULL;

ALTER TABLE tournament_round
  ADD CONSTRAINT fk_tr_segment_rule FOREIGN KEY (segment_rule_id)
    REFERENCES segment_rule(segment_rule_id);
