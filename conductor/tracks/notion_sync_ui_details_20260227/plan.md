# Implementation Plan: notion_sync_ui_details_20260227

## Phase 1: Core Service Enhancements [checkpoint: 5a4b3c2]

- [x] Task: Update `BaseNotionSyncService.java` to use dynamic `totalSteps` based on entity count.
- [x] Task: Update `BaseNotionSyncService.java` to call `addLogMessage` for individual item processing and results.
- [x] Task: Update `BaseNotionSyncService.java` to properly call `completeOperation` and `failOperation` at the end of the sync.
- [x] Task: Update `NotionApiExecutor.java` to emit log messages via `SyncProgressTracker` when rate limits are hit.
- [x] Task: Conductor - User Manual Verification 'Phase 1: Core Service Enhancements' (Protocol in workflow.md)

## Phase 2: Specific Sync Service Updates [checkpoint: 7b8c9d0]

- [x] Task: Audit `WrestlerSyncService.java`, `TeamSyncService.java`, etc. (inbound services) to ensure they use `addLogMessage` for detailed feedback.
- [x] Task: Standardize the logging format (emojis, timing) across all sync services.
- [x] Task: Ensure all services properly report success/failure to the `SyncProgressTracker`.
- [x] Task: Conductor - User Manual Verification 'Phase 2: Specific Sync Service Updates' (Protocol in workflow.md)

## Phase 3: UI and Verification [checkpoint: 9d8e7f6]

- [x] Task: Verify in `NotionSyncView.java` that the log entries are correctly displayed and auto-scroll to the bottom.
- [x] Task: Perform manual verification of both inbound and outbound syncs for various entities (Wrestlers, Factions, etc.).
- [x] Task: Verify that error conditions (e.g., Notion offline, invalid token) are clearly surfaced in the UI.
## Phase: Review Fixes
- [x] Task: Apply review suggestions [checkpoint: 4a49bb2]

