-- AI Storyline Director Tables
CREATE TABLE campaign_storyline (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    current_milestone_id BIGINT,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    CONSTRAINT fk_storyline_campaign FOREIGN KEY (campaign_id) REFERENCES campaign(id)
);

CREATE TABLE storyline_milestone (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    storyline_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    narrative_goal VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    display_order INT NOT NULL,
    next_on_success_id BIGINT,
    next_on_failure_id BIGINT,
    CONSTRAINT fk_milestone_storyline FOREIGN KEY (storyline_id) REFERENCES campaign_storyline(id),
    CONSTRAINT fk_milestone_next_success FOREIGN KEY (next_on_success_id) REFERENCES storyline_milestone(id),
    CONSTRAINT fk_milestone_next_failure FOREIGN KEY (next_on_failure_id) REFERENCES storyline_milestone(id)
);

-- Add circular reference back to current milestone
ALTER TABLE campaign_storyline 
ADD CONSTRAINT fk_storyline_current_milestone 
FOREIGN KEY (current_milestone_id) REFERENCES storyline_milestone(id);

-- Update campaign_state to track active storyline
ALTER TABLE campaign_state 
ADD COLUMN active_storyline_id BIGINT;

ALTER TABLE campaign_state 
ADD CONSTRAINT fk_campaign_state_storyline 
FOREIGN KEY (active_storyline_id) REFERENCES campaign_storyline(id);
