-- ATW-oahn: Allow template assignment rows to reference a tournament whose
-- participants feed the auto-attached segment (e.g. a tournament attached to a
-- PLE template resolves the entrants of the auto-attached TLC match).
ALTER TABLE show_template_segment_assignment
  ADD COLUMN tournament_id BIGINT;

ALTER TABLE show_template_segment_assignment
  ADD CONSTRAINT fk_sta_tournament FOREIGN KEY (tournament_id)
    REFERENCES tournament (tournament_id);
