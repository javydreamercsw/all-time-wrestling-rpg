-- H2 2.4.240; 
SET DB_CLOSE_DELAY -1;         
;              
CREATE USER IF NOT EXISTS "SA" SALT '22229aa0cc5b0750' HASH '33f4643b45947886ca0631324f27bef21a44fadfe87d627ad3fb45ced3b40a81' ADMIN;          
CREATE SEQUENCE "PUBLIC"."PASSWORD_RESET_TOKEN_SEQ" START WITH 1 INCREMENT BY 50;              
CREATE CACHED TABLE "PUBLIC"."flyway_schema_history"(
    "installed_rank" INTEGER NOT NULL,
    "version" CHARACTER VARYING(50),
    "description" CHARACTER VARYING(200) NOT NULL,
    "type" CHARACTER VARYING(20) NOT NULL,
    "script" CHARACTER VARYING(1000) NOT NULL,
    "checksum" INTEGER,
    "installed_by" CHARACTER VARYING(100) NOT NULL,
    "installed_on" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "execution_time" INTEGER NOT NULL,
    "success" BOOLEAN NOT NULL
);           
ALTER TABLE "PUBLIC"."flyway_schema_history" ADD CONSTRAINT "PUBLIC"."flyway_schema_history_pk" PRIMARY KEY("installed_rank"); 
-- 104 +/- SELECT COUNT(*) FROM PUBLIC.flyway_schema_history;  
INSERT INTO "PUBLIC"."flyway_schema_history" VALUES
(-1, NULL, '<< Flyway Schema History table created >>', 'TABLE', '', NULL, 'SA', TIMESTAMP '2026-06-23 02:00:40.290251', 0, TRUE),
(1, '1', 'Initial Schema', 'SQL', 'V1__Initial_Schema.sql', 1115964703, 'SA', TIMESTAMP '2026-06-23 02:00:40.536809', 131, TRUE),
(2, '2', 'DeckCard SetId', 'SQL', 'V2__DeckCard_SetId.sql', -788503740, 'SA', TIMESTAMP '2026-06-23 02:00:40.57017', 20, TRUE),
(3, '3', 'Add CardSet Name Column', 'SQL', 'V3__Add_CardSet_Name_Column.sql', 118948041, 'SA', TIMESTAMP '2026-06-23 02:00:40.58678', 8, TRUE),
(4, '4', 'Update Card Unique Constraint', 'SQL', 'V4__Update_Card_Unique_Constraint.sql', 659202034, 'SA', TIMESTAMP '2026-06-23 02:00:40.595329', 1, TRUE),
(5, '5', 'Add Adjudication Status To Segment', 'SQL', 'V5__Add_Adjudication_Status_To_Segment.sql', 2134148974, 'SA', TIMESTAMP '2026-06-23 02:00:40.612563', 11, TRUE),
(6, '6', 'Add Segment Order And Main Event', 'SQL', 'V6__Add_Segment_Order_And_Main_Event.sql', 619297846, 'SA', TIMESTAMP '2026-06-23 02:00:40.639873', 21, TRUE),
(7, '7', 'Add Inbox Item Table', 'SQL', 'V7__Add_Inbox_Item_Table.sql', 968071477, 'SA', TIMESTAMP '2026-06-23 02:00:40.648116', 1, TRUE),
(8, '8', 'Add Reference Id To Inbox Item', 'SQL', 'V8__Add_Reference_Id_To_Inbox_Item.sql', 989049951, 'SA', TIMESTAMP '2026-06-23 02:00:40.657652', 4, TRUE),
(9, '9', 'Add Bump Addition To Segment Rule', 'SQL', 'V9__Add_Bump_Addition_To_Segment_Rule.sql', 1037211787, 'SA', TIMESTAMP '2026-06-23 02:00:40.66778', 5, TRUE),
(10, '10', 'Add Image Url To Wrestler', 'SQL', 'V10__Add_Image_Url_To_Wrestler.sql', -1445120071, 'SA', TIMESTAMP '2026-06-23 02:00:40.686652', 13, TRUE),
(11, '11', 'Add Expected Matches And Promos To ShowType', 'SQL', 'V11__Add_Expected_Matches_And_Promos_To_ShowType.sql', -1728232004, 'SA', TIMESTAMP '2026-06-23 02:00:40.701837', 9, TRUE),
(12, '12', 'Add Last Sync To Entities', 'SQL', 'V12__Add_Last_Sync_To_Entities.sql', 1145996377, 'SA', TIMESTAMP '2026-06-23 02:00:40.753402', 46, TRUE),
(13, '13', 'Add External Id And Last Sync To SegmentRule', 'SQL', 'V13__Add_External_Id_And_Last_Sync_To_SegmentRule.sql', -566008599, 'SA', TIMESTAMP '2026-06-23 02:00:40.82853', 68, TRUE),
(14, '14', 'Add External Id And Last Sync To Show Template', 'SQL', 'V14__Add_External_Id_And_Last_Sync_To_Show_Template.sql', -1336096513, 'SA', TIMESTAMP '2026-06-23 02:00:40.839031', 5, TRUE),
(15, '15', 'Add Missing Sync Fields', 'SQL', 'V15__Add_Missing_Sync_Fields.sql', 1365334389, 'SA', TIMESTAMP '2026-06-23 02:00:40.906674', 61, TRUE),
(16, '16', 'Create Tier Boundary Table', 'SQL', 'V16__Create_Tier_Boundary_Table.sql', -1179887120, 'SA', TIMESTAMP '2026-06-23 02:00:40.913374', 1, TRUE),
(17, '18', 'Add Inbox Item Target', 'SQL', 'V18__Add_Inbox_Item_Target.sql', -1149624846, 'SA', TIMESTAMP '2026-06-23 02:00:40.920111', 2, TRUE),
(18, '19', 'Remove Reference Id From Inbox Item', 'SQL', 'V19__Remove_Reference_Id_From_Inbox_Item.sql', -1540103389, 'SA', TIMESTAMP '2026-06-23 02:00:40.927783', 3, TRUE),
(19, '20', 'Create Account Tables', 'SQL', 'V20__Create_Account_Tables.sql', 1011977414, 'SA', TIMESTAMP '2026-06-23 02:00:40.938654', 5, TRUE),
(20, '21', 'Insert Default Accounts', 'SQL', 'V21__Insert_Default_Accounts.sql', -1923279678, 'SA', TIMESTAMP '2026-06-23 02:00:40.955115', 8, TRUE),
(21, '22', 'Add Account To Wrestler', 'SQL', 'V22__Add_Account_To_Wrestler.sql', 76510181, 'SA', TIMESTAMP '2026-06-23 02:00:40.970485', 10, TRUE),
(22, '23', 'Rename feud heat event column', 'SQL', 'V23__Rename_feud_heat_event_column.sql', -497843593, 'SA', TIMESTAMP '2026-06-23 02:00:40.976474', 1, TRUE),
(23, '24', 'Create Game Setting Table', 'SQL', 'V24__Create_Game_Setting_Table.sql', 795307327, 'SA', TIMESTAMP '2026-06-23 02:00:40.981664', 1, TRUE),
(24, '25', 'Create Password Reset Token Table', 'SQL', 'V25__Create_Password_Reset_Token_Table.sql', -332824989, 'SA', TIMESTAMP '2026-06-23 02:00:40.988328', 2, TRUE),
(25, '26', 'Add Championship Type To Title', 'SQL', 'V26__Add_Championship_Type_To_Title.sql', -96901047, 'SA', TIMESTAMP '2026-06-23 02:00:40.998515', 5, TRUE);       
INSERT INTO "PUBLIC"."flyway_schema_history" VALUES
(26, '27', 'Add AI Settings', 'SQL', 'V27__Add_AI_Settings.sql', 1962182044, 'SA', TIMESTAMP '2026-06-23 02:00:41.005382', 2, TRUE),
(27, '28', 'Add Active To Wrestler', 'SQL', 'V28__Add_Active_To_Wrestler.sql', -338805172, 'SA', TIMESTAMP '2026-06-23 02:00:41.01768', 7, TRUE),
(28, '29', 'Create Holiday Table', 'SQL', 'V29__Create_Holiday_Table.sql', 599478441, 'SA', TIMESTAMP '2026-06-23 02:00:41.023209', 1, TRUE),
(29, '30', 'Populate Holiday Table', 'SQL', 'V30__Populate_Holiday_Table.sql', -1793461912, 'SA', TIMESTAMP '2026-06-23 02:00:41.037938', 4, TRUE),
(30, '31', 'Add Manager To Wrestler And Faction And Team', 'SQL', 'V31__Add_Manager_To_Wrestler_And_Faction_And_Team.sql', 378765621, 'SA', TIMESTAMP '2026-06-23 02:00:41.062102', 18, TRUE),
(31, '32', 'Add Description To Npc', 'SQL', 'V32__Add_Description_To_Npc.sql', 630994349, 'SA', TIMESTAMP '2026-06-23 02:00:41.070865', 4, TRUE),
(32, '33', 'Add Include In Rankings To Title', 'SQL', 'V33__Add_Include_In_Rankings_To_Title.sql', -647497361, 'SA', TIMESTAMP '2026-06-23 02:00:41.080043', 4, TRUE),
(33, '34', 'Add Won At Segment To Title Reign', 'SQL', 'V34__Add_Won_At_Segment_To_Title_Reign.sql', -332785961, 'SA', TIMESTAMP '2026-06-23 02:00:41.087715', 3, TRUE),
(34, '35', 'Rename Show Table', 'SQL', 'V35__Rename_Show_Table.sql', -888951185, 'SA', TIMESTAMP '2026-06-23 02:00:41.093472', 1, TRUE),
(35, '36', 'Add Unique Constraint To Wrestler Account', 'SQL', 'V36__Add_Unique_Constraint_To_Wrestler_Account.sql', -1288629724, 'SA', TIMESTAMP '2026-06-23 02:00:41.098565', 0, TRUE),
(36, '37', 'Create Campaign System', 'SQL', 'V37__Create_Campaign_System.sql', 1504350749, 'SA', TIMESTAMP '2026-06-23 02:00:41.168426', 54, TRUE),
(37, '38', 'Add Theme Preference And Default Setting', 'SQL', 'V38__Add_Theme_Preference_And_Default_Setting.sql', -913225845, 'SA', TIMESTAMP '2026-06-23 02:00:41.177908', 5, TRUE),
(38, '39', 'Add League System', 'SQL', 'V39__Add_League_System.sql', 602764493, 'SA', TIMESTAMP '2026-06-23 02:00:41.210349', 26, TRUE),
(39, '41', 'Add Image Url To Npc', 'SQL', 'V41__Add_Image_Url_To_Npc.sql', -1391881153, 'SA', TIMESTAMP '2026-06-23 02:00:41.217064', 2, TRUE),
(40, '42', 'Add Image Url To Show Template', 'SQL', 'V42__Add_Image_Url_To_Show_Template.sql', 2091528606, 'SA', TIMESTAMP '2026-06-23 02:00:41.223992', 3, TRUE),
(41, '43', 'Add Expected Matches And Promos To ShowTemplate', 'SQL', 'V43__Add_Expected_Matches_And_Promos_To_ShowTemplate.sql', -1363888031, 'SA', TIMESTAMP '2026-06-23 02:00:41.233469', 5, TRUE),
(42, '44', 'Add Recurrence And Defense Frequency', 'SQL', 'V44__Add_Recurrence_And_Defense_Frequency.sql', -33215304, 'SA', TIMESTAMP '2026-06-23 02:00:41.258976', 21, TRUE),
(43, '45', 'Remove LocalAI Settings', 'SQL', 'V45__Remove_LocalAI_Settings.sql', -2084740732, 'SA', TIMESTAMP '2026-06-23 02:00:41.266233', 3, TRUE),
(44, '46', 'Create Commentary Tables', 'SQL', 'V46__Create_Commentary_Tables.sql', 1578209880, 'SA', TIMESTAMP '2026-06-23 02:00:41.287461', 16, TRUE),
(45, '47', 'Create News Tables', 'SQL', 'V47__Create_News_Tables.sql', -2003982983, 'SA', TIMESTAMP '2026-06-23 02:00:41.292517', 1, TRUE),
(46, '48', 'Add Gender Constraint To ShowTemplate', 'SQL', 'V48__Add_Gender_Constraint_To_ShowTemplate.sql', -1162501534, 'SA', TIMESTAMP '2026-06-23 02:00:41.299541', 3, TRUE),
(47, '49', 'Add Legacy Fields To Account', 'SQL', 'V49__Add_Legacy_Fields_To_Account.sql', -508336936, 'SA', TIMESTAMP '2026-06-23 02:00:41.319137', 16, TRUE),
(48, '50', 'Create Achievement Tables', 'SQL', 'V50__Create_Achievement_Tables.sql', 533959700, 'SA', TIMESTAMP '2026-06-23 02:00:41.32574', 2, TRUE),
(49, '51', 'Add Affinity To Faction', 'SQL', 'V51__Add_Affinity_To_Faction.sql', 1676214726, 'SA', TIMESTAMP '2026-06-23 02:00:41.333353', 3, TRUE),
(50, '52', 'Remove Rivalry Unique Constraint', 'SQL', 'V52__Remove_Rivalry_Unique_Constraint.sql', 858104277, 'SA', TIMESTAMP '2026-06-23 02:00:42.831156', 1492, TRUE),
(51, '53', 'Create Storyline Tables', 'SQL', 'V53__Create_Storyline_Tables.sql', 1034900586, 'SA', TIMESTAMP '2026-06-23 02:00:42.850462', 12, TRUE);            
INSERT INTO "PUBLIC"."flyway_schema_history" VALUES
(52, '54', 'Add Npc Attributes', 'SQL', 'V54__Add_Npc_Attributes.sql', 1944315555, 'SA', TIMESTAMP '2026-06-23 02:00:42.857981', 3, TRUE),
(53, '55', 'Add Referee To Segment', 'SQL', 'V55__Add_Referee_To_Segment.sql', -14042474, 'SA', TIMESTAMP '2026-06-23 02:00:42.872699', 10, TRUE),
(54, '56', 'Add No Dq To Segment Rule', 'SQL', 'V56__Add_No_Dq_To_Segment_Rule.sql', 621834710, 'SA', TIMESTAMP '2026-06-23 02:00:42.878642', 2, TRUE),
(55, '57', 'Add Ringside Actions', 'SQL', 'V57__Add_Ringside_Actions.sql', 551019052, 'SA', TIMESTAMP '2026-06-23 02:00:42.885883', 3, TRUE),
(56, '58', 'Add Physical Condition To Wrestler', 'SQL', 'V58__Add_Physical_Condition_To_Wrestler.sql', -2031017789, 'SA', TIMESTAMP '2026-06-23 02:00:42.899563', 10, TRUE),
(57, '59', 'Create Location Arena Tables And Link Show', 'SQL', 'V59__Create_Location_Arena_Tables_And_Link_Show.sql', -777877363, 'SA', TIMESTAMP '2026-06-23 02:00:42.92371', 18, TRUE),
(58, '60', 'Remove Wrestler Account Unique Constraint', 'SQL', 'V60__Remove_Wrestler_Account_Unique_Constraint.sql', -1366350250, 'SA', TIMESTAMP '2026-06-23 02:00:42.927955', 0, TRUE),
(59, '61', 'Add Faction Alignment And Team Fields', 'SQL', 'V61__Add_Faction_Alignment_And_Team_Fields.sql', -393460592, 'SA', TIMESTAMP '2026-06-23 02:00:42.943115', 12, TRUE),
(60, '62', 'Add Updated At To Entities', 'SQL', 'V62__Add_Updated_At_To_Entities.sql', 588199953, 'SA', TIMESTAMP '2026-06-23 02:00:43.055187', 105, TRUE),
(61, '63', 'Initialize Set Enablement Settings', 'SQL', 'V63__Initialize_Set_Enablement_Settings.sql', 727117897, 'SA', TIMESTAMP '2026-06-23 02:00:43.062131', 1, TRUE),
(62, '64', 'Add Expansion Code To Wrestler', 'SQL', 'V64__Add_Expansion_Code_To_Wrestler.sql', -911082154, 'SA', TIMESTAMP '2026-06-23 02:00:43.082099', 14, TRUE),
(63, '65', 'Add Expansion Code To Npc', 'SQL', 'V65__Add_Expansion_Code_To_Npc.sql', 1736603957, 'SA', TIMESTAMP '2026-06-23 02:00:43.091275', 3, TRUE),
(64, '66', 'Add Image Url To Various Entities', 'SQL', 'V66__Add_Image_Url_To_Various_Entities.sql', 1061733307, 'SA', TIMESTAMP '2026-06-23 02:00:43.104074', 9, TRUE),
(65, '67', 'Create Wrestler Relationship Table', 'SQL', 'V67__Create_Wrestler_Relationship_Table.sql', -401169771, 'SA', TIMESTAMP '2026-06-23 02:00:43.110942', 2, TRUE),
(66, '68', 'Add GM Mode Financials And Logistics', 'SQL', 'V68__Add_GM_Mode_Financials_And_Logistics.sql', 1914270991, 'SA', TIMESTAMP '2026-06-23 02:00:43.144481', 29, TRUE),
(67, '69', 'Add Effect Script To Title', 'SQL', 'V69__Add_Effect_Script_To_Title.sql', 1842801306, 'SA', TIMESTAMP '2026-06-23 02:00:43.15067', 2, TRUE),
(68, '70', 'Add Show Attendance And Revenue', 'SQL', 'V70__Add_Show_Attendance_And_Revenue.sql', 1941232678, 'SA', TIMESTAMP '2026-06-23 02:00:43.161895', 7, TRUE),
(69, '71', 'Add Notes To Segment', 'SQL', 'V71__Add_Notes_To_Segment.sql', 1957484762, 'SA', TIMESTAMP '2026-06-23 02:00:43.169261', 4, TRUE),
(70, '72', 'Create Status Card Tables', 'SQL', 'V72__Create_Status_Card_Tables.sql', -1660610380, 'SA', TIMESTAMP '2026-06-23 02:00:43.178158', 5, TRUE),
(71, '73', 'add campaign ability card constraint', 'SQL', 'V73__add_campaign_ability_card_constraint.sql', 215371348, 'SA', TIMESTAMP '2026-06-23 02:00:43.181416', 0, TRUE),
(72, '74', 'Add Team Number To Segment Participant', 'SQL', 'V74__Add_Team_Number_To_Segment_Participant.sql', -1799038452, 'SA', TIMESTAMP '2026-06-23 02:00:43.187057', 2, TRUE),
(73, '75', 'Create Universe Table', 'SQL', 'V75__Create_Universe_Table.sql', 360951789, 'SA', TIMESTAMP '2026-06-23 02:00:43.191998', 1, TRUE),
(74, '76', 'Create Wrestler State Table', 'SQL', 'V76__Create_Wrestler_State_Table.sql', -2083864010, 'SA', TIMESTAMP '2026-06-23 02:00:43.198915', 3, TRUE),
(75, '77', 'Add Universe Membership', 'SQL', 'V77__Add_Universe_Membership.sql', -834416382, 'SA', TIMESTAMP '2026-06-23 02:00:43.204377', 2, TRUE),
(76, '78', 'Add Universe Settings Tables', 'SQL', 'V78__Add_Universe_Settings_Tables.sql', 1718709860, 'SA', TIMESTAMP '2026-06-23 02:00:43.210571', 2, TRUE),
(77, '79', 'Deactivate Same Faction Rivalries', 'SQL', 'V79__Deactivate_Same_Faction_Rivalries.sql', -869186165, 'SA', TIMESTAMP '2026-06-23 02:00:43.219719', 4, TRUE);        
INSERT INTO "PUBLIC"."flyway_schema_history" VALUES
(78, '80', 'Add RivalryId To Segment', 'SQL', 'V80__Add_RivalryId_To_Segment.sql', 1611206126, 'SA', TIMESTAMP '2026-06-23 02:00:43.227073', 3, TRUE),
(79, '81', 'Decouple Wrestler State', 'SQL', 'V81__Decouple_Wrestler_State.sql', 699881626, 'SA', TIMESTAMP '2026-06-23 02:00:43.259473', 26, TRUE),
(80, '82', 'Drop Deprecated Wrestler Columns', 'SQL', 'V82__Drop_Deprecated_Wrestler_Columns.sql', -479567210, 'SA', TIMESTAMP '2026-06-23 02:00:43.326091', 62, TRUE),
(81, '83', 'Backfill Universe Id', 'SQL', 'V83__Backfill_Universe_Id.sql', -1009108233, 'SA', TIMESTAMP '2026-06-23 02:00:43.331074', 1, TRUE),
(82, '84', 'Add Universe Alignment', 'SQL', 'V84__Add_Universe_Alignment.sql', -1733031706, 'SA', TIMESTAMP '2026-06-23 02:00:43.337273', 3, TRUE),
(83, '85', 'Seed Rivalry Lifecycle Settings', 'SQL', 'V85__Seed_Rivalry_Lifecycle_Settings.sql', 246113347, 'SA', TIMESTAMP '2026-06-23 02:00:43.341373', 0, TRUE),
(84, '86', 'Replace Defense Frequency With Type', 'SQL', 'V86__Replace_Defense_Frequency_With_Type.sql', 2101993557, 'SA', TIMESTAMP '2026-06-23 02:00:43.350194', 5, TRUE),
(85, '87', 'Add Outcome Matrix', 'SQL', 'V87__Add_Outcome_Matrix.sql', 1095930633, 'SA', TIMESTAMP '2026-06-23 02:00:43.355907', 2, TRUE),
(86, '88', 'Add DramaEvent Cleanup Index', 'SQL', 'V88__Add_DramaEvent_Cleanup_Index.sql', 1867476901, 'SA', TIMESTAMP '2026-06-23 02:00:43.359272', 0, TRUE),
(87, '89', 'Link Injury To InjuryType', 'SQL', 'V89__Link_Injury_To_InjuryType.sql', 1285009657, 'SA', TIMESTAMP '2026-06-23 02:00:43.367323', 4, TRUE),
(88, '90', 'Add Wrestler Season Snapshot', 'SQL', 'V90__Add_Wrestler_Season_Snapshot.sql', 710643383, 'SA', TIMESTAMP '2026-06-23 02:00:43.372705', 1, TRUE),
(89, '91', 'Add Universe To Rivalry', 'SQL', 'V91__Add_Universe_To_Rivalry.sql', -1401405617, 'SA', TIMESTAMP '2026-06-23 02:00:43.381124', 5, TRUE),
(90, '92', 'Add Universe Scoped Game Settings', 'SQL', 'V92__Add_Universe_Scoped_Game_Settings.sql', 865269514, 'SA', TIMESTAMP '2026-06-23 02:00:43.391735', 7, TRUE),
(91, '93', 'Migrate Credentials To Default Universe', 'SQL', 'V93__Migrate_Credentials_To_Default_Universe.sql', -1857336965, 'SA', TIMESTAMP '2026-06-23 02:00:43.396084', 1, TRUE),
(92, '94', 'Fix Universe Sequence', 'SQL', 'V94__Fix_Universe_Sequence.sql', -1386687509, 'SA', TIMESTAMP '2026-06-23 02:00:43.40384', 4, TRUE),
(93, '95', 'Create Universe Invite And Join Request Tables', 'SQL', 'V95__Create_Universe_Invite_And_Join_Request_Tables.sql', -1494912489, 'SA', TIMESTAMP '2026-06-23 02:00:43.410022', 2, TRUE),
(94, '96', 'Fix Account FK On Delete Rules', 'SQL', 'V96__Fix_Account_FK_On_Delete_Rules.sql', 1147779210, 'SA', TIMESTAMP '2026-06-23 02:00:43.42324', 9, TRUE),
(95, '97', 'Create Account Tutorial Completion', 'SQL', 'V97__Create_Account_Tutorial_Completion.sql', -558961027, 'SA', TIMESTAMP '2026-06-23 02:00:43.42814', 1, TRUE),
(96, '98', 'Add Current Encounter Id To Campaign State', 'SQL', 'V98__Add_Current_Encounter_Id_To_Campaign_State.sql', 746933968, 'SA', TIMESTAMP '2026-06-23 02:00:43.43434', 3, TRUE),
(97, '99', 'Add Subject To Inbox Item', 'SQL', 'V99__Add_Subject_To_Inbox_Item.sql', 1565727751, 'SA', TIMESTAMP '2026-06-23 02:00:43.439304', 1, TRUE),
(98, '100', 'Add Urgency To Inbox Item', 'SQL', 'V100__Add_Urgency_To_Inbox_Item.sql', 1277308947, 'SA', TIMESTAMP '2026-06-23 02:00:43.443437', 1, TRUE),
(99, '101', 'Add Action Fields To Inbox Item', 'SQL', 'V101__Add_Action_Fields_To_Inbox_Item.sql', 413950941, 'SA', TIMESTAMP '2026-06-23 02:00:43.448452', 2, TRUE),
(100, '102', 'Remove Notion Sync Columns', 'SQL', 'V102__Remove_Notion_Sync_Columns.sql', -1701633824, 'SA', TIMESTAMP '2026-06-23 02:00:43.632696', 176, TRUE),
(101, '103', 'Set Rivalry Resolution Defaults', 'SQL', 'V103__Set_Rivalry_Resolution_Defaults.sql', 182746202, 'SA', TIMESTAMP '2026-06-23 02:00:43.637742', 1, TRUE),
(102, '104', 'Add Expansion Code To Feature Entities', 'SQL', 'V104__Add_Expansion_Code_To_Feature_Entities.sql', 1118552986, 'SA', TIMESTAMP '2026-06-23 02:00:43.648804', 8, TRUE);            
INSERT INTO "PUBLIC"."flyway_schema_history" VALUES
(103, '105', 'Upgrade Gemini Model Name', 'SQL', 'V105__Upgrade_Gemini_Model_Name.sql', -886320525, 'SA', TIMESTAMP '2026-06-23 02:00:43.652257', 0, TRUE);
CREATE INDEX "PUBLIC"."flyway_schema_history_s_idx" ON "PUBLIC"."flyway_schema_history"("success" NULLS FIRST);
CREATE CACHED TABLE "PUBLIC"."SHOW_TYPE"(
    "SHOW_TYPE_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1 RESTART WITH 2) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING,
    "IS_PPV" BOOLEAN DEFAULT FALSE NOT NULL,
    "CREATION_DATE" TIMESTAMP NOT NULL,
    "EXPECTED_MATCHES" INTEGER DEFAULT 0 NOT NULL,
    "EXPECTED_PROMOS" INTEGER DEFAULT 0 NOT NULL
);       
ALTER TABLE "PUBLIC"."SHOW_TYPE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_3" PRIMARY KEY("SHOW_TYPE_ID");           
-- 1 +/- SELECT COUNT(*) FROM PUBLIC.SHOW_TYPE;
INSERT INTO "PUBLIC"."SHOW_TYPE" VALUES
(1, 'Weekly Show', NULL, FALSE, TIMESTAMP '2026-06-23 02:00:43.747385', 0, 0);         
CREATE CACHED TABLE "PUBLIC"."NEWS_ITEM"(
    "NEWS_ITEM_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "HEADLINE" CHARACTER VARYING(255) NOT NULL,
    "CONTENT" CHARACTER VARYING(2000) NOT NULL,
    "PUBLISH_DATE" TIMESTAMP NOT NULL,
    "CATEGORY" CHARACTER VARYING(50) NOT NULL,
    "IS_RUMOR" BOOLEAN DEFAULT FALSE NOT NULL,
    "IMPORTANCE" INTEGER DEFAULT 3 NOT NULL
);               
ALTER TABLE "PUBLIC"."NEWS_ITEM" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_B9C" PRIMARY KEY("NEWS_ITEM_ID");         
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.NEWS_ITEM;
CREATE CACHED TABLE "PUBLIC"."ACCOUNT"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1 RESTART WITH 5) NOT NULL,
    "USERNAME" CHARACTER VARYING(50) NOT NULL,
    "PASSWORD" CHARACTER VARYING(100) NOT NULL,
    "EMAIL" CHARACTER VARYING(100) NOT NULL,
    "ENABLED" BOOLEAN DEFAULT TRUE NOT NULL,
    "ACCOUNT_NON_EXPIRED" BOOLEAN DEFAULT TRUE NOT NULL,
    "ACCOUNT_NON_LOCKED" BOOLEAN DEFAULT TRUE NOT NULL,
    "CREDENTIALS_NON_EXPIRED" BOOLEAN DEFAULT TRUE NOT NULL,
    "FAILED_LOGIN_ATTEMPTS" INTEGER DEFAULT 0 NOT NULL,
    "LOCKED_UNTIL" TIMESTAMP,
    "LAST_LOGIN" TIMESTAMP,
    "CREATED_DATE" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "UPDATED_DATE" TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    "THEME_PREFERENCE" CHARACTER VARYING(50),
    "ACTIVE_WRESTLER_ID" BIGINT,
    "LEGACY_SCORE" BIGINT DEFAULT 0 NOT NULL,
    "PRESTIGE" BIGINT DEFAULT 0 NOT NULL,
    "SHOWS_BOOKED" INTEGER DEFAULT 0 NOT NULL
); 
ALTER TABLE "PUBLIC"."ACCOUNT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_E4" PRIMARY KEY("ID");      
-- 4 +/- SELECT COUNT(*) FROM PUBLIC.ACCOUNT;  
INSERT INTO "PUBLIC"."ACCOUNT" VALUES
(1, 'admin', '$2a$10$wKGJ2IuP7HwMP66VaqSdYuqo3S1lcXpl9oqQkTGuLaDYHfbH57hD6', 'admin@atwrpg.local', TRUE, TRUE, TRUE, TRUE, 0, NULL, NULL, TIMESTAMP '2026-06-23 02:00:40.948539', TIMESTAMP '2026-06-23 02:00:40.948539', NULL, NULL, 0, 0, 0),
(2, 'booker', '$2a$10$OrFNvKFkH5s/DvDzd301Me4v9bpIulbPNasymqmaxCqaUM.kVXHEi', 'booker@atwrpg.local', TRUE, TRUE, TRUE, TRUE, 0, NULL, NULL, TIMESTAMP '2026-06-23 02:00:40.948539', TIMESTAMP '2026-06-23 02:00:40.948539', NULL, NULL, 0, 0, 0),
(3, 'player', '$2a$10$oHciydemMfshOLiGK7g4KO.Epu07svrzinu7PFvdJws5PYK3pIKx.', 'player@atwrpg.local', TRUE, TRUE, TRUE, TRUE, 0, NULL, NULL, TIMESTAMP '2026-06-23 02:00:40.948539', TIMESTAMP '2026-06-23 02:00:40.948539', NULL, NULL, 0, 0, 0),
(4, 'viewer', '$2a$10$no8XHshPMFd14eBxIs9e2uYW8bXm/pT6MOZsXnw.RHyhmRWgvok06', 'viewer@atwrpg.local', TRUE, TRUE, TRUE, TRUE, 0, NULL, NULL, TIMESTAMP '2026-06-23 02:00:40.948539', TIMESTAMP '2026-06-23 02:00:40.948539', NULL, NULL, 0, 0, 0);    
CREATE INDEX "PUBLIC"."IDX_ACCOUNT_USERNAME" ON "PUBLIC"."ACCOUNT"("USERNAME" NULLS FIRST);    
CREATE INDEX "PUBLIC"."IDX_ACCOUNT_EMAIL" ON "PUBLIC"."ACCOUNT"("EMAIL" NULLS FIRST);          
CREATE INDEX "PUBLIC"."IDX_ACCOUNT_ENABLED" ON "PUBLIC"."ACCOUNT"("ENABLED" NULLS FIRST);      
CREATE CACHED TABLE "PUBLIC"."NPC"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "NPC_TYPE" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING(4000),
    "IMAGE_URL" CHARACTER VARYING(255),
    "GENDER" CHARACTER VARYING(255),
    "ALIGNMENT" CHARACTER VARYING(255),
    "ATTRIBUTES" CHARACTER VARYING,
    "EXPANSION_CODE" CHARACTER VARYING(255) DEFAULT 'BASE_GAME' NOT NULL
);       
ALTER TABLE "PUBLIC"."NPC" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_1" PRIMARY KEY("ID");           
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.NPC;      
CREATE CACHED TABLE "PUBLIC"."LOCATION"(
    "LOCATION_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER LARGE OBJECT,
    "IMAGE_URL" CHARACTER VARYING(255),
    "EXTERNAL_ID" CHARACTER VARYING(255),
    "LAST_SYNC" TIMESTAMP WITH TIME ZONE
); 
ALTER TABLE "PUBLIC"."LOCATION" ADD CONSTRAINT "PUBLIC"."PK_LOCATION" PRIMARY KEY("LOCATION_ID");              
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.LOCATION; 
CREATE CACHED TABLE "PUBLIC"."GAME_SETTING"(
    "SETTING_KEY" CHARACTER VARYING(255) NOT NULL,
    "SETTING_VALUE" CHARACTER VARYING(255) NOT NULL,
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1 RESTART WITH 33) NOT NULL,
    "UNIVERSE_ID" BIGINT
);      
ALTER TABLE "PUBLIC"."GAME_SETTING" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_8E" PRIMARY KEY("ID"); 
-- 32 +/- SELECT COUNT(*) FROM PUBLIC.GAME_SETTING;            
INSERT INTO "PUBLIC"."GAME_SETTING" VALUES
('AI_TIMEOUT', '300', 1, NULL),
('AI_PROVIDER_AUTO', 'true', 2, NULL),
('AI_OPENAI_ENABLED', 'false', 3, NULL),
('AI_OPENAI_API_URL', 'https://api.openai.com/v1/chat/completions', 4, NULL),
('AI_OPENAI_DEFAULT_MODEL', 'gpt-3.5-turbo', 5, NULL),
('AI_OPENAI_PREMIUM_MODEL', 'gpt-4', 6, NULL),
('AI_OPENAI_MAX_TOKENS', '1000', 7, NULL),
('AI_OPENAI_TEMPERATURE', '0.7', 8, NULL),
('AI_OPENAI_API_KEY', '', 9, 1),
('AI_CLAUDE_ENABLED', 'false', 10, NULL),
('AI_CLAUDE_API_URL', 'https://api.anthropic.com/v1/messages/', 11, NULL),
('AI_CLAUDE_MODEL_NAME', 'claude-3-haiku-20240307', 12, NULL),
('AI_CLAUDE_API_KEY', '', 13, 1),
('AI_GEMINI_ENABLED', 'false', 14, NULL),
('AI_GEMINI_API_URL', 'https://generativelanguage.googleapis.com/v1beta/models/', 15, NULL),
('AI_GEMINI_MODEL_NAME', 'gemini-3.1-flash-lite-preview', 16, NULL),
('AI_GEMINI_API_KEY', '', 17, 1),
('default_theme', 'light', 18, NULL),
('set_enabled_BASE_GAME', 'true', 19, NULL),
('set_enabled_EDDIE', 'true', 20, NULL),
('set_enabled_EXTREME', 'true', 21, NULL),
('set_enabled_MATT_CARDONA', 'true', 22, NULL),
('set_enabled_RUMBLE', 'true', 23, NULL),
('set_enabled_TRAILBLAZERS', 'true', 24, NULL),
('rivalry_resolution_threshold_ple', '30', 25, NULL),
('rivalry_resolution_threshold_regular', '35', 26, NULL),
('rivalry_resolution_on_regular_shows', 'false', 27, NULL),
('rivalry_max_duration_days', '0', 28, NULL),
('rivalry_heat_decay_enabled', 'false', 29, NULL),
('rivalry_heat_decay_per_interval', '1', 30, NULL),
('rivalry_heat_decay_interval_days', '7', 31, NULL),
('rivalry_resolution_min_heat', '10', 32, NULL);              
CREATE CACHED TABLE "PUBLIC"."PASSWORD_RESET_TOKEN"(
    "ID" BIGINT NOT NULL,
    "TOKEN" CHARACTER VARYING(255),
    "ACCOUNT_ID" BIGINT NOT NULL,
    "EXPIRY_DATE" TIMESTAMP
);            
ALTER TABLE "PUBLIC"."PASSWORD_RESET_TOKEN" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_C9" PRIMARY KEY("ID");         
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.PASSWORD_RESET_TOKEN;     
CREATE CACHED TABLE "PUBLIC"."RINGSIDE_ACTION"(
    "RINGSIDE_ACTION_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING(1000),
    "IMPACT" INTEGER NOT NULL,
    "RISK" INTEGER NOT NULL,
    "ALIGNMENT" CHARACTER VARYING(255) NOT NULL,
    "RINGSIDE_ACTION_TYPE_ID" BIGINT NOT NULL,
    "EXPANSION_CODE" CHARACTER VARYING(50) DEFAULT 'BASE_GAME' NOT NULL
);         
ALTER TABLE "PUBLIC"."RINGSIDE_ACTION" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_63" PRIMARY KEY("RINGSIDE_ACTION_ID");              
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.RINGSIDE_ACTION;          
CREATE CACHED TABLE "PUBLIC"."SEGMENT_RULE"(
    "SEGMENT_RULE_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING,
    "REQUIRES_HIGH_HEAT" BOOLEAN DEFAULT FALSE NOT NULL,
    "CREATION_DATE" TIMESTAMP NOT NULL,
    "BUMP_ADDITION" CHARACTER VARYING(255) DEFAULT 'NONE' NOT NULL,
    "NO_DQ" BOOLEAN DEFAULT FALSE NOT NULL,
    "EXPANSION_CODE" CHARACTER VARYING(50) DEFAULT 'BASE_GAME' NOT NULL
);
ALTER TABLE "PUBLIC"."SEGMENT_RULE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_ABC" PRIMARY KEY("SEGMENT_RULE_ID");   
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.SEGMENT_RULE;             
CREATE CACHED TABLE "PUBLIC"."TIER_BOUNDARY"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "TIER" CHARACTER VARYING(255) NOT NULL,
    "GENDER" CHARACTER VARYING(255) NOT NULL,
    "MIN_FANS" BIGINT NOT NULL,
    "MAX_FANS" BIGINT NOT NULL,
    "CHALLENGE_COST" BIGINT NOT NULL,
    "CONTENDER_ENTRY_FEE" BIGINT NOT NULL
);            
ALTER TABLE "PUBLIC"."TIER_BOUNDARY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_8B" PRIMARY KEY("ID");
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.TIER_BOUNDARY;            
CREATE CACHED TABLE "PUBLIC"."ROLE"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1 RESTART WITH 5) NOT NULL,
    "NAME" CHARACTER VARYING(50) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING(500)
);            
ALTER TABLE "PUBLIC"."ROLE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_267" PRIMARY KEY("ID");        
-- 4 +/- SELECT COUNT(*) FROM PUBLIC.ROLE;     
INSERT INTO "PUBLIC"."ROLE" VALUES
(1, 'ADMIN', 'Full system access - can manage accounts and all content'),
(2, 'BOOKER', 'Can manage shows, wrestlers, and content but not system administration'),
(3, 'PLAYER', 'Can manage own content and view most data'),
(4, 'VIEWER', 'Read-only access to content');
CREATE INDEX "PUBLIC"."IDX_ROLE_NAME" ON "PUBLIC"."ROLE"("NAME" NULLS FIRST);  
CREATE CACHED TABLE "PUBLIC"."HOLIDAY"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1 RESTART WITH 10) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING(255) NOT NULL,
    "THEME" CHARACTER VARYING(255) NOT NULL,
    "DECORATIONS" CHARACTER VARYING,
    "DAY_OF_MONTH" INTEGER,
    "HOLIDAY_MONTH" CHARACTER VARYING(255),
    "DAY_OF_WEEK" CHARACTER VARYING(255),
    "WEEK_OF_MONTH" INTEGER,
    "TYPE" CHARACTER VARYING(255) NOT NULL,
    "CREATION_DATE" TIMESTAMP NOT NULL,
    "EXTERNAL_ID" CHARACTER VARYING(255),
    "LAST_SYNC" TIMESTAMP
);
ALTER TABLE "PUBLIC"."HOLIDAY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_6B" PRIMARY KEY("ID");      
-- 9 +/- SELECT COUNT(*) FROM PUBLIC.HOLIDAY;  
INSERT INTO "PUBLIC"."HOLIDAY" VALUES
(1, 'New Year''s Day', 'New Year''s Day', U&'New Year\2019s Day decorations are typically clean, festive, and hopeful in tone. They often feature metallic accents like gold, silver, and champagne, paired with white or soft neutrals to suggest a fresh start. Banners and signage display the new year, while streamers, balloons, and confetti add energy without feeling heavy. Clocks, stars, and fireworks motifs symbolize time, renewal, and celebration. Table settings may include sparkling centerpieces, candles, and subtle glitter, creating a bright, optimistic atmosphere that feels celebratory but calm\2014marking both reflection and new beginnings.', 1, 'JANUARY', NULL, NULL, 'FIXED', TIMESTAMP '2026-06-23 02:00:41.034276', NULL, NULL),
(2, 'Valentine''s Day', 'Valentine''s Day', U&'Valentine\2019s Day decorations create a warm, romantic atmosphere centered on **reds, pinks, and soft whites**. Common elements include **hearts, roses, and love-themed banners**, often accented with **lace, ribbons, and soft lighting** like candles or string lights. **Floral arrangements, plush accents, and subtle metallic touches** add elegance, while table settings may feature **romantic centerpieces and themed place cards**, setting a cozy, intimate mood focused on love and affection.\000a', 14, 'FEBRUARY', NULL, NULL, 'FIXED', TIMESTAMP '2026-06-23 02:00:41.034276', NULL, NULL),
(3, 'St. Patrick''s Day', 'St. Patrick''s Day', U&'St. Patrick\2019s Day decorations are bright and festive, dominated by **shades of green** with accents of **gold and white**. Common elements include **shamrocks, leprechauns, rainbows, and pots of gold**, often paired with **Irish flags or Celtic patterns**. **Banners, garlands, and themed table d\00e9cor** add a playful touch, while touches of **gold foil or glitter** bring a sense of luck and celebration, creating a cheerful, lively atmosphere rooted in Irish tradition.\000a', 17, 'MARCH', NULL, NULL, 'FIXED', TIMESTAMP '2026-06-23 02:00:41.034276', NULL, NULL),
(4, 'Independence Day', 'Independence Day', U&'USA Independence Day decorations are bold and patriotic, featuring **red, white, and blue** throughout. Common elements include **American flags, stars, stripes, and bunting**, often paired with **fireworks imagery**. **Banners, balloons, and table d\00e9cor** showcase patriotic patterns, while **rustic or outdoor accents** like lanterns and string lights enhance the celebratory feel. The overall atmosphere is energetic and proud, reflecting national unity and summer celebration.\000a', 4, 'JULY', NULL, NULL, 'FIXED', TIMESTAMP '2026-06-23 02:00:41.034276', NULL, NULL),
(5, 'Halloween', 'Halloween', U&'Halloween decorations create a spooky yet playful atmosphere using **black, orange, and purple** as the primary colors. Common elements include **pumpkins, jack-o\2019-lanterns, ghosts, bats, spiders, and cobwebs**, often paired with **dim lighting, candles, or colored lights**. **Haunted house props, eerie silhouettes, and fog effects** add drama, while whimsical touches keep the mood fun and festive rather than frightening.\000a', 31, 'OCTOBER', NULL, NULL, 'FIXED', TIMESTAMP '2026-06-23 02:00:41.034276', NULL, NULL),
(6, 'Christmas Day', 'Christmas Day', U&'Christmas Day decorations create a warm, joyful atmosphere centered on **reds, greens, golds, and whites**. Common elements include **Christmas trees adorned with ornaments, lights, and garlands**, along with **wreaths, stockings, and nativity scenes**. **Twinkling lights, candles, and festive table settings** add warmth and sparkle, while touches of **pine, holly, and ribbon** evoke tradition, togetherness, and holiday cheer.\000a', 25, 'DECEMBER', NULL, NULL, 'FIXED', TIMESTAMP '2026-06-23 02:00:41.034276', NULL, NULL),
(7, 'Memorial Day', 'Memorial Day', U&'Memorial Day decorations are respectful and patriotic, featuring **red, white, and blue** with a more subdued tone than other holidays. Common elements include **American flags, banners, and bunting**, often paired with **stars, ribbons, and wreaths**. **Floral arrangements**, especially red and white flowers, and **simple table d\00e9cor** reflect remembrance and honor, creating an atmosphere that balances national pride with solemn respect.\000a', NULL, 'MAY', 'MONDAY', -1, 'FLOATING', TIMESTAMP '2026-06-23 02:00:41.034276', NULL, NULL);   
INSERT INTO "PUBLIC"."HOLIDAY" VALUES
(8, 'Labor Day', 'Labor Day', U&'Labor Day decorations are casual and patriotic, reflecting both national pride and the spirit of the working community. They often feature **red, white, and blue** with simple, relaxed elements like **flags, banners, and bunting**. **Outdoor-friendly d\00e9cor**, such as table coverings, string lights, and picnic accents, is common, creating a laid-back, celebratory atmosphere that marks the end of summer and honors workers\2019 contributions.\000a', NULL, 'SEPTEMBER', 'MONDAY', 1, 'FLOATING', TIMESTAMP '2026-06-23 02:00:41.034276', NULL, NULL),
(9, 'Thanksgiving', 'Thanksgiving', U&'Thanksgiving decorations create a warm, welcoming atmosphere inspired by the **fall harvest**. They feature **earthy tones** like orange, brown, gold, and deep red, with elements such as **pumpkins, gourds, autumn leaves, and cornucopias**. **Rustic table settings, candles, and natural textures** like wood and burlap add coziness, emphasizing gratitude, abundance, and togetherness.\000a', NULL, 'NOVEMBER', 'THURSDAY', 4, 'FLOATING', TIMESTAMP '2026-06-23 02:00:41.034276', NULL, NULL);
CREATE CACHED TABLE "PUBLIC"."TITLE_REIGN"(
    "TITLE_REIGN_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "TITLE_ID" BIGINT NOT NULL,
    "START_DATE" TIMESTAMP NOT NULL,
    "END_DATE" TIMESTAMP,
    "REIGN_NUMBER" INTEGER DEFAULT 1 NOT NULL,
    "NOTES" CHARACTER VARYING,
    "CREATION_DATE" TIMESTAMP NOT NULL,
    "WON_AT_SEGMENT_ID" BIGINT
);        
ALTER TABLE "PUBLIC"."TITLE_REIGN" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_EA" PRIMARY KEY("TITLE_REIGN_ID");      
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.TITLE_REIGN;              
CREATE CACHED TABLE "PUBLIC"."SHOW_TEMPLATE"(
    "TEMPLATE_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1 RESTART WITH 2) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING,
    "SHOW_TYPE_ID" BIGINT NOT NULL,
    "NOTION_URL" CHARACTER VARYING(500),
    "CREATION_DATE" TIMESTAMP NOT NULL,
    "IMAGE_URL" CHARACTER VARYING(512),
    "EXPECTED_MATCHES" INTEGER,
    "EXPECTED_PROMOS" INTEGER,
    "DURATION_DAYS" INTEGER DEFAULT 1,
    "RECURRENCE_TYPE" CHARACTER VARYING(20) DEFAULT 'NONE',
    "RECURRENCE_DAY_OF_WEEK" CHARACTER VARYING(20),
    "RECURRENCE_DAY_OF_MONTH" INTEGER,
    "RECURRENCE_WEEK_OF_MONTH" INTEGER,
    "RECURRENCE_MONTH" CHARACTER VARYING(20),
    "COMMENTARY_TEAM_ID" BIGINT,
    "GENDER_CONSTRAINT" CHARACTER VARYING(20)
);              
ALTER TABLE "PUBLIC"."SHOW_TEMPLATE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_5" PRIMARY KEY("TEMPLATE_ID");        
-- 1 +/- SELECT COUNT(*) FROM PUBLIC.SHOW_TEMPLATE;            
INSERT INTO "PUBLIC"."SHOW_TEMPLATE" VALUES
(1, 'Reference Template', NULL, 1, NULL, TIMESTAMP '2026-06-23 02:00:43.747561', NULL, NULL, NULL, 1, 'NONE', NULL, NULL, NULL, NULL, NULL, NULL); 
CREATE CACHED TABLE "PUBLIC"."INBOX_ITEM"(
    "INBOX_ITEM_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "EVENT_TYPE" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING(1024) NOT NULL,
    "EVENT_TIMESTAMP" TIMESTAMP NOT NULL,
    "IS_READ" BOOLEAN DEFAULT FALSE NOT NULL,
    "EXTERNAL_ID" CHARACTER VARYING(255),
    "LAST_SYNC" TIMESTAMP,
    "SUBJECT" CHARACTER VARYING(255),
    "URGENCY" CHARACTER VARYING(30) DEFAULT 'INFO' NOT NULL,
    "ACTION_TYPE" CHARACTER VARYING(50),
    "ACTION_PAYLOAD" CHARACTER VARYING(512)
);   
ALTER TABLE "PUBLIC"."INBOX_ITEM" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_8C" PRIMARY KEY("INBOX_ITEM_ID");        
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.INBOX_ITEM;               
CREATE CACHED TABLE "PUBLIC"."SEGMENT_TYPE"(
    "SEGMENT_TYPE_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1 RESTART WITH 2) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING,
    "CREATION_DATE" TIMESTAMP NOT NULL,
    "EXPANSION_CODE" CHARACTER VARYING(50) DEFAULT 'BASE_GAME' NOT NULL
);          
ALTER TABLE "PUBLIC"."SEGMENT_TYPE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_A" PRIMARY KEY("SEGMENT_TYPE_ID");     
-- 1 +/- SELECT COUNT(*) FROM PUBLIC.SEGMENT_TYPE;             
INSERT INTO "PUBLIC"."SEGMENT_TYPE" VALUES
(1, 'Match', NULL, TIMESTAMP '2026-06-23 02:00:43.749087', 'BASE_GAME');            
CREATE CACHED TABLE "PUBLIC"."WRESTLER_CONTRACT"(
    "CONTRACT_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "WRESTLER_ID" BIGINT NOT NULL,
    "LEAGUE_ID" BIGINT NOT NULL,
    "SALARY_PER_SHOW" DECIMAL(19, 2) NOT NULL,
    "START_DATE" TIMESTAMP NOT NULL,
    "EXPIRY_DATE" TIMESTAMP,
    "DURATION_WEEKS" INTEGER NOT NULL,
    "IS_INITIAL_DRAFT" BOOLEAN DEFAULT FALSE NOT NULL,
    "IS_ACTIVE" BOOLEAN DEFAULT TRUE NOT NULL,
    "CREATION_DATE" TIMESTAMP NOT NULL
);
ALTER TABLE "PUBLIC"."WRESTLER_CONTRACT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_1C" PRIMARY KEY("CONTRACT_ID");   
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.WRESTLER_CONTRACT;        
CREATE INDEX "PUBLIC"."IDX_CONTRACT_LEAGUE" ON "PUBLIC"."WRESTLER_CONTRACT"("LEAGUE_ID" NULLS FIRST);          
CREATE INDEX "PUBLIC"."IDX_CONTRACT_WRESTLER" ON "PUBLIC"."WRESTLER_CONTRACT"("WRESTLER_ID" NULLS FIRST);      
CREATE CACHED TABLE "PUBLIC"."DECK"(
    "DECK_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "WRESTLER_ID" BIGINT NOT NULL,
    "CREATION_DATE" TIMESTAMP NOT NULL
);
ALTER TABLE "PUBLIC"."DECK" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_1FF" PRIMARY KEY("DECK_ID");   
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.DECK;     
CREATE CACHED TABLE "PUBLIC"."COMMENTARY_TEAM"(
    "TEAM_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "EXTERNAL_ID" CHARACTER VARYING(255),
    "LAST_SYNC" TIMESTAMP
);               
ALTER TABLE "PUBLIC"."COMMENTARY_TEAM" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_6B5" PRIMARY KEY("TEAM_ID");        
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.COMMENTARY_TEAM;          
CREATE CACHED TABLE "PUBLIC"."CARD"(
    "CARD_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "TYPE" CHARACTER VARYING(100) NOT NULL,
    "DAMAGE" INTEGER DEFAULT 0 NOT NULL,
    "STAMINA" INTEGER DEFAULT 0 NOT NULL,
    "MOMENTUM" INTEGER DEFAULT 0 NOT NULL,
    "TARGET" INTEGER DEFAULT 0 NOT NULL,
    "NUMBER" INTEGER NOT NULL,
    "FINISHER" BOOLEAN DEFAULT FALSE NOT NULL,
    "SIGNATURE" BOOLEAN DEFAULT FALSE NOT NULL,
    "PIN" BOOLEAN DEFAULT FALSE NOT NULL,
    "TAUNT" BOOLEAN DEFAULT FALSE NOT NULL,
    "RECOVER" BOOLEAN DEFAULT FALSE NOT NULL,
    "CREATION_DATE" TIMESTAMP NOT NULL,
    "SET_ID" BIGINT NOT NULL
);    
ALTER TABLE "PUBLIC"."CARD" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_1F" PRIMARY KEY("CARD_ID");    
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.CARD;     
CREATE CACHED TABLE "PUBLIC"."DECK_CARD"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "DECK_ID" BIGINT NOT NULL,
    "CARD_ID" BIGINT NOT NULL,
    "AMOUNT" INTEGER DEFAULT 1 NOT NULL,
    "CREATION_DATE" TIMESTAMP NOT NULL,
    "SET_ID" BIGINT NOT NULL
);              
ALTER TABLE "PUBLIC"."DECK_CARD" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_BF" PRIMARY KEY("ID");    
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.DECK_CARD;
CREATE CACHED TABLE "PUBLIC"."RIVALRY"(
    "RIVALRY_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "WRESTLER1_ID" BIGINT NOT NULL,
    "WRESTLER2_ID" BIGINT NOT NULL,
    "HEAT" INTEGER DEFAULT 0 NOT NULL,
    "IS_ACTIVE" BOOLEAN DEFAULT TRUE NOT NULL,
    "STARTED_DATE" TIMESTAMP NOT NULL,
    "ENDED_DATE" TIMESTAMP,
    "STORYLINE_NOTES" CHARACTER VARYING,
    "CREATION_DATE" TIMESTAMP NOT NULL,
    "PRIORITY" INTEGER DEFAULT 0,
    "LEAGUE_ID" BIGINT,
    "UNIVERSE_ID" BIGINT NOT NULL
);      
ALTER TABLE "PUBLIC"."RIVALRY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_7" PRIMARY KEY("RIVALRY_ID");               
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.RIVALRY;  
CREATE INDEX "PUBLIC"."IDX_RIVALRY_WRESTLERS" ON "PUBLIC"."RIVALRY"("WRESTLER1_ID" NULLS FIRST, "WRESTLER2_ID" NULLS FIRST);   
CREATE CACHED TABLE "PUBLIC"."WRESTLING_SHOW"(
    "SHOW_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1 RESTART WITH 2) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING,
    "SHOW_DATE" TIMESTAMP,
    "SHOW_TYPE_ID" BIGINT NOT NULL,
    "SEASON_ID" BIGINT,
    "TEMPLATE_ID" BIGINT,
    "CREATION_DATE" TIMESTAMP NOT NULL,
    "LEAGUE_ID" BIGINT,
    "COMMENTARY_TEAM_ID" BIGINT,
    "ARENA_ID" BIGINT,
    "ATTENDANCE" INTEGER DEFAULT 0,
    "GATE_REVENUE" DECIMAL(19, 2) DEFAULT 0.00,
    "UNIVERSE_ID" BIGINT
);          
ALTER TABLE "PUBLIC"."WRESTLING_SHOW" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_26" PRIMARY KEY("SHOW_ID");          
-- 1 +/- SELECT COUNT(*) FROM PUBLIC.WRESTLING_SHOW;           
INSERT INTO "PUBLIC"."WRESTLING_SHOW" VALUES
(1, 'Reference Show', NULL, NULL, 1, NULL, NULL, TIMESTAMP '2026-06-23 02:00:43.747915', NULL, NULL, NULL, 0, 0.00, NULL);        
CREATE CACHED TABLE "PUBLIC"."UNIVERSE"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1 RESTART WITH 2) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "TYPE" CHARACTER VARYING(50) NOT NULL,
    "CREATION_DATE" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);             
