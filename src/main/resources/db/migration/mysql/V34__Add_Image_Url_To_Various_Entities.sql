-- V10: Add image_url to various entities for default image system support
ALTER TABLE title ADD COLUMN image_url VARCHAR(512);
ALTER TABLE team ADD COLUMN image_url VARCHAR(512);
ALTER TABLE faction ADD COLUMN image_url VARCHAR(512);
