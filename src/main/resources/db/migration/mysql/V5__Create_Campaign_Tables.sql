CREATE TABLE campaign (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wrestler_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id)
);

CREATE TABLE campaign_state (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    current_chapter INT NOT NULL DEFAULT 1,
    victory_points INT NOT NULL DEFAULT 0,
    skill_tokens INT NOT NULL DEFAULT 0,
    bumps INT NOT NULL DEFAULT 0,
    health_penalty INT NOT NULL DEFAULT 0,
    hand_size_penalty INT NOT NULL DEFAULT 0,
    stamina_penalty INT NOT NULL DEFAULT 0,
    last_sync TIMESTAMP,
    FOREIGN KEY (campaign_id) REFERENCES campaign(id)
);

CREATE TABLE wrestler_alignment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wrestler_id BIGINT NOT NULL,
    alignment_type VARCHAR(50) NOT NULL, -- FACE or HEEL
    level INT NOT NULL DEFAULT 0,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id)
);

CREATE TABLE backstage_action_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    action_date TIMESTAMP NOT NULL,
    dice_rolled INT NOT NULL,
    successes INT NOT NULL,
    outcome_description TEXT,
    FOREIGN KEY (campaign_id) REFERENCES campaign(id)
);

CREATE INDEX idx_campaign_wrestler ON campaign(wrestler_id);
CREATE INDEX idx_campaign_state_campaign ON campaign_state(campaign_id);
CREATE INDEX idx_wrestler_alignment_wrestler ON wrestler_alignment(wrestler_id);
CREATE INDEX idx_backstage_action_history_campaign ON backstage_action_history(campaign_id);