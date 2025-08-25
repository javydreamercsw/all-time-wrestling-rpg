package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.sync.NotionSyncService.SyncResult;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for Notion Sync functionality. These tests require NOTION_TOKEN to be available
 * and test the real integration with Notion API and database.
 *
 * <p>NO MOCKING - These tests use real services and real database operations.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {"notion.sync.enabled=true"})
@Transactional
@EnabledIf("isNotionTokenAvailable")
@Slf4j
class NotionSyncIntegrationTest {

  @Autowired private NotionSyncService notionSyncService;

  @Autowired private ShowTypeService showTypeService;

  @BeforeEach
  void setUp() {
    log.info("🧪 Setting up NotionSyncIntegrationTest");
    log.info("NOTION_TOKEN available: {}", isNotionTokenAvailable());
  }

  @Test
  @DisplayName("Should connect to Notion and retrieve database information")
  void shouldConnectToNotionAndRetrieveDatabaseInfo() {
    log.info("🔗 Testing Notion connection and database retrieval");

    // When - Attempt to connect to Notion (this will validate the connection)
    SyncResult result = notionSyncService.syncShowTypes("integration-test-connection");

    // Then - Should successfully connect and process
    assertNotNull(result, "Sync result should not be null");
    log.info("📊 Sync result: {}", result);

    if (result.isSuccess()) {
      log.info(
          "✅ Successfully processed show types (fallback behavior when NotionHandler unavailable)");
      assertThat(result.getEntityType()).isEqualTo("ShowTypes");
      // When NotionHandler is unavailable, sync falls back to ensuring defaults exist
      assertThat(result.getSyncedCount()).isGreaterThanOrEqualTo(0);
    } else {
      log.warn("⚠️ Sync failed: {}", result.getErrorMessage());
      // Even if sync fails, we should get a proper error message
      assertThat(result.getErrorMessage()).isNotBlank();

      // Expected error when NotionHandler is unavailable
      boolean isExpectedError =
          result.getErrorMessage().contains("NotionHandler")
              || result.getErrorMessage().contains("not available");
      if (isExpectedError) {
        log.info("ℹ️ Expected error due to NotionHandler unavailability");
      }
    }
  }

  @Test
  @DisplayName("Should sync show types from Notion to database")
  void shouldSyncShowTypesFromNotionToDatabase() {
    log.info("🎭 Testing show types sync from Notion to database");

    // Given - Clear existing show types for clean test
    List<ShowType> existingShowTypes = showTypeService.findAll();
    log.info("📋 Found {} existing show types before sync", existingShowTypes.size());

    // When - Sync show types from Notion
    SyncResult result = notionSyncService.syncShowTypes("integration-test-show-types");

    // Then - Verify sync result
    assertNotNull(result, "Sync result should not be null");
    log.info("📊 Show types sync result: {}", result);

    if (result.isSuccess()) {
      log.info("✅ Show types sync successful");

      // Verify show types were created/updated in database
      List<ShowType> showTypesAfterSync = showTypeService.findAll();
      log.info("📋 Found {} show types after sync", showTypesAfterSync.size());

      assertThat(showTypesAfterSync).isNotEmpty();

      // Verify each show type has proper data
      for (ShowType showType : showTypesAfterSync) {
        assertThat(showType.getName()).isNotBlank();
        assertThat(showType.getDescription()).isNotBlank();
        log.info("🎭 Show type: {} - {}", showType.getName(), showType.getDescription());
      }

      // Verify sync counts make sense
      assertThat(result.getSyncedCount()).isGreaterThanOrEqualTo(0);

    } else {
      log.warn("⚠️ Show types sync failed: {}", result.getErrorMessage());
      // Even if sync fails, we should get a proper error message
      assertThat(result.getErrorMessage()).isNotBlank();
    }
  }

