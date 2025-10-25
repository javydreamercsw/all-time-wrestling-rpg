# E2E Test Case: Full Show Lifecycle

**Test Case ID:** E2E-001

**Test Case Title:** Full Show Lifecycle

**Objective:** To verify the end-to-end functionality of creating, planning, narrating, and finalizing a show.

**Preconditions:**

*   The application is running.
*   The user is logged in.
*   There are existing wrestlers.
*   There is at least one of each of the following: show type, season, and show template.

**Test Steps:**

| Step | Action | Expected Result |
| :--- | :--- | :--- |
| 1 | **Navigate to the Show List** | The user is on the "Show List" view, and a grid of existing shows is displayed. |
| 2 | **Create a New Show** | Click the "Add Show" button. A dialog appears. Fill in the show details (name, type, season, template, date). Click "Save". | The new show appears in the show grid. A success notification is displayed. |
| 3 | **Navigate to Show Planning** | Find the newly created show in the grid and click the "Plan" button (or navigate to the "Show Planning" view and select the show). | The user is on the "Show Planning" view for the selected show. |
| 4 | **Load the Show Context** | Click the "Load Context" button. | The "Show Planning Context" text area is populated with JSON data representing the show's context. |
| 5 | **Propose Segments** | Click the "Propose Segments" button. | The "Proposed Segments" grid is populated with AI-generated segments. |
| 6 | **Approve Segments** | Click the "Approve Segments" button. | The proposed segments are saved to the show. A success notification is displayed. The grid is cleared. |
| 7 | **Navigate to Show Details** | Navigate to the "Show Details" view for the show. | The user is on the "Show Details" view, and the newly created segments are displayed in the segments grid. |
| 8 | **Narrate a Segment** | For one of the segments, click the "Narrate" button. | A narration dialog appears. |
| 9 | **Generate Narration** | In the narration dialog, click the "Generate Narration" button. | The narration text area is populated with AI-generated narration. |
| 10 | **Save Narration** | Click the "Save Narration" button. | The narration is saved for the segment. The dialog closes, and the segments grid in the "Show Details" view is updated to show the new narration. |
| 11 | **Summarize a Segment** | For the same segment, click the "Summarize" button. | The summary for the segment is generated and updated in the segments grid. A success notification is displayed. |
| 12 | **Edit a Segment to Assign a Winner** | For the same segment, click the "Edit" button. | An "Edit Segment" dialog appears. |
| 13 | **Assign a Winner** | In the "Edit Segment" dialog, select a winner from the "Winners" multi-select combo box. Click "Save Changes". | The winner is assigned to the segment. The dialog closes, and the segments grid in the "Show Details" view is updated to show the winner. A success notification is displayed. |
