-- Rescale rivalry heat to a 0-100 scale (x2.5, preserving band membership) and rescale the
-- heat-dependent game settings to match (ATW-czai).
-- NOT rescaled: rivalry_resolution_threshold_ple/regular — those compare against a 2d20 roll
-- (max 40), not heat. rivalry_resolution_on_regular_shows is a boolean.
UPDATE rivalry SET heat = LEAST(100, ROUND(heat * 2.5));
UPDATE multi_wrestler_feud SET heat = LEAST(100, ROUND(heat * 2.5));
UPDATE faction_rivalry SET heat = LEAST(100, ROUND(heat * 2.5));

-- New tunables: heat ceiling and blowoff vent.
MERGE INTO game_setting (setting_key, setting_value, universe_id)
    KEY (setting_key, universe_id)
VALUES ('rivalry.heat_ceiling', '100', NULL),
       ('rivalry.blowoff_heat_vent', '30', NULL);

-- Rescale heat-scale settings to the 0-100 scale (only if still at their seeded defaults).
UPDATE game_setting SET setting_value = '25'
    WHERE setting_key = 'contender.match_heat_bonus' AND setting_value = '10' AND universe_id IS NULL;
UPDATE game_setting SET setting_value = '3'
    WHERE setting_key = 'rivalry_heat_decay_per_interval' AND setting_value = '1' AND universe_id IS NULL;
UPDATE game_setting SET setting_value = '25'
    WHERE setting_key = 'rivalry_resolution_min_heat' AND setting_value = '10' AND universe_id IS NULL;
