-- Force reset LocalAI model to gpt-oss-120b which is verified to be present
UPDATE game_setting 
SET setting_value = 'gpt-oss-120b' 
WHERE setting_key = 'AI_LOCALAI_MODEL';