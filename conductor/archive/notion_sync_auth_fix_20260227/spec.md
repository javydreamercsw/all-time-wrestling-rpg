# Specification: notion_sync_auth_fix_20260227

- **Overview:**
  Fix a critical "Authentication object was not found" error during the outbound Notion synchronization process triggered from the Sync Dashboard.

- **Bug Analysis:**
  The error `An Authentication object was not found in the SecurityContext` indicates that Spring Security's context is missing at the point of the sync execution. This often occurs when a UI-triggered action (Vaadin) delegates to a service layer that runs in a background thread or a context where `SecurityContextHolder` is not inherited.

- **Functional Requirements:**

1. **Context Propagation:** Ensure the `SecurityContext` (the authenticated user) is properly propagated to the synchronization service.
2. **Auth Validation:** Verify that the outbound sync process can access the required security credentials/context without failure.
3. **Outbound Sync Execution:** Successfully execute "Sync all entities" for outbound synchronization without encountering the authentication error.
4. **Notion Token Configuration:** Provide a UI setting in "Game Settings" to configure the Notion Integration Token, allowing it to be persisted in the database instead of relying solely on environment variables.

- **Non-Functional Requirements:**
- **Security:** Maintain the integrity of the application's security model; do not bypass authentication checks.
- **Reliability:** Ensure the fix is robust for both immediate UI actions and any background processes it might trigger.
- **Acceptance Criteria:**

1. Navigate to **Configuration > Sync Dashboard**.
2. Select **"outbound"** mode.
3. Click **"Sync all entities"**.
4. The process initiates and completes without the `Authentication object was not found` error.
5. Verification of successful synchronization (via UI logs or entity status updates).

- **Out of Scope:**
- Performance optimization of the Notion API calls.
- Inbound synchronization logic (unless directly affected by the same root cause).
- New feature additions to the Sync Dashboard.

