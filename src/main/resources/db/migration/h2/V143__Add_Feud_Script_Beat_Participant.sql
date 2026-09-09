-- External participants on a feud script beat (surprise opponent, run-in extras).
-- The beat's feud wrestlers remain implicit (rivalry pair / active feud members);
-- this table only stores wrestlers from outside the feud, with their role so the
-- planning pass can place them on the correct team (opponent + extras = team 2).
CREATE TABLE feud_script_beat_participant (
    feud_script_beat_participant_id BIGINT NOT NULL AUTO_INCREMENT,
    beat_id                         BIGINT NOT NULL,
    wrestler_id                     BIGINT NOT NULL,
    role                            VARCHAR(16) NOT NULL,
    creation_date                   TIMESTAMP NOT NULL,
    PRIMARY KEY (feud_script_beat_participant_id),
    CONSTRAINT fk_fsbp_beat     FOREIGN KEY (beat_id)     REFERENCES feud_script_beat(feud_script_beat_id) ON DELETE CASCADE,
    CONSTRAINT fk_fsbp_wrestler FOREIGN KEY (wrestler_id) REFERENCES wrestler(wrestler_id)                 ON DELETE CASCADE,
    CONSTRAINT uq_fsbp_beat_wrestler UNIQUE (beat_id, wrestler_id)
);