ALTER TABLE "PUBLIC"."UNIVERSE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_1D" PRIMARY KEY("ID");     
-- 1 +/- SELECT COUNT(*) FROM PUBLIC.UNIVERSE; 
INSERT INTO "PUBLIC"."UNIVERSE" VALUES
(1, 'Default Universe', 'GLOBAL', TIMESTAMP '2026-06-23 02:00:43.191056');              
CREATE CACHED TABLE "PUBLIC"."TITLE"(
    "TITLE_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1 RESTART WITH 2) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING,
    "TIER" CHARACTER VARYING(255),
    "GENDER" CHARACTER VARYING(255),
    "IS_ACTIVE" BOOLEAN DEFAULT TRUE NOT NULL,
    "CREATION_DATE" TIMESTAMP NOT NULL,
    "CHAMPIONSHIP_TYPE" CHARACTER VARYING(255) DEFAULT 'SINGLE' NOT NULL,
    "INCLUDE_IN_RANKINGS" BOOLEAN DEFAULT TRUE NOT NULL,
    "IMAGE_URL" CHARACTER VARYING(512),
    "EFFECT_SCRIPT" CHARACTER VARYING(255),
    "UNIVERSE_ID" BIGINT,
    "DEFENSE_FREQUENCY_TYPE" CHARACTER VARYING(50),
    "EXPANSION_CODE" CHARACTER VARYING(50) DEFAULT 'BASE_GAME' NOT NULL
);            
ALTER TABLE "PUBLIC"."TITLE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_4" PRIMARY KEY("TITLE_ID");   
-- 1 +/- SELECT COUNT(*) FROM PUBLIC.TITLE;    
INSERT INTO "PUBLIC"."TITLE" VALUES
(1, 'Reference Championship', NULL, NULL, NULL, TRUE, TIMESTAMP '2026-06-23 02:00:43.747273', 'SINGLE', TRUE, NULL, NULL, NULL, NULL, 'BASE_GAME');        
CREATE CACHED TABLE "PUBLIC"."WRESTLER_RELATIONSHIP"(
    "RELATIONSHIP_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "WRESTLER1_ID" BIGINT NOT NULL,
    "WRESTLER2_ID" BIGINT NOT NULL,
    "RELATIONSHIP_TYPE" CHARACTER VARYING(255) NOT NULL,
    "LEVEL" INTEGER DEFAULT 50 NOT NULL,
    "IS_STORYLINE" BOOLEAN DEFAULT FALSE NOT NULL,
    "STARTED_DATE" TIMESTAMP NOT NULL,
    "CREATION_DATE" TIMESTAMP NOT NULL,
    "NOTES" CHARACTER VARYING(1000)
); 
ALTER TABLE "PUBLIC"."WRESTLER_RELATIONSHIP" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_A1" PRIMARY KEY("RELATIONSHIP_ID");           
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.WRESTLER_RELATIONSHIP;    
CREATE INDEX "PUBLIC"."IDX_REL_WRESTLERS" ON "PUBLIC"."WRESTLER_RELATIONSHIP"("WRESTLER1_ID" NULLS FIRST, "WRESTLER2_ID" NULLS FIRST);         
CREATE CACHED TABLE "PUBLIC"."INJURY_TYPE"(
    "INJURY_TYPE_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1 RESTART WITH 3) NOT NULL,
    "INJURY_NAME" CHARACTER VARYING(100) NOT NULL,
    "HEALTH_EFFECT" INTEGER,
    "STAMINA_EFFECT" INTEGER,
    "CARD_EFFECT" INTEGER,
    "SPECIAL_EFFECTS" CHARACTER VARYING,
    "EXTERNAL_ID" CHARACTER VARYING(255),
    "LAST_SYNC" TIMESTAMP
);       
ALTER TABLE "PUBLIC"."INJURY_TYPE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_C" PRIMARY KEY("INJURY_TYPE_ID");       
-- 2 +/- SELECT COUNT(*) FROM PUBLIC.INJURY_TYPE;              
INSERT INTO "PUBLIC"."INJURY_TYPE" VALUES
(1, 'Legacy Injury', 0, 0, 0, 'Placeholder for injuries that existed before injury types were introduced. Update to the correct type when known.', NULL, NULL),
(2, 'Sprain', -2, -1, 0, NULL, NULL, NULL);          
CREATE CACHED TABLE "PUBLIC"."CAMPAIGN"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1 RESTART WITH 2) NOT NULL,
    "WRESTLER_ID" BIGINT NOT NULL,
    "STATUS" CHARACTER VARYING(50) NOT NULL,
    "STARTED_AT" TIMESTAMP,
    "ENDED_AT" TIMESTAMP,
    "UNIVERSE_ID" BIGINT
);             
ALTER TABLE "PUBLIC"."CAMPAIGN" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_264" PRIMARY KEY("ID");    
-- 1 +/- SELECT COUNT(*) FROM PUBLIC.CAMPAIGN; 
INSERT INTO "PUBLIC"."CAMPAIGN" VALUES
(1, 1, 'ACTIVE', TIMESTAMP '2026-06-23 02:00:43.748636', NULL, NULL);   
CREATE INDEX "PUBLIC"."IDX_CAMPAIGN_WRESTLER" ON "PUBLIC"."CAMPAIGN"("WRESTLER_ID" NULLS FIRST);               
CREATE CACHED TABLE "PUBLIC"."WRESTLER_ALIGNMENT"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "WRESTLER_ID" BIGINT NOT NULL,
    "CAMPAIGN_ID" BIGINT,
    "ALIGNMENT_TYPE" CHARACTER VARYING(50) NOT NULL,
    "LEVEL" INTEGER DEFAULT 0 NOT NULL,
    "UNIVERSE_ID" BIGINT
);              
ALTER TABLE "PUBLIC"."WRESTLER_ALIGNMENT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_F8" PRIMARY KEY("ID");           
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.WRESTLER_ALIGNMENT;       
CREATE INDEX "PUBLIC"."IDX_WRESTLER_ALIGNMENT_WRESTLER" ON "PUBLIC"."WRESTLER_ALIGNMENT"("WRESTLER_ID" NULLS FIRST);           
CREATE INDEX "PUBLIC"."IDX_WRESTLER_ALIGNMENT_UNIVERSE" ON "PUBLIC"."WRESTLER_ALIGNMENT"("WRESTLER_ID" NULLS FIRST, "UNIVERSE_ID" NULLS FIRST);
CREATE CACHED TABLE "PUBLIC"."RINGSIDE_ACTION_TYPE"(
    "RINGSIDE_ACTION_TYPE_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "INCREASES_AWARENESS" BOOLEAN DEFAULT TRUE NOT NULL,
    "CAN_CAUSE_DQ" BOOLEAN DEFAULT TRUE NOT NULL,
    "BASE_RISK_MULTIPLIER" DOUBLE PRECISION DEFAULT 1.0 NOT NULL
);  
ALTER TABLE "PUBLIC"."RINGSIDE_ACTION_TYPE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_E0" PRIMARY KEY("RINGSIDE_ACTION_TYPE_ID");    
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.RINGSIDE_ACTION_TYPE;     
CREATE CACHED TABLE "PUBLIC"."INBOX_ITEM_TARGET"(
    "INBOX_ITEM_TARGET_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "INBOX_ITEM_ID" BIGINT,
    "TARGET_ID" CHARACTER VARYING(255) NOT NULL,
    "TARGET_TYPE" CHARACTER VARYING(20) DEFAULT 'ACCOUNT' NOT NULL
);
ALTER TABLE "PUBLIC"."INBOX_ITEM_TARGET" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_77" PRIMARY KEY("INBOX_ITEM_TARGET_ID");          
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.INBOX_ITEM_TARGET;        
CREATE CACHED TABLE "PUBLIC"."CAMPAIGN_STORYLINE"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "CAMPAIGN_ID" BIGINT NOT NULL,
    "TITLE" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING(1000),
    "STATUS" CHARACTER VARYING(20) NOT NULL,
    "CURRENT_MILESTONE_ID" BIGINT,
    "STARTED_AT" TIMESTAMP,
    "ENDED_AT" TIMESTAMP
); 
ALTER TABLE "PUBLIC"."CAMPAIGN_STORYLINE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_6A" PRIMARY KEY("ID");           
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.CAMPAIGN_STORYLINE;       
CREATE CACHED TABLE "PUBLIC"."STORYLINE_MILESTONE"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "STORYLINE_ID" BIGINT NOT NULL,
    "TITLE" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING(1000),
    "NARRATIVE_GOAL" CHARACTER VARYING(2000) NOT NULL,
    "STATUS" CHARACTER VARYING(20) NOT NULL,
    "DISPLAY_ORDER" INTEGER NOT NULL,
    "NEXT_ON_SUCCESS_ID" BIGINT,
    "NEXT_ON_FAILURE_ID" BIGINT
);         
ALTER TABLE "PUBLIC"."STORYLINE_MILESTONE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_29" PRIMARY KEY("ID");          
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.STORYLINE_MILESTONE;      
CREATE CACHED TABLE "PUBLIC"."SEGMENT_PARTICIPANT"(
    "SEGMENT_PARTICIPANT_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "SEGMENT_ID" BIGINT NOT NULL,
    "WRESTLER_ID" BIGINT NOT NULL,
    "IS_WINNER" BOOLEAN DEFAULT FALSE NOT NULL,
    "EXTERNAL_ID" CHARACTER VARYING(255),
    "LAST_SYNC" TIMESTAMP,
    "TEAM_NUMBER" INTEGER DEFAULT 1 NOT NULL
);     
ALTER TABLE "PUBLIC"."SEGMENT_PARTICIPANT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_B7" PRIMARY KEY("SEGMENT_PARTICIPANT_ID");      
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.SEGMENT_PARTICIPANT;      
CREATE CACHED TABLE "PUBLIC"."COMMENTATOR"(
    "COMMENTATOR_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "NPC_ID" BIGINT NOT NULL,
    "STYLE" CHARACTER VARYING(255),
    "CATCHPHRASE" CHARACTER VARYING(255),
    "PERSONA_DESCRIPTION" CHARACTER VARYING(4000),
    "EXTERNAL_ID" CHARACTER VARYING(255),
    "LAST_SYNC" TIMESTAMP
);         
ALTER TABLE "PUBLIC"."COMMENTATOR" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_FDB" PRIMARY KEY("COMMENTATOR_ID");     
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.COMMENTATOR;              
CREATE CACHED TABLE "PUBLIC"."ARENA"(
    "ARENA_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER LARGE OBJECT,
    "LOCATION_ID" BIGINT NOT NULL,
    "CAPACITY" INTEGER NOT NULL,
    "ALIGNMENT_BIAS" CHARACTER VARYING(255) NOT NULL,
    "IMAGE_URL" CHARACTER VARYING(255),
    "EXTERNAL_ID" CHARACTER VARYING(255),
    "LAST_SYNC" TIMESTAMP WITH TIME ZONE
);             
ALTER TABLE "PUBLIC"."ARENA" ADD CONSTRAINT "PUBLIC"."PK_ARENA" PRIMARY KEY("ARENA_ID");       
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.ARENA;    
CREATE CACHED TABLE "PUBLIC"."FACTION_HEAT_EVENT"(
    "FACTION_HEAT_EVENT_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "FACTION_RIVALRY_ID" BIGINT NOT NULL,
    "HEAT_CHANGE" INTEGER NOT NULL,
    "HEAT_AFTER_EVENT" INTEGER NOT NULL,
    "REASON" CHARACTER VARYING(255) NOT NULL,
    "EVENT_DATE" TIMESTAMP NOT NULL,
    "CREATION_DATE" TIMESTAMP NOT NULL,
    "EXTERNAL_ID" CHARACTER VARYING(255),
    "LAST_SYNC" TIMESTAMP
);        
ALTER TABLE "PUBLIC"."FACTION_HEAT_EVENT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_6" PRIMARY KEY("FACTION_HEAT_EVENT_ID");         
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.FACTION_HEAT_EVENT;       
CREATE CACHED TABLE "PUBLIC"."CARD_SET"(
    "SET_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "SET_CODE" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING,
    "RELEASE_DATE" DATE,
    "CREATION_DATE" TIMESTAMP NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL
);      
ALTER TABLE "PUBLIC"."CARD_SET" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_2E" PRIMARY KEY("SET_ID"); 
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.CARD_SET; 
CREATE CACHED TABLE "PUBLIC"."STATUS_CARD"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "STATUS_KEY" CHARACTER VARYING(255) NOT NULL,
    "LEVEL_1_NAME" CHARACTER VARYING(255) NOT NULL,
    "LEVEL_2_NAME" CHARACTER VARYING(255),
    "DESCRIPTION" CHARACTER VARYING,
    "POSITIVE" BOOLEAN DEFAULT TRUE NOT NULL,
    "LEVEL_1_EFFECT" CHARACTER VARYING(255),
    "LEVEL_2_EFFECT" CHARACTER VARYING(255),
    "FLIP_UP_CONDITION" CHARACTER VARYING(255),
    "FLIP_DOWN_CONDITION" CHARACTER VARYING(255),
    "DISCARD_CONDITION" CHARACTER VARYING(255)
);         
ALTER TABLE "PUBLIC"."STATUS_CARD" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_4CF" PRIMARY KEY("ID"); 
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.STATUS_CARD;              
CREATE CACHED TABLE "PUBLIC"."SEGMENT_SEGMENT_RULE"(
    "SEGMENT_ID" BIGINT NOT NULL,
    "SEGMENT_RULE_ID" BIGINT NOT NULL
);
ALTER TABLE "PUBLIC"."SEGMENT_SEGMENT_RULE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_C8" PRIMARY KEY("SEGMENT_ID", "SEGMENT_RULE_ID");              
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.SEGMENT_SEGMENT_RULE;     
CREATE CACHED TABLE "PUBLIC"."SEGMENT_TITLE"(
    "SEGMENT_ID" BIGINT NOT NULL,
    "TITLE_ID" BIGINT NOT NULL
);              
ALTER TABLE "PUBLIC"."SEGMENT_TITLE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_CC5" PRIMARY KEY("SEGMENT_ID", "TITLE_ID");           
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.SEGMENT_TITLE;            
CREATE CACHED TABLE "PUBLIC"."DRAMA_EVENT"(
    "DRAMA_EVENT_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1 RESTART WITH 2) NOT NULL,
    "TITLE" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING NOT NULL,
    "EVENT_TYPE" CHARACTER VARYING(255) NOT NULL,
    "SEVERITY" CHARACTER VARYING(255) NOT NULL,
    "EVENT_DATE" TIMESTAMP NOT NULL,
    "CREATION_DATE" TIMESTAMP NOT NULL,
    "HEAT_IMPACT" INTEGER,
    "FAN_IMPACT" INTEGER,
    "INJURY_CAUSED" BOOLEAN NOT NULL,
    "RIVALRY_CREATED" BOOLEAN NOT NULL,
    "RIVALRY_ENDED" BOOLEAN NOT NULL,
    "IS_PROCESSED" BOOLEAN NOT NULL,
    "PROCESSED_DATE" TIMESTAMP,
    "PROCESSING_NOTES" CHARACTER VARYING,
    "PRIMARY_WRESTLER_ID" BIGINT,
    "SECONDARY_WRESTLER_ID" BIGINT,
    "UNIVERSE_ID" BIGINT
);            
ALTER TABLE "PUBLIC"."DRAMA_EVENT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_C4" PRIMARY KEY("DRAMA_EVENT_ID");      
-- 1 +/- SELECT COUNT(*) FROM PUBLIC.DRAMA_EVENT;              
INSERT INTO "PUBLIC"."DRAMA_EVENT" VALUES
(1, 'Reference Event', 'Test drama event', 'BACKSTAGE_INCIDENT', 'NEUTRAL', TIMESTAMP '2026-06-23 02:00:43.748304', TIMESTAMP '2026-06-23 02:00:43.748304', NULL, NULL, FALSE, FALSE, FALSE, FALSE, NULL, NULL, NULL, NULL, NULL);   
CREATE INDEX "PUBLIC"."IDX_DRAMA_EVENT_PROCESSED_DATE" ON "PUBLIC"."DRAMA_EVENT"("IS_PROCESSED" NULLS FIRST, "EVENT_DATE" NULLS FIRST);        
CREATE CACHED TABLE "PUBLIC"."TITLE_CHAMPION"(
    "TITLE_ID" BIGINT NOT NULL,
    "WRESTLER_ID" BIGINT NOT NULL
);            
ALTER TABLE "PUBLIC"."TITLE_CHAMPION" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_3B" PRIMARY KEY("TITLE_ID", "WRESTLER_ID");          
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.TITLE_CHAMPION;           
CREATE CACHED TABLE "PUBLIC"."SEASON"(
    "SEASON_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING,
    "START_DATE" DATE NOT NULL,
    "END_DATE" DATE,
    "IS_ACTIVE" BOOLEAN DEFAULT TRUE NOT NULL,
    "CREATION_DATE" TIMESTAMP NOT NULL,
    "NOTION_ID" CHARACTER VARYING(255),
    "SHOWS_PER_PPV" INTEGER DEFAULT 5 NOT NULL,
    "LAST_SYNC" TIMESTAMP,
    "EXTERNAL_ID" CHARACTER VARYING(255),
    "BUDGET" DECIMAL(19, 2) DEFAULT 0.00,
    "DURATION_WEEKS" INTEGER
);     
ALTER TABLE "PUBLIC"."SEASON" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_9" PRIMARY KEY("SEASON_ID"); 
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.SEASON;   
CREATE CACHED TABLE "PUBLIC"."TITLE_CONTENDER"(
    "TITLE_ID" BIGINT NOT NULL,
    "WRESTLER_ID" BIGINT NOT NULL
);           
ALTER TABLE "PUBLIC"."TITLE_CONTENDER" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_C6" PRIMARY KEY("TITLE_ID", "WRESTLER_ID");         
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.TITLE_CONTENDER;          
CREATE CACHED TABLE "PUBLIC"."SEGMENT"(
    "SEGMENT_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "SHOW_ID" BIGINT NOT NULL,
    "SEGMENT_TYPE_ID" BIGINT NOT NULL,
    "WINNER_ID" BIGINT,
    "SEGMENT_DATE" TIMESTAMP NOT NULL,
    "DURATION_MINUTES" INTEGER,
    "SEGMENT_RATING" INTEGER,
    "STATUS" CHARACTER VARYING(255) NOT NULL,
    "NARRATION" CHARACTER VARYING,
    "SUMMARY" CHARACTER VARYING,
    "IS_TITLE_SEGMENT" BOOLEAN DEFAULT FALSE NOT NULL,
    "IS_NPC_GENERATED" BOOLEAN DEFAULT FALSE NOT NULL,
    "ADJUDICATION_STATUS" CHARACTER VARYING(255) DEFAULT 'ADJUDICATED' NOT NULL,
    "SEGMENT_ORDER" INTEGER DEFAULT 0 NOT NULL,
    "IS_MAIN_EVENT" BOOLEAN DEFAULT FALSE NOT NULL,
    "REFEREE_ID" BIGINT,
    "REFEREE_AWARENESS_LEVEL" INTEGER DEFAULT 0 NOT NULL,
    "CROWD_NOISE_LEVEL" INTEGER DEFAULT 0 NOT NULL,
    "NOTES" CHARACTER VARYING,
    "RIVALRY_ID" BIGINT DEFAULT NULL
); 
ALTER TABLE "PUBLIC"."SEGMENT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_A0" PRIMARY KEY("SEGMENT_ID");              
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.SEGMENT;  
CREATE CACHED TABLE "PUBLIC"."TITLE_REIGN_CHAMPION"(
    "TITLE_REIGN_ID" BIGINT NOT NULL,
    "WRESTLER_ID" BIGINT NOT NULL
);
ALTER TABLE "PUBLIC"."TITLE_REIGN_CHAMPION" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_54" PRIMARY KEY("TITLE_REIGN_ID", "WRESTLER_ID");              
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.TITLE_REIGN_CHAMPION;     
CREATE CACHED TABLE "PUBLIC"."FEUD_PARTICIPANT"(
    "FEUD_PARTICIPANT_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "FEUD_ID" BIGINT NOT NULL,
    "WRESTLER_ID" BIGINT NOT NULL,
    "ROLE" CHARACTER VARYING(255),
    "IS_ACTIVE" BOOLEAN DEFAULT TRUE NOT NULL,
    "JOINED_DATE" TIMESTAMP NOT NULL,
    "LEFT_DATE" TIMESTAMP,
    "LEFT_REASON" CHARACTER VARYING(255),
    "CREATION_DATE" TIMESTAMP NOT NULL
);            
ALTER TABLE "PUBLIC"."FEUD_PARTICIPANT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_C87" PRIMARY KEY("FEUD_PARTICIPANT_ID");           
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.FEUD_PARTICIPANT;         
CREATE CACHED TABLE "PUBLIC"."WRESTLER_STATE"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1 RESTART WITH 3) NOT NULL,
    "WRESTLER_ID" BIGINT NOT NULL,
    "UNIVERSE_ID" BIGINT NOT NULL,
    "FANS" BIGINT DEFAULT 0,
    "TIER" CHARACTER VARYING(255) NOT NULL,
    "BUMPS" INTEGER DEFAULT 0,
    "CURRENT_HEALTH" INTEGER,
    "PHYSICAL_CONDITION" INTEGER DEFAULT 100,
    "MORALE" INTEGER DEFAULT 100,
    "MANAGEMENT_STAMINA" INTEGER DEFAULT 100,
    "FACTION_ID" BIGINT,
    "MANAGER_ID" BIGINT,
    "UPDATED_AT" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);     
