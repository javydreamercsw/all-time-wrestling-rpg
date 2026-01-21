CREATE TABLE campaign_upgrade (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    type VARCHAR(50) NOT NULL,
    sub_type VARCHAR(50)
);

CREATE TABLE campaign_state_upgrades (
    campaign_state_id BIGINT NOT NULL,
    upgrade_id BIGINT NOT NULL,
    PRIMARY KEY (campaign_state_id, upgrade_id),
    CONSTRAINT fk_csu_state FOREIGN KEY (campaign_state_id) REFERENCES campaign_state(id),
    CONSTRAINT fk_csu_upgrade FOREIGN KEY (upgrade_id) REFERENCES campaign_upgrade(id)
);

ALTER TABLE wrestler_alignment ADD COLUMN campaign_id BIGINT;
ALTER TABLE wrestler_alignment ADD CONSTRAINT fk_wa_campaign FOREIGN KEY (campaign_id) REFERENCES campaign(id);