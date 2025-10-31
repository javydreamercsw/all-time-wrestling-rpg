# Task List

- [x] 1. Mark a match as a title match (revised):

    - [x] Database:
        - [ ] I'll create a new join table called segment_title with segment_id and title_id columns. This will represent the many-to-many relationship between segments and titles.
        - [ ] I will need to update the V6__Add_Title_To_Segment.sql migration file to create this table instead of adding a column to the segment table.
    - [x] Backend:
        - [x] I'll update the Segment entity to have a Set<Title> field.
        - [x] I'll update the SegmentService to handle the new titles field when creating and updating segments.
        - [x] I'll update the SegmentDTO to include a List<Integer> of titleIds.
    - [x] Frontend:
        - [x] In the EditSegmentDialog, I'll replace the ComboBox with a MultiSelectComboBox to select multiple titles for the match. This MultiSelectComboBox will be populated with the available titles.
        - [x] The MultiSelectComboBox will only be visible when the segment is a match.
        - [x] Added the titles column to the segment list in `ShowDetailView.java`.
        - [x] Reordered the title selection UI elements to appear before the narration fields.

- [ ] 2. View and update the #1 contender:

    - [x] Backend:
        - [x] I'll create a new method in the TitleService to update the #1 contender for a title. This method will take the titleId and the wrestlerId of the new #1 contender.
        - [x] I'll add a contender field to the Title entity to store the #1 contender.
    - [ ] Frontend:
        - [ ] In the TitleListView, I'll add a ComboBox to each row in the grid to select the #1 contender for that title.
        - [ ] The ComboBox will be populated with the wrestlers that are in the same tier as the title.
        - [ ] When a new #1 contender is selected, I'll call the new method in the TitleService to update the database.