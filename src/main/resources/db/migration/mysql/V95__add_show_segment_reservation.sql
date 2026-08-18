CREATE TABLE show_segment_reservation (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    show_id BIGINT NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    source_id BIGINT,
    label VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    segment_id BIGINT,
    CONSTRAINT fk_ssr_show FOREIGN KEY (show_id) REFERENCES wrestling_show(show_id),
    CONSTRAINT fk_ssr_segment FOREIGN KEY (segment_id) REFERENCES segment(segment_id),
    CONSTRAINT uq_ssr_segment UNIQUE (segment_id),
    CONSTRAINT uq_ssr_source UNIQUE (show_id, purpose, source_id)
);