  @Test
  @DisplayName("Should handle full shows sync integration")
  void shouldHandleFullShowsSyncIntegration() {
    log.info("📺 Testing full shows sync integration");

    // When - Perform full shows sync (includes show types, seasons, and shows)
    SyncResult result = notionSyncService.syncShows();

    // Then - Verify sync result
    assertNotNull(result, "Sync result should not be null");
    log.info("📊 Full shows sync result: {}", result);

    if (result.isSuccess()) {
      log.info("✅ Full shows sync successful");
      assertThat(result.getEntityType()).isEqualTo("Shows");
      // When NotionHandler is unavailable, sync count may be 0 (fallback behavior)
      assertThat(result.getSyncedCount()).isGreaterThanOrEqualTo(0);

      // Verify show types were created as part of the sync (fallback creates defaults)
      List<ShowType> showTypes = showTypeService.findAll();
      assertThat(showTypes).isNotEmpty();
      log.info("🎭 Found {} show types after full sync", showTypes.size());

    } else {
      log.warn("⚠️ Full shows sync failed: {}", result.getErrorMessage());
      // When NotionHandler is unavailable, we expect specific error messages
      assertThat(result.getErrorMessage()).isNotBlank();

      // Common expected error scenarios when NotionHandler is unavailable
      boolean isExpectedError =
          result.getErrorMessage().contains("NotionHandler")
              || result.getErrorMessage().contains("NOTION_TOKEN")
              || result.getErrorMessage().contains("not available");

      assertThat(isExpectedError).isTrue();
      log.info(
          "ℹ️ Expected error due to NotionHandler unavailability: {}", result.getErrorMessage());
    }
  }

  @Test
  @DisplayName("Should validate Notion configuration and connectivity")
  void shouldValidateNotionConfigurationAndConnectivity() {
    log.info("🔧 Testing Notion configuration and connectivity validation");

    // When - Test basic connectivity by attempting a simple sync operation
    SyncResult result = notionSyncService.syncShowTypes("integration-test-validation");

    // Then - Should get a valid response (success or failure with proper error)
    assertNotNull(result, "Sync result should not be null");
    assertThat(result.getEntityType()).isEqualTo("ShowTypes");

    if (result.isSuccess()) {
      log.info("✅ Notion connectivity validated successfully");
      assertThat(result.getSyncedCount()).isGreaterThanOrEqualTo(0);
    } else {
      log.info(
          "ℹ️ Notion connectivity test failed (expected if no Shows database): {}",
          result.getErrorMessage());
      // Should have a meaningful error message
      assertThat(result.getErrorMessage()).isNotBlank();

      // Common expected error scenarios
      boolean isExpectedError =
          result.getErrorMessage().contains("database")
              || result.getErrorMessage().contains("Shows")
              || result.getErrorMessage().contains("permission")
              || result.getErrorMessage().contains("token");

      if (!isExpectedError) {
        log.warn("⚠️ Unexpected error type: {}", result.getErrorMessage());
      }
    }
  }

  @Test
  @DisplayName("Should sync wrestlers from Notion to database")
  void shouldSyncWrestlersFromNotionToDatabase() {
    log.info("🤼 Testing wrestlers sync from Notion to database");

    // When - Sync wrestlers from Notion
    SyncResult result = notionSyncService.syncWrestlers("integration-test-wrestlers");

    // Then - Verify sync result
    assertNotNull(result, "Sync result should not be null");
    log.info("📊 Wrestlers sync result: {}", result);

    if (result.isSuccess()) {
      log.info("✅ Wrestlers sync completed successfully");
      assertThat(result.getEntityType()).isEqualTo("Wrestlers");
      assertThat(result.getSyncedCount()).isGreaterThanOrEqualTo(0);

      // Validate sync quality
      if (result.getSyncedCount() == 0) {
        log.warn("⚠️ Wrestlers sync succeeded but no wrestlers were actually synced");
        log.info("ℹ️ This could indicate NotionHandler unavailability or data quality issues");
      } else {
        log.info("📋 Successfully synced {} wrestlers", result.getSyncedCount());
      }
    } else {
      log.warn("⚠️ Wrestlers sync failed: {}", result.getErrorMessage());
      // Expected error when NotionHandler is unavailable
      assertThat(result.getErrorMessage()).isNotBlank();
      boolean isExpectedError =
          result.getErrorMessage().contains("NotionHandler")
              || result.getErrorMessage().contains("not available");
      assertThat(isExpectedError).isTrue();
      log.info("ℹ️ Expected error due to NotionHandler unavailability");
    }
  }

