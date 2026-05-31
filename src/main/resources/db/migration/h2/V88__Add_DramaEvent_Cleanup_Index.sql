-- Index supporting the weekly cleanup query that filters on (is_processed, event_date).
CREATE INDEX IF NOT EXISTS idx_drama_event_processed_date
    ON drama_event (is_processed, event_date);
