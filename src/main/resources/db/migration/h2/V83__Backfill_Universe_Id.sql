-- Backfill universe_id = 1 (Default Universe) for all entities that received the
-- universe_id column in V81 but had no backfill applied to pre-existing rows.
UPDATE title SET universe_id = 1 WHERE universe_id IS NULL;
UPDATE faction SET universe_id = 1 WHERE universe_id IS NULL;
UPDATE team SET universe_id = 1 WHERE universe_id IS NULL;
UPDATE injury SET universe_id = 1 WHERE universe_id IS NULL;
UPDATE drama_event SET universe_id = 1 WHERE universe_id IS NULL;
UPDATE campaign SET universe_id = 1 WHERE universe_id IS NULL;
UPDATE league SET universe_id = 1 WHERE universe_id IS NULL;
UPDATE wrestling_show SET universe_id = 1 WHERE universe_id IS NULL;
