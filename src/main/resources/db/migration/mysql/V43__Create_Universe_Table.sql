-- Creates the universe table required by V45 (universe_members) and V51 (wrestler_state)
CREATE TABLE IF NOT EXISTS universe (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO universe (id, name, type) VALUES (1, 'Default Universe', 'GLOBAL');
