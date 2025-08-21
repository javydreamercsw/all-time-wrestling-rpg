-- ATW RPG Database Migration V1: Core Tables
-- Creates the foundational tables for the All-Time Wrestling RPG system
-- H2 Database Compatible

-- ==================== CARD SYSTEM TABLES ====================

-- Create card_set table
CREATE TABLE card_set (
    card_set_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(500) NOT NULL UNIQUE,
    description LONGTEXT,
    release_date DATE,
    creation_date TIMESTAMP(6) NOT NULL
);

-- Create card table
CREATE TABLE card (
    card_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(500) NOT NULL,
    type VARCHAR(100) NOT NULL,
    damage INT NOT NULL DEFAULT 0,
    stamina INT NOT NULL DEFAULT 0,
    momentum INT NOT NULL DEFAULT 0,
    target INT NOT NULL DEFAULT 0,
    number INT NOT NULL,
    is_finisher BOOLEAN NOT NULL DEFAULT FALSE,
    is_signature BOOLEAN NOT NULL DEFAULT FALSE,
    is_pin BOOLEAN NOT NULL DEFAULT FALSE,
    is_taunt BOOLEAN NOT NULL DEFAULT FALSE,
    is_recover BOOLEAN NOT NULL DEFAULT FALSE,
    creation_date TIMESTAMP(6) NOT NULL,
    card_set_id BIGINT NOT NULL,
    
    FOREIGN KEY (card_set_id) REFERENCES card_set(card_set_id) ON DELETE CASCADE,
    UNIQUE (name, number, card_set_id)
);

-- ==================== SHOW SYSTEM TABLES ====================

-- Create season table
CREATE TABLE season (
    season_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(500) NOT NULL UNIQUE,
    description LONGTEXT,
    season_number INT NOT NULL CHECK (season_number >= 1),
    start_date DATE NOT NULL,
    end_date DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    creation_date TIMESTAMP(6) NOT NULL
);

-- Create show_type table
CREATE TABLE show_type (
    show_type_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(500) NOT NULL UNIQUE,
    description LONGTEXT,
    is_ppv BOOLEAN NOT NULL DEFAULT FALSE,
    creation_date TIMESTAMP(6) NOT NULL
);

-- Create show_template table
CREATE TABLE show_template (
    show_template_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(500) NOT NULL UNIQUE,
    description LONGTEXT,
    template_data LONGTEXT,
    creation_date TIMESTAMP(6) NOT NULL
);

-- Create show table
CREATE TABLE show (
    show_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(500) NOT NULL UNIQUE,
    description LONGTEXT NOT NULL,
    show_date DATE NOT NULL,
    creation_date TIMESTAMP(6) NOT NULL,
    show_type_id BIGINT NOT NULL,
    season_id BIGINT,
    template_id BIGINT,
    
    FOREIGN KEY (show_type_id) REFERENCES show_type(show_type_id) ON DELETE RESTRICT,
    FOREIGN KEY (season_id) REFERENCES season(season_id) ON DELETE SET NULL,
    FOREIGN KEY (template_id) REFERENCES show_template(show_template_id) ON DELETE SET NULL
);

-- ==================== MATCH SYSTEM TABLES ====================

-- Create match_type table
CREATE TABLE match_type (
    match_type_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(500) NOT NULL UNIQUE,
    description LONGTEXT,
    creation_date TIMESTAMP(6) NOT NULL
);

-- Create match_rule table
CREATE TABLE match_rule (
    match_rule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(500) NOT NULL UNIQUE,
    description LONGTEXT,
    creation_date TIMESTAMP(6) NOT NULL
);

-- ==================== WRESTLER SYSTEM TABLES ====================

