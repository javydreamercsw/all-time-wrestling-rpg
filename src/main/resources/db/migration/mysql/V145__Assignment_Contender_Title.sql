-- A template segment assignment may declare that its segment decides the #1 contender
-- for a title (e.g. the Abu Dhabi Rumble on the All Time Rumble PLE grants its winner a
-- world-title shot). Null = the assignment does not decide a contender.
ALTER TABLE show_template_segment_assignment
  ADD COLUMN contender_title_id BIGINT NULL,
  ADD CONSTRAINT fk_assignment_contender_title
    FOREIGN KEY (contender_title_id) REFERENCES title (title_id);
