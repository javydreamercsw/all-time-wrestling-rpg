-- Insert campaign-internal promo rules (idempotent; DataInitializer will upsert on next start)
INSERT IGNORE INTO segment_rule (name, description, requires_high_heat, no_dq, bump_addition, expansion_code, creation_date)
VALUES ('Faction Beatdown',
        'A non-match segment where one or more wrestlers are attacked by a group',
        0, 0, 'NONE', 'CUSTOM', NOW());

INSERT IGNORE INTO segment_rule (name, description, requires_high_heat, no_dq, bump_addition, expansion_code, creation_date)
VALUES ('GM Office Confrontation',
        'A backstage segment where the player confronts management',
        0, 0, 'NONE', 'CUSTOM', NOW());

INSERT IGNORE INTO segment_rule (name, description, requires_high_heat, no_dq, bump_addition, expansion_code, creation_date)
VALUES ('Performance Review',
        'A segment where management evaluates the player''s status and results',
        0, 0, 'NONE', 'CUSTOM', NOW());

-- Backfill join table: link existing segments to their matching new rule before re-pointing the type
INSERT IGNORE INTO segment_segment_rule (segment_id, segment_rule_id)
SELECT s.segment_id, r.segment_rule_id
FROM segment s
         JOIN segment_type st ON s.segment_type_id = st.segment_type_id
         JOIN segment_rule r ON r.name = st.name
WHERE st.name IN ('Faction Beatdown', 'GM Office Confrontation', 'Performance Review');

-- Re-point existing segments to the Promo type
UPDATE segment
SET segment_type_id = (SELECT segment_type_id FROM segment_type WHERE name = 'Promo')
WHERE segment_type_id IN (SELECT segment_type_id
                          FROM segment_type
                          WHERE name IN
                                ('Faction Beatdown', 'GM Office Confrontation', 'Performance Review'));

-- Remove the now-unused campaign segment types
DELETE FROM segment_type WHERE name IN ('Faction Beatdown', 'GM Office Confrontation', 'Performance Review');
