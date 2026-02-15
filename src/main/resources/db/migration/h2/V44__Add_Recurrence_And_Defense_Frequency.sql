ALTER TABLE show_template ADD COLUMN duration_days INT DEFAULT 1;
ALTER TABLE show_template ADD COLUMN recurrence_type VARCHAR(20) DEFAULT 'NONE';
ALTER TABLE show_template ADD COLUMN recurrence_day_of_week VARCHAR(20);
ALTER TABLE show_template ADD COLUMN recurrence_day_of_month INT;
ALTER TABLE show_template ADD COLUMN recurrence_week_of_month INT;
ALTER TABLE show_template ADD COLUMN recurrence_month VARCHAR(20);

ALTER TABLE title ADD COLUMN defense_frequency INT;

ALTER TABLE rivalry ADD COLUMN priority INT DEFAULT 0;