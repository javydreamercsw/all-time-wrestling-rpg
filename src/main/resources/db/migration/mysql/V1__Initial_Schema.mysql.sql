-- ========== Initial Schema for All-Time Wrestling RPG ==========
-- This single script defines the complete database schema for the first release,
-- consolidating and correcting all previous migration scripts to match the
-- current JPA entity definitions.

-- =================================================================
-- CORE WRESTLER & FACTION SYSTEM
-- =================================================================

-- Create role table
CREATE TABLE IF NOT EXISTS role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(500)
);

-- Create account table
CREATE TABLE IF NOT EXISTS account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP,
    last_login TIMESTAMP,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create account_roles join table
CREATE TABLE IF NOT EXISTS account_roles (
    account_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (account_id, role_id),
    FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE
);

CREATE TABLE npc (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    npc_type VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    external_id VARCHAR(255),
    last_sync TIMESTAMP
);

CREATE TABLE faction (
    faction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description LONGTEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    leader_id BIGINT,
    formed_date TIMESTAMP,
    disbanded_date TIMESTAMP,
    creation_date TIMESTAMP NOT NULL,
    external_id VARCHAR(255) UNIQUE,
    last_sync TIMESTAMP,
    manager_id BIGINT,
    FOREIGN KEY (manager_id) REFERENCES npc(id)
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
    image_url VARCHAR(255),
    last_sync TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    faction_id BIGINT,
    account_id BIGINT,
    manager_id BIGINT,
    FOREIGN KEY (faction_id) REFERENCES faction(faction_id) ON DELETE SET NULL,
    FOREIGN KEY (account_id) REFERENCES account(id),
    FOREIGN KEY (manager_id) REFERENCES npc(id)
);

-- Add the foreign key constraint for faction's leader
ALTER TABLE faction ADD CONSTRAINT fk_faction_leader FOREIGN KEY (leader_id) REFERENCES wrestler(wrestler_id) ON DELETE SET NULL;

CREATE TABLE injury_type (
    injury_type_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    injury_name VARCHAR(100) NOT NULL UNIQUE,
    health_effect INT,
    stamina_effect INT,
    card_effect INT,
    special_effects LONGTEXT,
    external_id VARCHAR(255) UNIQUE,
    last_sync TIMESTAMP
);

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
    last_sync TIMESTAMP,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE
);

-- =================================================================
-- TEAM SYSTEM
-- =================================================================

