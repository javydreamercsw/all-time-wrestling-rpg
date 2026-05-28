ALTER TABLE wrestler_alignment ADD COLUMN universe_id BIGINT NULL;
ALTER TABLE wrestler_alignment ADD CONSTRAINT fk_wrestler_alignment_universe
    FOREIGN KEY (universe_id) REFERENCES universe(id);
CREATE INDEX idx_wrestler_alignment_universe ON wrestler_alignment(wrestler_id, universe_id);
