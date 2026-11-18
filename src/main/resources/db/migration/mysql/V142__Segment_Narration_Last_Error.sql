-- Adjudication UX (ATW-v0vh): persistent per-segment narration failure mark.
-- NULL = last narration attempt succeeded (or none attempted). Lets the show view
-- mark failed segments and offer individual retry after the progress overlay closes.
ALTER TABLE segment ADD COLUMN narration_last_error VARCHAR(512);
