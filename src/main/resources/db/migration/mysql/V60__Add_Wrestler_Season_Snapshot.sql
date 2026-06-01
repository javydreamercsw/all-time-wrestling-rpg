-- Captures each wrestler's fan count at the start of every new season
CREATE TABLE wrestler_season_snapshot (
    snapshot_id   BIGINT    NOT NULL AUTO_INCREMENT PRIMARY KEY,
    wrestler_id   BIGINT    NOT NULL,
    season_id     BIGINT    NOT NULL,
    starting_fans BIGINT    NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE,
    FOREIGN KEY (season_id)   REFERENCES season(season_id)     ON DELETE CASCADE,
    UNIQUE KEY uq_wrestler_season (wrestler_id, season_id)
);
