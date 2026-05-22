-- Universe-Based State Isolation Migration (idempotent)

-- 1. Create universe table
CREATE TABLE IF NOT EXISTS universe (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Create Default Universe (only if not already present)
INSERT IGNORE INTO universe (id, name, type) VALUES (1, 'Default Universe', 'GLOBAL');

-- 3. Create wrestler_state table
CREATE TABLE IF NOT EXISTS wrestler_state (
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
    CONSTRAINT fk_ws_faction FOREIGN KEY (faction_id) REFERENCES faction(faction_id),
    CONSTRAINT fk_ws_manager FOREIGN KEY (manager_id) REFERENCES npc(id),
    UNIQUE KEY uk_wrestler_universe (wrestler_id, universe_id)
);

-- 4. Add universe_id to scoped entities (idempotent via prepared statements)
SET @s = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='injury' AND column_name='universe_id')=0,
    'ALTER TABLE injury ADD COLUMN universe_id BIGINT, ADD CONSTRAINT fk_injury_universe FOREIGN KEY (universe_id) REFERENCES universe(id)',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='title' AND column_name='universe_id')=0,
    'ALTER TABLE title ADD COLUMN universe_id BIGINT, ADD CONSTRAINT fk_title_universe FOREIGN KEY (universe_id) REFERENCES universe(id)',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='faction' AND column_name='universe_id')=0,
    'ALTER TABLE faction ADD COLUMN universe_id BIGINT, ADD CONSTRAINT fk_faction_universe FOREIGN KEY (universe_id) REFERENCES universe(id)',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='team' AND column_name='universe_id')=0,
    'ALTER TABLE team ADD COLUMN universe_id BIGINT, ADD CONSTRAINT fk_team_universe FOREIGN KEY (universe_id) REFERENCES universe(id)',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='drama_event' AND column_name='universe_id')=0,
    'ALTER TABLE drama_event ADD COLUMN universe_id BIGINT, ADD CONSTRAINT fk_drama_universe FOREIGN KEY (universe_id) REFERENCES universe(id)',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='campaign' AND column_name='universe_id')=0,
    'ALTER TABLE campaign ADD COLUMN universe_id BIGINT, ADD CONSTRAINT fk_campaign_universe FOREIGN KEY (universe_id) REFERENCES universe(id)',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='league' AND column_name='universe_id')=0,
    'ALTER TABLE league ADD COLUMN universe_id BIGINT, ADD CONSTRAINT fk_league_universe FOREIGN KEY (universe_id) REFERENCES universe(id)',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='wrestling_show' AND column_name='universe_id')=0,
    'ALTER TABLE wrestling_show ADD COLUMN universe_id BIGINT, ADD CONSTRAINT fk_wrestling_show_universe FOREIGN KEY (universe_id) REFERENCES universe(id)',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 5. Data Migration: populate wrestler_state from wrestler (skip already-migrated wrestlers)
INSERT INTO wrestler_state
(wrestler_id, universe_id, fans, tier, bumps, current_health, physical_condition, morale, management_stamina, faction_id, manager_id)
SELECT
w.wrestler_id, 1, w.fans, w.tier, w.bumps, w.current_health, w.physical_condition, w.morale, w.management_stamina, w.faction_id, w.manager_id
FROM wrestler w
WHERE NOT EXISTS (SELECT 1 FROM wrestler_state ws WHERE ws.wrestler_id = w.wrestler_id AND ws.universe_id = 1);

-- Link existing records to the Default Universe (safe to re-run, NULL rows only)
UPDATE faction SET universe_id = 1 WHERE universe_id IS NULL;
UPDATE team SET universe_id = 1 WHERE universe_id IS NULL;
UPDATE drama_event SET universe_id = 1 WHERE universe_id IS NULL;
UPDATE campaign SET universe_id = 1 WHERE universe_id IS NULL;
UPDATE league SET universe_id = 1 WHERE universe_id IS NULL;
UPDATE wrestling_show SET universe_id = 1 WHERE universe_id IS NULL;

-- Add league_id to rivalry
SET @s = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='rivalry' AND column_name='league_id')=0,
    'ALTER TABLE rivalry ADD COLUMN league_id BIGINT, ADD CONSTRAINT fk_rivalry_league FOREIGN KEY (league_id) REFERENCES league(id)',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
