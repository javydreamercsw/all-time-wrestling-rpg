-- ATW RPG Database Migration V2: Match and Rivalry Tables
-- Creates tables for matches, match results, and rivalry system
-- H2 Database Compatible

-- ==================== MATCH SYSTEM TABLES ====================

-- Create match table
CREATE TABLE match_table (
    match_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    show_id BIGINT NOT NULL,
    match_type_id BIGINT NOT NULL,
    match_order INT NOT NULL DEFAULT 1,
    is_title_match BOOLEAN NOT NULL DEFAULT FALSE,
    creation_date TIMESTAMP(6) NOT NULL,
    
    FOREIGN KEY (show_id) REFERENCES show(show_id) ON DELETE CASCADE,
    FOREIGN KEY (match_type_id) REFERENCES match_type(match_type_id) ON DELETE RESTRICT
);

-- Create match_result table
CREATE TABLE match_result (
    match_result_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    show_id BIGINT NOT NULL,
    match_type_id BIGINT NOT NULL,
    winner_id BIGINT,
    match_date TIMESTAMP(6) NOT NULL,
    is_title_match BOOLEAN NOT NULL DEFAULT FALSE,
    is_npc_generated BOOLEAN NOT NULL DEFAULT FALSE,
    notes LONGTEXT,
    creation_date TIMESTAMP(6) NOT NULL,
    
    FOREIGN KEY (show_id) REFERENCES show(show_id) ON DELETE CASCADE,
    FOREIGN KEY (match_type_id) REFERENCES match_type(match_type_id) ON DELETE RESTRICT,
    FOREIGN KEY (winner_id) REFERENCES wrestler(wrestler_id) ON DELETE SET NULL
);

-- Create match_participant table
CREATE TABLE match_participant (
    match_participant_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    match_result_id BIGINT NOT NULL,
    wrestler_id BIGINT NOT NULL,
    is_winner BOOLEAN NOT NULL DEFAULT FALSE,
    creation_date TIMESTAMP(6) NOT NULL,
    
    FOREIGN KEY (match_result_id) REFERENCES match_result(match_result_id) ON DELETE CASCADE,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE,
    UNIQUE (match_result_id, wrestler_id)
);

-- Create match_result_match_rule junction table (many-to-many)
CREATE TABLE match_result_match_rule (
    match_result_id BIGINT NOT NULL,
    match_rule_id BIGINT NOT NULL,
    
    PRIMARY KEY (match_result_id, match_rule_id),
    FOREIGN KEY (match_result_id) REFERENCES match_result(match_result_id) ON DELETE CASCADE,
    FOREIGN KEY (match_rule_id) REFERENCES match_rule(match_rule_id) ON DELETE CASCADE
);

-- ==================== RIVALRY SYSTEM TABLES ====================

