# Track Specification: Automated Game Guide Screenshots

## Overview

This track automates the collection of visual assets for the player game guide. By leveraging existing E2E tests and the Selenium infrastructure, we will enable a mechanism to capture specific, high-quality screenshots of game features (e.g., Campaign Dashboard, Match View, Alignment Track) whenever a dedicated build profile is active.

## Functional Requirements

1. **Maven Integration:** Implement a `generate-docs` Maven profile. When active, this profile will signal the E2E test suite to capture and save specific screenshots.
2. **Base Test Extension:** Update `AbstractE2ETest` to include a `takeDocScreenshot(String fileName)` method. This method will:
   * Check if the documentation generation mode is active.
   * Capture the current browser viewport.
   * Save the file to the `docs/screenshots/` directory.
3. **Targeted Captures:** Update existing E2E tests (like `CampaignE2ETest` and `CampaignTournamentE2ETest`) to call `takeDocScreenshot` at critical narrative or UI junctions.
4. **Directory Management:** Ensure the `docs/screenshots/` directory exists and is managed correctly (overwriting old screenshots to keep documentation current).

## Non-Functional Requirements

1. **Performance:** Screenshot capturing must be disabled by default during standard `mvn verify` or CI runs to avoid build bloat.
2. **Consistency:** Screenshots should be taken with a consistent browser resolution (e.g., 1280x800) to ensure uniform look in the guide.

## Acceptance Criteria

- [ ] Running `mvn verify -Pgenerate-docs` successfully generates PNG files in the `docs/screenshots/` folder.
- [ ] Screenshot filenames match the strings passed to the `takeDocScreenshot` method.
- [ ] The standard `mvn verify` build (without the profile) does not generate documentation screenshots.
- [ ] Captured images are clear and correctly represent the UI state at the time of the call.

## Out of Scope

- Automated generation of the Markdown text for the guide (this track focuses on image assets).
- Capture of mobile-specific screenshots (desktop-first focus).

