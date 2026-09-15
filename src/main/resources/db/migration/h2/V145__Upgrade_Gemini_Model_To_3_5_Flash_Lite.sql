-- ATW-2wwl: gemini-3.1-flash-lite-preview was retired by Google and returns errors.
-- Migrate any install still carrying the retired model to gemini-3.5-flash-lite.
-- User-customized model values (anything else) are intentionally left untouched.
UPDATE game_setting
SET setting_value = 'gemini-3.5-flash-lite'
WHERE setting_key = 'AI_GEMINI_MODEL_NAME'
  AND setting_value = 'gemini-3.1-flash-lite-preview';
