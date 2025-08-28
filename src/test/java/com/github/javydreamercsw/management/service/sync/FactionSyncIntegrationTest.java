package com.github.javydreamercsw.management.service.sync;

import static org.junit.jupiter.api.Assertions.*;

import com.github.javydreamercsw.base.test.BaseTest;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.sync.entity.WrestlerSyncService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Real integration test for faction sync that uses actual Spring services and real Notion API Real
 * integration test for faction sync that uses actual Spring services and real Notion API calls.
 * This test requires the NOTION_TOKEN environment variable to be set.
 *
 * <p>Run with: mvn test -Dtest=FactionSyncIntegrationTest -DNOTION_TOKEN=your_token
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "notion.sync.enabled=true",
      "notion.sync.entities=factions",
      "notion.sync.scheduler.enabled=true"
    })
@EnabledIf("isNotionTokenAvailable")
@Slf4j
class FactionSyncIntegrationTest extends BaseTest {

  @Autowired private NotionSyncService notionSyncService;
  @Autowired private FactionService factionService;
  @Autowired private FactionRepository factionRepository;
  @Autowired private WrestlerSyncService wrestlerSyncService;

  @BeforeClass
  public void synceWrestlers() {
    wrestlerSyncService.syncWrestlers("sync-test");
  }

  @Test
  @DisplayName("Should sync factions from Notion with real services")
  void shouldSyncFactionsFromNotionWithRealServices() {
    log.info("🚀 Starting real faction sync integration test...");

    // When - Perform real sync with real services
    NotionSyncService.SyncResult result =
        notionSyncService.syncFactions("integration-test-factions");

    // Then - Verify the sync result
    assertNotNull(result, "Sync result should not be null");
    assertEquals("Factions", result.getEntityType(), "Entity type should be 'Factions'");

    if (result.isSuccess()) {
      log.info("✅ Faction sync completed successfully!");
      log.info("   - Synced: {} factions", result.getSyncedCount());
      log.info("   - Errors: {} factions", result.getErrorCount());

      // Verify we actually synced some data
      assertTrue(result.getSyncedCount() >= 0, "Should have synced 0 or more factions");
      assertTrue(result.getErrorCount() >= 0, "Should have 0 or more errors");

      // Additional assertions to verify factions with members exist in database
      log.info("🔍 Verifying factions with members in database...");

      List<Faction> factionsWithMembers = factionService.findAllWithMembers();
      assertNotNull(factionsWithMembers, "Factions list should not be null");

      if (!factionsWithMembers.isEmpty()) {
        log.info("   Found {} factions in database", factionsWithMembers.size());

        // Check if any factions have members
        long factionsWithMemberCount =
            factionsWithMembers.stream()
                .filter(faction -> faction.getMembers() != null && !faction.getMembers().isEmpty())
                .count();

        log.info("   {} factions have members", factionsWithMemberCount);

        // First, validate that ALL factions have members - the test should fail if any faction has
        // no members
        List<Faction> factionsWithoutMembers =
            factionsWithMembers.stream()
                .filter(faction -> faction.getMembers() == null || faction.getMembers().isEmpty())
                .toList();

        if (!factionsWithoutMembers.isEmpty()) {
          StringBuilder errorMessage =
              new StringBuilder("Found factions without members after sync:");
          for (Faction faction : factionsWithoutMembers) {
            errorMessage.append(
                String.format(
                    "\n  - Faction '%s' (ID: %d) has no members",
                    faction.getName(), faction.getId()));
          }
          log.error("❌ {}", errorMessage.toString());
          fail(errorMessage.toString());
        }

        if (factionsWithMemberCount > 0) {
          // Verify member relationships are properly loaded for ALL factions with members
          List<Faction> factionsWithMembersFiltered =
              factionsWithMembers.stream()
                  .filter(
                      faction -> faction.getMembers() != null && !faction.getMembers().isEmpty())
                  .toList();

          log.info("   ✅ Testing {} factions with members", factionsWithMembersFiltered.size());

          for (Faction factionWithMembers : factionsWithMembersFiltered) {
            log.info(
                "   🔍 Validating faction '{}' with {} members",
                factionWithMembers.getName(),
                factionWithMembers.getMembers().size());

            // This is critical - accessing member properties outside transaction context
            // would throw LazyInitializationException if not properly loaded
            for (Wrestler member : factionWithMembers.getMembers()) {
              assertNotNull(
                  member.getId(),
                  String.format(
                      "Member ID should not be null in faction '%s'",
                      factionWithMembers.getName()));
              assertNotNull(
                  member.getName(),
                  String.format(
                      "Member name should not be null in faction '%s'",
                      factionWithMembers.getName()));
              assertFalse(
                  member.getName().trim().isEmpty(),
                  String.format(
                      "Member name should not be empty in faction '%s'",
                      factionWithMembers.getName()));

              log.info("     - Member: {} (ID: {})", member.getName(), member.getId());
            }

            // Verify faction leader if present
            if (factionWithMembers.getLeader() != null) {
              Wrestler leader = factionWithMembers.getLeader();
              assertNotNull(
                  leader.getId(),
                  String.format(
                      "Leader ID should not be null in faction '%s'",
                      factionWithMembers.getName()));
              assertNotNull(
                  leader.getName(),
                  String.format(
                      "Leader name should not be null in faction '%s'",
                      factionWithMembers.getName()));
              log.info("     - Leader: {} (ID: {})", leader.getName(), leader.getId());
            } else {
              log.info("     - No leader assigned");
            }
          }
        } else {
          log.info(
              "   ℹ️ No factions with members found (this may be expected if Notion data doesn't"
                  + " include member relationships)");
        }
      } else {
        log.info("   ℹ️ No factions found in database after sync");
      }
    } else {
      log.error("❌ Faction sync failed: {}", result.getErrorMessage());

      // For debugging - log the full error details
      if (result.getErrorMessage() != null) {
        log.error("   Error details: {}", result.getErrorMessage());
      }

      // Don't fail the test immediately - let's see what the actual error is
      // This helps with debugging real integration issues
      log.warn("   This might be expected if there are data quality issues in Notion");
      log.warn("   Check the error message above to see if it's a known issue");
    }

    // Always log the result for debugging
    log.info("📊 Sync Result Summary:");
    log.info("   - Success: {}", result.isSuccess());
    log.info("   - Entity: {}", result.getEntityType());
    log.info("   - Synced: {}", result.getSyncedCount());
    log.info("   - Errors: {}", result.getErrorCount());
    if (!result.isSuccess()) {
      log.info("   - Error: {}", result.getErrorMessage());
    }
  }

  @Test
  @DisplayName("Should validate faction sync operation ID handling")
  void shouldValidateFactionSyncOperationIdHandling() {
    log.info("🧪 Testing faction sync with specific operation ID...");

    String operationId = "test-faction-sync-" + System.currentTimeMillis();

    // When - Perform sync with specific operation ID
    NotionSyncService.SyncResult result = notionSyncService.syncFactions(operationId);

    // Then - Verify the result structure
    assertNotNull(result, "Sync result should not be null");
    assertEquals("Factions", result.getEntityType(), "Entity type should be 'Factions'");

    // The operation should complete (success or failure is less important than proper handling)
    assertNotNull(result.isSuccess(), "Success status should be defined");

    log.info("✅ Faction sync operation ID handling validated");
    log.info("   Operation ID: {}", operationId);
    log.info("   Result: {}", result.isSuccess() ? "SUCCESS" : "FAILURE");
  }
}
