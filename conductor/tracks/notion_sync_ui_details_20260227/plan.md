# Implementation Plan: notion_sync_ui_details_20260227

## Phase 1: Core Service Enhancements
- [ ] Task: Update `BaseNotionSyncService.java` to use dynamic `totalSteps` based on entity count.
- [ ] Task: Update `BaseNotionSyncService.java` to call `addLogMessage` for individual item processing and results.
- [ ] Task: Update `BaseNotionSyncService.java` to properly call `completeOperation` and `failOperation` at the end of the sync.
- [ ] Task: Update `NotionApiExecutor.java` to emit log messages via `SyncProgressTracker` when rate limits are hit.
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Core Service Enhancements' (Protocol in workflow.md)

## Phase 2: Specific Sync Service Updates
- [ ] Task: Audit `WrestlerSyncService.java`, `TeamSyncService.java`, etc. (inbound services) to ensure they use `addLogMessage` for detailed feedback.
- [ ] Task: Standardize the logging format (emojis, timing) across all sync services.
- [ ] Task: Ensure all services properly report success/failure to the `SyncProgressTracker`.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Specific Sync Service Updates' (Protocol in workflow.md)

## Phase 3: UI and Verification
- [ ] Task: Verify in `NotionSyncView.java` that the log entries are correctly displayed and auto-scroll to the bottom.
- [ ] Task: Perform manual verification of both inbound and outbound syncs for various entities (Wrestlers, Factions, etc.).
- [ ] Task: Verify that error conditions (e.g., Notion offline, invalid token) are clearly surfaced in the UI.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: UI and Verification' (Protocol in workflow.md)
