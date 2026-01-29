# Plan: Stability & Quality Hardening

## Phase 1: Fix Data Integrity Issues

1. **Analyze `TitleReign` and `Segment` relationship:**
   - Examine `TitleReign.java` to see how it references `Segment`.
   - Check if the foreign key has `ON DELETE SET NULL` or if it requires manual clearing.
2. **Fix `ShowDetailViewE2ETest.setupTestData`:**
   - Update the cleanup logic to clear `wonAtSegment` from any `TitleReign` before attempting to delete segments.
3. **Verify:** Run `ShowDetailViewE2ETest` multiple times.

## Phase 2: Optimize UI Tests

1. **Refactor `AbstractE2ETest`:**
   - Review `waitForVaadinElement` implementation.
   - Add `waitForGridToPopulate` and `waitForNotification` helper methods.
2. **Adjust Timeouts:**
   - Increase the default wait timeout from 10s to 30s or make it configurable via a property.
3. **Verify:** Run `AccountE2ETest` and `InboxViewE2ETest` in Docker environment.

## Phase 3: Campaign Unit Tests

1. **Create `CampaignServiceTest`:**
   - Test alignment changes (Face/Heel points).
   - Test chapter advancement logic.
2. **Create `TournamentServiceTest`:**
   - Test bracket generation for different roster sizes.
   - Test winner advancement logic.
3. **Verify:** Run `mvn test` and check coverage.

## Phase 4: Build Configuration

1. **Update `pom.xml`:**
   - Configure `maven-failsafe-plugin` with `rerunFailingTestsCount`.
2. **Verify:** Intentionally break a test and confirm it retries before failing the build.

