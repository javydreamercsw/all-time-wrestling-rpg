CREATE TABLE news_item (
    news_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    headline VARCHAR(255) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    publish_date TIMESTAMP NOT NULL,
    category VARCHAR(50) NOT NULL,
    is_rumor BOOLEAN NOT NULL DEFAULT FALSE,
    importance INT NOT NULL DEFAULT 3,
    external_id VARCHAR(255) UNIQUE,
    last_sync TIMESTAMP NULL
);
