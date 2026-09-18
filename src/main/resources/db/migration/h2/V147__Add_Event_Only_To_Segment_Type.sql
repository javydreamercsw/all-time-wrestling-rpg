-- ATW-0331: Flag segment types that are special event formats (e.g. Abu Dhabi
-- Rumble) so they are excluded from AI show-proposal candidate lists. Manual
-- selection by Booker/Admin remains allowed.
ALTER TABLE segment_type
  ADD COLUMN IF NOT EXISTS event_only BOOLEAN NOT NULL DEFAULT FALSE;
