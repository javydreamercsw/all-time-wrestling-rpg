# Specification: Upgrade to Vaadin 25

## Overview

This track covers the migration of the All Time Wrestling RPG project from Vaadin 24 to the latest stable version of Vaadin 25. The goal is to modernize the frontend framework, benefit from new features and performance improvements, and ensure the long-term maintainability of the application.

## Functional Requirements

### 1. Dependency Management

- Update `vaadin.version` in `pom.xml` to the latest stable 25.x release.
- Update `package.json` dependencies to align with the new Vaadin version.
- Perform a clean `npm install` to ensure the frontend dependency tree is consistent.

### 2. Code Migration and Refactoring

- Conduct a "Breaking Change Audit" using official Vaadin 25 release notes.
- Identify and replace deprecated classes, methods, and UI components across the codebase.
- Update imports to reflect any package renames or changes in the Vaadin 25 API.

### 3. Theming and Styling

- Verify the compatibility of custom CSS and Lumo theme adjustments.
- Update any theme-related configurations in `frontend/themes/default/` if necessary.

### 4. Build Configuration

- Update `vite.config.ts` and related frontend build tools to meet Vaadin 25 requirements.
- Ensure the production build (`-Pproduction`) functions correctly.

## Non-Functional Requirements

- **Stability:** The application must remain fully functional with zero regression in existing features.
- **Maintainability:** Eliminate as many deprecation warnings as possible to reduce future technical debt.
- **Code Quality:** Maintain existing coverage goals (>90% unit, >80% integration).

## Acceptance Criteria

- [ ] Project compiles successfully using `./mvnw clean compile`.
- [ ] No critical deprecation warnings remain related to the Vaadin framework.
- [ ] All unit tests pass (`mvn test`).
- [ ] All integration tests pass (`mvn verify -Pintegration-test`).
- [ ] All E2E tests pass (`mvn verify -Pe2e -Dheadless=true`).
- [ ] Visual verification of core screens (Login, Ranking, Profile) confirms no UI breakage.

## Out of Scope

- Implementing new UI features or business logic.
- Upgrading other major framework components (like Spring Boot) unless strictly required by Vaadin 25.

