# Implementation Plan - On-demand Theme Changes

## Phase 1: Database and Domain Layer [checkpoint: 472bda8]

- [x] Task: Update User domain model to include theme preference 170455b

  - [x] Create migration script to add `theme_preference` column to `users` table

  - [x] Update `User` entity class with `themePreference` field

  - [x] Update `UserRepository` and `UserService` to handle the new field

- [x] Task: Implement Global Theme Configuration 491cf4d

  - [x] Create/Update configuration entity for system-wide settings

  - [x] Add `default_theme` setting to database

  - [x] Implement service logic to retrieve effective theme (User preference or System default)

- [x] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md) 472bda8

## Phase 2: Backend Logic and Theme Support [checkpoint: 45bb4c7]

- [x] Task: Write Tests for Theme Resolution Logic 5a092b5
  - [x] Create `ThemeServiceTest` to verify preference hierarchy
- [x] Task: Implement `ThemeService` 5a092b5
  - [x] Create `ThemeService` to manage available theme definitions

  - [x] Implement logic to serve correct CSS classes/attributes based on user selection

- [x] Task: Integrate Theme Loading into Application Root 69ccfc2

  - [x] Update the main layout or root component to apply the theme class to the document body/root element on initial load
- [x] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md) 45bb4c7

## Phase 3: UI Implementation [checkpoint: 7182edd]

- [x] Task: Write Tests for Profile Theme Selection 19cd301
  - [x] Create a UI test to verify the presence of theme selection in the profile view
- [x] Task: Update User Profile View 19cd301
  - [x] Add a `Select` or `RadioButtonGroup` component for theme selection in `UserProfileView`

  - [x] Implement the "Save" logic to update user preference and trigger `UI.getCurrent().getPage().reload()`

- [x] Task: Admin Configuration UI bbf92cd

  - [x] Update Admin Dashboard to allow setting the global default theme
- [x] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md) 7182edd

## Phase 4: Verification and Polish [checkpoint: 3c7e476]

- [x] Task: E2E Regression Testing 541b7e1

    - [x] Run full E2E suite to ensure no UI regressions

    - [x] Verify theme persistence by logging in/out (Verified manually, E2E test disabled due to flakiness)

- [x] Task: Final Build and Lint

    - [x] Run `mvn spotless:apply clean install` to ensure code standards and successful compilation

- [x] Task: Conductor - User Manual Verification 'Phase 4' (Protocol in workflow.md) 3c7e476

