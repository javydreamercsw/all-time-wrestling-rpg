# Implementation Plan - Automated Game Guide Screenshots

## Phase 1: Build & Base Infrastructure

- [ ] Task: Create `generate-docs` Maven Profile
    - [ ] Update `pom.xml` to add the `generate-docs` profile.
    - [ ] Configure the profile to set a system property `generate.docs=true` during the `failsafe` execution.
- [ ] Task: Implement Screenshot Logic in `AbstractE2ETest`
    - [ ] Create a unit test to verify that `takeDocScreenshot` respects the `generate.docs` property.
    - [ ] Implement `takeDocScreenshot(String name)` in `AbstractE2ETest`.
    - [ ] Add logic to automatically create the `docs/screenshots` directory if it's missing.
    - [ ] Ensure consistent window sizing (1280x800) when the profile is active.
- [ ] Task: Conductor - User Manual Verification 'Build & Base Infrastructure' (Protocol in workflow.md)

## Phase 2: Feature Instrumentation

- [ ] Task: Instrument Campaign Dashboard Screenshots
    - [ ] Identify points in `CampaignE2ETest` to capture the Dashboard and Backstage views.
    - [ ] Add `takeDocScreenshot("campaign-dashboard")` and other relevant calls.
- [ ] Task: Instrument Tournament & Narrative Screenshots
    - [ ] Add calls in `CampaignTournamentE2ETest` to capture the bracket and "Match Complete" states.
    - [ ] Add calls in `CampaignAdvancedChaptersE2ETest` to capture narrative-specific UI elements.
- [ ] Task: Conductor - User Manual Verification 'Feature Instrumentation' (Protocol in workflow.md)

## Phase 3: Verification & Directory Management

- [ ] Task: Implement Directory Cleanup
    - [ ] Add logic to optionally clear the `docs/screenshots` folder at the start of a "generate-docs" run to prevent ghost assets.
- [ ] Task: Final Build Verification
    - [ ] Run `mvn verify -Pgenerate-docs -Dit.test=Campaign*E2ETest`.
    - [ ] Verify all expected PNG files exist in `docs/screenshots`.
    - [ ] Run `mvn verify` (without profile) and ensure no new files are created.
- [ ] Task: Conductor - User Manual Verification 'Verification & Directory Management' (Protocol in workflow.md)
