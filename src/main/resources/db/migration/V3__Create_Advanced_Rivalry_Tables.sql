-- ATW RPG Database Migration V3: Advanced Rivalry Features
-- Creates tables for factions, faction rivalries, multi-wrestler feuds, and storyline branching
-- H2 Database Compatible

-- ==================== FACTION TABLES ====================

-- Create faction table
CREATE TABLE faction (
    faction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(500) NOT NULL UNIQUE,
    description LONGTEXT,
    leader_id BIGINT,
    alignment VARCHAR(20) NOT NULL DEFAULT 'NEUTRAL' CHECK (alignment IN ('FACE', 'HEEL', 'TWEENER', 'NEUTRAL')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    formed_date TIMESTAMP(6) NOT NULL,
    disbanded_date TIMESTAMP(6),
    creation_date TIMESTAMP(6) NOT NULL,
    
    FOREIGN KEY (leader_id) REFERENCES wrestler(wrestler_id) ON DELETE SET NULL
);

-- Add faction_id foreign key constraint to wrestler table
ALTER TABLE wrestler 
ADD CONSTRAINT fk_wrestler_faction 
FOREIGN KEY (faction_id) REFERENCES faction(faction_id) ON DELETE SET NULL;

-- Create faction rivalry table
CREATE TABLE faction_rivalry (
    faction_rivalry_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    faction1_id BIGINT NOT NULL,
    faction2_id BIGINT NOT NULL,
    heat INT NOT NULL DEFAULT 0 CHECK (heat >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    started_date TIMESTAMP(6) NOT NULL,
    ended_date TIMESTAMP(6),
    storyline_notes LONGTEXT,
    creation_date TIMESTAMP(6) NOT NULL,
    
    FOREIGN KEY (faction1_id) REFERENCES faction(faction_id) ON DELETE CASCADE,
    FOREIGN KEY (faction2_id) REFERENCES faction(faction_id) ON DELETE CASCADE,
    UNIQUE (faction1_id, faction2_id),
    CHECK (faction1_id != faction2_id)
);

-- Create faction heat event table
CREATE TABLE faction_heat_event (
    faction_heat_event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    faction_rivalry_id BIGINT NOT NULL,
    heat_change INT NOT NULL,
    heat_after_event INT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    event_date TIMESTAMP(6) NOT NULL,
    creation_date TIMESTAMP(6) NOT NULL,
    
    FOREIGN KEY (faction_rivalry_id) REFERENCES faction_rivalry(faction_rivalry_id) ON DELETE CASCADE
);

-- ==================== MULTI-WRESTLER FEUD TABLES ====================

-- Create multi-wrestler feud table
CREATE TABLE multi_wrestler_feud (
    multi_wrestler_feud_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(500) NOT NULL UNIQUE,
    description LONGTEXT,
    heat INT NOT NULL DEFAULT 0 CHECK (heat >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    started_date TIMESTAMP(6) NOT NULL,
    ended_date TIMESTAMP(6),
    storyline_notes LONGTEXT,
    creation_date TIMESTAMP(6) NOT NULL
);

-- Create feud participant table
CREATE TABLE feud_participant (
    feud_participant_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    feud_id BIGINT NOT NULL,
    wrestler_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'NEUTRAL' CHECK (role IN ('ANTAGONIST', 'PROTAGONIST', 'SECONDARY_ANTAGONIST', 'SECONDARY_PROTAGONIST', 'NEUTRAL', 'WILD_CARD', 'AUTHORITY')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    joined_date TIMESTAMP(6) NOT NULL,
    left_date TIMESTAMP(6),
    left_reason VARCHAR(500),
    creation_date TIMESTAMP(6) NOT NULL,
    
    FOREIGN KEY (feud_id) REFERENCES multi_wrestler_feud(multi_wrestler_feud_id) ON DELETE CASCADE,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE,
    UNIQUE (feud_id, wrestler_id)
);

-- Create feud heat event table
CREATE TABLE feud_heat_event (
    feud_heat_event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    feud_id BIGINT NOT NULL,
    heat_change INT NOT NULL,
    heat_after_event INT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    event_date TIMESTAMP(6) NOT NULL,
    creation_date TIMESTAMP(6) NOT NULL,
    
    FOREIGN KEY (feud_id) REFERENCES multi_wrestler_feud(multi_wrestler_feud_id) ON DELETE CASCADE
);

-- ==================== STORYLINE BRANCHING TABLES ====================

-- Create storyline branch table
CREATE TABLE storyline_branch (
    storyline_branch_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(500) NOT NULL,
    description LONGTEXT,
    branch_type VARCHAR(30) NOT NULL CHECK (branch_type IN ('MATCH_OUTCOME', 'RIVALRY_ESCALATION', 'FACTION_DYNAMICS', 'TITLE_CHANGE', 'INJURY_RESPONSE', 'DRAMA_RESPONSE', 'FAN_REACTION', 'SEASONAL_EVENT', 'EXTERNAL_TRIGGER', 'TIME_BASED')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 1,
    creation_date TIMESTAMP(6) NOT NULL,
    activated_date TIMESTAMP(6),
    completed_date TIMESTAMP(6),
    triggering_match_id BIGINT,
    
    FOREIGN KEY (triggering_match_id) REFERENCES match_result(match_result_id) ON DELETE SET NULL
);

-- Create storyline branch condition table
CREATE TABLE storyline_branch_condition (
    storyline_branch_condition_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    storyline_branch_id BIGINT NOT NULL,
    condition_type VARCHAR(500) NOT NULL,
    condition_key VARCHAR(500) NOT NULL,
    condition_value LONGTEXT,
    is_condition_met BOOLEAN NOT NULL DEFAULT FALSE,
    condition_description VARCHAR(500),
    last_checked_date TIMESTAMP(6),
    met_date TIMESTAMP(6),
    creation_date TIMESTAMP(6) NOT NULL,
    
    FOREIGN KEY (storyline_branch_id) REFERENCES storyline_branch(storyline_branch_id) ON DELETE CASCADE
);

-- Create storyline branch effect table
CREATE TABLE storyline_branch_effect (
    storyline_branch_effect_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    storyline_branch_id BIGINT NOT NULL,
    effect_type VARCHAR(500) NOT NULL,
    effect_key VARCHAR(500) NOT NULL,
    effect_value LONGTEXT,
    is_executed BOOLEAN NOT NULL DEFAULT FALSE,
    execution_order INT NOT NULL DEFAULT 1,
    effect_description VARCHAR(500),
    executed_date TIMESTAMP(6),
    execution_result LONGTEXT,
    creation_date TIMESTAMP(6) NOT NULL,
    
    FOREIGN KEY (storyline_branch_id) REFERENCES storyline_branch(storyline_branch_id) ON DELETE CASCADE
);

-- ==================== CREATE INDEXES ====================

-- Faction system indexes
CREATE INDEX idx_faction_name ON faction (name);
CREATE INDEX idx_faction_active ON faction (is_active);
CREATE INDEX idx_faction_alignment ON faction (alignment);
CREATE INDEX idx_faction_leader ON faction (leader_id);
CREATE INDEX idx_faction_formed_date ON faction (formed_date);
CREATE INDEX idx_faction_rivalry_active ON faction_rivalry (is_active);
CREATE INDEX idx_faction_rivalry_heat ON faction_rivalry (heat);
CREATE INDEX idx_faction_rivalry_started ON faction_rivalry (started_date);
CREATE INDEX idx_faction_rivalry_faction1 ON faction_rivalry (faction1_id);
CREATE INDEX idx_faction_rivalry_faction2 ON faction_rivalry (faction2_id);
CREATE INDEX idx_faction_heat_event_rivalry ON faction_heat_event (faction_rivalry_id);
CREATE INDEX idx_faction_heat_event_date ON faction_heat_event (event_date);
CREATE INDEX idx_faction_heat_event_heat_change ON faction_heat_event (heat_change);

-- Multi-wrestler feud indexes
CREATE INDEX idx_multi_feud_name ON multi_wrestler_feud (name);
CREATE INDEX idx_multi_feud_active ON multi_wrestler_feud (is_active);
CREATE INDEX idx_multi_feud_heat ON multi_wrestler_feud (heat);
CREATE INDEX idx_multi_feud_started ON multi_wrestler_feud (started_date);
CREATE INDEX idx_feud_participant_feud ON feud_participant (feud_id);
CREATE INDEX idx_feud_participant_wrestler ON feud_participant (wrestler_id);
CREATE INDEX idx_feud_participant_role ON feud_participant (role);
CREATE INDEX idx_feud_participant_active ON feud_participant (is_active);
CREATE INDEX idx_feud_participant_joined ON feud_participant (joined_date);
CREATE INDEX idx_feud_heat_event_feud ON feud_heat_event (feud_id);
CREATE INDEX idx_feud_heat_event_date ON feud_heat_event (event_date);
CREATE INDEX idx_feud_heat_event_heat_change ON feud_heat_event (heat_change);

-- Storyline branching indexes
CREATE INDEX idx_storyline_branch_name ON storyline_branch (name);
CREATE INDEX idx_storyline_branch_type ON storyline_branch (branch_type);
CREATE INDEX idx_storyline_branch_active ON storyline_branch (is_active);
CREATE INDEX idx_storyline_branch_priority ON storyline_branch (priority);
CREATE INDEX idx_storyline_branch_created ON storyline_branch (creation_date);
CREATE INDEX idx_storyline_branch_activated ON storyline_branch (activated_date);
CREATE INDEX idx_storyline_branch_completed ON storyline_branch (completed_date);
CREATE INDEX idx_storyline_branch_triggering_match ON storyline_branch (triggering_match_id);
CREATE INDEX idx_storyline_condition_branch ON storyline_branch_condition (storyline_branch_id);
CREATE INDEX idx_storyline_condition_type ON storyline_branch_condition (condition_type);
CREATE INDEX idx_storyline_condition_met ON storyline_branch_condition (is_condition_met);
CREATE INDEX idx_storyline_condition_last_checked ON storyline_branch_condition (last_checked_date);
CREATE INDEX idx_storyline_condition_met_date ON storyline_branch_condition (met_date);
CREATE INDEX idx_storyline_effect_branch ON storyline_branch_effect (storyline_branch_id);
CREATE INDEX idx_storyline_effect_type ON storyline_branch_effect (effect_type);
CREATE INDEX idx_storyline_effect_executed ON storyline_branch_effect (is_executed);
CREATE INDEX idx_storyline_effect_order ON storyline_branch_effect (execution_order);
CREATE INDEX idx_storyline_effect_executed_date ON storyline_branch_effect (executed_date);
