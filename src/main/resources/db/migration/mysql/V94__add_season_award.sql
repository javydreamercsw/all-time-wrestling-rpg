CREATE TABLE season_award
(
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    season_id   BIGINT       NOT NULL,
    wrestler_id BIGINT       NOT NULL,
    award_type  VARCHAR(50)  NOT NULL,
    award_value VARCHAR(255),
    awarded_at  DATETIME(6)  NOT NULL,
    CONSTRAINT fk_sa_season   FOREIGN KEY (season_id)   REFERENCES season (season_id),
    CONSTRAINT fk_sa_wrestler FOREIGN KEY (wrestler_id) REFERENCES wrestler (wrestler_id),
    INDEX idx_sa_season (season_id)
);