CREATE TABLE team (
    team_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description LONGTEXT,
    wrestler1_id BIGINT NOT NULL,
    wrestler2_id BIGINT NOT NULL,
    faction_id BIGINT,
    status VARCHAR(255) NOT NULL,
    formed_date TIMESTAMP NOT NULL,
    disbanded_date TIMESTAMP,
    external_id VARCHAR(255),
    last_sync TIMESTAMP,
    manager_id BIGINT,
    FOREIGN KEY (wrestler1_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE,
    FOREIGN KEY (wrestler2_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE,
    FOREIGN KEY (faction_id) REFERENCES faction(faction_id) ON DELETE SET NULL,
    FOREIGN KEY (manager_id) REFERENCES npc(id)
);

-- =================================================================
-- CARD & DECK SYSTEM
-- =================================================================

CREATE TABLE card_set (
    set_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    set_code VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL UNIQUE,
    description LONGTEXT,
    release_date DATE,
    creation_date TIMESTAMP NOT NULL,
    external_id VARCHAR(255),
    last_sync TIMESTAMP
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
    external_id VARCHAR(255),
    last_sync TIMESTAMP,
    FOREIGN KEY (set_id) REFERENCES card_set(set_id) ON DELETE CASCADE,
    UNIQUE (number, set_id)
);

CREATE TABLE deck (
    deck_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wrestler_id BIGINT NOT NULL,
    creation_date TIMESTAMP NOT NULL,
    external_id VARCHAR(255),
    last_sync TIMESTAMP,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE
);

CREATE TABLE deck_card (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    deck_id BIGINT NOT NULL,
    card_id BIGINT NOT NULL,
    set_id BIGINT NOT NULL,
    amount INT NOT NULL DEFAULT 1,
    creation_date TIMESTAMP NOT NULL,
    external_id VARCHAR(255),
    last_sync TIMESTAMP,
    FOREIGN KEY (deck_id) REFERENCES deck(deck_id) ON DELETE CASCADE,
    FOREIGN KEY (card_id) REFERENCES card(card_id) ON DELETE CASCADE,
    FOREIGN KEY (set_id) REFERENCES card_set(set_id),
    UNIQUE (deck_id, card_id, set_id)
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
    shows_per_ppv INT DEFAULT 5 NOT NULL,
    external_id VARCHAR(255),
    last_sync TIMESTAMP
);

CREATE TABLE show_type (
    show_type_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description LONGTEXT,
    is_ppv BOOLEAN NOT NULL DEFAULT FALSE,
    expected_matches INT NOT NULL DEFAULT 0,
    expected_promos INT NOT NULL DEFAULT 0,
    creation_date TIMESTAMP NOT NULL,
    external_id VARCHAR(255),
    last_sync TIMESTAMP
);

CREATE TABLE show_template (
    template_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description LONGTEXT,
    show_type_id BIGINT NOT NULL,
    notion_url VARCHAR(500),
    external_id VARCHAR(255) UNIQUE,
    creation_date TIMESTAMP NOT NULL,
    last_sync TIMESTAMP,
    FOREIGN KEY (show_type_id) REFERENCES show_type(show_type_id) ON DELETE RESTRICT
);

CREATE TABLE `show` (
    show_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description LONGTEXT,
    show_date TIMESTAMP,
    show_type_id BIGINT NOT NULL,
    season_id BIGINT,
    template_id BIGINT,
    external_id VARCHAR(255) UNIQUE,
    creation_date TIMESTAMP NOT NULL,
    last_sync TIMESTAMP,
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
    creation_date TIMESTAMP NOT NULL,
    external_id VARCHAR(255),
    last_sync TIMESTAMP
);

CREATE TABLE segment_rule (
    segment_rule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description LONGTEXT,
    requires_high_heat BOOLEAN NOT NULL DEFAULT FALSE,
    bump_addition VARCHAR(255) NOT NULL DEFAULT 'NONE',
    creation_date TIMESTAMP NOT NULL,
    external_id VARCHAR(255),
    last_sync TIMESTAMP
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
    adjudication_status VARCHAR(255) NOT NULL DEFAULT 'ADJUDICATED',
    segment_order INT NOT NULL DEFAULT 0,
    is_main_event BOOLEAN NOT NULL DEFAULT FALSE,
    narration LONGTEXT,
    summary LONGTEXT,
    is_title_segment BOOLEAN NOT NULL DEFAULT FALSE,
    is_npc_generated BOOLEAN NOT NULL DEFAULT FALSE,
    external_id VARCHAR(255) UNIQUE,
    last_sync TIMESTAMP,
    FOREIGN KEY (show_id) REFERENCES `show`(show_id) ON DELETE CASCADE,
    FOREIGN KEY (segment_type_id) REFERENCES segment_type(segment_type_id) ON DELETE RESTRICT,
    FOREIGN KEY (winner_id) REFERENCES wrestler(wrestler_id) ON DELETE SET NULL
);

CREATE TABLE segment_participant (
    segment_participant_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    segment_id BIGINT NOT NULL,
    wrestler_id BIGINT NOT NULL,
    is_winner BOOLEAN NOT NULL DEFAULT FALSE,
    external_id VARCHAR(255),
    last_sync TIMESTAMP,
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
    championship_type VARCHAR(255) NOT NULL DEFAULT 'SINGLE',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    include_in_rankings BOOLEAN NOT NULL DEFAULT TRUE,
    creation_date TIMESTAMP NOT NULL,
    external_id VARCHAR(255) UNIQUE,
    last_sync TIMESTAMP
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
    last_sync TIMESTAMP,
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
    external_id VARCHAR(255),
    last_sync TIMESTAMP,
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
    external_id VARCHAR(255),
    last_sync TIMESTAMP,
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
    external_id VARCHAR(255),
    last_sync TIMESTAMP,
    FOREIGN KEY (faction1_id) REFERENCES faction(faction_id) ON DELETE CASCADE,
    FOREIGN KEY (faction2_id) REFERENCES faction(faction_id) ON DELETE CASCADE,
    UNIQUE (faction1_id, faction2_id),
    CHECK (faction1_id <> faction2_id)
);

CREATE TABLE faction_heat_event (
    faction_heat_event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    faction_rivalry_id BIGINT NOT NULL,
    heat_change INT NOT NULL,
    heat_after_event INT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    event_date TIMESTAMP NOT NULL,
    creation_date TIMESTAMP NOT NULL,
    external_id VARCHAR(255),
    last_sync TIMESTAMP,
    FOREIGN KEY (faction_rivalry_id) REFERENCES faction_rivalry(faction_rivalry_id) ON DELETE CASCADE
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
    external_id VARCHAR(255),
    last_sync TIMESTAMP,
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
    creation_date TIMESTAMP NOT NULL,
    external_id VARCHAR(255),
    last_sync TIMESTAMP
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
    external_id VARCHAR(255),
    last_sync TIMESTAMP,
    FOREIGN KEY (feud_id) REFERENCES multi_wrestler_feud(multi_wrestler_feud_id) ON DELETE CASCADE,
    FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id) ON DELETE CASCADE,
    UNIQUE (feud_id, wrestler_id)
);

CREATE TABLE feud_heat_event (
    feud_heat_event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    feud_id BIGINT NOT NULL,
    heat_change INT NOT NULL,
    heat_after_event INT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    event_date TIMESTAMP NOT NULL,
    creation_date TIMESTAMP NOT NULL,
    external_id VARCHAR(255),
    last_sync TIMESTAMP,
    FOREIGN KEY (feud_id) REFERENCES multi_wrestler_feud(multi_wrestler_feud_id) ON DELETE CASCADE
);

CREATE TABLE inbox_item (
    inbox_item_id BIGINT NOT NULL AUTO_INCREMENT,
    event_type VARCHAR(255) NOT NULL,
    description VARCHAR(1024) NOT NULL,
    event_timestamp TIMESTAMP NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    external_id VARCHAR(255),
    last_sync TIMESTAMP,
    PRIMARY KEY (inbox_item_id)
);
CREATE TABLE tier_boundary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tier VARCHAR(255) NOT NULL,
    gender VARCHAR(255) NOT NULL,
    min_fans BIGINT NOT NULL,
    max_fans BIGINT NOT NULL,
    challenge_cost BIGINT NOT NULL,
    contender_entry_fee BIGINT NOT NULL,
    CONSTRAINT uc_tier_boundary_tier_gender UNIQUE (tier, gender)
);
CREATE TABLE inbox_item_target (
    inbox_item_target_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inbox_item_id BIGINT,
    target_id VARCHAR(255) NOT NULL,
    last_sync TIMESTAMP,
    external_id VARCHAR(255) UNIQUE,
    FOREIGN KEY (inbox_item_id) REFERENCES inbox_item(inbox_item_id)
);
CREATE TABLE game_setting (
    setting_key VARCHAR(255) NOT NULL,
    setting_value VARCHAR(255) NOT NULL,
    CONSTRAINT pk_gamesetting PRIMARY KEY (setting_key)
);
CREATE TABLE password_reset_token (
  id BIGINT NOT NULL,
  token VARCHAR(255),
  account_id BIGINT NOT NULL,
  expiry_date TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_password_reset_token_account
    FOREIGN KEY (account_id)
    REFERENCES account(id)
);
CREATE TABLE holiday (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    description VARCHAR(255) NOT NULL UNIQUE,
    theme VARCHAR(255) NOT NULL,
    decorations TEXT,
    day_of_month INT,
    holiday_month VARCHAR(255),
    day_of_week VARCHAR(255),
    week_of_month INT,
    type VARCHAR(255) NOT NULL,
    creation_date TIMESTAMP NOT NULL,
    external_id VARCHAR(255) UNIQUE,
    last_sync TIMESTAMP
);
-- Create indexes
CREATE INDEX idx_account_username ON account(username);
CREATE INDEX idx_account_email ON account(email);
CREATE INDEX idx_account_enabled ON account(enabled);
CREATE INDEX idx_role_name ON role(name);

