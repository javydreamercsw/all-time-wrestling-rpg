CREATE TABLE inbox_item (
    inbox_item_id BIGINT NOT NULL AUTO_INCREMENT,
    event_type VARCHAR(255) NOT NULL,
    description VARCHAR(1024) NOT NULL,
    event_timestamp TIMESTAMP NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (inbox_item_id)
);
