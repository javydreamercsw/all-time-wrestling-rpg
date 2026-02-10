-- Fix the LocalAI model name by removing the colon which causes loading issues
UPDATE game_setting 
SET setting_value = 'llama-3.2-1b-instruct' 
WHERE setting_key = 'AI_LOCALAI_MODEL' 
AND setting_value = 'llama-3.2-1b-instruct:q4_k_m';