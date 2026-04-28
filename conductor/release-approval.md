# Release Approval Workflow

## Objective

To add a manual approval step to the GitHub Actions release workflow. This allows testing the snapshot (pre-release) artifacts and download links on GitHub Pages before officially finalizing the release, publishing the Docker image, and merging to `main`.

## Key Files & Context

- `.github/workflows/release.yml`: The primary release workflow file.

## Implementation Steps

1. **Mark Initial Release as Pre-release:**
   - In the `build_and_release` job of `.github/workflows/release.yml`, change `prerelease: false` to `prerelease: true`. This ensures the initial GitHub release is created as a pre-release, allowing users to download artifacts for testing without it being considered the final stable version.
2. **Add Approval Job:**
   - Insert a new job named `approve_release` after the `deploy_pages` job.
   - Make it depend on (`needs:`) both `build_desktop_installers` and `deploy_pages`.
   - Configure it to use a GitHub Environment named `release-approval`. This triggers the manual approval pause.
3. **Add Finalize Release Job:**
   - Insert a new job named `finalize_release` that depends on (`needs:`) `approve_release`.
   - Add a step to execute `gh release edit v${{ github.event.inputs.version }} --prerelease=false` using the `GITHUB_TOKEN`. This marks the pre-release as a final release.
4. **Update Dependencies for Final Steps:**
   - Modify the `build_and_publish_docker` and `merge_to_main` jobs to depend on (`needs:`) the `finalize_release` job instead of their previous dependencies.

## Verification & Testing

- **Dry Run:** We cannot easily trigger a full release workflow on GitHub Actions without creating tags and merging code. The verification will consist of reviewing the YAML syntax for errors.
- **User Configuration Requirement:** The user **MUST** create a new Environment named `release-approval` in their GitHub repository settings (Settings -> Environments) and configure it to require reviewers before running the workflow.

