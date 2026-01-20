ALTER TABLE campaign_state ADD COLUMN matches_played INT DEFAULT 0 NOT NULL;
ALTER TABLE campaign_state ADD COLUMN wins INT DEFAULT 0 NOT NULL;
ALTER TABLE campaign_state ADD COLUMN losses INT DEFAULT 0 NOT NULL;
ALTER TABLE campaign_state ADD COLUMN rival_id BIGINT;
ALTER TABLE campaign_state ADD CONSTRAINT fk_campaign_state_rival FOREIGN KEY (rival_id) REFERENCES npc(id);

CREATE TABLE campaign_encounter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    chapter_number INT NOT NULL,
    narrative_text TEXT NOT NULL,
    player_choice TEXT,
    alignment_shift INT DEFAULT 0 NOT NULL,
    vp_reward INT DEFAULT 0 NOT NULL,
    encounter_date TIMESTAMP NOT NULL,
    FOREIGN KEY (campaign_id) REFERENCES campaign(id)
);

CREATE INDEX idx_campaign_encounter_campaign ON campaign_encounter(campaign_id);
