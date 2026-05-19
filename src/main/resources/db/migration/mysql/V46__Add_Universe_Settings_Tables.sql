CREATE TABLE IF NOT EXISTS universe_expansion_settings (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    universe_id BIGINT NOT NULL,
    expansion_code VARCHAR(255) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT uk_universe_expansion UNIQUE (universe_id, expansion_code),
    CONSTRAINT fk_ues_universe FOREIGN KEY (universe_id) REFERENCES universe (id)
);

CREATE TABLE IF NOT EXISTS universe_wrestler_exclusions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    universe_id BIGINT NOT NULL,
    wrestler_id BIGINT NOT NULL,
    CONSTRAINT uk_universe_wrestler UNIQUE (universe_id, wrestler_id),
    CONSTRAINT fk_uwe_universe FOREIGN KEY (universe_id) REFERENCES universe (id),
    CONSTRAINT fk_uwe_wrestler FOREIGN KEY (wrestler_id) REFERENCES wrestler (wrestler_id)
);