ALTER TABLE "PUBLIC"."WRESTLER_STATE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_46" PRIMARY KEY("ID");               
-- 2 +/- SELECT COUNT(*) FROM PUBLIC.WRESTLER_STATE;           
INSERT INTO "PUBLIC"."WRESTLER_STATE" VALUES
(1, 1, 1, 1000, 'ROOKIE', 0, 12, 100, 100, 100, NULL, NULL, TIMESTAMP '2026-06-23 02:00:43.746664'),
(2, 2, 1, 500, 'VETERAN', 2, 14, 100, 100, 100, NULL, NULL, TIMESTAMP '2026-06-23 02:00:43.747002');         
CREATE CACHED TABLE "PUBLIC"."LEAGUE"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "COMMISSIONER_ID" BIGINT NOT NULL,
    "STATUS" CHARACTER VARYING(50) NOT NULL,
    "MAX_PICKS_PER_PLAYER" INTEGER DEFAULT 1 NOT NULL,
    "BUDGET" DECIMAL(19, 2) DEFAULT 0.00,
    "DURATION_WEEKS" INTEGER,
    "LOCKER_ROOM_MORALE" INTEGER DEFAULT 100 NOT NULL,
    "UNIVERSE_ID" BIGINT
);              
ALTER TABLE "PUBLIC"."LEAGUE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_85" PRIMARY KEY("ID");       
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.LEAGUE;   
CREATE CACHED TABLE "PUBLIC"."WRESTLER"(
    "WRESTLER_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1 RESTART WITH 3) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "STARTING_STAMINA" INTEGER NOT NULL,
    "LOW_STAMINA" INTEGER NOT NULL,
    "STARTING_HEALTH" INTEGER NOT NULL,
    "LOW_HEALTH" INTEGER NOT NULL,
    "DECK_SIZE" INTEGER NOT NULL,
    "CREATION_DATE" TIMESTAMP NOT NULL,
    "IS_PLAYER" BOOLEAN NOT NULL,
    "GENDER" CHARACTER VARYING(255),
    "DESCRIPTION" CHARACTER VARYING(4000),
    "IMAGE_URL" CHARACTER VARYING(255),
    "ACCOUNT_ID" BIGINT,
    "ACTIVE" BOOLEAN DEFAULT TRUE NOT NULL,
    "DRIVE" INTEGER DEFAULT 1 NOT NULL,
    "RESILIENCE" INTEGER DEFAULT 1 NOT NULL,
    "CHARISMA" INTEGER DEFAULT 1 NOT NULL,
    "BRAWL" INTEGER DEFAULT 1 NOT NULL,
    "HERITAGE_TAG" CHARACTER VARYING(255),
    "EXPANSION_CODE" CHARACTER VARYING(255) DEFAULT 'BASE_GAME' NOT NULL
);          
ALTER TABLE "PUBLIC"."WRESTLER" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_B" PRIMARY KEY("WRESTLER_ID");             
-- 2 +/- SELECT COUNT(*) FROM PUBLIC.WRESTLER; 
INSERT INTO "PUBLIC"."WRESTLER" VALUES
(1, 'Reference Wrestler', 12, 2, 12, 2, 12, TIMESTAMP '2026-06-23 02:00:43.746121', FALSE, 'MALE', NULL, NULL, NULL, TRUE, 1, 1, 1, 1, NULL, 'BASE_GAME'),
(2, 'Reference Wrestler 2', 14, 3, 14, 3, 14, TIMESTAMP '2026-06-23 02:00:43.746395', FALSE, 'FEMALE', NULL, NULL, NULL, TRUE, 1, 1, 1, 1, NULL, 'BASE_GAME');               
CREATE CACHED TABLE "PUBLIC"."INJURY"(
    "INJURY_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "WRESTLER_ID" BIGINT NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING,
    "SEVERITY" CHARACTER VARYING(255) NOT NULL,
    "HEALTH_PENALTY" INTEGER NOT NULL,
    "IS_ACTIVE" BOOLEAN NOT NULL,
    "INJURY_DATE" TIMESTAMP NOT NULL,
    "HEALED_DATE" TIMESTAMP,
    "HEALING_COST" BIGINT NOT NULL,
    "INJURY_NOTES" CHARACTER VARYING,
    "CREATION_DATE" TIMESTAMP NOT NULL,
    "STAMINA_PENALTY" INTEGER DEFAULT 0 NOT NULL,
    "HAND_SIZE_PENALTY" INTEGER DEFAULT 0 NOT NULL,
    "UNIVERSE_ID" BIGINT,
    "INJURY_TYPE_ID" BIGINT NOT NULL
);               
ALTER TABLE "PUBLIC"."INJURY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_8" PRIMARY KEY("INJURY_ID"); 
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.INJURY;   
CREATE CACHED TABLE "PUBLIC"."HEAT_EVENT"(
    "HEAT_EVENT_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "RIVALRY_ID" BIGINT NOT NULL,
    "HEAT_CHANGE" INTEGER NOT NULL,
    "HEAT_AFTER_EVENT" INTEGER NOT NULL,
    "REASON" CHARACTER VARYING(500) NOT NULL,
    "EVENT_DATE" TIMESTAMP NOT NULL,
    "CREATION_DATE" TIMESTAMP NOT NULL,
    "EXTERNAL_ID" CHARACTER VARYING(255),
    "LAST_SYNC" TIMESTAMP
);
ALTER TABLE "PUBLIC"."HEAT_EVENT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_CE" PRIMARY KEY("HEAT_EVENT_ID");        
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.HEAT_EVENT;               
CREATE CACHED TABLE "PUBLIC"."FEUD_HEAT_EVENT"(
    "FEUD_HEAT_EVENT_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "FEUD_ID" BIGINT NOT NULL,
    "HEAT_CHANGE" INTEGER NOT NULL,
    "HEAT_AFTER_EVENT" INTEGER NOT NULL,
    "REASON" CHARACTER VARYING(500) NOT NULL,
    "EVENT_DATE" TIMESTAMP NOT NULL,
    "CREATION_DATE" TIMESTAMP NOT NULL
);              
ALTER TABLE "PUBLIC"."FEUD_HEAT_EVENT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_F" PRIMARY KEY("FEUD_HEAT_EVENT_ID");               
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.FEUD_HEAT_EVENT;          
CREATE CACHED TABLE "PUBLIC"."MULTI_WRESTLER_FEUD"(
    "MULTI_WRESTLER_FEUD_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING,
    "HEAT" INTEGER DEFAULT 0 NOT NULL,
    "IS_ACTIVE" BOOLEAN DEFAULT TRUE NOT NULL,
    "STARTED_DATE" TIMESTAMP NOT NULL,
    "ENDED_DATE" TIMESTAMP,
    "STORYLINE_NOTES" CHARACTER VARYING,
    "CREATION_DATE" TIMESTAMP NOT NULL
);  
ALTER TABLE "PUBLIC"."MULTI_WRESTLER_FEUD" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_B3" PRIMARY KEY("MULTI_WRESTLER_FEUD_ID");      
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.MULTI_WRESTLER_FEUD;      
CREATE CACHED TABLE "PUBLIC"."LOCATION_CULTURAL_TAG"(
    "LOCATION_ID" BIGINT NOT NULL,
    "CULTURAL_TAG" CHARACTER VARYING(255)
);          
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.LOCATION_CULTURAL_TAG;    
CREATE CACHED TABLE "PUBLIC"."ACCOUNT_ACHIEVEMENT"(
    "ACCOUNT_ID" BIGINT NOT NULL,
    "ACHIEVEMENT_ID" BIGINT NOT NULL
);  
ALTER TABLE "PUBLIC"."ACCOUNT_ACHIEVEMENT" ADD CONSTRAINT "PUBLIC"."PK_ACCOUNT_ACHIEVEMENT" PRIMARY KEY("ACCOUNT_ID", "ACHIEVEMENT_ID");       
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.ACCOUNT_ACHIEVEMENT;      
CREATE CACHED TABLE "PUBLIC"."ACCOUNT_ROLES"(
    "ACCOUNT_ID" BIGINT NOT NULL,
    "ROLE_ID" BIGINT NOT NULL
);               
ALTER TABLE "PUBLIC"."ACCOUNT_ROLES" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_D" PRIMARY KEY("ACCOUNT_ID", "ROLE_ID");              
-- 4 +/- SELECT COUNT(*) FROM PUBLIC.ACCOUNT_ROLES;            
INSERT INTO "PUBLIC"."ACCOUNT_ROLES" VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 4);    
CREATE CACHED TABLE "PUBLIC"."LEAGUE_EXCLUDED_WRESTLER"(
    "LEAGUE_ID" BIGINT NOT NULL,
    "WRESTLER_ID" BIGINT NOT NULL
); 
ALTER TABLE "PUBLIC"."LEAGUE_EXCLUDED_WRESTLER" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_9B" PRIMARY KEY("LEAGUE_ID", "WRESTLER_ID");               
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.LEAGUE_EXCLUDED_WRESTLER; 
CREATE CACHED TABLE "PUBLIC"."WRESTLER_STATUS"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "WRESTLER_ID" BIGINT NOT NULL,
    "STATUS_CARD_ID" BIGINT NOT NULL,
    "LEVEL" INTEGER DEFAULT 1 NOT NULL,
    "CREATION_DATE" TIMESTAMP NOT NULL,
    "LAST_UPDATED" TIMESTAMP NOT NULL
);     
ALTER TABLE "PUBLIC"."WRESTLER_STATUS" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_7B" PRIMARY KEY("ID");              
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.WRESTLER_STATUS;          
CREATE INDEX "PUBLIC"."IDX_WRESTLER_STATUS_WRESTLER" ON "PUBLIC"."WRESTLER_STATUS"("WRESTLER_ID" NULLS FIRST); 
CREATE CACHED TABLE "PUBLIC"."ARENA_ENVIRONMENTAL_TRAIT"(
    "ARENA_ID" BIGINT NOT NULL,
    "ENVIRONMENTAL_TRAIT" CHARACTER VARYING(255)
);  
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.ARENA_ENVIRONMENTAL_TRAIT;
CREATE CACHED TABLE "PUBLIC"."CAMPAIGN_COMPLETED_CHAPTERS"(
    "CAMPAIGN_STATE_ID" BIGINT NOT NULL,
    "CHAPTER_ID" CHARACTER VARYING(255) NOT NULL
);       
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.CAMPAIGN_COMPLETED_CHAPTERS;              
CREATE CACHED TABLE "PUBLIC"."OUTCOME_MATRIX"(
    "OUTCOME_MATRIX_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER LARGE OBJECT,
    "CATEGORY" CHARACTER VARYING(50) NOT NULL
);  
ALTER TABLE "PUBLIC"."OUTCOME_MATRIX" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_869" PRIMARY KEY("OUTCOME_MATRIX_ID");               
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.OUTCOME_MATRIX;           
CREATE CACHED TABLE "PUBLIC"."OUTCOME_MATRIX_ENTRY"(
    "OUTCOME_MATRIX_ENTRY_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "OUTCOME_MATRIX_ID" BIGINT NOT NULL,
    "DICE_ROLL" INTEGER NOT NULL,
    "TEMPLATE_TEXT" CHARACTER LARGE OBJECT NOT NULL,
    "HEAT_DELTA" INTEGER,
    "FAN_DELTA" BIGINT,
    "TV_GRADE_DELTA" INTEGER,
    "GRUDGE_GRADE_DELTA" INTEGER,
    "INJURY_CAUSED" BOOLEAN DEFAULT FALSE NOT NULL,
    "REDIRECT_MATRIX_ID" BIGINT
);    
ALTER TABLE "PUBLIC"."OUTCOME_MATRIX_ENTRY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_A02" PRIMARY KEY("OUTCOME_MATRIX_ENTRY_ID");   
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.OUTCOME_MATRIX_ENTRY;     
CREATE CACHED TABLE "PUBLIC"."BACKSTAGE_ACTION_HISTORY"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "CAMPAIGN_ID" BIGINT NOT NULL,
    "ACTION_TYPE" CHARACTER VARYING(50) NOT NULL,
    "ACTION_DATE" TIMESTAMP NOT NULL,
    "DICE_ROLLED" INTEGER NOT NULL,
    "SUCCESSES" INTEGER NOT NULL,
    "OUTCOME_DESCRIPTION" CHARACTER VARYING
);              
ALTER TABLE "PUBLIC"."BACKSTAGE_ACTION_HISTORY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_F6" PRIMARY KEY("ID");     
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.BACKSTAGE_ACTION_HISTORY; 
CREATE INDEX "PUBLIC"."IDX_BACKSTAGE_ACTION_HISTORY_CAMPAIGN" ON "PUBLIC"."BACKSTAGE_ACTION_HISTORY"("CAMPAIGN_ID" NULLS FIRST);               
CREATE CACHED TABLE "PUBLIC"."CAMPAIGN_ABILITY_CARD"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING,
    "ALIGNMENT_TYPE" CHARACTER VARYING(50) NOT NULL,
    "LEVEL" INTEGER NOT NULL,
    "EFFECT_SCRIPT" CHARACTER VARYING(255),
    "ONE_TIME_USE" BOOLEAN DEFAULT TRUE NOT NULL,
    "TIMING" CHARACTER VARYING(50),
    "SECONDARY_EFFECT_SCRIPT" CHARACTER VARYING(255),
    "SECONDARY_ONE_TIME_USE" BOOLEAN DEFAULT FALSE NOT NULL,
    "SECONDARY_TIMING" CHARACTER VARYING(50)
);        
ALTER TABLE "PUBLIC"."CAMPAIGN_ABILITY_CARD" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_10" PRIMARY KEY("ID");        
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.CAMPAIGN_ABILITY_CARD;    
CREATE INDEX "PUBLIC"."IDX_CAMPAIGN_ABILITY_CARD_ALIGNMENT" ON "PUBLIC"."CAMPAIGN_ABILITY_CARD"("ALIGNMENT_TYPE" NULLS FIRST, "LEVEL" NULLS FIRST);            
CREATE CACHED TABLE "PUBLIC"."CAMPAIGN_STATE_CARDS"(
    "CAMPAIGN_STATE_ID" BIGINT NOT NULL,
    "CARD_ID" BIGINT NOT NULL
); 
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.CAMPAIGN_STATE_CARDS;     
CREATE CACHED TABLE "PUBLIC"."FACTION"(
    "FACTION_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1 RESTART WITH 2) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING,
    "IS_ACTIVE" BOOLEAN DEFAULT TRUE NOT NULL,
    "LEADER_ID" BIGINT,
    "FORMED_DATE" TIMESTAMP,
    "DISBANDED_DATE" TIMESTAMP,
    "CREATION_DATE" TIMESTAMP NOT NULL,
    "MANAGER_ID" BIGINT,
    "AFFINITY" INTEGER DEFAULT 0 NOT NULL,
    "ALIGNMENT" CHARACTER VARYING(50),
    "IMAGE_URL" CHARACTER VARYING(512),
    "UNIVERSE_ID" BIGINT
);            
ALTER TABLE "PUBLIC"."FACTION" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_E" PRIMARY KEY("FACTION_ID");               
-- 1 +/- SELECT COUNT(*) FROM PUBLIC.FACTION;  
INSERT INTO "PUBLIC"."FACTION" VALUES
(1, 'Test Faction', 'Reference snapshot faction', TRUE, NULL, NULL, NULL, TIMESTAMP '2026-06-23 02:00:43.745828', NULL, 0, NULL, NULL, NULL);            
CREATE CACHED TABLE "PUBLIC"."CAMPAIGN_ENCOUNTER"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "CAMPAIGN_ID" BIGINT NOT NULL,
    "CHAPTER_ID" CHARACTER VARYING(255) NOT NULL,
    "NARRATIVE_TEXT" CHARACTER VARYING NOT NULL,
    "PLAYER_CHOICE" CHARACTER VARYING,
    "ALIGNMENT_SHIFT" INTEGER DEFAULT 0 NOT NULL,
    "VP_REWARD" INTEGER DEFAULT 0 NOT NULL,
    "ENCOUNTER_DATE" TIMESTAMP NOT NULL
);              
ALTER TABLE "PUBLIC"."CAMPAIGN_ENCOUNTER" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_5F" PRIMARY KEY("ID");           
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.CAMPAIGN_ENCOUNTER;       
CREATE INDEX "PUBLIC"."IDX_CAMPAIGN_ENCOUNTER_CAMPAIGN" ON "PUBLIC"."CAMPAIGN_ENCOUNTER"("CAMPAIGN_ID" NULLS FIRST);           
CREATE CACHED TABLE "PUBLIC"."CAMPAIGN_UPGRADE"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING(1000) NOT NULL,
    "TYPE" CHARACTER VARYING(50) NOT NULL,
    "SUB_TYPE" CHARACTER VARYING(50)
);   
ALTER TABLE "PUBLIC"."CAMPAIGN_UPGRADE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_52" PRIMARY KEY("ID");             
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.CAMPAIGN_UPGRADE;         
CREATE CACHED TABLE "PUBLIC"."CAMPAIGN_STATE_UPGRADES"(
    "CAMPAIGN_STATE_ID" BIGINT NOT NULL,
    "UPGRADE_ID" BIGINT NOT NULL
);           
ALTER TABLE "PUBLIC"."CAMPAIGN_STATE_UPGRADES" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_25" PRIMARY KEY("CAMPAIGN_STATE_ID", "UPGRADE_ID");         
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.CAMPAIGN_STATE_UPGRADES;  
CREATE CACHED TABLE "PUBLIC"."LEAGUE_MEMBERSHIP"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "LEAGUE_ID" BIGINT NOT NULL,
    "MEMBER_ID" BIGINT NOT NULL,
    "ROLE" CHARACTER VARYING(50) NOT NULL
);      
ALTER TABLE "PUBLIC"."LEAGUE_MEMBERSHIP" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_86" PRIMARY KEY("ID");            
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.LEAGUE_MEMBERSHIP;        
CREATE CACHED TABLE "PUBLIC"."LEAGUE_ROSTER"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "LEAGUE_ID" BIGINT NOT NULL,
    "OWNER_ID" BIGINT NOT NULL,
    "WRESTLER_ID" BIGINT NOT NULL,
    "WINS" INTEGER DEFAULT 0 NOT NULL,
    "LOSSES" INTEGER DEFAULT 0 NOT NULL,
    "DRAWS" INTEGER DEFAULT 0 NOT NULL
);           
ALTER TABLE "PUBLIC"."LEAGUE_ROSTER" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_DC" PRIMARY KEY("ID");
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.LEAGUE_ROSTER;            
CREATE CACHED TABLE "PUBLIC"."DRAFT"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "LEAGUE_ID" BIGINT NOT NULL,
    "STATUS" CHARACTER VARYING(50) NOT NULL,
    "CURRENT_TURN_USER_ID" BIGINT,
    "CURRENT_ROUND" INTEGER DEFAULT 1 NOT NULL,
    "CURRENT_PICK_NUMBER" INTEGER DEFAULT 1 NOT NULL,
    "DIRECTION" INTEGER DEFAULT 1 NOT NULL
);            
ALTER TABLE "PUBLIC"."DRAFT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_3E" PRIMARY KEY("ID");        
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.DRAFT;    
CREATE CACHED TABLE "PUBLIC"."DRAFT_PICK"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "DRAFT_ID" BIGINT NOT NULL,
    "USER_ID" BIGINT NOT NULL,
    "WRESTLER_ID" BIGINT NOT NULL,
    "PICK_NUMBER" INTEGER NOT NULL,
    "ROUND" INTEGER NOT NULL
);      
ALTER TABLE "PUBLIC"."DRAFT_PICK" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_CD" PRIMARY KEY("ID");   
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.DRAFT_PICK;               
CREATE CACHED TABLE "PUBLIC"."MATCH_FULFILLMENT"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "SEGMENT_ID" BIGINT NOT NULL,
    "LEAGUE_ID" BIGINT NOT NULL,
    "STATUS" CHARACTER VARYING(50) NOT NULL,
    "WINNER_ID" BIGINT,
    "SUBMITTED_BY_ID" BIGINT
);             
ALTER TABLE "PUBLIC"."MATCH_FULFILLMENT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_EC" PRIMARY KEY("ID");            
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.MATCH_FULFILLMENT;        
CREATE CACHED TABLE "PUBLIC"."COMMENTARY_TEAM_MEMBERS"(
    "TEAM_ID" BIGINT NOT NULL,
    "COMMENTATOR_ID" BIGINT NOT NULL
); 
ALTER TABLE "PUBLIC"."COMMENTARY_TEAM_MEMBERS" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_7D" PRIMARY KEY("TEAM_ID", "COMMENTATOR_ID");               
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.COMMENTARY_TEAM_MEMBERS;  
CREATE CACHED TABLE "PUBLIC"."ACHIEVEMENT"(
    "ACHIEVEMENT_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "ACHIEVEMENT_KEY" CHARACTER VARYING(100) NOT NULL,
    "NAME" CHARACTER VARYING(100) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING(1000) NOT NULL,
    "XP_VALUE" INTEGER NOT NULL,
    "CATEGORY" CHARACTER VARYING(50) NOT NULL,
    "ICON_URL" CHARACTER VARYING(512),
    "EXTERNAL_ID" CHARACTER VARYING(255),
    "LAST_SYNC" TIMESTAMP,
    "EXPANSION_CODE" CHARACTER VARYING(50) DEFAULT 'BASE_GAME' NOT NULL
); 
ALTER TABLE "PUBLIC"."ACHIEVEMENT" ADD CONSTRAINT "PUBLIC"."PK_ACHIEVEMENT" PRIMARY KEY("ACHIEVEMENT_ID");     
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.ACHIEVEMENT;              
CREATE CACHED TABLE "PUBLIC"."FACTION_RIVALRY"(
    "FACTION_RIVALRY_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "FACTION1_ID" BIGINT NOT NULL,
    "FACTION2_ID" BIGINT NOT NULL,
    "HEAT" INTEGER DEFAULT 0 NOT NULL,
    "IS_ACTIVE" BOOLEAN DEFAULT TRUE NOT NULL,
    "STARTED_DATE" TIMESTAMP NOT NULL,
    "ENDED_DATE" TIMESTAMP,
    "STORYLINE_NOTES" CHARACTER VARYING,
    "CREATION_DATE" TIMESTAMP NOT NULL,
    "EXTERNAL_ID" CHARACTER VARYING(255),
    "LAST_SYNC" TIMESTAMP
);
ALTER TABLE "PUBLIC"."FACTION_RIVALRY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_E2" PRIMARY KEY("FACTION_RIVALRY_ID");              
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.FACTION_RIVALRY;          
CREATE CACHED TABLE "PUBLIC"."WRESTLER_SEASON_SNAPSHOT"(
    "SNAPSHOT_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "WRESTLER_ID" BIGINT NOT NULL,
    "SEASON_ID" BIGINT NOT NULL,
    "STARTING_FANS" BIGINT DEFAULT 0 NOT NULL,
    "CREATED_AT" TIMESTAMP NOT NULL
);           
ALTER TABLE "PUBLIC"."WRESTLER_SEASON_SNAPSHOT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_D5" PRIMARY KEY("SNAPSHOT_ID");            
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.WRESTLER_SEASON_SNAPSHOT; 
CREATE CACHED TABLE "PUBLIC"."TEAM"(
    "TEAM_ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "DESCRIPTION" CHARACTER VARYING,
    "WRESTLER1_ID" BIGINT NOT NULL,
    "WRESTLER2_ID" BIGINT NOT NULL,
    "FACTION_ID" BIGINT,
    "STATUS" CHARACTER VARYING(255) NOT NULL,
    "FORMED_DATE" TIMESTAMP NOT NULL,
    "DISBANDED_DATE" TIMESTAMP,
    "MANAGER_ID" BIGINT,
    "THEME_SONG" CHARACTER VARYING(255),
    "ARTIST" CHARACTER VARYING(255),
    "TEAM_FINISHER" CHARACTER VARYING(255),
    "IMAGE_URL" CHARACTER VARYING(512),
    "UNIVERSE_ID" BIGINT
);
ALTER TABLE "PUBLIC"."TEAM" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_2" PRIMARY KEY("TEAM_ID");     
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.TEAM;     
CREATE CACHED TABLE "PUBLIC"."WRESTLER_STATUS_HISTORY"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "WRESTLER_ID" BIGINT NOT NULL,
    "STATUS_CARD_ID" BIGINT NOT NULL,
    "ACTION" CHARACTER VARYING(255) NOT NULL,
    "OLD_LEVEL" INTEGER,
    "NEW_LEVEL" INTEGER,
    "CREATION_DATE" TIMESTAMP NOT NULL
);            
ALTER TABLE "PUBLIC"."WRESTLER_STATUS_HISTORY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_64" PRIMARY KEY("ID");      
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.WRESTLER_STATUS_HISTORY;  
CREATE INDEX "PUBLIC"."IDX_WRESTLER_STATUS_HISTORY_WRESTLER" ON "PUBLIC"."WRESTLER_STATUS_HISTORY"("WRESTLER_ID" NULLS FIRST); 
CREATE CACHED TABLE "PUBLIC"."MILESTONE_STATUS_REWARDS"(
    "MILESTONE_ID" BIGINT NOT NULL,
    "STATUS_KEY" CHARACTER VARYING(255) NOT NULL
);               
ALTER TABLE "PUBLIC"."MILESTONE_STATUS_REWARDS" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_B5" PRIMARY KEY("MILESTONE_ID", "STATUS_KEY");             
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.MILESTONE_STATUS_REWARDS; 
CREATE CACHED TABLE "PUBLIC"."UNIVERSE_MEMBERS"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1 RESTART WITH 5) NOT NULL,
    "UNIVERSE_ID" BIGINT NOT NULL,
    "ACCOUNT_ID" BIGINT NOT NULL,
    "ROLE" CHARACTER VARYING(20) DEFAULT 'MEMBER' NOT NULL,
    "JOINED_DATE" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);    
