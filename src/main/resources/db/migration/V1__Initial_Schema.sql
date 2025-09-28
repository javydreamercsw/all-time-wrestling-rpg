-- ========== Initial Schema for All-Time Wrestling RPG ==========
-- This single script defines the complete database schema for the first release,
-- consolidating and correcting all previous migration scripts to match the
-- current JPA entity definitions.

-- =================================================================
-- CORE WRESTLER & FACTION SYSTEM
-- =================================================================

CREATE TABLE faction (
    faction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description LONGTEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    leader_id BIGINT,
    formed_date TIMESTAMP,
    disbanded_date TIMESTAMP,
    creation_date TIMESTAMP NOT NULL,
    external_id VARCHAR(255) UNIQUE
    -- Note: leader_id cannot have a foreign key constraint yet as wrestler table does not exist.
);

CREATE TABLE wrestler (
    wrestler_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    starting_stamina INT NOT NULL,
    low_stamina INT NOT NULL,
    starting_health INT NOT NULL,
    low_health INT NOT NULL,
    deck_size INT NOT NULL,
    creation_date TIMESTAMP NOT NULL,
    external_id VARCHAR(255) UNIQUE,
    fans BIGINT DEFAULT 0,
    tier VARCHAR(255) NOT NULL,
    bumps INT DEFAULT 0,
    current_health INT,
    is_player BOOLEAN NOT NULL,
    gender VARCHAR(255),
    description VARCHAR(4000),
    faction_id BIGINT,
    FOREIGN KEY (faction_id) REFERENCES faction(faction_id) ON DELETE SET NULL
);

-- Add the foreign key constraint for faction's leader
ALTER TABLE faction ADD CONSTRAINT fk_faction_leader FOREIGN KEY (leader_id) REFERENCES wrestler(wrestler_id) ON DELETE SET NULL;

CREATE TABLE injury (
    injury_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wrestler_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description LONGTEXT,
    severity VARCHAR(255) NOT NULL,
    health_penalty INT NOT NULL,
    is_active BOOLEAN NOT NULL,
    injury_date TIMESTAMP NOT NULL,
    healed_date TIMESTAMP,
    healing_cost BIGINT NOT NULL,
    injury_notes LONGTEXT,
    creation_date TIMESTAMP NOT NULL,
    external_id VARCHAR(255) UNIQUE,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE
);

CREATE TABLE npc (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    npc_type VARCHAR(255) NOT NULL,
    external_id VARCHAR(255)
);

-- =================================================================
-- CARD & DECK SYSTEM
-- =================================================================

CREATE TABLE card_set (
    set_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    set_code VARCHAR(255) NOT NULL UNIQUE,
    description LONGTEXT,
    release_date DATE,
    creation_date TIMESTAMP NOT NULL
);

CREATE TABLE card (
    card_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    damage INT NOT NULL DEFAULT 0,
    stamina INT NOT NULL DEFAULT 0,
    momentum INT NOT NULL DEFAULT 0,
    target INT NOT NULL DEFAULT 0,
    number INT NOT NULL,
    finisher BOOLEAN NOT NULL DEFAULT FALSE,
    signature BOOLEAN NOT NULL DEFAULT FALSE,
    pin BOOLEAN NOT NULL DEFAULT FALSE,
    taunt BOOLEAN NOT NULL DEFAULT FALSE,
    recover BOOLEAN NOT NULL DEFAULT FALSE,
    creation_date TIMESTAMP NOT NULL,
    set_id BIGINT NOT NULL,
    FOREIGN KEY (set_id) REFERENCES card_set(set_id) ON DELETE CASCADE,
    UNIQUE (name, number, set_id)
);

CREATE TABLE deck (
    deck_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wrestler_id BIGINT NOT NULL,
    creation_date TIMESTAMP NOT NULL,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE
);

CREATE TABLE deck_card (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    deck_id BIGINT NOT NULL,
    card_id BIGINT NOT NULL,
    amount INT NOT NULL DEFAULT 1,
    creation_date TIMESTAMP NOT NULL,
    FOREIGN KEY (deck_id) REFERENCES deck(deck_id) ON DELETE CASCADE,
    FOREIGN KEY (card_id) REFERENCES card(card_id) ON DELETE CASCADE,
    UNIQUE (deck_id, card_id)
);

-- =================================================================
-- SHOW & SEASON SYSTEM
-- =================================================================

CREATE TABLE season (
    season_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description LONGTEXT,
    start_date DATE NOT NULL,
    end_date DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL,
    notion_id VARCHAR(255),
    shows_per_ppv INT DEFAULT 5 NOT NULL
);