-- Insert default roles
INSERT INTO role (name, description) VALUES
    ('ADMIN', 'Full system access - can manage accounts and all content'),
    ('BOOKER', 'Can manage shows, wrestlers, and content but not system administration'),
    ('PLAYER', 'Can manage own content and view most data'),
    ('VIEWER', 'Read-only access to content');

-- Insert default accounts (passwords are BCrypt encoded: admin123, booker123, player123, viewer123)
-- BCrypt rounds: 10
-- Note: These passwords should be changed in production!
INSERT INTO account (username, password, email, enabled, account_non_expired, account_non_locked, credentials_non_expired)
VALUES
    ('admin', '$2a$10$wKGJ2IuP7HwMP66VaqSdYuqo3S1lcXpl9oqQkTGuLaDYHfbH57hD6', 'admin@atwrpg.local', TRUE, TRUE, TRUE, TRUE),
    ('booker', '$2a$10$OrFNvKFkH5s/DvDzd301Me4v9bpIulbPNasymqmaxCqaUM.kVXHEi', 'booker@atwrpg.local', TRUE, TRUE, TRUE, TRUE),
    ('player', '$2a$10$oHciydemMfshOLiGK7g4KO.Epu07svrzinu7PFvdJws5PYK3pIKx.', 'player@atwrpg.local', TRUE, TRUE, TRUE, TRUE),
    ('viewer', '$2a$10$no8XHshPMFd14eBxIs9e2uYW8bXm/pT6MOZsXnw.RHyhmRWgvok06', 'viewer@atwrpg.local', TRUE, TRUE, TRUE, TRUE);