ALTER TABLE "PUBLIC"."UNIVERSE_MEMBERS" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_AC" PRIMARY KEY("ID");             
-- 4 +/- SELECT COUNT(*) FROM PUBLIC.UNIVERSE_MEMBERS;         
INSERT INTO "PUBLIC"."UNIVERSE_MEMBERS" VALUES
(1, 1, 1, 'MEMBER', TIMESTAMP '2026-06-23 02:00:43.203002'),
(2, 1, 2, 'MEMBER', TIMESTAMP '2026-06-23 02:00:43.203002'),
(3, 1, 3, 'MEMBER', TIMESTAMP '2026-06-23 02:00:43.203002'),
(4, 1, 4, 'MEMBER', TIMESTAMP '2026-06-23 02:00:43.203002');             
CREATE CACHED TABLE "PUBLIC"."UNIVERSE_EXPANSION_SETTINGS"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "UNIVERSE_ID" BIGINT NOT NULL,
    "EXPANSION_CODE" CHARACTER VARYING(255) NOT NULL,
    "ENABLED" BOOLEAN DEFAULT TRUE NOT NULL
);   
ALTER TABLE "PUBLIC"."UNIVERSE_EXPANSION_SETTINGS" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_254" PRIMARY KEY("ID"); 
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.UNIVERSE_EXPANSION_SETTINGS;              
CREATE CACHED TABLE "PUBLIC"."UNIVERSE_WRESTLER_EXCLUSIONS"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "UNIVERSE_ID" BIGINT NOT NULL,
    "WRESTLER_ID" BIGINT NOT NULL
);  
ALTER TABLE "PUBLIC"."UNIVERSE_WRESTLER_EXCLUSIONS" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_93" PRIMARY KEY("ID"); 
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.UNIVERSE_WRESTLER_EXCLUSIONS;             
CREATE CACHED TABLE "PUBLIC"."UNIVERSE_INVITE"(
    "ID" CHARACTER VARYING(36) NOT NULL,
    "UNIVERSE_ID" BIGINT NOT NULL,
    "TYPE" CHARACTER VARYING(20) NOT NULL,
    "CREATED_BY" BIGINT,
    "CREATED_AT" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "EXPIRES_AT" TIMESTAMP,
    "REVOKED_AT" TIMESTAMP,
    "MAX_USES" INTEGER,
    "USE_COUNT" INTEGER DEFAULT 0 NOT NULL
);   
ALTER TABLE "PUBLIC"."UNIVERSE_INVITE" ADD CONSTRAINT "PUBLIC"."PK_UNIVERSE_INVITE" PRIMARY KEY("ID");         
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.UNIVERSE_INVITE;          
CREATE INDEX "PUBLIC"."IDX_UNIVERSE_INVITE_UNIVERSE" ON "PUBLIC"."UNIVERSE_INVITE"("UNIVERSE_ID" NULLS FIRST); 
CREATE CACHED TABLE "PUBLIC"."UNIVERSE_JOIN_REQUEST"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "UNIVERSE_ID" BIGINT NOT NULL,
    "INVITE_ID" CHARACTER VARYING(36),
    "ACCOUNT_ID" BIGINT,
    "REQUESTER_NAME" CHARACTER VARYING(255) NOT NULL,
    "REQUESTER_EMAIL" CHARACTER VARYING(255),
    "STATUS" CHARACTER VARYING(20) DEFAULT 'PENDING' NOT NULL,
    "REQUESTED_AT" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "RESOLVED_AT" TIMESTAMP,
    "RESOLVED_BY" BIGINT,
    "NOTES" CHARACTER VARYING(1000)
);            
ALTER TABLE "PUBLIC"."UNIVERSE_JOIN_REQUEST" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_EF" PRIMARY KEY("ID");        
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.UNIVERSE_JOIN_REQUEST;    
CREATE INDEX "PUBLIC"."IDX_UNIVERSE_JOIN_REQUEST_UNIVERSE" ON "PUBLIC"."UNIVERSE_JOIN_REQUEST"("UNIVERSE_ID" NULLS FIRST);     
CREATE INDEX "PUBLIC"."IDX_UNIVERSE_JOIN_REQUEST_STATUS" ON "PUBLIC"."UNIVERSE_JOIN_REQUEST"("STATUS" NULLS FIRST);            
CREATE INDEX "PUBLIC"."IDX_UNIVERSE_JOIN_REQUEST_ACCOUNT" ON "PUBLIC"."UNIVERSE_JOIN_REQUEST"("ACCOUNT_ID" NULLS FIRST);       
CREATE CACHED TABLE "PUBLIC"."ACCOUNT_TUTORIAL_COMPLETION"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "ACCOUNT_ID" BIGINT NOT NULL,
    "UNIVERSE_TYPE" CHARACTER VARYING(20) NOT NULL,
    "CURRENT_STEP" INTEGER DEFAULT 0 NOT NULL,
    "COMPLETED_AT" TIMESTAMP
);      
ALTER TABLE "PUBLIC"."ACCOUNT_TUTORIAL_COMPLETION" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_37" PRIMARY KEY("ID");  
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.ACCOUNT_TUTORIAL_COMPLETION;              
CREATE CACHED TABLE "PUBLIC"."CAMPAIGN_STATE"(
    "ID" BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
    "CAMPAIGN_ID" BIGINT NOT NULL,
    "CURRENT_CHAPTER_ID" CHARACTER VARYING(255),
    "VICTORY_POINTS" INTEGER DEFAULT 0 NOT NULL,
    "SKILL_TOKENS" INTEGER DEFAULT 0 NOT NULL,
    "HEALTH_PENALTY" INTEGER DEFAULT 0 NOT NULL,
    "OPPONENT_HEALTH_PENALTY" INTEGER DEFAULT 0 NOT NULL,
    "HAND_SIZE_PENALTY" INTEGER DEFAULT 0 NOT NULL,
    "STAMINA_PENALTY" INTEGER DEFAULT 0 NOT NULL,
    "CURRENT_PHASE" CHARACTER VARYING(50) DEFAULT 'BACKSTAGE' NOT NULL,
    "ACTIONS_TAKEN" INTEGER DEFAULT 0 NOT NULL,
    "LAST_ACTION_TYPE" CHARACTER VARYING(50),
    "LAST_ACTION_SUCCESS" BOOLEAN DEFAULT TRUE,
    "PROMO_UNLOCKED" BOOLEAN DEFAULT FALSE NOT NULL,
    "ATTACK_UNLOCKED" BOOLEAN DEFAULT FALSE NOT NULL,
    "PENDING_L1_PICKS" INTEGER DEFAULT 0 NOT NULL,
    "PENDING_L2_PICKS" INTEGER DEFAULT 0 NOT NULL,
    "PENDING_L3_PICKS" INTEGER DEFAULT 0 NOT NULL,
    "MATCHES_PLAYED" INTEGER DEFAULT 0 NOT NULL,
    "WINS" INTEGER DEFAULT 0 NOT NULL,
    "LOSSES" INTEGER DEFAULT 0 NOT NULL,
    "RIVAL_ID" BIGINT,
    "CURRENT_MATCH_ID" BIGINT,
    "MOMENTUM_BONUS" INTEGER DEFAULT 0 NOT NULL,
    "CURRENT_GAME_DATE" DATE,
    "LAST_SYNC" TIMESTAMP,
    "FEATURE_DATA" CHARACTER LARGE OBJECT,
    "ACTIVE_STORYLINE_ID" BIGINT,
    "CURRENT_ENCOUNTER_ID" CHARACTER VARYING(255)
);      
ALTER TABLE "PUBLIC"."CAMPAIGN_STATE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_66" PRIMARY KEY("ID");               
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.CAMPAIGN_STATE;           
CREATE INDEX "PUBLIC"."IDX_CAMPAIGN_STATE_CAMPAIGN" ON "PUBLIC"."CAMPAIGN_STATE"("CAMPAIGN_ID" NULLS FIRST);   
CREATE INDEX "PUBLIC"."IDX_CAMPAIGN_STATE_CURRENT_MATCH" ON "PUBLIC"."CAMPAIGN_STATE"("CURRENT_MATCH_ID" NULLS FIRST);         
ALTER TABLE "PUBLIC"."FACTION_RIVALRY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_E259E9" CHECK("FACTION1_ID" <> "FACTION2_ID") NOCHECK;              
ALTER TABLE "PUBLIC"."RIVALRY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_732B5" CHECK("WRESTLER1_ID" <> "WRESTLER2_ID") NOCHECK;     
ALTER TABLE "PUBLIC"."LEAGUE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_859" UNIQUE NULLS DISTINCT ("NAME");         
ALTER TABLE "PUBLIC"."DECK_CARD" ADD CONSTRAINT "PUBLIC"."UQ_DECK_CARD_DECK_CARD_SET" UNIQUE NULLS DISTINCT ("DECK_ID", "CARD_ID", "SET_ID");  
ALTER TABLE "PUBLIC"."CARD_SET" ADD CONSTRAINT "PUBLIC"."UQ_CARD_SET_NAME" UNIQUE NULLS DISTINCT ("NAME");     
ALTER TABLE "PUBLIC"."UNIVERSE_EXPANSION_SETTINGS" ADD CONSTRAINT "PUBLIC"."UK_UNIVERSE_EXPANSION" UNIQUE NULLS DISTINCT ("UNIVERSE_ID", "EXPANSION_CODE");    
ALTER TABLE "PUBLIC"."TEAM" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_27" UNIQUE NULLS DISTINCT ("NAME");            
ALTER TABLE "PUBLIC"."FACTION_RIVALRY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_E259E" UNIQUE NULLS DISTINCT ("FACTION1_ID", "FACTION2_ID");        
ALTER TABLE "PUBLIC"."WRESTLER_STATE" ADD CONSTRAINT "PUBLIC"."UK_WRESTLER_UNIVERSE" UNIQUE NULLS DISTINCT ("WRESTLER_ID", "UNIVERSE_ID");     
ALTER TABLE "PUBLIC"."NPC" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_12" UNIQUE NULLS DISTINCT ("NAME");             
ALTER TABLE "PUBLIC"."SEGMENT_TYPE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_AB" UNIQUE NULLS DISTINCT ("NAME");    
ALTER TABLE "PUBLIC"."LEAGUE_ROSTER" ADD CONSTRAINT "PUBLIC"."UK_LEAGUE_WRESTLER" UNIQUE NULLS DISTINCT ("LEAGUE_ID", "WRESTLER_ID");          
ALTER TABLE "PUBLIC"."COMMENTATOR" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_FDBB1" UNIQUE NULLS DISTINCT ("EXTERNAL_ID");           
ALTER TABLE "PUBLIC"."DRAFT_PICK" ADD CONSTRAINT "PUBLIC"."UK_DRAFT_WRESTLER" UNIQUE NULLS DISTINCT ("DRAFT_ID", "WRESTLER_ID");               
ALTER TABLE "PUBLIC"."WRESTLER_SEASON_SNAPSHOT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_D5AF2" UNIQUE NULLS DISTINCT ("WRESTLER_ID", "SEASON_ID"); 
ALTER TABLE "PUBLIC"."FEUD_PARTICIPANT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_C875DB" UNIQUE NULLS DISTINCT ("FEUD_ID", "WRESTLER_ID");          
ALTER TABLE "PUBLIC"."ARENA" ADD CONSTRAINT "PUBLIC"."UC_ARENA_EXTERNAL_ID" UNIQUE NULLS DISTINCT ("EXTERNAL_ID");             
ALTER TABLE "PUBLIC"."RINGSIDE_ACTION_TYPE" ADD CONSTRAINT "PUBLIC"."UK_RINGSIDE_ACTION_TYPE_NAME" UNIQUE NULLS DISTINCT ("NAME");             
ALTER TABLE "PUBLIC"."RINGSIDE_ACTION" ADD CONSTRAINT "PUBLIC"."UK_RINGSIDE_ACTION_NAME" UNIQUE NULLS DISTINCT ("NAME");       
ALTER TABLE "PUBLIC"."CARD" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_1F73" UNIQUE NULLS DISTINCT ("NAME", "NUMBER", "SET_ID");      
ALTER TABLE "PUBLIC"."UNIVERSE_WRESTLER_EXCLUSIONS" ADD CONSTRAINT "PUBLIC"."UK_UNIVERSE_WRESTLER" UNIQUE NULLS DISTINCT ("UNIVERSE_ID", "WRESTLER_ID");       
ALTER TABLE "PUBLIC"."STATUS_CARD" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_4CF0" UNIQUE NULLS DISTINCT ("STATUS_KEY");             
ALTER TABLE "PUBLIC"."COMMENTATOR" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_FDBB" UNIQUE NULLS DISTINCT ("NPC_ID"); 
ALTER TABLE "PUBLIC"."OUTCOME_MATRIX_ENTRY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_A02C82" UNIQUE NULLS DISTINCT ("OUTCOME_MATRIX_ID", "DICE_ROLL");              
ALTER TABLE "PUBLIC"."DECK_CARD" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_BF75B" UNIQUE NULLS DISTINCT ("DECK_ID", "CARD_ID");      
ALTER TABLE "PUBLIC"."TIER_BOUNDARY" ADD CONSTRAINT "PUBLIC"."UC_TIER_BOUNDARY_TIER_GENDER" UNIQUE NULLS DISTINCT ("TIER", "GENDER");          
ALTER TABLE "PUBLIC"."ACHIEVEMENT" ADD CONSTRAINT "PUBLIC"."UC_ACHIEVEMENT_KEY" UNIQUE NULLS DISTINCT ("ACHIEVEMENT_KEY");     
ALTER TABLE "PUBLIC"."HOLIDAY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_6BE" UNIQUE NULLS DISTINCT ("DESCRIPTION"); 
ALTER TABLE "PUBLIC"."COMMENTARY_TEAM" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_6B5B6" UNIQUE NULLS DISTINCT ("EXTERNAL_ID");       
ALTER TABLE "PUBLIC"."GAME_SETTING" ADD CONSTRAINT "PUBLIC"."UQ_GAME_SETTING_KEY_UNIVERSE" UNIQUE NULLS DISTINCT ("SETTING_KEY", "UNIVERSE_ID");               
ALTER TABLE "PUBLIC"."INJURY_TYPE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_CC2" UNIQUE NULLS DISTINCT ("EXTERNAL_ID");             
ALTER TABLE "PUBLIC"."SEGMENT_RULE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_ABC0" UNIQUE NULLS DISTINCT ("NAME");  
ALTER TABLE "PUBLIC"."SEASON" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_91" UNIQUE NULLS DISTINCT ("NAME");          
ALTER TABLE "PUBLIC"."ROLE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_2678" UNIQUE NULLS DISTINCT ("NAME");          
ALTER TABLE "PUBLIC"."HOLIDAY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_6BE0" UNIQUE NULLS DISTINCT ("EXTERNAL_ID");
ALTER TABLE "PUBLIC"."SEGMENT_PARTICIPANT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_B73F0" UNIQUE NULLS DISTINCT ("SEGMENT_ID", "WRESTLER_ID");     
ALTER TABLE "PUBLIC"."WRESTLER_RELATIONSHIP" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_A16679" UNIQUE NULLS DISTINCT ("WRESTLER1_ID", "WRESTLER2_ID", "RELATIONSHIP_TYPE");          
ALTER TABLE "PUBLIC"."ACCOUNT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_E49" UNIQUE NULLS DISTINCT ("USERNAME");    
ALTER TABLE "PUBLIC"."LOCATION" ADD CONSTRAINT "PUBLIC"."UC_LOCATION_NAME" UNIQUE NULLS DISTINCT ("NAME");     
ALTER TABLE "PUBLIC"."ACCOUNT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_E49F" UNIQUE NULLS DISTINCT ("EMAIL");      
ALTER TABLE "PUBLIC"."CARD_SET" ADD CONSTRAINT "PUBLIC"."UQ_CARD_SET_SET_CODE" UNIQUE NULLS DISTINCT ("SET_CODE");             
ALTER TABLE "PUBLIC"."DRAFT_PICK" ADD CONSTRAINT "PUBLIC"."UK_DRAFT_PICK_NUMBER" UNIQUE NULLS DISTINCT ("DRAFT_ID", "PICK_NUMBER");            
ALTER TABLE "PUBLIC"."CARD" ADD CONSTRAINT "PUBLIC"."UQ_CARD_NUMBER_SET_ID" UNIQUE NULLS DISTINCT ("NUMBER", "SET_ID");        
ALTER TABLE "PUBLIC"."TITLE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_4C" UNIQUE NULLS DISTINCT ("NAME");           
ALTER TABLE "PUBLIC"."FACTION" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_E9" UNIQUE NULLS DISTINCT ("NAME");         
ALTER TABLE "PUBLIC"."CARD_SET" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_2E3" UNIQUE NULLS DISTINCT ("SET_CODE");   
ALTER TABLE "PUBLIC"."ARENA" ADD CONSTRAINT "PUBLIC"."UC_ARENA_NAME" UNIQUE NULLS DISTINCT ("NAME");           
ALTER TABLE "PUBLIC"."LEAGUE_MEMBERSHIP" ADD CONSTRAINT "PUBLIC"."UK_LEAGUE_MEMBER" UNIQUE NULLS DISTINCT ("LEAGUE_ID", "MEMBER_ID");          
ALTER TABLE "PUBLIC"."INJURY_TYPE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_CC" UNIQUE NULLS DISTINCT ("INJURY_NAME");              
ALTER TABLE "PUBLIC"."SHOW_TYPE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_36" UNIQUE NULLS DISTINCT ("NAME");       
ALTER TABLE "PUBLIC"."UNIVERSE_MEMBERS" ADD CONSTRAINT "PUBLIC"."UK_UNIVERSE_MEMBER" UNIQUE NULLS DISTINCT ("UNIVERSE_ID", "ACCOUNT_ID");      
ALTER TABLE "PUBLIC"."COMMENTARY_TEAM" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_6B5B" UNIQUE NULLS DISTINCT ("NAME");               
ALTER TABLE "PUBLIC"."ACHIEVEMENT" ADD CONSTRAINT "PUBLIC"."UC_ACHIEVEMENT_EXTERNAL_ID" UNIQUE NULLS DISTINCT ("EXTERNAL_ID"); 
ALTER TABLE "PUBLIC"."ACCOUNT_TUTORIAL_COMPLETION" ADD CONSTRAINT "PUBLIC"."UQ_ATC" UNIQUE NULLS DISTINCT ("ACCOUNT_ID", "UNIVERSE_TYPE");     
ALTER TABLE "PUBLIC"."SHOW_TEMPLATE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_5B" UNIQUE NULLS DISTINCT ("NAME");   
ALTER TABLE "PUBLIC"."LOCATION" ADD CONSTRAINT "PUBLIC"."UC_LOCATION_EXTERNAL_ID" UNIQUE NULLS DISTINCT ("EXTERNAL_ID");       
ALTER TABLE "PUBLIC"."CAMPAIGN_ABILITY_CARD" ADD CONSTRAINT "PUBLIC"."UK_NAME_ALIGNMENT_LEVEL" UNIQUE NULLS DISTINCT ("NAME", "ALIGNMENT_TYPE", "LEVEL");      
ALTER TABLE "PUBLIC"."TEAM" ADD CONSTRAINT "PUBLIC"."FK_TEAM_MANAGER" FOREIGN KEY("MANAGER_ID") REFERENCES "PUBLIC"."NPC"("ID") NOCHECK;       
ALTER TABLE "PUBLIC"."DRAFT_PICK" ADD CONSTRAINT "PUBLIC"."FK_PICK_DRAFT" FOREIGN KEY("DRAFT_ID") REFERENCES "PUBLIC"."DRAFT"("ID") NOCHECK;   
ALTER TABLE "PUBLIC"."RIVALRY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_732" FOREIGN KEY("WRESTLER2_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") ON DELETE CASCADE NOCHECK;   
ALTER TABLE "PUBLIC"."GAME_SETTING" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_8E9" FOREIGN KEY("UNIVERSE_ID") REFERENCES "PUBLIC"."UNIVERSE"("ID") ON DELETE CASCADE NOCHECK;        
ALTER TABLE "PUBLIC"."SEGMENT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_A070" FOREIGN KEY("SHOW_ID") REFERENCES "PUBLIC"."WRESTLING_SHOW"("SHOW_ID") ON DELETE CASCADE NOCHECK;     
ALTER TABLE "PUBLIC"."CAMPAIGN_STATE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_66CF" FOREIGN KEY("RIVAL_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") NOCHECK; 
ALTER TABLE "PUBLIC"."WRESTLER_STATUS" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_7BC" FOREIGN KEY("WRESTLER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") NOCHECK;              
ALTER TABLE "PUBLIC"."ACCOUNT_ACHIEVEMENT" ADD CONSTRAINT "PUBLIC"."FK_ACCOUNT_ACHIEVEMENT_ACCOUNT" FOREIGN KEY("ACCOUNT_ID") REFERENCES "PUBLIC"."ACCOUNT"("ID") NOCHECK;     
ALTER TABLE "PUBLIC"."INBOX_ITEM_TARGET" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_77D9" FOREIGN KEY("INBOX_ITEM_ID") REFERENCES "PUBLIC"."INBOX_ITEM"("INBOX_ITEM_ID") NOCHECK;     
ALTER TABLE "PUBLIC"."SHOW_TEMPLATE" ADD CONSTRAINT "PUBLIC"."FK_SHOW_TEMPLATE_COMMENTARY_TEAM" FOREIGN KEY("COMMENTARY_TEAM_ID") REFERENCES "PUBLIC"."COMMENTARY_TEAM"("TEAM_ID") ON DELETE SET NULL NOCHECK; 
ALTER TABLE "PUBLIC"."LEAGUE_EXCLUDED_WRESTLER" ADD CONSTRAINT "PUBLIC"."FK_EXCLUDED_LEAGUE" FOREIGN KEY("LEAGUE_ID") REFERENCES "PUBLIC"."LEAGUE"("ID") ON DELETE CASCADE NOCHECK;            
ALTER TABLE "PUBLIC"."CARD" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_1F7" FOREIGN KEY("SET_ID") REFERENCES "PUBLIC"."CARD_SET"("SET_ID") ON DELETE CASCADE NOCHECK; 
ALTER TABLE "PUBLIC"."DRAFT_PICK" ADD CONSTRAINT "PUBLIC"."FK_PICK_WRESTLER" FOREIGN KEY("WRESTLER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") NOCHECK; 
ALTER TABLE "PUBLIC"."LOCATION_CULTURAL_TAG" ADD CONSTRAINT "PUBLIC"."FK_LOC_CULT_TAG_ON_LOCATION" FOREIGN KEY("LOCATION_ID") REFERENCES "PUBLIC"."LOCATION"("LOCATION_ID") NOCHECK;           
ALTER TABLE "PUBLIC"."SEGMENT_PARTICIPANT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_B73" FOREIGN KEY("SEGMENT_ID") REFERENCES "PUBLIC"."SEGMENT"("SEGMENT_ID") ON DELETE CASCADE NOCHECK;           
ALTER TABLE "PUBLIC"."SEGMENT_TITLE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_CC58" FOREIGN KEY("SEGMENT_ID") REFERENCES "PUBLIC"."SEGMENT"("SEGMENT_ID") ON DELETE CASCADE NOCHECK;
ALTER TABLE "PUBLIC"."DECK_CARD" ADD CONSTRAINT "PUBLIC"."FK_DECK_CARD_SET" FOREIGN KEY("SET_ID") REFERENCES "PUBLIC"."CARD_SET"("SET_ID") NOCHECK;            
ALTER TABLE "PUBLIC"."TEAM" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_273" FOREIGN KEY("WRESTLER1_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") ON DELETE CASCADE NOCHECK;      
ALTER TABLE "PUBLIC"."STORYLINE_MILESTONE" ADD CONSTRAINT "PUBLIC"."FK_MILESTONE_NEXT_SUCCESS" FOREIGN KEY("NEXT_ON_SUCCESS_ID") REFERENCES "PUBLIC"."STORYLINE_MILESTONE"("ID") NOCHECK;      
ALTER TABLE "PUBLIC"."UNIVERSE_INVITE" ADD CONSTRAINT "PUBLIC"."FK_INVITE_CREATED_BY" FOREIGN KEY("CREATED_BY") REFERENCES "PUBLIC"."ACCOUNT"("ID") ON DELETE SET NULL NOCHECK;
ALTER TABLE "PUBLIC"."FACTION" ADD CONSTRAINT "PUBLIC"."FK_FACTION_MANAGER" FOREIGN KEY("MANAGER_ID") REFERENCES "PUBLIC"."NPC"("ID") NOCHECK; 
ALTER TABLE "PUBLIC"."SEGMENT_SEGMENT_RULE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_C84" FOREIGN KEY("SEGMENT_ID") REFERENCES "PUBLIC"."SEGMENT"("SEGMENT_ID") ON DELETE CASCADE NOCHECK;          
ALTER TABLE "PUBLIC"."COMMENTATOR" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_FDBB14" FOREIGN KEY("NPC_ID") REFERENCES "PUBLIC"."NPC"("ID") ON DELETE CASCADE NOCHECK;
ALTER TABLE "PUBLIC"."WRESTLER_ALIGNMENT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_F8F8" FOREIGN KEY("CAMPAIGN_ID") REFERENCES "PUBLIC"."CAMPAIGN"("ID") NOCHECK;   
ALTER TABLE "PUBLIC"."WRESTLER_STATE" ADD CONSTRAINT "PUBLIC"."FK_WS_WRESTLER" FOREIGN KEY("WRESTLER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") NOCHECK;               
ALTER TABLE "PUBLIC"."DECK_CARD" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_BF7" FOREIGN KEY("DECK_ID") REFERENCES "PUBLIC"."DECK"("DECK_ID") ON DELETE CASCADE NOCHECK;              
ALTER TABLE "PUBLIC"."CAMPAIGN_STATE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_66CF1" FOREIGN KEY("CURRENT_MATCH_ID") REFERENCES "PUBLIC"."SEGMENT"("SEGMENT_ID") NOCHECK;          
ALTER TABLE "PUBLIC"."LEAGUE_ROSTER" ADD CONSTRAINT "PUBLIC"."FK_ROSTER_LEAGUE" FOREIGN KEY("LEAGUE_ID") REFERENCES "PUBLIC"."LEAGUE"("ID") NOCHECK;           
ALTER TABLE "PUBLIC"."WRESTLER_STATE" ADD CONSTRAINT "PUBLIC"."FK_WS_FACTION" FOREIGN KEY("FACTION_ID") REFERENCES "PUBLIC"."FACTION"("FACTION_ID") NOCHECK;   
ALTER TABLE "PUBLIC"."FEUD_PARTICIPANT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_C875D" FOREIGN KEY("WRESTLER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") ON DELETE CASCADE NOCHECK;         
ALTER TABLE "PUBLIC"."TITLE_CONTENDER" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_C6B1" FOREIGN KEY("WRESTLER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") ON DELETE CASCADE NOCHECK;           
ALTER TABLE "PUBLIC"."TEAM" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_273A5" FOREIGN KEY("FACTION_ID") REFERENCES "PUBLIC"."FACTION"("FACTION_ID") ON DELETE SET NULL NOCHECK;       
ALTER TABLE "PUBLIC"."ACCOUNT_ROLES" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_D0E" FOREIGN KEY("ROLE_ID") REFERENCES "PUBLIC"."ROLE"("ID") ON DELETE CASCADE NOCHECK;               
ALTER TABLE "PUBLIC"."INJURY" ADD CONSTRAINT "PUBLIC"."FK_INJURY_INJURY_TYPE" FOREIGN KEY("INJURY_TYPE_ID") REFERENCES "PUBLIC"."INJURY_TYPE"("INJURY_TYPE_ID") NOCHECK;       
ALTER TABLE "PUBLIC"."WRESTLING_SHOW" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_26D2" FOREIGN KEY("SHOW_TYPE_ID") REFERENCES "PUBLIC"."SHOW_TYPE"("SHOW_TYPE_ID") ON DELETE RESTRICT NOCHECK;        
ALTER TABLE "PUBLIC"."UNIVERSE_WRESTLER_EXCLUSIONS" ADD CONSTRAINT "PUBLIC"."FK_UWE_UNIVERSE" FOREIGN KEY("UNIVERSE_ID") REFERENCES "PUBLIC"."UNIVERSE"("ID") NOCHECK;         
ALTER TABLE "PUBLIC"."UNIVERSE_JOIN_REQUEST" ADD CONSTRAINT "PUBLIC"."FK_REQUEST_RESOLVED_BY" FOREIGN KEY("RESOLVED_BY") REFERENCES "PUBLIC"."ACCOUNT"("ID") ON DELETE SET NULL NOCHECK;       
ALTER TABLE "PUBLIC"."CAMPAIGN_STATE_UPGRADES" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_2518" FOREIGN KEY("UPGRADE_ID") REFERENCES "PUBLIC"."CAMPAIGN_UPGRADE"("ID") NOCHECK;       
ALTER TABLE "PUBLIC"."TITLE_CHAMPION" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_3B7F" FOREIGN KEY("WRESTLER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") ON DELETE CASCADE NOCHECK;            
ALTER TABLE "PUBLIC"."FACTION_RIVALRY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_E25" FOREIGN KEY("FACTION1_ID") REFERENCES "PUBLIC"."FACTION"("FACTION_ID") ON DELETE CASCADE NOCHECK;              
ALTER TABLE "PUBLIC"."ARENA" ADD CONSTRAINT "PUBLIC"."FK_ARENA_ON_LOCATION" FOREIGN KEY("LOCATION_ID") REFERENCES "PUBLIC"."LOCATION"("LOCATION_ID") NOCHECK;  
ALTER TABLE "PUBLIC"."WRESTLER_STATUS_HISTORY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_64C" FOREIGN KEY("WRESTLER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") NOCHECK;      
ALTER TABLE "PUBLIC"."DECK" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_1FF4" FOREIGN KEY("WRESTLER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") ON DELETE CASCADE NOCHECK;      
ALTER TABLE "PUBLIC"."CAMPAIGN" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_264E" FOREIGN KEY("WRESTLER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") NOCHECK;    
ALTER TABLE "PUBLIC"."WRESTLER_CONTRACT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_1CA5" FOREIGN KEY("WRESTLER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") ON DELETE CASCADE NOCHECK;         
ALTER TABLE "PUBLIC"."DRAFT" ADD CONSTRAINT "PUBLIC"."FK_DRAFT_LEAGUE" FOREIGN KEY("LEAGUE_ID") REFERENCES "PUBLIC"."LEAGUE"("ID") NOCHECK;    
ALTER TABLE "PUBLIC"."LEAGUE_MEMBERSHIP" ADD CONSTRAINT "PUBLIC"."FK_MEMBERSHIP_MEMBER" FOREIGN KEY("MEMBER_ID") REFERENCES "PUBLIC"."ACCOUNT"("ID") NOCHECK;  
ALTER TABLE "PUBLIC"."UNIVERSE_JOIN_REQUEST" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_EF1EF" FOREIGN KEY("ACCOUNT_ID") REFERENCES "PUBLIC"."ACCOUNT"("ID") ON DELETE SET NULL NOCHECK;              
ALTER TABLE "PUBLIC"."ACCOUNT_ACHIEVEMENT" ADD CONSTRAINT "PUBLIC"."FK_ACCOUNT_ACHIEVEMENT_ACHIEVEMENT" FOREIGN KEY("ACHIEVEMENT_ID") REFERENCES "PUBLIC"."ACHIEVEMENT"("ACHIEVEMENT_ID") NOCHECK;             
ALTER TABLE "PUBLIC"."LEAGUE_ROSTER" ADD CONSTRAINT "PUBLIC"."FK_ROSTER_WRESTLER" FOREIGN KEY("WRESTLER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") NOCHECK;            
ALTER TABLE "PUBLIC"."SEGMENT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_A0702" FOREIGN KEY("SEGMENT_TYPE_ID") REFERENCES "PUBLIC"."SEGMENT_TYPE"("SEGMENT_TYPE_ID") ON DELETE RESTRICT NOCHECK;     
ALTER TABLE "PUBLIC"."UNIVERSE_EXPANSION_SETTINGS" ADD CONSTRAINT "PUBLIC"."FK_UES_UNIVERSE" FOREIGN KEY("UNIVERSE_ID") REFERENCES "PUBLIC"."UNIVERSE"("ID") NOCHECK;          
ALTER TABLE "PUBLIC"."UNIVERSE_WRESTLER_EXCLUSIONS" ADD CONSTRAINT "PUBLIC"."FK_UWE_WRESTLER" FOREIGN KEY("WRESTLER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") NOCHECK;
ALTER TABLE "PUBLIC"."UNIVERSE_MEMBERS" ADD CONSTRAINT "PUBLIC"."FK_UM_ACCOUNT" FOREIGN KEY("ACCOUNT_ID") REFERENCES "PUBLIC"."ACCOUNT"("ID") ON DELETE CASCADE NOCHECK;       
ALTER TABLE "PUBLIC"."MATCH_FULFILLMENT" ADD CONSTRAINT "PUBLIC"."FK_FULFILLMENT_WINNER" FOREIGN KEY("WINNER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") NOCHECK;       
ALTER TABLE "PUBLIC"."TITLE_REIGN" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_EA1" FOREIGN KEY("TITLE_ID") REFERENCES "PUBLIC"."TITLE"("TITLE_ID") ON DELETE CASCADE NOCHECK;         
ALTER TABLE "PUBLIC"."TEAM" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_273A" FOREIGN KEY("WRESTLER2_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") ON DELETE CASCADE NOCHECK;     
ALTER TABLE "PUBLIC"."WRESTLING_SHOW" ADD CONSTRAINT "PUBLIC"."FK_WRESTLING_SHOW_ON_ARENA" FOREIGN KEY("ARENA_ID") REFERENCES "PUBLIC"."ARENA"("ARENA_ID") NOCHECK;            
ALTER TABLE "PUBLIC"."SEGMENT_SEGMENT_RULE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_C843" FOREIGN KEY("SEGMENT_RULE_ID") REFERENCES "PUBLIC"."SEGMENT_RULE"("SEGMENT_RULE_ID") ON DELETE CASCADE NOCHECK;          
ALTER TABLE "PUBLIC"."STORYLINE_MILESTONE" ADD CONSTRAINT "PUBLIC"."FK_MILESTONE_STORYLINE" FOREIGN KEY("STORYLINE_ID") REFERENCES "PUBLIC"."CAMPAIGN_STORYLINE"("ID") NOCHECK;
ALTER TABLE "PUBLIC"."SEGMENT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_A0702B" FOREIGN KEY("WINNER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") ON DELETE SET NULL NOCHECK;  
ALTER TABLE "PUBLIC"."WRESTLER_STATE" ADD CONSTRAINT "PUBLIC"."FK_WS_MANAGER" FOREIGN KEY("MANAGER_ID") REFERENCES "PUBLIC"."NPC"("ID") NOCHECK;               
ALTER TABLE "PUBLIC"."MATCH_FULFILLMENT" ADD CONSTRAINT "PUBLIC"."FK_FULFILLMENT_SEGMENT" FOREIGN KEY("SEGMENT_ID") REFERENCES "PUBLIC"."SEGMENT"("SEGMENT_ID") NOCHECK;       
ALTER TABLE "PUBLIC"."SEGMENT_PARTICIPANT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_B73F" FOREIGN KEY("WRESTLER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") ON DELETE CASCADE NOCHECK;       
ALTER TABLE "PUBLIC"."WRESTLER_STATUS_HISTORY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_64C2" FOREIGN KEY("STATUS_CARD_ID") REFERENCES "PUBLIC"."STATUS_CARD"("ID") NOCHECK;        
ALTER TABLE "PUBLIC"."WRESTLER_CONTRACT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_1CA55" FOREIGN KEY("LEAGUE_ID") REFERENCES "PUBLIC"."LEAGUE"("ID") ON DELETE CASCADE NOCHECK;     
ALTER TABLE "PUBLIC"."TITLE_REIGN_CHAMPION" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_54ED" FOREIGN KEY("WRESTLER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") ON DELETE CASCADE NOCHECK;      
ALTER TABLE "PUBLIC"."LEAGUE" ADD CONSTRAINT "PUBLIC"."FK_LEAGUE_COMMISSIONER" FOREIGN KEY("COMMISSIONER_ID") REFERENCES "PUBLIC"."ACCOUNT"("ID") NOCHECK;     
ALTER TABLE "PUBLIC"."COMMENTARY_TEAM_MEMBERS" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_7D2" FOREIGN KEY("TEAM_ID") REFERENCES "PUBLIC"."COMMENTARY_TEAM"("TEAM_ID") ON DELETE CASCADE NOCHECK;     
ALTER TABLE "PUBLIC"."CAMPAIGN_STATE_CARDS" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_4F0" FOREIGN KEY("CARD_ID") REFERENCES "PUBLIC"."CAMPAIGN_ABILITY_CARD"("ID") NOCHECK;         
ALTER TABLE "PUBLIC"."SEGMENT_TITLE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_CC58D" FOREIGN KEY("TITLE_ID") REFERENCES "PUBLIC"."TITLE"("TITLE_ID") ON DELETE CASCADE NOCHECK;     
ALTER TABLE "PUBLIC"."WRESTLING_SHOW" ADD CONSTRAINT "PUBLIC"."FK_SHOW_COMMENTARY_TEAM" FOREIGN KEY("COMMENTARY_TEAM_ID") REFERENCES "PUBLIC"."COMMENTARY_TEAM"("TEAM_ID") ON DELETE SET NULL NOCHECK;         
ALTER TABLE "PUBLIC"."WRESTLER_ALIGNMENT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_F8F" FOREIGN KEY("WRESTLER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") NOCHECK;           
ALTER TABLE "PUBLIC"."WRESTLER_RELATIONSHIP" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_A1667" FOREIGN KEY("WRESTLER2_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") ON DELETE CASCADE NOCHECK;   
ALTER TABLE "PUBLIC"."WRESTLER_SEASON_SNAPSHOT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_D5AF" FOREIGN KEY("SEASON_ID") REFERENCES "PUBLIC"."SEASON"("SEASON_ID") ON DELETE CASCADE NOCHECK;        
ALTER TABLE "PUBLIC"."OUTCOME_MATRIX_ENTRY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_A02C8" FOREIGN KEY("REDIRECT_MATRIX_ID") REFERENCES "PUBLIC"."OUTCOME_MATRIX"("OUTCOME_MATRIX_ID") ON DELETE SET NULL NOCHECK; 
ALTER TABLE "PUBLIC"."DRAFT_PICK" ADD CONSTRAINT "PUBLIC"."FK_PICK_USER" FOREIGN KEY("USER_ID") REFERENCES "PUBLIC"."ACCOUNT"("ID") NOCHECK;   
ALTER TABLE "PUBLIC"."DECK_CARD" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_BF75" FOREIGN KEY("CARD_ID") REFERENCES "PUBLIC"."CARD"("CARD_ID") ON DELETE CASCADE NOCHECK;             
ALTER TABLE "PUBLIC"."DRAFT" ADD CONSTRAINT "PUBLIC"."FK_DRAFT_TURN_USER" FOREIGN KEY("CURRENT_TURN_USER_ID") REFERENCES "PUBLIC"."ACCOUNT"("ID") NOCHECK;     
ALTER TABLE "PUBLIC"."WRESTLING_SHOW" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_26D2FD" FOREIGN KEY("TEMPLATE_ID") REFERENCES "PUBLIC"."SHOW_TEMPLATE"("TEMPLATE_ID") ON DELETE SET NULL NOCHECK;    
ALTER TABLE "PUBLIC"."UNIVERSE_MEMBERS" ADD CONSTRAINT "PUBLIC"."FK_UM_UNIVERSE" FOREIGN KEY("UNIVERSE_ID") REFERENCES "PUBLIC"."UNIVERSE"("ID") NOCHECK;      
ALTER TABLE "PUBLIC"."LEAGUE_EXCLUDED_WRESTLER" ADD CONSTRAINT "PUBLIC"."FK_EXCLUDED_WRESTLER" FOREIGN KEY("WRESTLER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") ON DELETE CASCADE NOCHECK;             
ALTER TABLE "PUBLIC"."UNIVERSE_JOIN_REQUEST" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_EF1" FOREIGN KEY("UNIVERSE_ID") REFERENCES "PUBLIC"."UNIVERSE"("ID") ON DELETE CASCADE NOCHECK;               
ALTER TABLE "PUBLIC"."WRESTLING_SHOW" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_26D2F" FOREIGN KEY("SEASON_ID") REFERENCES "PUBLIC"."SEASON"("SEASON_ID") ON DELETE SET NULL NOCHECK;
ALTER TABLE "PUBLIC"."CAMPAIGN_ENCOUNTER" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_5F9" FOREIGN KEY("CAMPAIGN_ID") REFERENCES "PUBLIC"."CAMPAIGN"("ID") NOCHECK;    
ALTER TABLE "PUBLIC"."TITLE_CHAMPION" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_3B7" FOREIGN KEY("TITLE_ID") REFERENCES "PUBLIC"."TITLE"("TITLE_ID") ON DELETE CASCADE NOCHECK;      
ALTER TABLE "PUBLIC"."CAMPAIGN_STATE" ADD CONSTRAINT "PUBLIC"."FK_CAMPAIGN_STATE_STORYLINE" FOREIGN KEY("ACTIVE_STORYLINE_ID") REFERENCES "PUBLIC"."CAMPAIGN_STORYLINE"("ID") NOCHECK;         
ALTER TABLE "PUBLIC"."COMMENTARY_TEAM_MEMBERS" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_7D2A" FOREIGN KEY("COMMENTATOR_ID") REFERENCES "PUBLIC"."COMMENTATOR"("COMMENTATOR_ID") ON DELETE CASCADE NOCHECK;          
ALTER TABLE "PUBLIC"."WRESTLING_SHOW" ADD CONSTRAINT "PUBLIC"."FK_SHOW_LEAGUE" FOREIGN KEY("LEAGUE_ID") REFERENCES "PUBLIC"."LEAGUE"("ID") NOCHECK;            
ALTER TABLE "PUBLIC"."SEGMENT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_A0702B1" FOREIGN KEY("REFEREE_ID") REFERENCES "PUBLIC"."NPC"("ID") NOCHECK; 
ALTER TABLE "PUBLIC"."WRESTLER_ALIGNMENT" ADD CONSTRAINT "PUBLIC"."FK_WRESTLER_ALIGNMENT_UNIVERSE" FOREIGN KEY("UNIVERSE_ID") REFERENCES "PUBLIC"."UNIVERSE"("ID") NOCHECK;    
ALTER TABLE "PUBLIC"."SHOW_TEMPLATE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_5B40" FOREIGN KEY("SHOW_TYPE_ID") REFERENCES "PUBLIC"."SHOW_TYPE"("SHOW_TYPE_ID") ON DELETE RESTRICT NOCHECK;         
ALTER TABLE "PUBLIC"."PASSWORD_RESET_TOKEN" ADD CONSTRAINT "PUBLIC"."FK_PASSWORD_RESET_TOKEN_ACCOUNT" FOREIGN KEY("ACCOUNT_ID") REFERENCES "PUBLIC"."ACCOUNT"("ID") NOCHECK;   
ALTER TABLE "PUBLIC"."CAMPAIGN_STORYLINE" ADD CONSTRAINT "PUBLIC"."FK_STORYLINE_CURRENT_MILESTONE" FOREIGN KEY("CURRENT_MILESTONE_ID") REFERENCES "PUBLIC"."STORYLINE_MILESTONE"("ID") NOCHECK;
ALTER TABLE "PUBLIC"."STORYLINE_MILESTONE" ADD CONSTRAINT "PUBLIC"."FK_MILESTONE_NEXT_FAILURE" FOREIGN KEY("NEXT_ON_FAILURE_ID") REFERENCES "PUBLIC"."STORYLINE_MILESTONE"("ID") NOCHECK;      
ALTER TABLE "PUBLIC"."TITLE_REIGN_CHAMPION" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_54E" FOREIGN KEY("TITLE_REIGN_ID") REFERENCES "PUBLIC"."TITLE_REIGN"("TITLE_REIGN_ID") ON DELETE CASCADE NOCHECK;              
ALTER TABLE "PUBLIC"."BACKSTAGE_ACTION_HISTORY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_F64" FOREIGN KEY("CAMPAIGN_ID") REFERENCES "PUBLIC"."CAMPAIGN"("ID") NOCHECK;              
ALTER TABLE "PUBLIC"."CAMPAIGN_STATE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_66C" FOREIGN KEY("CAMPAIGN_ID") REFERENCES "PUBLIC"."CAMPAIGN"("ID") NOCHECK;        
ALTER TABLE "PUBLIC"."LEAGUE_MEMBERSHIP" ADD CONSTRAINT "PUBLIC"."FK_MEMBERSHIP_LEAGUE" FOREIGN KEY("LEAGUE_ID") REFERENCES "PUBLIC"."LEAGUE"("ID") NOCHECK;   
ALTER TABLE "PUBLIC"."FACTION_RIVALRY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_E259" FOREIGN KEY("FACTION2_ID") REFERENCES "PUBLIC"."FACTION"("FACTION_ID") ON DELETE CASCADE NOCHECK;             
ALTER TABLE "PUBLIC"."FEUD_PARTICIPANT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_C875" FOREIGN KEY("FEUD_ID") REFERENCES "PUBLIC"."MULTI_WRESTLER_FEUD"("MULTI_WRESTLER_FEUD_ID") ON DELETE CASCADE NOCHECK;        
ALTER TABLE "PUBLIC"."ARENA_ENVIRONMENTAL_TRAIT" ADD CONSTRAINT "PUBLIC"."FK_ARENA_ENV_TRAIT_ON_ARENA" FOREIGN KEY("ARENA_ID") REFERENCES "PUBLIC"."ARENA"("ARENA_ID") NOCHECK;
ALTER TABLE "PUBLIC"."RIVALRY" ADD CONSTRAINT "PUBLIC"."FK_RIVALRY_LEAGUE" FOREIGN KEY("LEAGUE_ID") REFERENCES "PUBLIC"."LEAGUE"("ID") NOCHECK;
ALTER TABLE "PUBLIC"."LEAGUE_ROSTER" ADD CONSTRAINT "PUBLIC"."FK_ROSTER_OWNER" FOREIGN KEY("OWNER_ID") REFERENCES "PUBLIC"."ACCOUNT"("ID") NOCHECK;            
ALTER TABLE "PUBLIC"."MATCH_FULFILLMENT" ADD CONSTRAINT "PUBLIC"."FK_FULFILLMENT_SUBMITTER" FOREIGN KEY("SUBMITTED_BY_ID") REFERENCES "PUBLIC"."ACCOUNT"("ID") NOCHECK;        
ALTER TABLE "PUBLIC"."RIVALRY" ADD CONSTRAINT "PUBLIC"."FK_RIVALRY_UNIVERSE" FOREIGN KEY("UNIVERSE_ID") REFERENCES "PUBLIC"."UNIVERSE"("ID") NOCHECK;          
ALTER TABLE "PUBLIC"."MILESTONE_STATUS_REWARDS" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_B50" FOREIGN KEY("MILESTONE_ID") REFERENCES "PUBLIC"."STORYLINE_MILESTONE"("ID") NOCHECK;  
ALTER TABLE "PUBLIC"."CAMPAIGN_COMPLETED_CHAPTERS" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_42" FOREIGN KEY("CAMPAIGN_STATE_ID") REFERENCES "PUBLIC"."CAMPAIGN_STATE"("ID") NOCHECK;
ALTER TABLE "PUBLIC"."CAMPAIGN_STATE_UPGRADES" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_251" FOREIGN KEY("CAMPAIGN_STATE_ID") REFERENCES "PUBLIC"."CAMPAIGN_STATE"("ID") NOCHECK;   
ALTER TABLE "PUBLIC"."INJURY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_80F" FOREIGN KEY("WRESTLER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") ON DELETE CASCADE NOCHECK;     
ALTER TABLE "PUBLIC"."TITLE_CONTENDER" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_C6B" FOREIGN KEY("TITLE_ID") REFERENCES "PUBLIC"."TITLE"("TITLE_ID") ON DELETE CASCADE NOCHECK;     
ALTER TABLE "PUBLIC"."OUTCOME_MATRIX_ENTRY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_A02C" FOREIGN KEY("OUTCOME_MATRIX_ID") REFERENCES "PUBLIC"."OUTCOME_MATRIX"("OUTCOME_MATRIX_ID") ON DELETE CASCADE NOCHECK;    
ALTER TABLE "PUBLIC"."CAMPAIGN_STATE_CARDS" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_4F" FOREIGN KEY("CAMPAIGN_STATE_ID") REFERENCES "PUBLIC"."CAMPAIGN_STATE"("ID") NOCHECK;       
ALTER TABLE "PUBLIC"."FACTION" ADD CONSTRAINT "PUBLIC"."FK_FACTION_LEADER" FOREIGN KEY("LEADER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") ON DELETE SET NULL NOCHECK;  
ALTER TABLE "PUBLIC"."CAMPAIGN_STORYLINE" ADD CONSTRAINT "PUBLIC"."FK_STORYLINE_CAMPAIGN" FOREIGN KEY("CAMPAIGN_ID") REFERENCES "PUBLIC"."CAMPAIGN"("ID") NOCHECK;             
ALTER TABLE "PUBLIC"."DRAMA_EVENT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_C4E6" FOREIGN KEY("SECONDARY_WRESTLER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") ON DELETE SET NULL NOCHECK;    
ALTER TABLE "PUBLIC"."ACCOUNT_ROLES" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_D0" FOREIGN KEY("ACCOUNT_ID") REFERENCES "PUBLIC"."ACCOUNT"("ID") ON DELETE CASCADE NOCHECK;          
ALTER TABLE "PUBLIC"."WRESTLING_SHOW" ADD CONSTRAINT "PUBLIC"."FK_WRESTLING_SHOW_UNIVERSE" FOREIGN KEY("UNIVERSE_ID") REFERENCES "PUBLIC"."UNIVERSE"("ID") NOCHECK;            
ALTER TABLE "PUBLIC"."WRESTLER_RELATIONSHIP" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_A166" FOREIGN KEY("WRESTLER1_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") ON DELETE CASCADE NOCHECK;    
ALTER TABLE "PUBLIC"."WRESTLER_STATUS" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_7BC0" FOREIGN KEY("STATUS_CARD_ID") REFERENCES "PUBLIC"."STATUS_CARD"("ID") NOCHECK;
ALTER TABLE "PUBLIC"."MATCH_FULFILLMENT" ADD CONSTRAINT "PUBLIC"."FK_FULFILLMENT_LEAGUE" FOREIGN KEY("LEAGUE_ID") REFERENCES "PUBLIC"."LEAGUE"("ID") NOCHECK;  
ALTER TABLE "PUBLIC"."FEUD_HEAT_EVENT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_FD" FOREIGN KEY("FEUD_ID") REFERENCES "PUBLIC"."MULTI_WRESTLER_FEUD"("MULTI_WRESTLER_FEUD_ID") ON DELETE CASCADE NOCHECK;           
ALTER TABLE "PUBLIC"."DRAMA_EVENT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_C4E" FOREIGN KEY("PRIMARY_WRESTLER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") ON DELETE SET NULL NOCHECK;       
ALTER TABLE "PUBLIC"."FACTION_HEAT_EVENT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_68" FOREIGN KEY("FACTION_RIVALRY_ID") REFERENCES "PUBLIC"."FACTION_RIVALRY"("FACTION_RIVALRY_ID") ON DELETE CASCADE NOCHECK;     
ALTER TABLE "PUBLIC"."HEAT_EVENT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_CE2" FOREIGN KEY("RIVALRY_ID") REFERENCES "PUBLIC"."RIVALRY"("RIVALRY_ID") ON DELETE CASCADE NOCHECK;    
ALTER TABLE "PUBLIC"."UNIVERSE_JOIN_REQUEST" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_EF1E" FOREIGN KEY("INVITE_ID") REFERENCES "PUBLIC"."UNIVERSE_INVITE"("ID") ON DELETE SET NULL NOCHECK;        
ALTER TABLE "PUBLIC"."RIVALRY" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_73" FOREIGN KEY("WRESTLER1_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") ON DELETE CASCADE NOCHECK;    
ALTER TABLE "PUBLIC"."RINGSIDE_ACTION" ADD CONSTRAINT "PUBLIC"."FK_RINGSIDE_ACTION_TYPE" FOREIGN KEY("RINGSIDE_ACTION_TYPE_ID") REFERENCES "PUBLIC"."RINGSIDE_ACTION_TYPE"("RINGSIDE_ACTION_TYPE_ID") NOCHECK; 
ALTER TABLE "PUBLIC"."UNIVERSE_INVITE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_D5F" FOREIGN KEY("UNIVERSE_ID") REFERENCES "PUBLIC"."UNIVERSE"("ID") ON DELETE CASCADE NOCHECK;     
ALTER TABLE "PUBLIC"."WRESTLER_SEASON_SNAPSHOT" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_D5A" FOREIGN KEY("WRESTLER_ID") REFERENCES "PUBLIC"."WRESTLER"("WRESTLER_ID") ON DELETE CASCADE NOCHECK;   
ALTER TABLE "PUBLIC"."ACCOUNT_TUTORIAL_COMPLETION" ADD CONSTRAINT "PUBLIC"."FK_ATC_ACCOUNT" FOREIGN KEY("ACCOUNT_ID") REFERENCES "PUBLIC"."ACCOUNT"("ID") ON DELETE CASCADE NOCHECK;           
ALTER TABLE "PUBLIC"."WRESTLER_STATE" ADD CONSTRAINT "PUBLIC"."FK_WS_UNIVERSE" FOREIGN KEY("UNIVERSE_ID") REFERENCES "PUBLIC"."UNIVERSE"("ID") NOCHECK;        
ALTER TABLE "PUBLIC"."WRESTLER" ADD CONSTRAINT "PUBLIC"."FK_WRESTLER_ACCOUNT" FOREIGN KEY("ACCOUNT_ID") REFERENCES "PUBLIC"."ACCOUNT"("ID") NOCHECK;           
ALTER TABLE "PUBLIC"."TITLE_REIGN" ADD CONSTRAINT "PUBLIC"."FK_TITLE_REIGN_WON_AT_SEGMENT" FOREIGN KEY("WON_AT_SEGMENT_ID") REFERENCES "PUBLIC"."SEGMENT"("SEGMENT_ID") NOCHECK;               
