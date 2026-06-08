-- Migrate credential settings to the default (first GLOBAL) universe so they
-- are not inherited by other universes. If no universe exists yet this is a
-- no-op and the credentials stay global until the app creates the first universe.
UPDATE game_setting
SET universe_id = (
    SELECT id FROM universe
    WHERE type = 'GLOBAL'
    ORDER BY id ASC
    LIMIT 1
)
WHERE setting_key IN (
    'notion_token',
    'AI_OPENAI_API_KEY',
    'AI_CLAUDE_API_KEY',
    'AI_GEMINI_API_KEY',
    'AI_POLLINATIONS_API_KEY'
)
AND universe_id IS NULL
AND EXISTS (SELECT 1 FROM universe WHERE type = 'GLOBAL');
