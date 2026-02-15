# Implementation Plan: Inbox Visibility Fix

## Step 1: Reinforce Reproduction Tests

- [x] Create `InboxVisibilityIT.java` to reproduce the issue.

- [x] Refactor `InboxVisibilityIT.java` to correctly clean up data between tests and avoid `DataIntegrityViolationException`.

- [x] Add tests for Admin/Booker visibility.

## Step 2: Fix InboxService logic

- [x] Modify `InboxService.search` to enforce visibility:
  - If user is not Admin/Booker:
    - If `targets` and `accountId` are null/empty, force filter by current user's targets.

    - If filters ARE provided, ensure they are restricted to the user's own targets.

    - Use `cb.disjunction()` for unauthorized access attempts.

## Step 3: Verification

- [x] Run `InboxVisibilityIT` and ensure all tests pass.

- [x] Run `InboxServiceIT` to ensure no regressions.

- [ ] Verify manually if possible (via UI if needed, but tests are priority).

## Step 4: Finalize

- Update documentation if necessary.
- Merge/Commit changes.

