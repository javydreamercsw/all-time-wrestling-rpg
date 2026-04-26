# Agent Behavioral Mandates

This file defines foundational mandates for AI agents working in this repository. These instructions take absolute precedence over general workflows and tool defaults.

## Testing & Feedback Loop Workflow

To ensure a fast feedback loop and efficient context usage, always follow this hierarchical testing strategy when fixing bugs or implementing features:

1.  **Surgical Fix & Targeted Verification:**
	-   When a failure is identified, apply the fix.
	-   **Immediately** run ONLY the specific test class or method that failed.
	-   **Goal:** Confirm the fix works for the reported issue without waiting for the entire suite.
	-   **Command Example:** `./mvnw test -Dtest=ClassName#methodName`

2.  **Progressive Validation:**
	-   **ONLY** after the targeted test passes, proceed to wider validation in this order:
	-   **Level 1: Basic Unit Tests:** Run all unit tests to ensure no core regressions.
		-   Command: `./mvnw test`
	-   **Level 2: Integration Tests:** Run the integration suite.
		-   Command: `./mvnw verify -Pintegration-test`
	-   **Level 3: End-to-End (E2E) Tests:** Run the full E2E suite.
		-   Command: `./mvnw verify -Pe2e`

3.  **Concurrency Safety:**
	-   Do not start Level 2 or Level 3 tests until all previous levels have passed.
	-   If a failure occurs at any level, restart the loop from Step 1 for that new failure.

## Database & Environment Management

-   **Clean Slate:** Integration tests rely on `AbstractIntegrationTest.baseSetUp()` for database cleanup and initialization.
-   **No Redundant Cleanup:** Do not add manual `repository.deleteAll()` or `universeRepository.save()` calls in individual test `setUp()` methods if they extend `AbstractIntegrationTest`.
-   **Shared Resources:** Use the `protected Universe defaultUniverse` provided by the parent class.
-   **L1 Cache Management:** Always ensure `entityManager.clear()` is called after database resets to avoid stale data issues in transactional tests.

## Security Context

-   **Precedence:** Method-level security (`@PreAuthorize`) is active.
-   **Mocking:** Use `@WithCustomMockUser` or manual `loginAs("admin")` in `setUp()` to ensure a valid security context.
-   **Context Propagation:** Background threads must use `MODE_INHERITABLETHREADLOCAL` (already configured in `AbstractIntegrationTest` and `Application`).

## File Edits & Code Integrity

-   **Surgical Precision:** When using `replace` or `write_file`, verify that you are NOT removing unrelated code, such as other tests in a test class, necessary imports, or documentation.
-   **Verification:** After a major refactor or replacement, verify the file content (using `read_file` if needed) to ensure that the surrounding logic remains intact and that no unintended deletions occurred.
-   **Context Awareness:** Always check for similar method names or patterns to avoid replacing the wrong occurrence when `allow_multiple` is false.
