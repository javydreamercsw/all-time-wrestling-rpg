UPDATE game_setting
SET setting_value = 'gemini-3.1-flash-lite-preview'
WHERE setting_key = 'AI_GEMINI_MODEL_NAME'
  AND setting_value = 'gemini-2.5-flash';