-- Assign roles to accounts
INSERT INTO account_roles (account_id, role_id)
SELECT a.id, r.id FROM account a, role r WHERE a.username = 'admin' AND r.name = 'ADMIN';

INSERT INTO account_roles (account_id, role_id)
SELECT a.id, r.id FROM account a, role r WHERE a.username = 'booker' AND r.name = 'BOOKER';

INSERT INTO account_roles (account_id, role_id)
SELECT a.id, r.id FROM account a, role r WHERE a.username = 'player' AND r.name = 'PLAYER';

INSERT INTO account_roles (account_id, role_id)
SELECT a.id, r.id FROM account a, role r WHERE a.username = 'viewer' AND r.name = 'VIEWER';
-- Insert default AI settings
INSERT INTO game_setting (setting_key, setting_value) VALUES
('AI_TIMEOUT', '300'),
('AI_PROVIDER_AUTO', 'true'),

('AI_OPENAI_ENABLED', 'false'),
('AI_OPENAI_API_URL', 'https://api.openai.com/v1/chat/completions'),
('AI_OPENAI_DEFAULT_MODEL', 'gpt-3.5-turbo'),
('AI_OPENAI_PREMIUM_MODEL', 'gpt-4'),
('AI_OPENAI_MAX_TOKENS', '1000'),
('AI_OPENAI_TEMPERATURE', '0.7'),
('AI_OPENAI_API_KEY', ''),

('AI_CLAUDE_ENABLED', 'false'),
('AI_CLAUDE_API_URL', 'https://api.anthropic.com/v1/messages/'),
('AI_CLAUDE_MODEL_NAME', 'claude-3-haiku-20240307'),
('AI_CLAUDE_API_KEY', ''),

('AI_GEMINI_ENABLED', 'false'),
('AI_GEMINI_API_URL', 'https://generativelanguage.googleapis.com/v1beta/models/'),
('AI_GEMINI_MODEL_NAME', 'gemini-2.5-flash'),
('AI_GEMINI_API_KEY', ''),