-- Create wrestler table
CREATE TABLE wrestler (
    wrestler_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(500) NOT NULL UNIQUE,
    real_name VARCHAR(500),
    health INT NOT NULL DEFAULT 10 CHECK (health >= 0),
    fans INT NOT NULL DEFAULT 0 CHECK (fans >= 0),
    tier VARCHAR(20) NOT NULL DEFAULT 'JOBBER' CHECK (tier IN ('JOBBER', 'ENHANCEMENT', 'MIDCARD', 'UPPER_MIDCARD', 'MAIN_EVENT', 'LEGEND')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_npc BOOLEAN NOT NULL DEFAULT FALSE,
    debut_date DATE,
    creation_date TIMESTAMP(6) NOT NULL,
    faction_id BIGINT
);

-- Create deck table
CREATE TABLE deck (
    deck_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wrestler_id BIGINT NOT NULL,
    creation_date TIMESTAMP(6) NOT NULL,
    
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE
);

-- Create deck_card table
CREATE TABLE deck_card (
    deck_card_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    deck_id BIGINT NOT NULL,
    card_id BIGINT NOT NULL,
    amount INT NOT NULL DEFAULT 1 CHECK (amount > 0),
    creation_date TIMESTAMP(6) NOT NULL,
    
    FOREIGN KEY (deck_id) REFERENCES deck(deck_id) ON DELETE CASCADE,
    FOREIGN KEY (card_id) REFERENCES card(card_id) ON DELETE CASCADE,
    UNIQUE (deck_id, card_id)
);

-- ==================== TITLE SYSTEM TABLES ====================

-- Create title table
CREATE TABLE title (
    title_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(500) NOT NULL UNIQUE,
    description LONGTEXT,
    tier VARCHAR(20) NOT NULL CHECK (tier IN ('WORLD', 'SECONDARY', 'MIDCARD', 'TAG_TEAM', 'SPECIAL')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_vacant BOOLEAN NOT NULL DEFAULT TRUE,
    current_champion_id BIGINT,
    title_won_date TIMESTAMP(6),
    creation_date TIMESTAMP(6) NOT NULL,
    
    FOREIGN KEY (current_champion_id) REFERENCES wrestler(wrestler_id) ON DELETE SET NULL
);

-- Create title_reign table
CREATE TABLE title_reign (
    title_reign_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title_id BIGINT NOT NULL,
    wrestler_id BIGINT NOT NULL,
    won_date TIMESTAMP(6) NOT NULL,
    lost_date TIMESTAMP(6),
    days_held INT,
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    creation_date TIMESTAMP(6) NOT NULL,

    FOREIGN KEY (title_id) REFERENCES title(title_id) ON DELETE CASCADE,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE
);

-- ==================== CREATE INDEXES ====================

-- Card system indexes
CREATE INDEX idx_card_set_name ON card_set (name);
CREATE INDEX idx_card_set_release_date ON card_set (release_date);
CREATE INDEX idx_card_name ON card (name);
CREATE INDEX idx_card_type ON card (type);
CREATE INDEX idx_card_number ON card (number);
CREATE INDEX idx_card_set ON card (card_set_id);

-- Show system indexes
CREATE INDEX idx_season_name ON season (name);
CREATE INDEX idx_season_number ON season (season_number);
CREATE INDEX idx_season_active ON season (is_active);
CREATE INDEX idx_season_dates ON season (start_date, end_date);
CREATE INDEX idx_show_type_name ON show_type (name);
CREATE INDEX idx_show_type_ppv ON show_type (is_ppv);
CREATE INDEX idx_show_template_name ON show_template (name);
CREATE INDEX idx_show_name ON show (name);
CREATE INDEX idx_show_date ON show (show_date);
CREATE INDEX idx_show_type ON show (show_type_id);
CREATE INDEX idx_show_season ON show (season_id);

-- Match system indexes
CREATE INDEX idx_match_type_name ON match_type (name);
CREATE INDEX idx_match_rule_name ON match_rule (name);

-- Wrestler system indexes
CREATE INDEX idx_wrestler_name ON wrestler (name);
CREATE INDEX idx_wrestler_tier ON wrestler (tier);
CREATE INDEX idx_wrestler_active ON wrestler (is_active);
CREATE INDEX idx_wrestler_npc ON wrestler (is_npc);
CREATE INDEX idx_wrestler_faction ON wrestler (faction_id);
CREATE INDEX idx_deck_wrestler ON deck (wrestler_id);
CREATE INDEX idx_deck_card_deck ON deck_card (deck_id);
CREATE INDEX idx_deck_card_card ON deck_card (card_id);

-- Title system indexes
CREATE INDEX idx_title_name ON title (name);
CREATE INDEX idx_title_tier ON title (tier);
CREATE INDEX idx_title_active ON title (is_active);
CREATE INDEX idx_title_vacant ON title (is_vacant);
CREATE INDEX idx_title_champion ON title (current_champion_id);
CREATE INDEX idx_title_reign_title ON title_reign (title_id);
CREATE INDEX idx_title_reign_wrestler ON title_reign (wrestler_id);
CREATE INDEX idx_title_reign_current ON title_reign (is_current);
CREATE INDEX idx_title_reign_dates ON title_reign (won_date, lost_date);
