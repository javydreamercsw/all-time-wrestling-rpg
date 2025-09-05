CREATE TABLE npc (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    npc_type VARCHAR(255) NOT NULL,
    external_id VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE (name)
);
