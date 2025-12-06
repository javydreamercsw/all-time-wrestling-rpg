The integration tests are currently failing primarily due to issues with how your Notion workspace is set up or how test data is managed within Notion. While the network connectivity to `api.notion.com` seems resolved, the Notion API is still returning `404 object_not_found` errors for specific pages.

**Reasoning for `expected: not <null>` (externalId) failures:**

Multiple integration tests (e.g., `WrestlerNotionSyncServiceIT`, `ShowNotionSyncServiceIT`, `SeasonNotionSyncServiceIT`, etc.) are failing because the `externalId` of the synced entities remains `null`. This happens when the application tries to either:
1.  **Create a new page in Notion:** If the creation fails, the `externalId` (which is Notion's page ID) will not be returned and set on the local entity.
2.  **Update an existing page in Notion:** If the `externalId` exists on the local entity but refers to a Notion page that is either deleted, has an incorrect ID, or is not shared with the integration, then the update operation (or subsequent retrieval attempts, like for page content) will fail with a `404 object_not_found` error.

The logs explicitly show messages like:
`ERROR c.g.j.b.util.NotionBlocksRetriever - Failed to retrieve blocks. Status: 404, Body: {"object":"error","status":404,"code":"object_not_found","message":"Could not find block with ID: [UUID]. Make sure the relevant pages and databases are shared with your integration."}`

This indicates that specific Notion *pages* (referred to by their UUIDs) are not found or not accessible by the integration. This is distinct from the databases themselves not being found, as the NotionHandler successfully initialized and loaded all 16 databases.

**Reasoning for `DataIntegrityViolationException` (`wrestler_id=28`) in `SegmentSyncServiceNotionIT`:**

This error indicates a foreign key constraint violation. Specifically, the test is attempting to insert a `SegmentParticipant` record that references a `Wrestler` with a database ID of `28`, but no `Wrestler` with that ID exists in the test database at that moment.

Our debug logs confirmed that the `Wrestler`s created by the test (`Wrestler 1` and `Wrestler 2`) are assigned IDs `1` and `2`, respectively. This means the `wrestler_id=28` is not coming from these test-created `Wrestler`s. It suggests that:
1.  The mocked Notion `SegmentPage` data (or actual Notion data if the service attempts to load it) implicitly references a `Wrestler` that would map to database ID `28`.
2.  The test setup, despite calling `clearAllRepositories()`, might not be completely isolating test runs, or there's a specific `Wrestler` with ID `28` expected by the `SegmentSyncService` that is not being created or is being prematurely deleted in the test environment.

**Updated Action Required (Critical: Notion Data Management):**

It is crucial that your Notion workspace is correctly prepared for these integration tests. This includes not just the databases, but also ensuring the *pages* within those databases are properly managed.

1.  **Review your Notion Workspace and Integration:**
	*   **Verify Database Content:** Ensure that the Notion databases you've set up contain the necessary sample pages (e.g., Wrestler pages, Show pages, Season pages, etc.) that the application expects to sync. These tests often rely on a baseline of data in Notion.
	*   **Share Individual Pages (if necessary):** While you've likely shared the *databases* with your Notion integration, verify that if any specific *pages* are being directly referenced by UUIDs in the tests (or implicitly by the Notion API's behavior), those individual pages are also shared with your integration. The error message "Could not find block with ID: [UUID]" directly points to this.
	*   **Ensure Correct Page Properties/Schema:** Double-check that the Notion pages within your databases have the exact properties (columns) with the correct types (e.g., 'Name' as Title, 'External ID' as Rich Text/ID, 'Show Type' as Relation, etc.) that the application's NotionPage objects are expecting.
	*   **Test Data Consistency:** It is possible that your local `DataInitializer` populates some entities with `externalId`s that the Notion tests then attempt to use, but those `externalId`s do not correspond to actual pages in your Notion workspace. This would lead to 404s.

2.  **For the `wrestler_id=28` issue in `SegmentSyncServiceNotionIT`:**
	*   This is highly indicative that a `Wrestler` with an internal database ID of `28` is expected to exist when `SegmentParticipant`s are being saved. Given our debug logs show `Wrestler`s being created with IDs `1` and `2`, this ID `28` must be coming from somewhere external to the explicit `Wrestler`s created in that test method. It's plausible that a Notion page being processed maps to a `Wrestler` that the `SegmentSyncService` is trying to link by this ID, but the actual `Wrestler` is not present or correctly identified in your test database.

**Please address the Notion workspace setup and ensure that the required sample data (both databases and pages within them) are present and properly shared with your integration.**

Once you are confident that your Notion workspace and its data are correctly configured and accessible by the `NOTION_TOKEN`, please re-run the integration tests. After that, I will investigate specific test code if these issues persist.
