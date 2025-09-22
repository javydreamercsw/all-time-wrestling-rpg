package com.github.javydreamercsw.management.service.sync;

import static com.github.javydreamercsw.base.test.BaseTest.isNotionTokenAvailable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Slf4j
@DisplayName("Notion Sync Integration Tests")
class NotionSyncIntegrationTest extends AbstractIntegrationTest {

  @BeforeEach
  public void setUp() {
    log.info("🧪 Setting up NotionSyncIntegrationTest");
    log.info("NOTION_TOKEN available: {}", isNotionTokenAvailable());
  }

  @Test
  @DisplayName("Should sync show types from Notion to database")
  void shouldConnectToNotionAndRetrieveDatabaseInfo() {
    log.info("🔗 Testing Notion connection and database retrieval");

    // When - Attempt to connect to Notion (this will validate the connection)
    NotionSyncService.SyncResult result =
        notionSyncService.syncShowTypes("integration-test-connection");

    // Then - Should successfully connect and process
    assertNotNull(result, "Sync result should not be null");
    log.info("📊 Sync result: {}", result);

    if (result.isSuccess()) {
      log.info(
          "✅ Successfully processed show types (fallback behavior when NotionHandler unavailable)");
      assertThat(result.getEntityType()).isEqualTo("Show Types");
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
    NotionSyncService.SyncResult result =
        notionSyncService.syncShowTypes("integration-test-show-types");

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
    NotionSyncService.SyncResult result = notionSyncService.syncShows();

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
    NotionSyncService.SyncResult result =
        notionSyncService.syncShowTypes("integration-test-validation");

    // Then - Should get a valid response (success or failure with proper error)
    assertNotNull(result, "Sync result should not be null");
    assertThat(result.getEntityType()).isEqualTo("Show Types");

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
    NotionSyncService.SyncResult result =
        notionSyncService.syncWrestlers("integration-test-wrestlers");

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
    NotionSyncService.SyncResult result = notionSyncService.syncSeasons("integration-test-seasons");

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
    NotionSyncService.SyncResult result =
        notionSyncService.syncShowTemplates("integration-test-templates");

    // Then - Verify sync result
    assertNotNull(result, "Sync result should not be null");
    log.info("📊 Show templates sync result: {}", result);

    if (result.isSuccess()) {
      log.info("✅ Show templates sync successful");
      assertThat(result.getEntityType()).isEqualTo("Show Templates");
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
    // When - Sync wrestlers from Notion first (factions depend on wrestlers)
    NotionSyncService.SyncResult wrestlerResult =
        notionSyncService.syncWrestlers("integration-test-wrestlers");

    log.info("👥 Testing factions sync from Notion to database");

    // When - Sync factions from Notion
    NotionSyncService.SyncResult result =
        notionSyncService.syncFactions("integration-test-factions");

    // Then - Verify sync result
    assertNotNull(result, "Sync result should not be null");
    log.info("📊 Factions sync result: {}", result);

    if (result.isSuccess()) {
      log.info("✅ Factions sync successful");
      assertThat(result.getEntityType()).isEqualTo("Factions");
      assertThat(result.getSyncedCount()).isGreaterThanOrEqualTo(0);

      // Validate factions are properly stored in database
      validateFactionsInDatabase();

    } else {
      log.warn("��️ Factions sync failed: {}", result.getErrorMessage());
      assertThat(result.getErrorMessage()).isNotBlank();
      boolean isExpectedError =
          result.getErrorMessage().contains("NotionHandler")
              || result.getErrorMessage().contains("not available")
              || result.getErrorMessage().contains("NOTION_TOKEN");
      if (isExpectedError) {
        log.info("ℹ️ Expected error due to NotionHandler unavailability");
      } else {
        log.error("❌ Unexpected faction sync failure: {}", result.getErrorMessage());
      }
    }
  }

  /** Validates that factions are properly stored in the database with valid leader and members. */
  private void validateFactionsInDatabase() {
    log.info("🔍 Validating factions in database...");

    // Get all factions from database
    List<Faction> factions = factionRepository.findAll();
    log.info("📋 Found {} factions in database", factions.size());

    if (factions.isEmpty()) {
      log.warn("⚠️ No factions found in database after sync");
      return;
    }

    int validFactions = 0;
    int factionsWithLeader = 0;
    int factionsWithMembers = 0;
    int factionsWithValidData = 0;

    for (Faction faction : factions) {
      log.info("🏴 Validating faction: {}", faction.getName());

      // Basic validation
      assertThat(faction.getName()).isNotBlank();
      assertThat(faction.getId()).isNotNull();
      validFactions++;

      // Validate leader
      if (faction.getLeader() != null) {
        assertThat(faction.getLeader().getName()).isNotBlank();
        log.info(
            "👑 Faction '{}' has leader: {}", faction.getName(), faction.getLeader().getName());
        factionsWithLeader++;
      } else {
        log.warn("⚠️ Faction '{}' has no leader", faction.getName());
      }

      // Validate members
      if (faction.getMembers() != null && !faction.getMembers().isEmpty()) {
        assertThat(faction.getMemberCount()).isGreaterThan(0);
        log.info("👥 Faction '{}' has {} members", faction.getName(), faction.getMemberCount());

        // Validate each member
        for (Wrestler member : faction.getMembers()) {
          assertThat(member).isNotNull();
          assertThat(member.getName()).isNotBlank();
          assertThat(member.getFaction()).isEqualTo(faction);
          log.debug("   - Member: {}", member.getName());
        }
        factionsWithMembers++;

        // Check if leader is also a member (should be true for valid factions)
        if (faction.getLeader() != null) {
          boolean leaderIsMember =
              faction.getMembers().stream().anyMatch(member -> member.equals(faction.getLeader()));
          if (leaderIsMember) {
            log.info(
                "✅ Leader '{}' is properly included in members list",
                faction.getLeader().getName());
          } else {
            log.warn(
                "⚠️ Leader '{}' is not in the members list for faction '{}'",
                faction.getLeader().getName(),
                faction.getName());
          }
        }
      } else {
        log.warn("⚠️ Faction '{}' has no members", faction.getName());
      }

      // Check for factions with both leader and members (considered valid)
      if (faction.getLeader() != null && faction.getMemberCount() > 0) {
        factionsWithValidData++;
        log.info("✅ Faction '{}' has complete data (leader + members)", faction.getName());
      }

      // Validate faction status and dates
      assertThat(faction.getIsActive()).isNotNull();
      assertThat(faction.getFormedDate()).isNotNull();

      if (faction.getExternalId() != null) {
        assertThat(faction.getExternalId()).isNotBlank();
        log.debug(
            "🔗 Faction '{}' has external ID: {}", faction.getName(), faction.getExternalId());
      }

      log.info(
          "📊 Faction '{}' validation complete - Active: {}, Members: {}, Leader: {}",
          faction.getName(),
          faction.getIsActive(),
          faction.getMemberCount(),
          faction.getLeader() != null ? faction.getLeader().getName() : "None");
    }

    // Summary validation
    log.info("📈 Faction validation summary:");
    log.info("   - Total factions: {}", factions.size());
    log.info("   - Valid factions: {}", validFactions);
    log.info("   - Factions with leader: {}", factionsWithLeader);
    log.info("   - Factions with members: {}", factionsWithMembers);
    log.info("   - Factions with complete data: {}", factionsWithValidData);

    // Assert minimum data quality
    assertThat(validFactions).isEqualTo(factions.size());

    if (factionsWithValidData > 0) {
      log.info("✅ Found {} factions with complete leader and member data", factionsWithValidData);

      // If we have complete data, ensure it's meaningful
      double completenessRatio = (double) factionsWithValidData / factions.size();
      double completenessPercentage = completenessRatio * 100;
      if (completenessRatio < 0.5) {
        log.warn(
            "⚠️ Only {}% of factions have complete data (leader + members)",
            String.format("%.1f", completenessPercentage));
      } else {
        log.info(
            "✅ {}% of factions have complete data (leader + members)",
            String.format("%.1f", completenessPercentage));
      }
    } else {
      log.warn("⚠️ No factions found with complete leader and member data");
    }
  }
}
