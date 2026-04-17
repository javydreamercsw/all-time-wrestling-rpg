-- Phase 1 & 2: Schema Decoupling and Data Migration

-- 1. Create a Global Universe league if no leagues exist
INSERT INTO league (name, status, max_picks_per_player, budget, locker_room_morale)
SELECT 'Global Universe', 'ACTIVE', 100, 1000000.00, 100
WHERE NOT EXISTS (SELECT 1 FROM league);

-- 2. Create wrestler_league_state table
CREATE TABLE wrestler_league_state (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wrestler_id BIGINT NOT NULL,
    league_id BIGINT NOT NULL,
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
    CONSTRAINT fk_wls_wrestler FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id),
    CONSTRAINT fk_wls_league FOREIGN KEY (league_id) REFERENCES league(id),
    CONSTRAINT fk_wls_faction FOREIGN KEY (faction_id) REFERENCES faction(id),
    CONSTRAINT fk_wls_manager FOREIGN KEY (manager_id) REFERENCES npc(id),
    UNIQUE KEY uk_wrestler_league (wrestler_id, league_id)
);

-- 3. Add league_id to injury and title
ALTER TABLE injury ADD COLUMN league_id BIGINT;
ALTER TABLE injury ADD CONSTRAINT fk_injury_league FOREIGN KEY (league_id) REFERENCES league(id);

ALTER TABLE title ADD COLUMN league_id BIGINT;
ALTER TABLE title ADD CONSTRAINT fk_title_league FOREIGN KEY (league_id) REFERENCES league(id);

-- 4. Data Migration: Copy global state to the first league
-- This ensures no data is lost during the refactor.
INSERT INTO wrestler_league_state 
(wrestler_id, league_id, fans, tier, bumps, current_health, physical_condition, morale, management_stamina, faction_id, manager_id)
SELECT 
w.wrestler_id, (SELECT id FROM league ORDER BY id ASC LIMIT 1), w.fans, w.tier, w.bumps, w.current_health, w.physical_condition, w.morale, w.management_stamina, w.faction_id, w.manager_id
FROM wrestler w;

-- 5. Update injuries and titles to point to the first league
UPDATE injury SET league_id = (SELECT id FROM league ORDER BY id ASC LIMIT 1) WHERE league_id IS NULL;
UPDATE title SET league_id = (SELECT id FROM league ORDER BY id ASC LIMIT 1) WHERE league_id IS NULL;
