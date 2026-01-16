-- Database Performance Optimization Indexes
-- This script creates indexes to improve query performance for the ATW RPG Management System

-- ==================== WRESTLER TABLE INDEXES ====================

-- Index for wrestler name searches (frequently used in UI and API)
CREATE INDEX IF NOT EXISTS idx_wrestler_name ON wrestler(name);

-- Index for wrestler tier queries (used in match booking)
CREATE INDEX IF NOT EXISTS idx_wrestler_tier ON wrestler(tier);

-- Index for wrestler creation date (used in reporting and analytics)
CREATE INDEX IF NOT EXISTS idx_wrestler_creation_date ON wrestler(creation_date);

-- Index for wrestler external ID (used in Notion sync operations)
CREATE INDEX IF NOT EXISTS idx_wrestler_external_id ON wrestler(external_id);

-- Index for wrestler faction (used in faction-based queries)
CREATE INDEX IF NOT EXISTS idx_wrestler_faction ON wrestler(faction_id);

-- ==================== SHOW TABLE INDEXES ====================

-- Index for show name searches
CREATE INDEX IF NOT EXISTS idx_show_name ON wrestling_show(name);

-- Index for show date queries (critical for calendar views and date-based filtering)
CREATE INDEX IF NOT EXISTS idx_show_date ON wrestling_show(show_date);

-- Index for show type filtering
CREATE INDEX IF NOT EXISTS idx_show_type ON wrestling_show(show_type_id);

-- Index for show season filtering
CREATE INDEX IF NOT EXISTS idx_show_season ON wrestling_show(season_id);

-- Index for show template relationships
CREATE INDEX IF NOT EXISTS idx_show_template ON wrestling_show(template_id);

-- Index for show external ID (Notion sync)
CREATE INDEX IF NOT EXISTS idx_show_external_id ON wrestling_show(external_id);

-- Composite index for date range queries (most common show query pattern)
CREATE INDEX IF NOT EXISTS idx_show_date_type ON wrestling_show(show_date, show_type_id);

-- ==================== RIVALRY TABLE INDEXES ====================

-- Index for active rivalry queries (frequently used)
CREATE INDEX IF NOT EXISTS idx_rivalry_active ON rivalry(is_active);

-- Index for wrestler1 in rivalries
CREATE INDEX IF NOT EXISTS idx_rivalry_wrestler1 ON rivalry(wrestler1_id);

-- Index for wrestler2 in rivalries
CREATE INDEX IF NOT EXISTS idx_rivalry_wrestler2 ON rivalry(wrestler2_id);

-- Index for rivalry heat (used in match booking algorithms)
CREATE INDEX IF NOT EXISTS idx_rivalry_heat ON rivalry(heat);

-- Composite index for finding rivalries between specific wrestlers
CREATE INDEX IF NOT EXISTS idx_rivalry_wrestlers ON rivalry(wrestler1_id, wrestler2_id, is_active);

-- Index for rivalry creation date (analytics)
CREATE INDEX IF NOT EXISTS idx_rivalry_creation_date ON rivalry(creation_date);

-- ==================== INJURY TABLE INDEXES ====================

-- Index for active injuries (critical for wrestler availability checks)
CREATE INDEX IF NOT EXISTS idx_injury_active ON injury(is_active);

-- Index for wrestler injuries
CREATE INDEX IF NOT EXISTS idx_injury_wrestler ON injury(wrestler_id);

-- Index for injury severity (used in health calculations)
CREATE INDEX IF NOT EXISTS idx_injury_severity ON injury(severity);

-- Index for injury date (used in healing calculations)
CREATE INDEX IF NOT EXISTS idx_injury_date ON injury(injury_date);

-- Composite index for finding active injuries by wrestler
CREATE INDEX IF NOT EXISTS idx_injury_wrestler_active ON injury(wrestler_id, is_active);

-- Composite index for severity-based queries
CREATE INDEX IF NOT EXISTS idx_injury_active_severity ON injury(is_active, severity);

-- ==================== TITLE TABLE INDEXES ====================

-- Index for title name searches
CREATE INDEX IF NOT EXISTS idx_title_name ON title(name);

-- Index for title tier (used in title hierarchy)
CREATE INDEX IF NOT EXISTS idx_title_tier ON title(tier);

-- Index for active titles
CREATE INDEX IF NOT EXISTS idx_title_active ON title(is_active);

-- ==================== TITLE REIGN TABLE INDEXES ====================

-- Index for current title reigns (calculated as end_date IS NULL)
CREATE INDEX IF NOT EXISTS idx_title_reign_current ON title_reign(end_date);

-- Index for title reigns by title
CREATE INDEX IF NOT EXISTS idx_title_reign_title ON title_reign(title_id);

-- Index for title reigns by champion
CREATE INDEX IF NOT EXISTS idx_title_reign_wrestler ON title_reign_champion(wrestler_id);

-- Index for reign start date
CREATE INDEX IF NOT EXISTS idx_title_reign_start_date ON title_reign(start_date);

-- Composite index for current reigns by title (using end_date for current check)
CREATE INDEX IF NOT EXISTS idx_title_reign_title_current ON title_reign(title_id, end_date);

-- ==================== MULTI WRESTLER FEUD TABLE INDEXES ====================

