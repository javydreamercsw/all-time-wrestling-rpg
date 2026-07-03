-- Remap segments using "Extreme" to "No DQ" (the canonical name after rule consolidation),
-- then remove the now-orphaned "Extreme" rule row.
UPDATE segment_segment_rule
SET segment_rule_id = (SELECT segment_rule_id FROM segment_rule WHERE name = 'No DQ')
WHERE segment_rule_id = (SELECT segment_rule_id FROM segment_rule WHERE name = 'Extreme');

DELETE FROM segment_rule WHERE name = 'Extreme';
