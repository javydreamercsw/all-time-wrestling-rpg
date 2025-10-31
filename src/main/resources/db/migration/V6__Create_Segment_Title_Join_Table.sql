CREATE TABLE segment_title (
    segment_id BIGINT NOT NULL,
    title_id BIGINT NOT NULL,
    PRIMARY KEY (segment_id, title_id),
    FOREIGN KEY (segment_id) REFERENCES segment(segment_id),
    FOREIGN KEY (title_id) REFERENCES title(title_id)
);