-- Campaign System Tables

CREATE TABLE campaign (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wrestler_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    started_at DATETIME,
    ended_at DATETIME,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id)
);

CREATE TABLE campaign_state (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    current_chapter_id VARCHAR(255),
    victory_points INT NOT NULL DEFAULT 0,
    skill_tokens INT NOT NULL DEFAULT 0,
    bumps INT NOT NULL DEFAULT 0,
    health_penalty INT NOT NULL DEFAULT 0,
    hand_size_penalty INT NOT NULL DEFAULT 0,
    stamina_penalty INT NOT NULL DEFAULT 0,
    current_phase VARCHAR(50) DEFAULT 'BACKSTAGE' NOT NULL,
    actions_taken INT DEFAULT 0 NOT NULL,
    last_action_type VARCHAR(50),
    last_action_success BOOLEAN DEFAULT TRUE,
    promo_unlocked BOOLEAN DEFAULT FALSE NOT NULL,
    attack_unlocked BOOLEAN DEFAULT FALSE NOT NULL,
    pending_l1_picks INT DEFAULT 0 NOT NULL,
    pending_l2_picks INT DEFAULT 0 NOT NULL,
    pending_l3_picks INT DEFAULT 0 NOT NULL,
    matches_played INT DEFAULT 0 NOT NULL,
    wins INT DEFAULT 0 NOT NULL,
    losses INT DEFAULT 0 NOT NULL,
    rival_id BIGINT,
    finals_phase BOOLEAN DEFAULT FALSE NOT NULL,
    tournament_winner BOOLEAN DEFAULT FALSE NOT NULL,
    failed_to_qualify BOOLEAN DEFAULT FALSE NOT NULL,
    current_match_id BIGINT,
    last_sync DATETIME,
    FOREIGN KEY (campaign_id) REFERENCES campaign(id),
    FOREIGN KEY (rival_id) REFERENCES npc(id),
    FOREIGN KEY (current_match_id) REFERENCES segment(segment_id)
);

CREATE TABLE campaign_completed_chapters (
    campaign_state_id BIGINT NOT NULL,
    chapter_id VARCHAR(255) NOT NULL,
    FOREIGN KEY (campaign_state_id) REFERENCES campaign_state(id)
);

CREATE TABLE wrestler_alignment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wrestler_id BIGINT NOT NULL,
    alignment_type VARCHAR(50) NOT NULL, -- FACE, HEEL or NEUTRAL
    level INT NOT NULL DEFAULT 0,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id)
);

CREATE TABLE backstage_action_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    action_date DATETIME NOT NULL,
    dice_rolled INT NOT NULL,
    successes INT NOT NULL,
    outcome_description TEXT,
    FOREIGN KEY (campaign_id) REFERENCES campaign(id)
);

CREATE TABLE campaign_ability_card (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    alignment_type VARCHAR(50) NOT NULL, -- FACE or HEEL
    level INT NOT NULL,
    effect_script VARCHAR(255),
    one_time_use BOOLEAN DEFAULT TRUE NOT NULL,
    timing VARCHAR(50),
    secondary_effect_script VARCHAR(255),
    secondary_one_time_use BOOLEAN DEFAULT FALSE NOT NULL,
    secondary_timing VARCHAR(50)
);

CREATE TABLE campaign_state_cards (
    campaign_state_id BIGINT NOT NULL,
    card_id BIGINT NOT NULL,
    FOREIGN KEY (campaign_state_id) REFERENCES campaign_state(id),
    FOREIGN KEY (card_id) REFERENCES campaign_ability_card(id)
);

CREATE TABLE campaign_encounter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    chapter_id VARCHAR(255) NOT NULL,
    narrative_text TEXT NOT NULL,
    player_choice TEXT,
    alignment_shift INT DEFAULT 0 NOT NULL,
    vp_reward INT DEFAULT 0 NOT NULL,
    encounter_date DATETIME NOT NULL,
    FOREIGN KEY (campaign_id) REFERENCES campaign(id)
);

CREATE INDEX idx_campaign_wrestler ON campaign(wrestler_id);
CREATE INDEX idx_campaign_state_campaign ON campaign_state(campaign_id);
CREATE INDEX idx_wrestler_alignment_wrestler ON wrestler_alignment(wrestler_id);
CREATE INDEX idx_backstage_action_history_campaign ON backstage_action_history(campaign_id);
CREATE INDEX idx_campaign_ability_card_alignment ON campaign_ability_card(alignment_type, level);
CREATE INDEX idx_campaign_encounter_campaign ON campaign_encounter(campaign_id);
CREATE INDEX idx_campaign_state_current_match ON campaign_state(current_match_id);

-- Updates to existing tables
ALTER TABLE injury ADD COLUMN stamina_penalty INT DEFAULT 0 NOT NULL;
ALTER TABLE injury ADD COLUMN hand_size_penalty INT DEFAULT 0 NOT NULL;

ALTER TABLE wrestler ADD COLUMN drive INT DEFAULT 1 NOT NULL;
ALTER TABLE wrestler ADD COLUMN resilience INT DEFAULT 1 NOT NULL;
ALTER TABLE wrestler ADD COLUMN charisma INT DEFAULT 1 NOT NULL;
ALTER TABLE wrestler ADD COLUMN brawl INT DEFAULT 1 NOT NULL;