CREATE TABLE show_type (
    show_type_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description LONGTEXT,
    is_ppv BOOLEAN NOT NULL DEFAULT FALSE,
    creation_date TIMESTAMP NOT NULL
);

CREATE TABLE show_template (
    template_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description LONGTEXT,
    show_type_id BIGINT NOT NULL,
    notion_url VARCHAR(500),
    external_id VARCHAR(255) UNIQUE,
    creation_date TIMESTAMP NOT NULL,
    FOREIGN KEY (show_type_id) REFERENCES show_type(show_type_id) ON DELETE RESTRICT
);

CREATE TABLE show (
    show_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description LONGTEXT,
    show_date TIMESTAMP,
    show_type_id BIGINT NOT NULL,
    season_id BIGINT,
    template_id BIGINT,
    external_id VARCHAR(255) UNIQUE,
    creation_date TIMESTAMP NOT NULL,
    FOREIGN KEY (show_type_id) REFERENCES show_type(show_type_id) ON DELETE RESTRICT,
    FOREIGN KEY (season_id) REFERENCES season(season_id) ON DELETE SET NULL,
    FOREIGN KEY (template_id) REFERENCES show_template(template_id) ON DELETE SET NULL
);

-- =================================================================
-- SEGMENT SYSTEM
-- =================================================================

CREATE TABLE segment_type (
    segment_type_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description LONGTEXT,
    creation_date TIMESTAMP NOT NULL
);

CREATE TABLE segment_rule (
    segment_rule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description LONGTEXT,
    requires_high_heat BOOLEAN NOT NULL DEFAULT FALSE,
    creation_date TIMESTAMP NOT NULL
);

CREATE TABLE segment (
    segment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    show_id BIGINT NOT NULL,
    segment_type_id BIGINT NOT NULL,
    winner_id BIGINT,
    segment_date TIMESTAMP NOT NULL,
    duration_minutes INT,
    segment_rating INT,
    status VARCHAR(255) NOT NULL,
    narration LONGTEXT,
    is_title_segment BOOLEAN NOT NULL DEFAULT FALSE,
    is_npc_generated BOOLEAN NOT NULL DEFAULT FALSE,
    external_id VARCHAR(255) UNIQUE,
    FOREIGN KEY (show_id) REFERENCES show(show_id) ON DELETE CASCADE,
    FOREIGN KEY (segment_type_id) REFERENCES segment_type(segment_type_id) ON DELETE RESTRICT,
    FOREIGN KEY (winner_id) REFERENCES wrestler(wrestler_id) ON DELETE SET NULL
);

CREATE TABLE segment_participant (
    segment_participant_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    segment_id BIGINT NOT NULL,
    wrestler_id BIGINT NOT NULL,
    is_winner BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (segment_id) REFERENCES segment(segment_id) ON DELETE CASCADE,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE,
    UNIQUE (segment_id, wrestler_id)
);

CREATE TABLE segment_segment_rule (
    segment_id BIGINT NOT NULL,
    segment_rule_id BIGINT NOT NULL,
    PRIMARY KEY (segment_id, segment_rule_id),
    FOREIGN KEY (segment_id) REFERENCES segment(segment_id) ON DELETE CASCADE,
    FOREIGN KEY (segment_rule_id) REFERENCES segment_rule(segment_rule_id) ON DELETE CASCADE
);

CREATE TABLE title (
    title_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description LONGTEXT,
    tier VARCHAR(255),
    gender VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL,
    external_id VARCHAR(255) UNIQUE
);

CREATE TABLE segment_title (
    segment_id BIGINT NOT NULL,
    title_id BIGINT NOT NULL,
    PRIMARY KEY (segment_id, title_id),
    FOREIGN KEY (segment_id) REFERENCES segment(segment_id) ON DELETE CASCADE,
    FOREIGN KEY (title_id) REFERENCES title(title_id) ON DELETE CASCADE
);

-- =================================================================
-- TITLE SYSTEM
-- =================================================================

CREATE TABLE title_champion (
    title_id BIGINT NOT NULL,
    wrestler_id BIGINT NOT NULL,
    PRIMARY KEY (title_id, wrestler_id),
    FOREIGN KEY (title_id) REFERENCES title(title_id) ON DELETE CASCADE,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE
);

CREATE TABLE title_contender (
    title_id BIGINT NOT NULL,
    wrestler_id BIGINT NOT NULL,
    PRIMARY KEY (title_id, wrestler_id),
    FOREIGN KEY (title_id) REFERENCES title(title_id) ON DELETE CASCADE,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE
);

