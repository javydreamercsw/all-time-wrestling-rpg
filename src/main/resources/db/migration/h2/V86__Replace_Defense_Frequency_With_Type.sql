-- Replace integer defense_frequency with DefenseFrequencyType enum

ALTER TABLE title ADD COLUMN defense_frequency_type VARCHAR(50);

-- Backfill: map old integer (show count) to nearest semantic type
UPDATE title SET defense_frequency_type = 'WEEKLY'    WHERE defense_frequency <= 1;
UPDATE title SET defense_frequency_type = 'BI_WEEKLY' WHERE defense_frequency = 2;
UPDATE title SET defense_frequency_type = 'PLE'       WHERE defense_frequency >= 3;

-- Backfill nulls by tier: MAIN_EVENTER -> PLE, MIDCARDER -> BI_WEEKLY, all others -> WEEKLY
UPDATE title SET defense_frequency_type = 'PLE'       WHERE defense_frequency IS NULL AND tier = 'MAIN_EVENTER';
UPDATE title SET defense_frequency_type = 'BI_WEEKLY' WHERE defense_frequency IS NULL AND tier = 'MIDCARDER';
UPDATE title SET defense_frequency_type = 'WEEKLY'    WHERE defense_frequency IS NULL AND tier NOT IN ('MAIN_EVENTER', 'MIDCARDER');

ALTER TABLE title DROP COLUMN defense_frequency;
