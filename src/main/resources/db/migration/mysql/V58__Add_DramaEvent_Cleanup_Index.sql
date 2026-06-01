-- Index supporting the weekly cleanup query that filters on (is_processed, event_date).
ALTER TABLE drama_event
    ADD INDEX idx_drama_event_processed_date (is_processed, event_date);
