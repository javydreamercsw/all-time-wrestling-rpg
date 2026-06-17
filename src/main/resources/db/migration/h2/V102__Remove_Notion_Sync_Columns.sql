-- Remove Notion sync columns from all entities that previously extended AbstractSyncableEntity.
-- These columns (external_id, last_sync, updated_at) were added to support Notion integration
-- which has been removed.

-- Tables from V12 (Add_Last_Sync_To_Entities)
ALTER TABLE wrestler DROP COLUMN IF EXISTS external_id;
ALTER TABLE wrestler DROP COLUMN IF EXISTS last_sync;
ALTER TABLE wrestler DROP COLUMN IF EXISTS updated_at;

ALTER TABLE wrestling_show DROP COLUMN IF EXISTS external_id;
ALTER TABLE wrestling_show DROP COLUMN IF EXISTS last_sync;
ALTER TABLE wrestling_show DROP COLUMN IF EXISTS updated_at;

ALTER TABLE title DROP COLUMN IF EXISTS external_id;
ALTER TABLE title DROP COLUMN IF EXISTS last_sync;
ALTER TABLE title DROP COLUMN IF EXISTS updated_at;

ALTER TABLE faction DROP COLUMN IF EXISTS external_id;
ALTER TABLE faction DROP COLUMN IF EXISTS last_sync;
ALTER TABLE faction DROP COLUMN IF EXISTS updated_at;

ALTER TABLE team DROP COLUMN IF EXISTS external_id;
ALTER TABLE team DROP COLUMN IF EXISTS last_sync;
ALTER TABLE team DROP COLUMN IF EXISTS updated_at;

ALTER TABLE injury DROP COLUMN IF EXISTS external_id;
ALTER TABLE injury DROP COLUMN IF EXISTS last_sync;
ALTER TABLE injury DROP COLUMN IF EXISTS updated_at;

ALTER TABLE segment DROP COLUMN IF EXISTS external_id;
ALTER TABLE segment DROP COLUMN IF EXISTS last_sync;
ALTER TABLE segment DROP COLUMN IF EXISTS updated_at;

-- Tables from V13 (Add_External_Id_And_Last_Sync_To_SegmentRule)
ALTER TABLE segment_rule DROP COLUMN IF EXISTS external_id;
ALTER TABLE segment_rule DROP COLUMN IF EXISTS last_sync;
ALTER TABLE segment_rule DROP COLUMN IF EXISTS updated_at;

ALTER TABLE show_type DROP COLUMN IF EXISTS external_id;
ALTER TABLE show_type DROP COLUMN IF EXISTS last_sync;
ALTER TABLE show_type DROP COLUMN IF EXISTS updated_at;

ALTER TABLE card_set DROP COLUMN IF EXISTS external_id;
ALTER TABLE card_set DROP COLUMN IF EXISTS last_sync;
ALTER TABLE card_set DROP COLUMN IF EXISTS updated_at;

ALTER TABLE segment_type DROP COLUMN IF EXISTS external_id;
ALTER TABLE segment_type DROP COLUMN IF EXISTS last_sync;
ALTER TABLE segment_type DROP COLUMN IF EXISTS updated_at;

ALTER TABLE card DROP COLUMN IF EXISTS external_id;
ALTER TABLE card DROP COLUMN IF EXISTS last_sync;
ALTER TABLE card DROP COLUMN IF EXISTS updated_at;

ALTER TABLE deck DROP COLUMN IF EXISTS external_id;
ALTER TABLE deck DROP COLUMN IF EXISTS last_sync;
ALTER TABLE deck DROP COLUMN IF EXISTS updated_at;

ALTER TABLE deck_card DROP COLUMN IF EXISTS external_id;
ALTER TABLE deck_card DROP COLUMN IF EXISTS last_sync;
ALTER TABLE deck_card DROP COLUMN IF EXISTS updated_at;

ALTER TABLE rivalry DROP COLUMN IF EXISTS external_id;
ALTER TABLE rivalry DROP COLUMN IF EXISTS last_sync;
ALTER TABLE rivalry DROP COLUMN IF EXISTS updated_at;

-- Tables from V14 (Add_External_Id_And_Last_Sync_To_Show_Template)
ALTER TABLE show_template DROP COLUMN IF EXISTS external_id;
ALTER TABLE show_template DROP COLUMN IF EXISTS last_sync;
ALTER TABLE show_template DROP COLUMN IF EXISTS updated_at;

-- Tables from V15 (Add_Missing_Sync_Fields)
ALTER TABLE title_reign DROP COLUMN IF EXISTS external_id;
ALTER TABLE title_reign DROP COLUMN IF EXISTS last_sync;
ALTER TABLE title_reign DROP COLUMN IF EXISTS updated_at;

