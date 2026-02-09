# Implementation Plan - LocalAI Hot Enabling

## Phase 1: Backend Service Enhancements
- [ ] Task: Enhance `LocalAIService` with Health Check Logic
- [ ] Task: Implement Runtime Reconfiguration in `SegmentNarrationServiceFactory`
- [ ] Task: Implement Model Switching Logic
- [ ] Task: Create `LocalAIProcessManager` for Lifecycle (Docker/Process)
- [ ] Task: Ensure `SegmentSummaryService` Prioritizes LocalAI
    - [ ] Verify `SegmentNarrationServiceFactory` priority logic or add specific override.
- [ ] Task: Conductor - User Manual Verification 'Backend Service Enhancements' (Protocol in workflow.md)

## Phase 2: UI Integration
- [ ] Task: Update `AiSettingsView` with Toggle, Model Select, and Start/Stop buttons.
- [ ] Task: Implement Status Polling for LocalAI process status.
- [ ] Task: Conductor - User Manual Verification 'UI Integration' (Protocol in workflow.md)

## Phase 3: Testing & Persistence
- [ ] Task: Integration Tests for Hot-Enabling and Summary Handover.
- [ ] Task: Conductor - User Manual Verification 'Testing & Persistence' (Protocol in workflow.md)
