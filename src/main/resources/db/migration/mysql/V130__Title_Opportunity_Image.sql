-- Briefcase artwork (ATW-jpki): optional image for the briefcase badge on the career view,
-- the wrestler profile panel and the tournament detail view. Mirrors Title.image_url; NULL
-- renders the emoji badge fallback.
ALTER TABLE title_opportunity ADD COLUMN image_url VARCHAR(512) NULL;
