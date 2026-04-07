CREATE TABLE ringside_action_type (
    ringside_action_type_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    increases_awareness BOOLEAN NOT NULL DEFAULT TRUE,
    can_cause_dq BOOLEAN NOT NULL DEFAULT TRUE,
    base_risk_multiplier DOUBLE NOT NULL DEFAULT 1.0,
    external_id VARCHAR(255),
    last_sync DATETIME(6),
    UNIQUE KEY uk_ringside_action_type_name (name),
    UNIQUE KEY uk_ringside_action_type_external_id (external_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ringside_action (
    ringside_action_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(1000),
    impact INT NOT NULL,
    risk INT NOT NULL,
    alignment VARCHAR(255) NOT NULL,
    ringside_action_type_id BIGINT NOT NULL,
    external_id VARCHAR(255),
    last_sync DATETIME(6),
    CONSTRAINT fk_ringside_action_type FOREIGN KEY (ringside_action_type_id) REFERENCES ringside_action_type(ringside_action_type_id),
    UNIQUE KEY uk_ringside_action_name (name),
    UNIQUE KEY uk_ringside_action_external_id (external_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
