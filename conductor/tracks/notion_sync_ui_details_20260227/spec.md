# Specification: notion_sync_ui_details_20260227

- **Overview:**
Enhance the Notion Synchronization UI to provide real-time, detailed feedback during sync operations. Currently, the UI is often stuck on "Initializing Sync" and provides minimal information about the actual work being performed.

- **Problem Statement:**
Users triggering a Notion sync (especially outbound) see a "stuck" UI with little to no indication of progress or errors. Logs show significant activity and intermittent failures that are not being communicated back to the user interface.

- **Functional Requirements:**
1.  **Detailed UI Logging:**
    -   Implement real-time logging in the Sync Dashboard for each entity being processed.
    -   Display individual item success/failure results (e.g., "✅ Synced Wrestler: John Doe", "❌ Failed to sync Team: DX").
    -   Include timing information for each phase (Fetch, Convert, Save).
    -   Log Notion API status updates (e.g., rate limit delays).
2.  **Hybrid Progress Tracking:**
    -   Update the progress bar to reflect both high-level phases and individual item progress.
    -   Fix `BaseNotionSyncService` to correctly report total steps based on entity count instead of a hardcoded value of 1.
3.  **Proper Operation Completion:**
    -   Ensure `completeOperation` and `failOperation` are called correctly in all sync services (especially outbound ones) so the UI can properly transition from "In Progress" to "Completed" or "Failed".
4.  **Error Visibility:**
    -   Ensure specific error messages from the backend logs are surfaced in the UI sync log.

- **Non-Functional Requirements:**
- **Performance:** Ensure frequent UI updates do not cause performance degradation (use batching if necessary).
- **Usability:** Maintain a clean, readable log that doesn't overwhelm the user but provides sufficient detail for troubleshooting.

- **Acceptance Criteria:**
1.  Trigger an outbound Wrestlers sync.
2.  The UI log should show individual wrestlers being saved to Notion.
3.  The progress bar should advance as each wrestler is processed.
4.  The log should show a summary and timing info upon completion.
5.  If an error occurs (like a missing database or rate limit), it should be clearly logged in the UI.
