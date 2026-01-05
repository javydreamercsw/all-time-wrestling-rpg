ALTER TABLE wrestler ADD COLUMN manager_id BIGINT;
ALTER TABLE wrestler ADD CONSTRAINT fk_wrestler_manager FOREIGN KEY (manager_id) REFERENCES npc(id);

ALTER TABLE faction ADD COLUMN manager_id BIGINT;
ALTER TABLE faction ADD CONSTRAINT fk_faction_manager FOREIGN KEY (manager_id) REFERENCES npc(id);

ALTER TABLE team ADD COLUMN manager_id BIGINT;
ALTER TABLE team ADD CONSTRAINT fk_team_manager FOREIGN KEY (manager_id) REFERENCES npc(id);
