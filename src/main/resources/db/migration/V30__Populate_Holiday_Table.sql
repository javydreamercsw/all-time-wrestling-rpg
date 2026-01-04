INSERT INTO holiday (description, theme, day_of_month, holiday_month, type, creation_date) VALUES
('New Year''s Day', 'New Year''s Day', 1, 'JANUARY', 'FIXED', NOW()),
('Valentine''s Day', 'Valentine''s Day', 14, 'FEBRUARY', 'FIXED', NOW()),
('St. Patrick''s Day', 'St. Patrick''s Day', 17, 'MARCH', 'FIXED', NOW()),
('Independence Day', 'Independence Day', 4, 'JULY', 'FIXED', NOW()),
('Halloween', 'Halloween', 31, 'OCTOBER', 'FIXED', NOW()),
('Veterans Day', 'Veterans Day', 11, 'NOVEMBER', 'FIXED', NOW()),
('Christmas Day', 'Christmas Day', 25, 'DECEMBER', 'FIXED', NOW());

INSERT INTO holiday (description, theme, day_of_week, week_of_month, holiday_month, type, creation_date) VALUES
('Martin Luther King Jr. Day', 'Martin Luther King Jr. Day', 'MONDAY', 3, 'JANUARY', 'FLOATING', NOW()),
('Presidents'' Day', 'Presidents'' Day', 'MONDAY', 3, 'FEBRUARY', 'FLOATING', NOW()),
('Memorial Day', 'Memorial Day', 'MONDAY', -1, 'MAY', 'FLOATING', NOW()),
('Labor Day', 'Labor Day', 'MONDAY', 1, 'SEPTEMBER', 'FLOATING', NOW()),
('Thanksgiving', 'Thanksgiving', 'THURSDAY', 4, 'NOVEMBER', 'FLOATING', NOW());