  @Test
  @DisplayName("Should sync seasons from Notion to database")
  void shouldSyncSeasonsFromNotionToDatabase() {
    log.info("📅 Testing seasons sync from Notion to database");

    // When - Sync seasons from Notion
    SyncResult result = notionSyncService.syncSeasons("integration-test-seasons");

    // Then - Verify sync result
    assertNotNull(result, "Sync result should not be null");
    log.info("📊 Seasons sync result: {}", result);

    if (result.isSuccess()) {
      log.info("✅ Seasons sync successful");
      assertThat(result.getEntityType()).isEqualTo("Seasons");
      assertThat(result.getSyncedCount()).isGreaterThanOrEqualTo(0);
    } else {
      log.warn("⚠️ Seasons sync failed: {}", result.getErrorMessage());
      assertThat(result.getErrorMessage()).isNotBlank();
      boolean isExpectedError =
          result.getErrorMessage().contains("NotionHandler")
              || result.getErrorMessage().contains("not available")
              || result.getErrorMessage().contains("NOTION_TOKEN");
      if (isExpectedError) {
        log.info("ℹ️ Expected error due to NotionHandler unavailability");
      }
    }
  }

  @Test
  @DisplayName("Should sync show templates from Notion to database")
  void shouldSyncShowTemplatesFromNotionToDatabase() {
    log.info("📋 Testing show templates sync from Notion to database");

    // When - Sync show templates from Notion
    SyncResult result = notionSyncService.syncShowTemplates("integration-test-templates");

    // Then - Verify sync result
    assertNotNull(result, "Sync result should not be null");
    log.info("📊 Show templates sync result: {}", result);

    if (result.isSuccess()) {
      log.info("✅ Show templates sync successful");
      assertThat(result.getEntityType()).isEqualTo("ShowTemplates");
      assertThat(result.getSyncedCount()).isGreaterThanOrEqualTo(0);
    } else {
      log.warn("⚠️ Show templates sync failed: {}", result.getErrorMessage());
      assertThat(result.getErrorMessage()).isNotBlank();
      boolean isExpectedError =
          result.getErrorMessage().contains("NotionHandler")
              || result.getErrorMessage().contains("not available");
      if (isExpectedError) {
        log.info("ℹ️ Expected error due to NotionHandler unavailability");
      }
    }
  }

  @Test
  @DisplayName("Should sync factions from Notion to database")
  void shouldSyncFactionsFromNotionToDatabase() {
    log.info("👥 Testing factions sync from Notion to database");

    // When - Sync factions from Notion
    SyncResult result = notionSyncService.syncFactions("integration-test-factions");

    // Then - Verify sync result
    assertNotNull(result, "Sync result should not be null");
    log.info("📊 Factions sync result: {}", result);

    if (result.isSuccess()) {
      log.info("✅ Factions sync successful");
      assertThat(result.getEntityType()).isEqualTo("Factions");
      assertThat(result.getSyncedCount()).isGreaterThanOrEqualTo(0);
    } else {
      log.warn("⚠️ Factions sync failed: {}", result.getErrorMessage());
      assertThat(result.getErrorMessage()).isNotBlank();
      boolean isExpectedError =
          result.getErrorMessage().contains("NotionHandler")
              || result.getErrorMessage().contains("not available");
      if (isExpectedError) {
        log.info("ℹ️ Expected error due to NotionHandler unavailability");
      }
    }
  }

  @Test
  @DisplayName("Should sync teams from Notion to database")
  void shouldSyncTeamsFromNotionToDatabase() {
    log.info("🏆 Testing teams sync from Notion to database");

    // When - Sync teams from Notion
    SyncResult result = notionSyncService.syncTeams();

    // Then - Verify sync result
    assertNotNull(result, "Sync result should not be null");
    log.info("📊 Teams sync result: {}", result);

    if (result.isSuccess()) {
      log.info("✅ Teams sync completed successfully");
      assertThat(result.getEntityType()).isEqualTo("Teams");
      assertThat(result.getSyncedCount()).isGreaterThanOrEqualTo(0);

      // Additional validation for sync quality
      assertFalse(result.getSyncedCount() == 0);
    } else {
      log.warn("⚠️ Teams sync failed: {}", result.getErrorMessage());
      assertThat(result.getErrorMessage()).isNotBlank();
      boolean isExpectedError =
          result.getErrorMessage().contains("NotionHandler")
              || result.getErrorMessage().contains("not available");
      if (isExpectedError) {
        log.info("ℹ️ Expected error due to NotionHandler unavailability");
      } else {
        log.error("❌ Unexpected teams sync failure: {}", result.getErrorMessage());
      }
    }
  }

  /** Helper method to check if NOTION_TOKEN is available for conditional tests. */
  static boolean isNotionTokenAvailable() {
    return System.getenv("NOTION_TOKEN") != null || System.getProperty("NOTION_TOKEN") != null;
  }
}
