CREATE TABLE feud_script (
    feud_script_id   BIGINT NOT NULL AUTO_INCREMENT,
    name             VARCHAR(255) NOT NULL,
    rivalry_id       BIGINT,
    feud_id          BIGINT,
    status           VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    max_ple_appearances INT NOT NULL DEFAULT 3,
    created_date     TIMESTAMP NOT NULL,
    PRIMARY KEY (feud_script_id),
    CONSTRAINT fk_fs_rivalry  FOREIGN KEY (rivalry_id)  REFERENCES rivalry(rivalry_id)  ON DELETE SET NULL,
    CONSTRAINT fk_fs_feud     FOREIGN KEY (feud_id)     REFERENCES multi_wrestler_feud(multi_wrestler_feud_id) ON DELETE SET NULL
);

CREATE TABLE feud_script_beat (
    feud_script_beat_id BIGINT NOT NULL AUTO_INCREMENT,
    script_id           BIGINT NOT NULL,
    beat_order          INT NOT NULL,
    segment_type        VARCHAR(128) NOT NULL,
    segment_rule        VARCHAR(128),
    target_show_id      BIGINT,
    reservation_id      BIGINT,
    winner_control      VARCHAR(16) NOT NULL DEFAULT 'AI_PICKS',
    planned_winner_id   BIGINT,
    is_culmination      BOOLEAN NOT NULL DEFAULT FALSE,
    notes               VARCHAR(255),
    actual_segment_id   BIGINT,
    beat_status         VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    PRIMARY KEY (feud_script_beat_id),
    CONSTRAINT fk_fsb_script      FOREIGN KEY (script_id)         REFERENCES feud_script(feud_script_id) ON DELETE CASCADE,
    CONSTRAINT fk_fsb_show        FOREIGN KEY (target_show_id)    REFERENCES wrestling_show(show_id)     ON DELETE SET NULL,
    CONSTRAINT fk_fsb_reservation FOREIGN KEY (reservation_id)    REFERENCES show_segment_reservation(id) ON DELETE SET NULL,
    CONSTRAINT fk_fsb_winner      FOREIGN KEY (planned_winner_id) REFERENCES wrestler(wrestler_id)        ON DELETE SET NULL,
    CONSTRAINT fk_fsb_segment     FOREIGN KEY (actual_segment_id) REFERENCES segment(segment_id)          ON DELETE SET NULL
);
