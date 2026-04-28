# Automated Snapshot Cleanup on Rejection

## Objective
To implement an automated "Watcher" workflow that detects when a "Create Release" run fails or is rejected during the manual approval phase. Upon detection, it automatically cleans up the orphaned pre-release and its associated git tag, enabling a seamless "fix and retry" loop.

## Key Files & Context
- `.github/workflows/release.yml`: The primary release workflow that creates the pre-release and pauses for approval.
- `.github/workflows/cleanup-on-rejection.yml` (New): The automated watcher workflow.

## Implementation Steps
1.  **Create Watcher Workflow:**
    -   Create a new GitHub Actions workflow file: `.github/workflows/cleanup-on-rejection.yml`.
    -   Configure it to trigger using `workflow_run`. It should watch the "Create Release" workflow and trigger on `completed`.
    -   Add a job condition to only run if the watched workflow's conclusion is `failure` (which includes rejections) or `cancelled`.
2.  **Define Cleanup Logic:**
    -   Checkout the repository using the branch that triggered the failed run (`${{ github.event.workflow_run.head_branch }}`).
    -   Extract the intended release version from `pom.xml` on that branch.
    -   Use the GitHub CLI (`gh release view`) to check if a release matching that version (with a 'v' prefix) exists.
    -   **Safety Check:** Use `gh release view <tag> --json isPrerelease` to ensure the release is still marked as a pre-release before deleting it. This prevents accidental deletion of stable releases if a subsequent job fails.
    -   If it is a pre-release, use `gh release delete <tag> --yes` and `git push origin :refs/tags/<tag>` to remove the artifacts.

## Verification & Testing
-   Review the YAML syntax of the new `cleanup-on-rejection.yml` file.
-   Ensure the safety check (verifying `isPrerelease`) is correctly implemented to protect finalized releases.