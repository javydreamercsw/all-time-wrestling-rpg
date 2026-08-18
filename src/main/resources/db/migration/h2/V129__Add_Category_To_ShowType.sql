ALTER TABLE show_type ADD COLUMN IF NOT EXISTS category VARCHAR(50) NOT NULL DEFAULT 'OTHER';
UPDATE show_type SET category = 'PLE'    WHERE LOWER(name) LIKE '%ple%' OR LOWER(name) LIKE '%premium%';
UPDATE show_type SET category = 'WEEKLY' WHERE LOWER(name) = 'weekly';
ALTER TABLE show_type DROP COLUMN IF EXISTS is_ppv;
