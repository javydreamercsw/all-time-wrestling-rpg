-- Remove Notion sync columns from all entities.
-- These columns (external_id, last_sync, updated_at) were added to support Notion integration
-- which has been removed. MySQL drops associated unique indexes automatically when the column
-- is dropped, so no separate DROP INDEX statements are needed.
-- Note: no IF EXISTS — columns are guaranteed to exist from prior migrations.

ALTER TABLE npc DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE faction DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE wrestler DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE injury_type DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE injury DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE team DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE card_set DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE card DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE deck DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE deck_card DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE season DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE show_type DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE show_template DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE wrestling_show DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE segment_type DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE segment_rule DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE segment DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE segment_participant DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE title DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE title_reign DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE rivalry DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE heat_event DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE faction_rivalry DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE faction_heat_event DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE drama_event DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE multi_wrestler_feud DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE feud_participant DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE feud_heat_event DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE inbox_item DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE inbox_item_target DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE holiday DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE news_item DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE ringside_action_type DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE ringside_action DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE commentary_team DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE commentator DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE wrestler_relationship DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE wrestler_contract DROP COLUMN external_id, DROP COLUMN last_sync, DROP COLUMN updated_at;
ALTER TABLE campaign DROP COLUMN updated_at;
ALTER TABLE achievement DROP COLUMN updated_at;
ALTER TABLE location DROP COLUMN updated_at;
ALTER TABLE arena DROP COLUMN updated_at;
