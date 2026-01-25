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

## Phase 2: Backend Logic and Theme Support

- [ ] Task: Write Tests for Theme Resolution Logic
  - [ ] Create `ThemeServiceTest` to verify preference hierarchy
- [ ] Task: Implement `ThemeService`
  - [ ] Create `ThemeService` to manage available theme definitions
  - [ ] Implement logic to serve correct CSS classes/attributes based on user selection
- [ ] Task: Integrate Theme Loading into Application Root
  - [ ] Update the main layout or root component to apply the theme class to the document body/root element on initial load
- [ ] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

## Phase 3: UI Implementation

- [ ] Task: Write Tests for Profile Theme Selection
  - [ ] Create a UI test to verify the presence of theme selection in the profile view
- [ ] Task: Update User Profile View
  - [ ] Add a `Select` or `RadioButtonGroup` component for theme selection in `UserProfileView`
  - [ ] Implement the "Save" logic to update user preference and trigger `UI.getCurrent().getPage().reload()`
- [ ] Task: Admin Configuration UI
  - [ ] Update Admin Dashboard to allow setting the global default theme
- [ ] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)

## Phase 4: Verification and Polish

- [ ] Task: E2E Regression Testing
  - [ ] Run full E2E suite to ensure no UI regressions
  - [ ] Verify theme persistence by logging in/out
- [ ] Task: Final Build and Lint
  - [ ] Run `mvn spotless:apply clean install` to ensure code standards and successful compilation
- [ ] Task: Conductor - User Manual Verification 'Phase 4' (Protocol in workflow.md)

