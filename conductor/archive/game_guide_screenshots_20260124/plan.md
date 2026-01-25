# Implementation Plan - Automated Game Guide Screenshots

## Phase 1: Build & Base Infrastructure

- [x] Task: Create `generate-docs` Maven Profile
  - [x] Update `pom.xml` to add the `generate-docs` profile.
  - [x] Configure the profile to set a system property `generate.docs=true` during the `failsafe` execution.
- [x] Task: Implement Screenshot Logic in `AbstractE2ETest`
  - [x] Create a unit test to verify that `takeDocScreenshot` respects the `generate.docs` property.
  - [x] Implement `takeDocScreenshot(String name)` in `AbstractE2ETest`.
  - [x] Add logic to automatically create the `docs/screenshots` directory if it's missing.
  - [x] Ensure consistent window sizing (1280x800) when the profile is active.
- [x] Task: Conductor - User Manual Verification 'Build & Base Infrastructure' (Protocol in workflow.md)

## Phase 2: Feature Instrumentation

- [x] Task: Instrument Campaign Dashboard Screenshots
  - [x] Identify points in `CampaignE2ETest` to capture the Dashboard and Backstage views.
  - [x] Add `takeDocScreenshot("campaign-dashboard")` and other relevant calls.
- [x] Task: Instrument Tournament & Narrative Screenshots
  - [x] Add calls in `CampaignTournamentE2ETest` to capture the bracket and "Match Complete" states.
  - [x] Add calls in `CampaignAdvancedChaptersE2ETest` to capture narrative-specific UI elements.
  - [x] **Deviation:** Created `CampaignDocsE2ETest` to centralize documentation screenshots and handle specific setup requirements (like different campaign chapters).
- [x] Task: Conductor - User Manual Verification 'Feature Instrumentation' (Protocol in workflow.md)

## Phase 3: Verification & Directory Management

- [x] Task: Implement Directory Cleanup
  - [x] Add logic to optionally clear the `docs/screenshots` folder at the start of a "generate-docs" run to prevent ghost assets.
- [x] Task: Final Build Verification
  - [x] Run `mvn verify -Pgenerate-docs -Dit.test=Campaign*E2ETest`.
  - [x] Verify all expected PNG files exist in `docs/screenshots`.
  - [x] Run `mvn verify` (without profile) and ensure no new files are created.
- [x] Task: Conductor - User Manual Verification 'Verification & Directory Management' (Protocol in workflow.md)