ALTER TABLE drama_event DROP COLUMN IF EXISTS external_id;
ALTER TABLE drama_event DROP COLUMN IF EXISTS last_sync;
ALTER TABLE drama_event DROP COLUMN IF EXISTS updated_at;

ALTER TABLE npc DROP COLUMN IF EXISTS external_id;
ALTER TABLE npc DROP COLUMN IF EXISTS last_sync;
ALTER TABLE npc DROP COLUMN IF EXISTS updated_at;

ALTER TABLE feud_participant DROP COLUMN IF EXISTS external_id;
ALTER TABLE feud_participant DROP COLUMN IF EXISTS last_sync;
ALTER TABLE feud_participant DROP COLUMN IF EXISTS updated_at;

ALTER TABLE multi_wrestler_feud DROP COLUMN IF EXISTS external_id;
ALTER TABLE multi_wrestler_feud DROP COLUMN IF EXISTS last_sync;
ALTER TABLE multi_wrestler_feud DROP COLUMN IF EXISTS updated_at;

ALTER TABLE feud_heat_event DROP COLUMN IF EXISTS external_id;
ALTER TABLE feud_heat_event DROP COLUMN IF EXISTS last_sync;
ALTER TABLE feud_heat_event DROP COLUMN IF EXISTS updated_at;

-- Tables from V18 (Add_Inbox_Item_Target)
ALTER TABLE inbox_item_target DROP COLUMN IF EXISTS external_id;
ALTER TABLE inbox_item_target DROP COLUMN IF EXISTS last_sync;
ALTER TABLE inbox_item_target DROP COLUMN IF EXISTS updated_at;

-- Tables from V47 (Create_News_Tables)
ALTER TABLE news_item DROP COLUMN IF EXISTS external_id;
ALTER TABLE news_item DROP COLUMN IF EXISTS last_sync;
ALTER TABLE news_item DROP COLUMN IF EXISTS updated_at;

-- Tables from V57 (Add_Ringside_Actions)
ALTER TABLE ringside_action_type DROP CONSTRAINT IF EXISTS uk_ringside_action_type_external_id;
ALTER TABLE ringside_action_type DROP COLUMN IF EXISTS external_id;
ALTER TABLE ringside_action_type DROP COLUMN IF EXISTS last_sync;
ALTER TABLE ringside_action_type DROP COLUMN IF EXISTS updated_at;

ALTER TABLE ringside_action DROP CONSTRAINT IF EXISTS uk_ringside_action_external_id;
ALTER TABLE ringside_action DROP COLUMN IF EXISTS external_id;
ALTER TABLE ringside_action DROP COLUMN IF EXISTS last_sync;
ALTER TABLE ringside_action DROP COLUMN IF EXISTS updated_at;

-- Tables from V62 (Add_Updated_At_To_Entities) -- additional tables
ALTER TABLE faction_heat_event DROP COLUMN IF EXISTS updated_at;
ALTER TABLE heat_event DROP COLUMN IF EXISTS updated_at;
ALTER TABLE segment_participant DROP COLUMN IF EXISTS updated_at;
ALTER TABLE faction_rivalry DROP COLUMN IF EXISTS updated_at;
ALTER TABLE injury_type DROP COLUMN IF EXISTS updated_at;
ALTER TABLE season DROP COLUMN IF EXISTS updated_at;
ALTER TABLE inbox_item DROP COLUMN IF EXISTS updated_at;
ALTER TABLE holiday DROP COLUMN IF EXISTS updated_at;
ALTER TABLE achievement DROP COLUMN IF EXISTS updated_at;
ALTER TABLE location DROP COLUMN IF EXISTS updated_at;
ALTER TABLE arena DROP COLUMN IF EXISTS updated_at;
ALTER TABLE commentary_team DROP COLUMN IF EXISTS updated_at;
ALTER TABLE commentator DROP COLUMN IF EXISTS updated_at;
ALTER TABLE campaign DROP COLUMN IF EXISTS updated_at;

-- Tables from V67 (Create_Wrestler_Relationship_Table)
ALTER TABLE wrestler_relationship DROP COLUMN IF EXISTS external_id;
ALTER TABLE wrestler_relationship DROP COLUMN IF EXISTS last_sync;
ALTER TABLE wrestler_relationship DROP COLUMN IF EXISTS updated_at;

-- Tables from V68 (Add_GM_Mode_Financials_And_Logistics)
ALTER TABLE wrestler_contract DROP COLUMN IF EXISTS external_id;
ALTER TABLE wrestler_contract DROP COLUMN IF EXISTS last_sync;
ALTER TABLE wrestler_contract DROP COLUMN IF EXISTS updated_at;
