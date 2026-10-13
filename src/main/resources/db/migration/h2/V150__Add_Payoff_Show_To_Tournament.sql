-- ATW-xbn4: One-time tournaments attached to a host show. The payoff (final or
-- champion showcase) books on the host show exactly once; the non-final rounds
-- pace automatically onto the weekly shows before it. Template pairing
-- (tournament_id on show_template_segment_assignment) stays reserved for
-- recurring tournaments.
ALTER TABLE tournament
  ADD COLUMN IF NOT EXISTS payoff_show_id BIGINT;

ALTER TABLE tournament
  ADD COLUMN IF NOT EXISTS payoff_segment_type_id BIGINT;

ALTER TABLE tournament
  ADD COLUMN IF NOT EXISTS payoff_segment_rule_id BIGINT;

ALTER TABLE tournament
  ADD CONSTRAINT IF NOT EXISTS fk_tournament_payoff_show FOREIGN KEY (payoff_show_id)
    REFERENCES wrestling_show (show_id);

ALTER TABLE tournament
  ADD CONSTRAINT IF NOT EXISTS fk_tournament_payoff_type FOREIGN KEY (payoff_segment_type_id)
    REFERENCES segment_type (segment_type_id);

ALTER TABLE tournament
  ADD CONSTRAINT IF NOT EXISTS fk_tournament_payoff_rule FOREIGN KEY (payoff_segment_rule_id)
    REFERENCES segment_rule (segment_rule_id);
