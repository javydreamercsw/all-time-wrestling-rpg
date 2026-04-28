# Snapshot Release Retries and Cleanup

## Objective
To clearly define the "fix and retry" process for the snapshot release workflow, ensuring that if a pre-release is rejected during the manual approval phase, it can be easily cleaned up or updated without cluttering the repository.

## Key Files & Context
- `.github/workflows/release.yml`: The main release workflow that creates the pre-release.
- `.github/workflows/discard-release.yml` (New): A manual workflow to delete a failed/rejected pre-release and its associated tag.

## Implementation Steps
1.  **Create Discard Workflow:**
    -   Create a new GitHub Actions workflow file: `.github/workflows/discard-release.yml`.
    -   Configure it to run manually (`workflow_dispatch`) and accept a `version` input (e.g., `1.8.0`).
    -   Use the GitHub CLI (`gh release delete`) to remove the pre-release and its associated tag from the repository.
2.  **Update Release Workflow for Retries (Optional but Recommended):**
    -   In `.github/workflows/release.yml`, ensure the `softprops/action-gh-release@v2` step is configured to `make_latest: false` for the pre-release to avoid confusing users if a pre-release is published after a stable release. (Note: `softprops/action-gh-release` handles existing tags by appending/updating assets, which makes retrying safe).

## Process to Fix and Retry (For the User)
1.  **Reject:** If the snapshot artifacts or documentation look incorrect, click "Reject" in the `release-approval` environment on GitHub.
2.  **Cleanup (Optional):** Run the new "Discard Release" workflow to delete the broken pre-release and tag from GitHub.
3.  **Fix:** Make the necessary code or documentation changes on your local `release/1.X.X` branch (or `main`), commit, and push.
4.  **Retry:** Trigger the "Create Release" workflow again with the exact same version number. It will generate a fresh pre-release, update the artifacts, and pause again for your approval.

## Verification
-   Review the YAML syntax of the new `discard-release.yml` file.