CREATE TABLE title_reign (
    title_reign_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_id VARCHAR(255),
    title_id BIGINT NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    reign_number INT NOT NULL DEFAULT 1,
    notes LONGTEXT,
    creation_date TIMESTAMP NOT NULL,
    FOREIGN KEY (title_id) REFERENCES title(title_id) ON DELETE CASCADE
);

CREATE TABLE title_reign_champion (
    title_reign_id BIGINT NOT NULL,
    wrestler_id BIGINT NOT NULL,
    PRIMARY KEY (title_reign_id, wrestler_id),
    FOREIGN KEY (title_reign_id) REFERENCES title_reign(title_reign_id) ON DELETE CASCADE,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE
);

-- =================================================================
-- RIVALRY & STORYLINE SYSTEM
-- =================================================================

CREATE TABLE rivalry (
    rivalry_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wrestler1_id BIGINT NOT NULL,
    wrestler2_id BIGINT NOT NULL,
    heat INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    started_date TIMESTAMP NOT NULL,
    ended_date TIMESTAMP,
    storyline_notes LONGTEXT,
    creation_date TIMESTAMP NOT NULL,
    FOREIGN KEY (wrestler1_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE,
    FOREIGN KEY (wrestler2_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE,
    UNIQUE (wrestler1_id, wrestler2_id),
    CHECK (wrestler1_id <> wrestler2_id)
);

CREATE TABLE heat_event (
    heat_event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rivalry_id BIGINT NOT NULL,
    heat_change INT NOT NULL,
    heat_after_event INT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    event_date TIMESTAMP NOT NULL,
    creation_date TIMESTAMP NOT NULL,
    FOREIGN KEY (rivalry_id) REFERENCES rivalry(rivalry_id) ON DELETE CASCADE
);

CREATE TABLE faction_rivalry (
    faction_rivalry_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    faction1_id BIGINT NOT NULL,
    faction2_id BIGINT NOT NULL,
    heat INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    started_date TIMESTAMP NOT NULL,
    ended_date TIMESTAMP,
    storyline_notes LONGTEXT,
    creation_date TIMESTAMP NOT NULL,
    FOREIGN KEY (faction1_id) REFERENCES faction(faction_id) ON DELETE CASCADE,
    FOREIGN KEY (faction2_id) REFERENCES faction(faction_id) ON DELETE CASCADE,
    UNIQUE (faction1_id, faction2_id),
    CHECK (faction1_id <> faction2_id)
);

CREATE TABLE drama_event (
    drama_event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description LONGTEXT NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    severity VARCHAR(255) NOT NULL,
    event_date TIMESTAMP NOT NULL,
    creation_date TIMESTAMP NOT NULL,
    heat_impact INT,
    fan_impact INT,
    injury_caused BOOLEAN NOT NULL,
    rivalry_created BOOLEAN NOT NULL,
    rivalry_ended BOOLEAN NOT NULL,
    is_processed BOOLEAN NOT NULL,
    processed_date TIMESTAMP,
    processing_notes LONGTEXT,
    primary_wrestler_id BIGINT,
    secondary_wrestler_id BIGINT,
    FOREIGN KEY (primary_wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE SET NULL,
    FOREIGN KEY (secondary_wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE SET NULL
);

-- =================================================================
-- MULTI-WRESTLER FEUD SYSTEM (ADDED)
-- =================================================================

CREATE TABLE multi_wrestler_feud (
    multi_wrestler_feud_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description LONGTEXT,
    heat INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    started_date TIMESTAMP NOT NULL,
    ended_date TIMESTAMP,
    storyline_notes LONGTEXT,
    creation_date TIMESTAMP NOT NULL
);

CREATE TABLE feud_participant (
    feud_participant_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    feud_id BIGINT NOT NULL,
    wrestler_id BIGINT NOT NULL,
    role VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    joined_date TIMESTAMP NOT NULL,
    left_date TIMESTAMP,
    left_reason VARCHAR(255),
    creation_date TIMESTAMP NOT NULL,
    FOREIGN KEY (feud_id) REFERENCES multi_wrestler_feud(multi_wrestler_feud_id) ON DELETE CASCADE,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE,
    UNIQUE (feud_id, wrestler_id)
);

CREATE TABLE feud_heat_event (
    feud_heat_event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    multi_wrestler_feud_id BIGINT NOT NULL,
    heat_change INT NOT NULL,
    heat_after_event INT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    event_date TIMESTAMP NOT NULL,
    creation_date TIMESTAMP NOT NULL,
    FOREIGN KEY (multi_wrestler_feud_id) REFERENCES multi_wrestler_feud(multi_wrestler_feud_id) ON DELETE CASCADE
);
