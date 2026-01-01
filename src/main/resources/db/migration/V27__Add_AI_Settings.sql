INSERT INTO game_setting (setting_key, setting_value) VALUES
('AI_TIMEOUT', '300'),
('AI_PROVIDER_AUTO', 'true'),

('AI_OPENAI_ENABLED', 'false'),
('AI_OPENAI_API_URL', 'https://api.openai.com/v1/chat/completions'),
('AI_OPENAI_DEFAULT_MODEL', 'gpt-3.5-turbo'),
('AI_OPENAI_PREMIUM_MODEL', 'gpt-4'),
('AI_OPENAI_MAX_TOKENS', '1000'),
('AI_OPENAI_TEMPERATURE', '0.7'),

('AI_CLAUDE_ENABLED', 'false'),
('AI_CLAUDE_API_URL', 'https://api.anthropic.com/v1/messages/'),
('AI_CLAUDE_MODEL_NAME', 'claude-3-haiku-20240307'),

('AI_GEMINI_ENABLED', 'false'),
('AI_GEMINI_API_URL', 'https://generativelanguage.googleapis.com/v1beta/models/'),
('AI_GEMINI_MODEL_NAME', 'gemini-2.5-flash'),

('AI_LOCALAI_ENABLED', 'false'),
('AI_LOCALAI_BASE_URL', 'http://localhost:8088'),
('AI_LOCALAI_MODEL', 'llama-3.2-1b-instruct:q4_k_m'),
('AI_LOCALAI_MODEL_URL', '');
