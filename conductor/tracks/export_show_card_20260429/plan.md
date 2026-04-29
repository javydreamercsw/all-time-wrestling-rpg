# Implementation Plan: Export Show Card

## Phase 0: Setup

- [x] Task: Create a new git branch for this feature (`feat/export-show-card`). [0e9a54c]

## Phase 1: Extensible Export Service and Formatter Logic [checkpoint: 560a760]

- [x] Task: Define ShowCardFormatter Interface [56fe22c]
- [x] Task: Implement Markdown Formatter [ab6d953]
- [x] Task: Implement Social Media Formatters [ab6d953]
- [x] Task: Implement Export Service with Auto-Discovery [ab6d953]
- [x] Task: Conductor - User Manual Verification 'Phase 1: Extensible Export Service and Formatter Logic' (Protocol in workflow.md) [560a760]

## Phase 2: UI - Export Modal Dialog [checkpoint: 3ff941d]

- [x] Task: Create ExportModal Component [32bae45]
- [x] Task: Integrate Export Service with Modal [32bae45]
- [x] Task: Conductor - User Manual Verification 'Phase 2: UI - Export Modal Dialog' (Protocol in workflow.md) [3ff941d]

## Phase 3: UI - Integration into Show Views

- [ ] Task: Integrate Export into Show List View
  - [ ] Add an "Export Card" action to the context menu/action buttons for each row in the Show List view.
  - [ ] Bind the action to open the `ShowExportDialog` with the selected show.
- [ ] Task: Integrate Export into Show Details View
  - [ ] Add an "Export Card" button to the Show Details view.
  - [ ] Bind the button to open the `ShowExportDialog` with the current show.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: UI - Integration into Show Views' (Protocol in workflow.md)

