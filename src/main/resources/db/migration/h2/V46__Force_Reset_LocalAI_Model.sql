-- Force reset LocalAI model to llama-3.1-8b and set the verified URL
UPDATE game_setting 
SET setting_value = 'llama-3.1-8b' 
WHERE setting_key = 'AI_LOCALAI_MODEL';

UPDATE game_setting 
SET setting_value = 'https://huggingface.co/bartowski/Meta-Llama-3.1-8B-Instruct-GGUF/resolve/main/Meta-Llama-3.1-8B-Instruct-Q4_K_M.gguf' 
WHERE setting_key = 'AI_LOCALAI_MODEL_URL';