-- Index for active feuds
CREATE INDEX IF NOT EXISTS idx_multi_feud_active ON multi_wrestler_feud(is_active);

-- Index for feud heat (used in storyline algorithms)
CREATE INDEX IF NOT EXISTS idx_multi_feud_heat ON multi_wrestler_feud(heat);

-- Index for feud creation date
CREATE INDEX IF NOT EXISTS idx_multi_feud_creation_date ON multi_wrestler_feud(creation_date);

-- Index for feud name searches
CREATE INDEX IF NOT EXISTS idx_multi_feud_name ON multi_wrestler_feud(name);

-- ==================== FEUD PARTICIPANT TABLE INDEXES ====================

-- Index for active participants
CREATE INDEX IF NOT EXISTS idx_feud_participant_active ON feud_participant(is_active);

-- Index for participants by feud
CREATE INDEX IF NOT EXISTS idx_feud_participant_feud ON feud_participant(feud_id);

-- Index for participants by wrestler
CREATE INDEX IF NOT EXISTS idx_feud_participant_wrestler ON feud_participant(wrestler_id);

-- Composite index for active participants by feud
CREATE INDEX IF NOT EXISTS idx_feud_participant_feud_active ON feud_participant(feud_id, is_active);

-- ==================== SEASON TABLE INDEXES ====================

-- Index for season name searches
CREATE INDEX IF NOT EXISTS idx_season_name ON season(name);

-- Index for season start date
CREATE INDEX IF NOT EXISTS idx_season_start_date ON season(start_date);

-- Index for season end date
CREATE INDEX IF NOT EXISTS idx_season_end_date ON season(end_date);

-- Index for season creation date
CREATE INDEX IF NOT EXISTS idx_season_creation_date ON season(creation_date);

-- ==================== SHOW TYPE TABLE INDEXES ====================

-- Index for show type name searches
CREATE INDEX IF NOT EXISTS idx_show_type_name ON show_type(name);

-- ==================== SHOW TEMPLATE TABLE INDEXES ====================

-- Index for template name searches
CREATE INDEX IF NOT EXISTS idx_show_template_name ON show_template(name);

-- Index for template by show type
CREATE INDEX IF NOT EXISTS idx_show_template_type ON show_template(show_type_id);

-- Index for template external ID (Notion sync)
CREATE INDEX IF NOT EXISTS idx_show_template_external_id ON show_template(external_id);

-- ==================== SEGMENT RULE TABLE INDEXES ====================

-- Index for match stipulation name searches
CREATE INDEX IF NOT EXISTS idx_segment_rule_name ON segment_rule(name);

-- Index for high heat match stipulations
CREATE INDEX IF NOT EXISTS idx_segment_rule_high_heat ON segment_rule(requires_high_heat);

-- ==================== SEGMENT TYPE TABLE INDEXES ====================

-- Index for match type name searches
CREATE INDEX IF NOT EXISTS idx_segment_type_name ON segment_type(name);

-- ==================== PERFORMANCE MONITORING INDEXES ====================

-- These indexes help with performance monitoring and analytics

-- Index for tracking entity creation patterns (using creation_date for date-based queries)
CREATE INDEX IF NOT EXISTS idx_wrestler_creation_month ON wrestler(creation_date);
CREATE INDEX IF NOT EXISTS idx_show_creation_month ON wrestling_show(creation_date);
CREATE INDEX IF NOT EXISTS idx_rivalry_creation_month ON rivalry(creation_date);

-- ==================== COMPOSITE INDEXES FOR COMPLEX QUERIES ====================

-- Index for wrestler availability checks (combines injury and faction data)
-- This supports queries that check if wrestlers are available for matches
CREATE INDEX IF NOT EXISTS idx_wrestler_availability ON wrestler(tier, faction_id, creation_date);

-- Index for show calendar queries (date + type combinations)
CREATE INDEX IF NOT EXISTS idx_show_calendar ON wrestling_show(show_date DESC, show_type_id, season_id);

-- Index for rivalry heat tracking (active rivalries by heat level)
CREATE INDEX IF NOT EXISTS idx_rivalry_heat_tracking ON rivalry(is_active, heat DESC, creation_date);

-- Index for injury recovery tracking
CREATE INDEX IF NOT EXISTS idx_injury_recovery ON injury(is_active, severity, injury_date);

-- ==================== FOREIGN KEY PERFORMANCE INDEXES ====================

-- Additional indexes on foreign keys that might not be automatically indexed
CREATE INDEX IF NOT EXISTS idx_show_fk_composite ON wrestling_show(show_type_id, season_id, template_id);
CREATE INDEX IF NOT EXISTS idx_title_reign_fk_composite ON title_reign_champion(title_reign_id, wrestler_id);

-- ==================== FULL TEXT SEARCH INDEXES (H2 Specific) ====================

-- Full text search indexes for name-based searches
-- Note: H2 supports full-text search with FULLTEXT indexes
CREATE INDEX IF NOT EXISTS idx_wrestler_name_fulltext ON wrestler(name);
CREATE INDEX IF NOT EXISTS idx_show_name_fulltext ON wrestling_show(name);
CREATE INDEX IF NOT EXISTS idx_title_name_fulltext ON title(name);

-- ==================== STATISTICS UPDATE ====================

-- Update table statistics for better query planning (H2 specific)
-- This helps the query optimizer make better decisions
ANALYZE;