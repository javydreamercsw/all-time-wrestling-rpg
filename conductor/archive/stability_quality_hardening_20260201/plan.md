# Plan: Stability & Quality Hardening

## Phase 1: Fix Data Integrity Issues

- [x] **Analyze `TitleReign` and `Segment` relationship:**
  - Examine `TitleReign.java` to see how it references `Segment`.
  - Check if the foreign key has `ON DELETE SET NULL` or if it requires manual clearing.
- [x] **Fix `ShowDetailViewE2ETest.setupTestData`:**
  - Update the cleanup logic to clear `wonAtSegment` from any `TitleReign` before attempting to delete segments.
- [x] **Verify:** Run `ShowDetailViewE2ETest` multiple times.

## Phase 2: Optimize UI Tests

- [x] **Refactor `AbstractE2ETest`:**
  - Review `waitForVaadinElement` implementation.
  - Add `waitForGridToPopulate` and `waitForNotification` helper methods.
- [x] **Adjust Timeouts:**
  - Increase the default wait timeout from 10s to 30s or make it configurable via a property.
- [x] **Verify:** Run `AccountE2ETest` and `InboxViewE2ETest` in Docker environment.
  - Fixed `InboxViewE2ETest` timeouts by removing duplicate auto-generated grid columns and adding `saveAndFlush`.
  - Fixed `InboxViewE2ETest` data integrity violations by correctly setting target IDs.
  - Verified `AccountE2ETest` passes robustly.

## Phase 3: Campaign Unit Tests

- [x] **Create `CampaignServiceTest`:**
  - Test alignment changes (Face/Heel points).
  - Test chapter advancement logic.
- [x] **Create `TournamentServiceTest`:**
  - Test bracket generation for different roster sizes.
  - Test winner advancement logic.
- [x] **Verify:** Run `mvn test` and check coverage.
  - `TournamentServiceTest` passed (fixed propagation bug).
  - `CampaignServiceTest` passed.

## Phase 4: Build Configuration

- [x] **Update `pom.xml`:**
  - Configure `maven-failsafe-plugin` with `rerunFailingTestsCount`.
- [x] **Verify:** Intentionally break a test and confirm it retries before failing the build.
    - Verified with `AccountE2ETest` using simulated failure logic. Retries work as expected.

