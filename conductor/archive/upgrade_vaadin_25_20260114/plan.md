# Plan: Upgrade to Vaadin 25

## Phase 1: Dependency Update & Initial Audit

- [x] Task 1.1: Update `vaadin.version` in `pom.xml` and sync `package.json`. [4ce17fb]
- [x] Task 1.2: Perform a clean build `./mvnw clean compile` and capture all deprecation warnings and compilation errors. [4ce17fb]
- [x] Task 1.3: Update `vite.config.ts` and ensure the frontend development server starts correctly. [Done]
- [x] Task: Conductor - User Manual Verification 'Phase 1: Dependency Update & Initial Audit' (Protocol in workflow.md)

## Phase 2: Refactoring & Deprecation Cleanup

- [x] Task 2.1: Resolve compilation errors related to package renames or removed APIs in Vaadin 25. [Done]
- [x] Task 2.2: Systematically replace deprecated UI components and methods with their modern equivalents based on the initial audit. [Done]
- [x] Task 2.3: Update imports across the project to align with new Vaadin 25 structures. [Done]
- [x] Task: Conductor - User Manual Verification 'Phase 2: Refactoring & Deprecation Cleanup' (Protocol in workflow.md)

## Phase 3: Theming & Build Verification

- [x] Task 3.1: Audit `src/main/frontend/themes/default/` and verify that custom CSS and Lumo adjustments render correctly. [Done]
- [x] Task 3.2: Verify the production build process using `mvn package -Pproduction`. [Done]
- [x] Task: Conductor - User Manual Verification 'Phase 3: Theming & Build Verification' (Protocol in workflow.md)

## Phase 4: Regression Testing & Finalization

- [x] Task 4.1: Execute full unit test suite (`mvn test`) and fix any regressions.
- [x] Task 4.2: Execute integration tests (`mvn verify -Pintegration-test`) to ensure backend-frontend communication is stable.
- [x] Task 4.3: Execute headful/headless E2E tests (`mvn verify -Pe2e -Dheadless=true`) to verify critical user journeys.
- [x] Task: Conductor - User Manual Verification 'Phase 4: Regression Testing & Finalization' (Protocol in workflow.md)

