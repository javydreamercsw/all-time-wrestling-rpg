# Specification: Stability & Quality Hardening

## Goal

The goal of this track is to achieve a 100% pass rate for E2E tests in all environments (local, Docker, and eventually CI) and to ensure that new features like the Campaign system are covered by robust unit tests.

## Requirements

### 1. Resolve Data Integrity Violations

- **Problem:** `ShowDetailViewE2ETest` frequently fails with `Referential integrity constraint violation: "FK_TITLE_REIGN_WON_AT_SEGMENT"`.
- **Requirement:** Ensure that when segments are deleted or reset during tests, all dependent entities (like `TitleReign` records that reference them) are either cleared or handled correctly to avoid blocking deletion.

### 2. Robust UI Testing

- **Problem:** `AccountE2ETest` and `InboxViewE2ETest` experience timeouts, especially when running in resource-constrained environments like Docker.
- **Requirement:** Refactor the wait logic in `AbstractE2ETest` to handle slow-loading elements more gracefully. Use specific Vaadin component state checks (e.g., waiting for Grid to have data) instead of generic timeouts where possible.

### 3. Campaign Logic Verification

- **Problem:** The `CampaignService` and related services have grown complex but primarily rely on E2E tests for verification.
- **Requirement:** Implement a suite of unit tests for `CampaignService`, `TournamentService`, and `CampaignUpgradeService`. These tests should mock the database and focus on logic branching, alignment calculations, and tournament bracket generation.

### 4. CI/CD Resilience

- **Problem:** Transient issues (network, container startup speed) can cause occasional test failures that aren't indicative of code bugs.
- **Requirement:** Configure the test runner to retry failed integration and E2E tests. A single retry should be sufficient to filter out most transient flakes.

## Success Criteria

- `mvn verify -Pe2e -Dheadless=true` passes consistently 5 times in a row.
- `CampaignService` unit test coverage reaches > 85%.
- Test retry configuration is documented and active.

