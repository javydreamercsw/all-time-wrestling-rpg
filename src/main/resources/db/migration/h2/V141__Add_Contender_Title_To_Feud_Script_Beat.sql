-- A beat may designate the winner of its segment as #1 contender for a title
-- (CONTENDER_DESIGNATION outcome). Nullable: most beats have no contender payload.
ALTER TABLE feud_script_beat ADD COLUMN contender_title_id BIGINT NULL;
ALTER TABLE feud_script_beat ADD CONSTRAINT fk_feud_script_beat_contender_title
    FOREIGN KEY (contender_title_id) REFERENCES title (title_id);
