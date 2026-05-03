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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wrestler_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wrestler_id BIGINT NOT NULL,
    status_card_id BIGINT NOT NULL,
    level INT NOT NULL DEFAULT 1,
    creation_date DATETIME(6) NOT NULL,
    last_updated DATETIME(6) NOT NULL,
    CONSTRAINT fk_wrestler_status_wrestler FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id),
    CONSTRAINT fk_wrestler_status_card FOREIGN KEY (status_card_id) REFERENCES status_card(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wrestler_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wrestler_id BIGINT NOT NULL,
    status_card_id BIGINT NOT NULL,
    action VARCHAR(255) NOT NULL,
    old_level INT,
    new_level INT,
    creation_date DATETIME(6) NOT NULL,
    CONSTRAINT fk_wrestler_status_history_wrestler FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id),
    CONSTRAINT fk_wrestler_status_history_card FOREIGN KEY (status_card_id) REFERENCES status_card(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE milestone_status_rewards (
    milestone_id BIGINT NOT NULL,
    status_key VARCHAR(255) NOT NULL,
    PRIMARY KEY (milestone_id, status_key),
    CONSTRAINT fk_milestone_status_milestone FOREIGN KEY (milestone_id) REFERENCES storyline_milestone(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_wrestler_status_wrestler ON wrestler_status(wrestler_id);
CREATE INDEX idx_wrestler_status_history_wrestler ON wrestler_status_history(wrestler_id);
