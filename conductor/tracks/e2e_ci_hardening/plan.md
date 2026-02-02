# Implementation Plan - E2E CI Hardening

## 1. Referential Integrity Fixes

Many tests fail during `tearDown()` because they try to delete wrestlers that are referenced in `LEAGUE_ROSTER`.
- **Action:** Create a utility method in `AbstractE2ETest` to clean up all league/roster data before entity deletion.
- **Affected Tests:** `WrestlerProfileViewE2ETest`, `WrestlerListViewE2ETest`, `PlayerViewE2ETest`, `BookerJourneyE2ETest`.

## 2. Account & Unique Constraint Fixes

`AccountFormE2ETest` fails due to `delete_me` username already existing.
- **Action:** Append a unique suffix to usernames created in tests or ensure a clean state at the start of the test.
- **Affected Tests:** `AccountFormE2ETest`.

## 3. Filtering and Race Conditions

`GenderFilteringE2ETest` fails because the grid doesn't update fast enough after tier recalculation.
- **Action:** Add explicit waits for the grid to refresh and use more specific selectors.
- **Affected Tests:** `GenderFilteringE2ETest`.

## 4. UI Selector Stabilization

`LeagueDocsE2ETest` fails finding `#draft-wrestler-btn-70`.
- **Action:** Use dynamic button finding based on text or a more stable attribute if the ID is auto-generated.
- **Affected Tests:** `LeagueDocsE2ETest`.
