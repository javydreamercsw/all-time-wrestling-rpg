# Implementation Plan: Export Show Card

## Phase 0: Setup
- [x] Task: Create a new git branch for this feature (`feat/export-show-card`). [0e9a54c]

## Phase 1: Extensible Export Service and Formatter Logic
- [ ] Task: Define ShowCardFormatter Interface
    - [ ] Create an interface `ShowCardFormatter` with methods like `getFormatName()`, `format(Show show)`, and potentially an `order()` or `priority()` for sorting in the UI.
- [ ] Task: Implement Markdown Formatter
    - [ ] Write unit tests for `MarkdownShowCardFormatter`.
    - [ ] Implement the logic and annotate with `@Component` for Spring discovery.
- [ ] Task: Implement Social Media Formatters
    - [ ] Write unit tests for Facebook, X, and Bluesky formatter implementations.
    - [ ] Implement the respective logic and annotate with `@Component`.
- [ ] Task: Implement Export Service with Auto-Discovery
    - [ ] Write unit tests for `ShowExportService` that takes a Show and a format name, delegating to the correct formatter.
    - [ ] Implement `ShowExportService`, injecting a `List<ShowCardFormatter>` so Spring automatically discovers all formatter implementations.
    - [ ] Add a method to `ShowExportService` to return all available format names for the UI dropdown.
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Extensible Export Service and Formatter Logic' (Protocol in workflow.md)

## Phase 2: UI - Export Modal Dialog
- [ ] Task: Create ExportModal Component
    - [ ] Create a Vaadin Dialog component `ShowExportDialog`.
    - [ ] Retrieve available formats from `ShowExportService` and populate a format selector (ComboBox).
    - [ ] Add a read-only text area for the preview.
    - [ ] Add a "Copy to Clipboard" button using Vaadin's clipboard capabilities.
- [ ] Task: Integrate Export Service with Modal
    - [ ] Wire the format selector to call the `ShowExportService` and update the text area with the generated text based on the selected show and format.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: UI - Export Modal Dialog' (Protocol in workflow.md)

## Phase 3: UI - Integration into Show Views
- [ ] Task: Integrate Export into Show List View
    - [ ] Add an "Export Card" action to the context menu/action buttons for each row in the Show List view.
    - [ ] Bind the action to open the `ShowExportDialog` with the selected show.
- [ ] Task: Integrate Export into Show Details View
    - [ ] Add an "Export Card" button to the Show Details view.
    - [ ] Bind the button to open the `ShowExportDialog` with the current show.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: UI - Integration into Show Views' (Protocol in workflow.md)