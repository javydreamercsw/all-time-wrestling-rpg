-- V35: Create wrestler_relationship table for interpersonal social layer
CREATE TABLE wrestler_relationship (
    relationship_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wrestler1_id BIGINT NOT NULL,
    wrestler2_id BIGINT NOT NULL,
    relationship_type VARCHAR(255) NOT NULL,
    level INT NOT NULL DEFAULT 50,
    is_storyline BOOLEAN NOT NULL DEFAULT FALSE,
    started_date DATETIME NOT NULL,
    creation_date DATETIME NOT NULL,
    notes VARCHAR(1000),
    external_id VARCHAR(255) UNIQUE,
    last_sync DATETIME,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_rel_wrestler1 FOREIGN KEY (wrestler1_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE,
    CONSTRAINT fk_rel_wrestler2 FOREIGN KEY (wrestler2_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE,
    UNIQUE KEY (wrestler1_id, wrestler2_id, relationship_type)
);

CREATE INDEX idx_rel_wrestlers ON wrestler_relationship(wrestler1_id, wrestler2_id);