('AI_LOCALAI_ENABLED', 'false'),
('AI_LOCALAI_BASE_URL', 'http://localhost:8088'),
('AI_LOCALAI_MODEL', 'llama-3.2-1b-instruct:q4_k_m'),
('AI_LOCALAI_MODEL_URL', '');
INSERT INTO holiday (description, theme, decorations, day_of_month, holiday_month, type, creation_date) VALUES
('New Year''s Day', 'New Year''s Day', 'New Year’s Day decorations are typically clean, festive, and hopeful in tone. They often feature metallic accents like gold, silver, and champagne, paired with white or soft neutrals to suggest a fresh start. Banners and signage display the new year, while streamers, balloons, and confetti add energy without feeling heavy. Clocks, stars, and fireworks motifs symbolize time, renewal, and celebration. Table settings may include sparkling centerpieces, candles, and subtle glitter, creating a bright, optimistic atmosphere that feels celebratory but calm—marking both reflection and new beginnings.', 1, 'JANUARY', 'FIXED', NOW()),
('Valentine''s Day', 'Valentine''s Day', 'Valentine’s Day decorations create a warm, romantic atmosphere centered on **reds, pinks, and soft whites**. Common elements include **hearts, roses, and love-themed banners**, often accented with **lace, ribbons, and soft lighting** like candles or string lights. **Floral arrangements, plush accents, and subtle metallic touches** add elegance, while table settings may feature **romantic centerpieces and themed place cards**, setting a cozy, intimate mood focused on love and affection.
', 14, 'FEBRUARY', 'FIXED', NOW()),
('St. Patrick''s Day', 'St. Patrick''s Day','St. Patrick’s Day decorations are bright and festive, dominated by **shades of green** with accents of **gold and white**. Common elements include **shamrocks, leprechauns, rainbows, and pots of gold**, often paired with **Irish flags or Celtic patterns**. **Banners, garlands, and themed table décor** add a playful touch, while touches of **gold foil or glitter** bring a sense of luck and celebration, creating a cheerful, lively atmosphere rooted in Irish tradition.
', 17, 'MARCH', 'FIXED', NOW()),
('Independence Day', 'Independence Day','USA Independence Day decorations are bold and patriotic, featuring **red, white, and blue** throughout. Common elements include **American flags, stars, stripes, and bunting**, often paired with **fireworks imagery**. **Banners, balloons, and table décor** showcase patriotic patterns, while **rustic or outdoor accents** like lanterns and string lights enhance the celebratory feel. The overall atmosphere is energetic and proud, reflecting national unity and summer celebration.
', 4, 'JULY', 'FIXED', NOW()),
('Halloween', 'Halloween','Halloween decorations create a spooky yet playful atmosphere using **black, orange, and purple** as the primary colors. Common elements include **pumpkins, jack-o’-lanterns, ghosts, bats, spiders, and cobwebs**, often paired with **dim lighting, candles, or colored lights**. **Haunted house props, eerie silhouettes, and fog effects** add drama, while whimsical touches keep the mood fun and festive rather than frightening.
', 31, 'OCTOBER', 'FIXED', NOW()),
('Christmas Day', 'Christmas Day','Christmas Day decorations create a warm, joyful atmosphere centered on **reds, greens, golds, and whites**. Common elements include **Christmas trees adorned with ornaments, lights, and garlands**, along with **wreaths, stockings, and nativity scenes**. **Twinkling lights, candles, and festive table settings** add warmth and sparkle, while touches of **pine, holly, and ribbon** evoke tradition, togetherness, and holiday cheer.
', 25, 'DECEMBER', 'FIXED', NOW());

INSERT INTO holiday (description, theme, decorations, day_of_week, week_of_month, holiday_month, type, creation_date) VALUES
('Memorial Day', 'Memorial Day','Memorial Day decorations are respectful and patriotic, featuring **red, white, and blue** with a more subdued tone than other holidays. Common elements include **American flags, banners, and bunting**, often paired with **stars, ribbons, and wreaths**. **Floral arrangements**, especially red and white flowers, and **simple table décor** reflect remembrance and honor, creating an atmosphere that balances national pride with solemn respect.
', 'MONDAY', -1, 'MAY', 'FLOATING', NOW()),
('Labor Day', 'Labor Day','Labor Day decorations are casual and patriotic, reflecting both national pride and the spirit of the working community. They often feature **red, white, and blue** with simple, relaxed elements like **flags, banners, and bunting**. **Outdoor-friendly décor**, such as table coverings, string lights, and picnic accents, is common, creating a laid-back, celebratory atmosphere that marks the end of summer and honors workers’ contributions.
', 'MONDAY', 1, 'SEPTEMBER', 'FLOATING', NOW()),
('Thanksgiving', 'Thanksgiving','Thanksgiving decorations create a warm, welcoming atmosphere inspired by the **fall harvest**. They feature **earthy tones** like orange, brown, gold, and deep red, with elements such as **pumpkins, gourds, autumn leaves, and cornucopias**. **Rustic table settings, candles, and natural textures** like wood and burlap add coziness, emphasizing gratitude, abundance, and togetherness.
', 'THURSDAY', 4, 'NOVEMBER', 'FLOATING', NOW());