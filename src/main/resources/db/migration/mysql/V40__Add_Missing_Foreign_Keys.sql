ALTER TABLE wrestling_show ADD COLUMN universe_id BIGINT;
ALTER TABLE wrestling_show ADD CONSTRAINT fk_wrestling_show_universe FOREIGN KEY (universe_id) REFERENCES universe(id);

UPDATE wrestling_show SET universe_id = 1 WHERE universe_id IS NULL;

ALTER TABLE rivalry ADD COLUMN league_id BIGINT;
ALTER TABLE rivalry ADD CONSTRAINT fk_rivalry_league FOREIGN KEY (league_id) REFERENCES league(id);
