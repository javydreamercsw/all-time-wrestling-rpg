-- Full-segment setup on a feud script beat: explicit team layouts and title stakes.
--
-- team_number: 1-based team assignment for ANY participant row (feud member or external).
-- NULL = legacy quick-path (feud wrestlers = team 1, externals = team 2, derived at read
-- time). Rows carrying role FEUD_MEMBER mark the arc's own wrestlers under an explicit
-- per-beat team layout; a custom layout places every feud wrestler exactly once.
ALTER TABLE feud_script_beat_participant ADD COLUMN team_number INT NULL;

-- Title stakes on the beat, mirroring segment.is_title_segment + segment_title:
-- adjudication awards/defends the title to the segment winner once the beat completes.
ALTER TABLE feud_script_beat ADD COLUMN is_title_segment BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE feud_script_beat_title (
    beat_id  BIGINT NOT NULL,
    title_id BIGINT NOT NULL,
    PRIMARY KEY (beat_id, title_id),
    CONSTRAINT fk_fsbt_beat  FOREIGN KEY (beat_id)  REFERENCES feud_script_beat(feud_script_beat_id) ON DELETE CASCADE,
    CONSTRAINT fk_fsbt_title FOREIGN KEY (title_id) REFERENCES title(title_id)                        ON DELETE CASCADE
);
