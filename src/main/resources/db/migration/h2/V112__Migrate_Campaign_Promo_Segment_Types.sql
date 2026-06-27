-- Insert campaign-internal promo rules (idempotent; DataInitializer will upsert on next start)
MERGE INTO segment_rule (name, description, requires_high_heat, no_dq, bump_addition, expansion_code, creation_date)
    KEY (name)
    VALUES ('Faction Beatdown',
            'A non-match segment where one or more wrestlers are attacked by a group',
            FALSE, FALSE, 'NONE', 'CUSTOM', NOW());

MERGE INTO segment_rule (name, description, requires_high_heat, no_dq, bump_addition, expansion_code, creation_date)
    KEY (name)
    VALUES ('GM Office Confrontation',
            'A backstage segment where the player confronts management',
            FALSE, FALSE, 'NONE', 'CUSTOM', NOW());

MERGE INTO segment_rule (name, description, requires_high_heat, no_dq, bump_addition, expansion_code, creation_date)
    KEY (name)
    VALUES ('Performance Review',
            'A segment where management evaluates the player''s status and results',
            FALSE, FALSE, 'NONE', 'CUSTOM', NOW());

-- Backfill join table: link existing segments to their matching new rule before re-pointing the type
INSERT INTO segment_segment_rule (segment_id, segment_rule_id)
SELECT s.segment_id, r.segment_rule_id
FROM segment s
         JOIN segment_type st ON s.segment_type_id = st.segment_type_id
         JOIN segment_rule r ON r.name = st.name
WHERE st.name IN ('Faction Beatdown', 'GM Office Confrontation', 'Performance Review')
  AND NOT EXISTS (SELECT 1
                  FROM segment_segment_rule x
                  WHERE x.segment_id = s.segment_id
                    AND x.segment_rule_id = r.segment_rule_id);

-- Re-point existing segments to the Promo type
UPDATE segment
SET segment_type_id = (SELECT segment_type_id FROM segment_type WHERE name = 'Promo')
WHERE segment_type_id IN (SELECT segment_type_id
                          FROM segment_type
                          WHERE name IN
                                ('Faction Beatdown', 'GM Office Confrontation', 'Performance Review'));

-- Remove the now-unused campaign segment types
DELETE FROM segment_type WHERE name IN ('Faction Beatdown', 'GM Office Confrontation', 'Performance Review');
