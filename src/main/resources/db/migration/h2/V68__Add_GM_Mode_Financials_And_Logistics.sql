-- V68: Add GM Mode fields to Season and League, and create wrestler_contract table
ALTER TABLE season ADD COLUMN budget DECIMAL(19, 2) DEFAULT 0.00;
ALTER TABLE season ADD COLUMN duration_weeks INT;

ALTER TABLE league ADD COLUMN budget DECIMAL(19, 2) DEFAULT 0.00;
ALTER TABLE league ADD COLUMN duration_weeks INT;
ALTER TABLE league ADD COLUMN locker_room_morale INT NOT NULL DEFAULT 100;

ALTER TABLE wrestler ADD COLUMN morale INT NOT NULL DEFAULT 100;
ALTER TABLE wrestler ADD COLUMN management_stamina INT NOT NULL DEFAULT 100;

ALTER TABLE segment ADD COLUMN crowd_noise_level INT NOT NULL DEFAULT 0;

CREATE TABLE wrestler_contract (
    contract_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wrestler_id BIGINT NOT NULL,
    league_id BIGINT NOT NULL,
    salary_per_show DECIMAL(19, 2) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    expiry_date TIMESTAMP,
    duration_weeks INT NOT NULL,
    is_initial_draft BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL,
    external_id VARCHAR(255) UNIQUE,
    last_sync TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE,
    FOREIGN KEY (league_id) REFERENCES league(id) ON DELETE CASCADE
);

CREATE INDEX idx_contract_league ON wrestler_contract(league_id);
CREATE INDEX idx_contract_wrestler ON wrestler_contract(wrestler_id);
