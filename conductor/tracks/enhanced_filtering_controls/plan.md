# Implementation Plan: Enhanced Filtering Controls

## Phase 1: Data Model & Persistence

- [x] Update `ShowTemplate` entity to include `genderConstraint` (Enum: MALE, FEMALE, BOTH).
- [x] Create a database migration to add the new column to `show_template` table.
- [x] Update `ShowTemplateRepository` and `ShowTemplateService` to handle the new field.

## Phase 2: Show Template UI

- [x] Update the `ShowTemplateView` (or relevant admin view) to allow editing the `genderConstraint`.
- [x] Ensure the field has a default value of `BOTH`.

## Phase 3: Segment Booking UI

- [x] Identify the component responsible for wrestler selection in segments (e.g., `SegmentParticipantDialog` or similar).
- [x] Add `ComboBox` or `CheckboxGroup` for Alignment filtering.
- [x] Add `ComboBox` or `CheckboxGroup` for Gender filtering.
- [x] Implement the filtering logic in the data provider used by the wrestler selection component.

## Phase 4: Integration

- [x] Implement logic to pull the default gender filter from the active show's template when opening a segment.
- [x] Ensure manual overrides are possible within the segment UI.

## Phase 5: Verification

- [x] Unit tests for `ShowTemplate` persistence.
- [x] Integration tests for filtering logic.
- [x] E2E tests for the new UI controls and reactive filtering.

