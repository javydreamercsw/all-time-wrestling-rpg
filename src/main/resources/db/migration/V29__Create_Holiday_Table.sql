CREATE TABLE holiday (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    description VARCHAR(255) NOT NULL UNIQUE,
    theme VARCHAR(255) NOT NULL,
    decorations TEXT,
    day_of_month INT,
    holiday_month VARCHAR(255),
    day_of_week VARCHAR(255),
    week_of_month INT,
    type VARCHAR(255) NOT NULL,
    creation_date TIMESTAMP NOT NULL,
    external_id VARCHAR(255) UNIQUE,
    last_sync TIMESTAMP
);
