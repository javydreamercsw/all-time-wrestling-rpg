-- Status Card Tables

CREATE TABLE status_card (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    status_key VARCHAR(255) NOT NULL UNIQUE,
    level_1_name VARCHAR(255) NOT NULL,
    level_2_name VARCHAR(255),
    description TEXT,
    positive BOOLEAN NOT NULL DEFAULT TRUE,
    level_1_effect VARCHAR(255),
    level_2_effect VARCHAR(255),
    flip_up_condition VARCHAR(255),
    flip_down_condition VARCHAR(255),
    discard_condition VARCHAR(255)
);

CREATE TABLE wrestler_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wrestler_id BIGINT NOT NULL,
    status_card_id BIGINT NOT NULL,
    level INT NOT NULL DEFAULT 1,
    creation_date TIMESTAMP NOT NULL,
    last_updated TIMESTAMP NOT NULL,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id),
    FOREIGN KEY (status_card_id) REFERENCES status_card(id)
);

CREATE TABLE wrestler_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wrestler_id BIGINT NOT NULL,
    status_card_id BIGINT NOT NULL,
    action VARCHAR(255) NOT NULL,
    old_level INT,
    new_level INT,
    creation_date TIMESTAMP NOT NULL,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id),
    FOREIGN KEY (status_card_id) REFERENCES status_card(id)
);

CREATE TABLE milestone_status_rewards (
    milestone_id BIGINT NOT NULL,
    status_key VARCHAR(255) NOT NULL,
    PRIMARY KEY (milestone_id, status_key),
    FOREIGN KEY (milestone_id) REFERENCES storyline_milestone(id)
);

CREATE INDEX idx_wrestler_status_wrestler ON wrestler_status(wrestler_id);
CREATE INDEX idx_wrestler_status_history_wrestler ON wrestler_status_history(wrestler_id);