-- Create rivalry table
CREATE TABLE rivalry (
    rivalry_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wrestler1_id BIGINT NOT NULL,
    wrestler2_id BIGINT NOT NULL,
    heat INT NOT NULL DEFAULT 0 CHECK (heat >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    started_date TIMESTAMP(6) NOT NULL,
    ended_date TIMESTAMP(6),
    storyline_notes LONGTEXT,
    creation_date TIMESTAMP(6) NOT NULL,
    
    FOREIGN KEY (wrestler1_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE,
    FOREIGN KEY (wrestler2_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE,
    UNIQUE (wrestler1_id, wrestler2_id),
    CHECK (wrestler1_id != wrestler2_id)
);

-- Create heat_event table
CREATE TABLE heat_event (
    heat_event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rivalry_id BIGINT NOT NULL,
    heat_change INT NOT NULL,
    heat_after_event INT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    event_date TIMESTAMP(6) NOT NULL,
    creation_date TIMESTAMP(6) NOT NULL,
    
    FOREIGN KEY (rivalry_id) REFERENCES rivalry(rivalry_id) ON DELETE CASCADE
);

-- ==================== INJURY SYSTEM TABLES ====================

-- Create injury table
CREATE TABLE injury (
    injury_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wrestler_id BIGINT NOT NULL,
    injury_type VARCHAR(20) NOT NULL DEFAULT 'MINOR' CHECK (injury_type IN ('MINOR', 'MODERATE', 'MAJOR', 'SEVERE')),
    severity INT NOT NULL DEFAULT 1 CHECK (severity >= 1 AND severity <= 5),
    health_reduction INT NOT NULL DEFAULT 1 CHECK (health_reduction >= 0),
    is_healed BOOLEAN NOT NULL DEFAULT FALSE,
    injury_date TIMESTAMP(6) NOT NULL,
    healed_date TIMESTAMP(6),
    description VARCHAR(500),
    creation_date TIMESTAMP(6) NOT NULL,
    
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE
);

-- ==================== DRAMA SYSTEM TABLES ====================

-- Create drama_event table
CREATE TABLE drama_event (
    drama_event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(500) NOT NULL,
    description LONGTEXT,
    drama_type VARCHAR(30) NOT NULL CHECK (drama_type IN ('BACKSTAGE_INCIDENT', 'INJURY_ANGLE', 'BETRAYAL', 'ALLIANCE_FORMATION', 'TITLE_CONTROVERSY', 'SUSPENSION', 'RETURN', 'RETIREMENT', 'DEBUT', 'OTHER')),
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    event_date TIMESTAMP(6) NOT NULL,
    is_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolution_notes LONGTEXT,
    creation_date TIMESTAMP(6) NOT NULL
);

-- Create drama_event_wrestler junction table (many-to-many)
CREATE TABLE drama_event_wrestler (
    drama_event_id BIGINT NOT NULL,
    wrestler_id BIGINT NOT NULL,
    
    PRIMARY KEY (drama_event_id, wrestler_id),
    FOREIGN KEY (drama_event_id) REFERENCES drama_event(drama_event_id) ON DELETE CASCADE,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE
);

-- ==================== CREATE INDEXES ====================

-- Match system indexes
CREATE INDEX idx_match_show ON match_table (show_id);
CREATE INDEX idx_match_type ON match_table (match_type_id);
CREATE INDEX idx_match_order ON match_table (match_order);
CREATE INDEX idx_match_title ON match_table (is_title_match);
CREATE INDEX idx_match_result_show ON match_result (show_id);
CREATE INDEX idx_match_result_type ON match_result (match_type_id);
CREATE INDEX idx_match_result_winner ON match_result (winner_id);
CREATE INDEX idx_match_result_date ON match_result (match_date);
CREATE INDEX idx_match_result_title ON match_result (is_title_match);
CREATE INDEX idx_match_result_npc ON match_result (is_npc_generated);
CREATE INDEX idx_match_participant_match ON match_participant (match_result_id);
CREATE INDEX idx_match_participant_wrestler ON match_participant (wrestler_id);
CREATE INDEX idx_match_participant_winner ON match_participant (is_winner);
CREATE INDEX idx_match_result_rule_match ON match_result_match_rule (match_result_id);
CREATE INDEX idx_match_result_rule_rule ON match_result_match_rule (match_rule_id);

-- Rivalry system indexes
CREATE INDEX idx_rivalry_wrestler1 ON rivalry (wrestler1_id);
CREATE INDEX idx_rivalry_wrestler2 ON rivalry (wrestler2_id);
CREATE INDEX idx_rivalry_active ON rivalry (is_active);
CREATE INDEX idx_rivalry_heat ON rivalry (heat);
CREATE INDEX idx_rivalry_started ON rivalry (started_date);
CREATE INDEX idx_heat_event_rivalry ON heat_event (rivalry_id);
CREATE INDEX idx_heat_event_date ON heat_event (event_date);
CREATE INDEX idx_heat_event_heat_change ON heat_event (heat_change);

-- Injury system indexes
CREATE INDEX idx_injury_wrestler ON injury (wrestler_id);
CREATE INDEX idx_injury_type ON injury (injury_type);
CREATE INDEX idx_injury_severity ON injury (severity);
CREATE INDEX idx_injury_healed ON injury (is_healed);
CREATE INDEX idx_injury_date ON injury (injury_date);

-- Drama system indexes
CREATE INDEX idx_drama_event_name ON drama_event (name);
CREATE INDEX idx_drama_event_type ON drama_event (drama_type);
CREATE INDEX idx_drama_event_severity ON drama_event (severity);
CREATE INDEX idx_drama_event_date ON drama_event (event_date);
CREATE INDEX idx_drama_event_resolved ON drama_event (is_resolved);
CREATE INDEX idx_drama_wrestler_event ON drama_event_wrestler (drama_event_id);
CREATE INDEX idx_drama_wrestler_wrestler ON drama_event_wrestler (wrestler_id);
