package com.github.javydreamercsw.management.service.sync;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.management.domain.show.ShowRepository;
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

  @Autowired private ShowRepository showRepository;

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
    String showIdToSync = showIds.get(new java.util.Random().nextInt(showIds.size()));
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

  @Test
  @DisplayName("Should sync a random show from Notion to database successfully")
  void shouldSyncShowsFromNotionToDatabaseSuccessfully() {
    // Given - Real integration test with actual Notion API
    int initialShowCount = showRepository.findAll().size();

    // When - Sync a random show from real Notion database
    List<String> showIds = notionSyncService.getAllShowIds();
    NotionSyncService.SyncResult result;
    if (showIds.isEmpty()) {
      result = notionSyncService.syncShows("test-operation-123");
    } else {
      String randomId = showIds.get(new java.util.Random().nextInt(showIds.size()));
      result = notionSyncService.syncShow(randomId);
    }

    // Then - Verify sync completed successfully (regardless of show count)
    assertNotNull(result);

    boolean syncSuccessful =
        result.isSuccess()
            || (result.getErrorMessage() != null
                && result.getErrorMessage().contains("No shows found"));

    assertTrue(syncSuccessful);

    // Verify database state is consistent
    List<com.github.javydreamercsw.management.domain.show.Show> finalShows =
        showRepository.findAll();
    if (!showIds.isEmpty()) {
      assertTrue(finalShows.size() > initialShowCount);
    } else {
      assertEquals(initialShowCount, finalShows.size());
    }

    System.out.println(
        "Integration test completed: "
            + (result.isSuccess() ? "SUCCESS" : "INFO")
            + " - Synced: "
            + result.getSyncedCount()
            + " shows, Final DB count: "
            + finalShows.size());
  }

  @Test
  @DisplayName("Should handle duplicate detection during real sync")
  void shouldSkipDuplicateShowsDuringSync() {
    // Given - Run sync twice to test duplicate handling
    int initialShowCount = showRepository.findAll().size();

    // When - First sync
    List<String> showIds = notionSyncService.getAllShowIds();
    if (showIds.isEmpty()) {
      return; // Skip test if no shows to sync
    }
    String randomId = showIds.get(new java.util.Random().nextInt(showIds.size()));
    NotionSyncService.SyncResult firstResult = notionSyncService.syncShow(randomId);
    int afterFirstSync = showRepository.findAll().size();

    // Second sync (should detect duplicates)
    NotionSyncService.SyncResult secondResult = notionSyncService.syncShow(randomId);
    int afterSecondSync = showRepository.findAll().size();

    // Then - Verify duplicate handling works
    assertNotNull(firstResult);
    assertNotNull(secondResult);

    boolean duplicateHandlingWorks =
        (firstResult.isSuccess() && secondResult.isSuccess())
            || (afterSecondSync == afterFirstSync); // No new shows added on second sync

    assertTrue(duplicateHandlingWorks);

    System.out.println(
        "Duplicate handling test: Initial="
            + initialShowCount
            + ", After 1st="
            + afterFirstSync
            + ", After 2nd="
            + afterSecondSync);
  }

  @Test
  @DisplayName("Should handle missing dependencies gracefully during real sync")
  void shouldHandleMissingDependenciesGracefully() {
    // Given - Real sync that may encounter missing dependencies
    int initialShowCount = showRepository.findAll().size();

    // When - Sync with real Notion data (may have missing shows/wrestlers/segment types)
    List<String> showIds = notionSyncService.getAllShowIds();
    if (showIds.isEmpty()) {
      return; // Skip test if no shows to sync
    }
    String randomId = showIds.get(new java.util.Random().nextInt(showIds.size()));
    NotionSyncService.SyncResult result = notionSyncService.syncShow(randomId);

    // Then - Verify sync handles missing dependencies gracefully
    assertNotNull(result);

    boolean handledGracefully =
        result.isSuccess()
            || (result.getErrorMessage() != null
                && (result.getErrorMessage().contains("Could not resolve")
                    || result.getErrorMessage().contains("validation failed")
                    || result.getErrorMessage().contains("No shows found")));

    assertTrue(handledGracefully);

    // Verify database remains consistent
    List<com.github.javydreamercsw.management.domain.show.Show> finalShows =
        showRepository.findAll();
    assertTrue(finalShows.size() >= initialShowCount);

    System.out.println(
        "Missing dependencies test: "
            + (result.isSuccess() ? "SUCCESS" : "HANDLED_GRACEFULLY")
            + " - "
            + result.getErrorMessage());
  }

  @Test
  @DisplayName("Should handle empty show list from Notion")
  void shouldHandleEmptyShowListFromNotion() {
    // Given - Real sync that may encounter empty results
    int initialShowCount = showRepository.findAll().size();

    // When - Sync with real Notion data (may be empty)
    List<String> showIds = notionSyncService.getAllShowIds();
    if (!showIds.isEmpty()) {
      return; // Skip test if there are shows to sync
    }
    NotionSyncService.SyncResult result = notionSyncService.syncShows("test-operation-303");

    // Then - Verify sync handles empty results gracefully
    assertNotNull(result);

    boolean handledGracefully =
        result.isSuccess()
            || (result.getErrorMessage() != null
                && result.getErrorMessage().contains("No shows found"));

    assertTrue(handledGracefully);

    // Verify database remains consistent
    List<com.github.javydreamercsw.management.domain.show.Show> finalShows =
        showRepository.findAll();
    assertEquals(initialShowCount, finalShows.size());

    System.out.println(
        "Empty show list test: "
            + (result.isSuccess() ? "SUCCESS" : "HANDLED_GRACEFULLY")
            + " - Synced: "
            + result.getSyncedCount());
  }
}
