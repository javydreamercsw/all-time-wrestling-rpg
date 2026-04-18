-- Universe-Based State Isolation Migration

-- 1. Create universe table
CREATE TABLE universe (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Create Default Universe
INSERT INTO universe (name, type) VALUES ('Default Universe', 'GLOBAL');

-- 3. Create wrestler_state table (replaces WrestlerState)
CREATE TABLE wrestler_state (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wrestler_id BIGINT NOT NULL,
    universe_id BIGINT NOT NULL,
    fans BIGINT DEFAULT 0,
    tier VARCHAR(255) NOT NULL,
    bumps INT DEFAULT 0,
    current_health INT,
    physical_condition INT DEFAULT 100,
    morale INT DEFAULT 100,
    management_stamina INT DEFAULT 100,
    faction_id BIGINT,
    manager_id BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ws_wrestler FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id),
    CONSTRAINT fk_ws_universe FOREIGN KEY (universe_id) REFERENCES universe(id),
    CONSTRAINT fk_ws_faction FOREIGN KEY (faction_id) REFERENCES faction(id),
    CONSTRAINT fk_ws_manager FOREIGN KEY (manager_id) REFERENCES npc(id),
    UNIQUE KEY uk_wrestler_universe (wrestler_id, universe_id)
);

-- 4. Scoping existing entities to Universe
ALTER TABLE injury ADD COLUMN universe_id BIGINT;
ALTER TABLE injury ADD CONSTRAINT fk_injury_universe FOREIGN KEY (universe_id) REFERENCES universe(id);

ALTER TABLE title ADD COLUMN universe_id BIGINT;
ALTER TABLE title ADD CONSTRAINT fk_title_universe FOREIGN KEY (universe_id) REFERENCES universe(id);

ALTER TABLE faction ADD COLUMN universe_id BIGINT;
ALTER TABLE faction ADD CONSTRAINT fk_faction_universe FOREIGN KEY (universe_id) REFERENCES universe(id);

ALTER TABLE team ADD COLUMN universe_id BIGINT;
ALTER TABLE team ADD CONSTRAINT fk_team_universe FOREIGN KEY (universe_id) REFERENCES universe(id);

ALTER TABLE drama_event ADD COLUMN universe_id BIGINT;
ALTER TABLE drama_event ADD CONSTRAINT fk_drama_universe FOREIGN KEY (universe_id) REFERENCES universe(id);

ALTER TABLE campaign ADD COLUMN universe_id BIGINT;
ALTER TABLE campaign ADD CONSTRAINT fk_campaign_universe FOREIGN KEY (universe_id) REFERENCES universe(id);

ALTER TABLE league ADD COLUMN universe_id BIGINT;
ALTER TABLE league ADD CONSTRAINT fk_league_universe FOREIGN KEY (universe_id) REFERENCES universe(id);

-- 5. Data Migration: Copy global state to the Default Universe (ID 1)
INSERT INTO wrestler_state 
(wrestler_id, universe_id, fans, tier, bumps, current_health, physical_condition, morale, management_stamina, faction_id, manager_id)
SELECT 
w.wrestler_id, 1, w.fans, w.tier, w.bumps, w.current_health, w.physical_condition, w.morale, w.management_stamina, w.faction_id, w.manager_id
FROM wrestler w;

-- Link existing records to the Default Universe
UPDATE injury SET universe_id = 1;
UPDATE title SET universe_id = 1;
UPDATE faction SET universe_id = 1;
UPDATE team SET universe_id = 1;
UPDATE drama_event SET universe_id = 1;
UPDATE campaign SET universe_id = 1;
UPDATE league SET universe_id = 1;
