CREATE TABLE wrestler_title_cooldown (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wrestler_state_id BIGINT NOT NULL,
    title_id BIGINT NOT NULL,
    failed_challenge_date DATE NOT NULL,
    CONSTRAINT fk_wtc_wrestler_state FOREIGN KEY (wrestler_state_id) REFERENCES wrestler_state (id) ON DELETE CASCADE,
    CONSTRAINT fk_wtc_title FOREIGN KEY (title_id) REFERENCES title (title_id) ON DELETE CASCADE,
    CONSTRAINT uq_wtc_state_title UNIQUE (wrestler_state_id, title_id)
);
