package com.github.javydreamercsw.management.service.sync;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Real integration test for show sync that uses actual Spring services and real Notion API calls.
 * This test requires the NOTION_TOKEN environment variable to be set.
 *
 * <p>Run with: mvn test -Dtest=ShowSyncRealIntegrationTest -DNOTION_TOKEN=your_token
 */
@Slf4j
@DisplayName("Show Sync Integration Tests")
class ShowSyncIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private NotionSyncService notionSyncService; // When - Perform real sync with real services

  @Test
  @DisplayName("Should validate sync result structure")
  void shouldValidateSyncResultStructure() {
    log.info("🔍 Testing sync result structure validation...");

    // When - Perform sync (with or without token)
    List<String> showIds = notionSyncService.getAllShowIds();
    if (showIds.isEmpty()) {
      log.warn("⏭️ Skipping real integration test - no shows found in Notion");
      return;
    }
    String showIdToSync = showIds.get(0);
    NotionSyncService.SyncResult result = notionSyncService.syncShow(showIdToSync);

    // Then - Verify result structure is always valid
    assertNotNull(result, "Sync result should never be null");
    assertNotNull(result.getEntityType(), "Entity type should never be null");
    assertEquals("Show", result.getEntityType(), "Entity type should be 'Show'");

    // Verify numeric fields are valid
    assertTrue(result.getSyncedCount() >= 0, "Synced count should be non-negative");
    assertTrue(result.getErrorCount() >= 0, "Error count should be non-negative");

    // If failed, should have error message
    if (!result.isSuccess()) {
      assertNotNull(result.getErrorMessage(), "Failed sync should have error message");
      assertFalse(result.getErrorMessage().trim().isEmpty(), "Error message should not be empty");
    }

    log.info("✅ Sync result structure is valid");
  }
}
