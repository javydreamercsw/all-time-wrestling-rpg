-- Creates the wrestler_state table required by V47 (Deactivate_Same_Faction_Rivalries)
-- V51 (Decouple_Wrestler_State) uses CREATE TABLE IF NOT EXISTS so it will safely skip this
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

-- Populate wrestler_state from existing wrestler data for the Default Universe.
-- Conditional: skipped if wrestler.fans was already dropped by V52 (idempotent with V51).
SET @s = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'wrestler' AND column_name = 'fans') > 0,
    'INSERT INTO wrestler_state (wrestler_id, universe_id, fans, tier, bumps, current_health, physical_condition, morale, management_stamina, faction_id, manager_id) SELECT w.wrestler_id, 1, w.fans, w.tier, w.bumps, w.current_health, w.physical_condition, w.morale, w.management_stamina, w.faction_id, w.manager_id FROM wrestler w WHERE NOT EXISTS (SELECT 1 FROM wrestler_state ws WHERE ws.wrestler_id = w.wrestler_id AND ws.universe_id = 1)',
    'SELECT 1'
);
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
