package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.test.BaseTest;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.faction.FactionRivalry;
import com.github.javydreamercsw.management.domain.faction.FactionRivalryRepository;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "notion.sync.enabled=true",
      "notion.sync.entities.rivalries=true",
      "notion.sync.entities.faction-rivalries=true"
    })
@ActiveProfiles("test")
@Transactional
@DisplayName("Rivalry Sync Integration Tests")
@EnabledIf("isNotionTokenAvailable")
class RivalrySyncIntegrationTest extends BaseTest {

  @Autowired private NotionSyncService notionSyncService;

  @Autowired private RivalryRepository rivalryRepository;
  @Autowired private FactionRivalryRepository factionRivalryRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private FactionRepository factionRepository;

  @BeforeEach
  void setUp() {
    // Clean up existing data
    rivalryRepository.deleteAll();
    factionRivalryRepository.deleteAll();
    wrestlerRepository.deleteAll();
    factionRepository.deleteAll();

    // Sync wrestlers and factions to ensure they exist for rivalries
    notionSyncService.syncWrestlers("test-setup-wrestlers");
    notionSyncService.syncFactions("test-setup-factions");

    // Create test data
    Wrestler testWrestler1 = new Wrestler();
    testWrestler1.setName("Test Wrestler 1");
    testWrestler1.setDescription("First test wrestler");
    testWrestler1.setIsPlayer(false);
    testWrestler1.setTier(WrestlerTier.ROOKIE);
    testWrestler1.setStartingHealth(100);
    testWrestler1.setStartingStamina(100);
    testWrestler1.setLowHealth(20);
    testWrestler1.setLowStamina(20);
    testWrestler1.setDeckSize(40);
    testWrestler1.setFans(1000L);
    wrestlerRepository.saveAndFlush(testWrestler1);

    Wrestler testWrestler2 = new Wrestler();
    testWrestler2.setName("Test Wrestler 2");
    testWrestler2.setDescription("Second test wrestler");
    testWrestler2.setIsPlayer(false);
    testWrestler2.setTier(WrestlerTier.ROOKIE);
    testWrestler2.setStartingHealth(100);
    testWrestler2.setStartingStamina(100);
    testWrestler2.setLowHealth(20);
    testWrestler2.setLowStamina(20);
    testWrestler2.setDeckSize(40);
    testWrestler2.setFans(1000L);
    wrestlerRepository.saveAndFlush(testWrestler2);

    Faction testFaction1 = new Faction();
    testFaction1.setName("Test Faction 1");
    testFaction1.setDescription("First test faction");
    factionRepository.saveAndFlush(testFaction1);

    Faction testFaction2 = new Faction();
    testFaction2.setName("Test Faction 2");
    testFaction2.setDescription("Second test faction");
    factionRepository.saveAndFlush(testFaction2);
  }

  @Test
  @DisplayName("Should sync rivalries from Notion to database successfully")
  void shouldSyncRivalriesFromNotionToDatabaseSuccessfully() {
    // Given - Real integration test with actual Notion API
    int initialRivalryCount = rivalryRepository.findAll().size();

    // When - Sync rivalries from real Notion database
    NotionSyncService.SyncResult result =
        notionSyncService.syncRivalries("test-operation-rivalry-123");

    // Then - Verify sync completed successfully (regardless of rivalry count)
    assertThat(result).isNotNull();

    // Integration test should succeed if:
    // 1. No errors occurred during sync, OR
    // 2. Sync completed with some rivalries processed
    boolean syncSuccessful =
        result.isSuccess()
            || (result.getErrorMessage() != null
                && result.getErrorMessage().contains("No rivalries found"));

    assertThat(syncSuccessful).isTrue();

    // Verify database state is consistent
    List<Rivalry> finalRivalries = rivalryRepository.findAll();
    assertThat(finalRivalries.size())
        .withFailMessage(
            "Integration test completed: "
                + (result.isSuccess() ? "SUCCESS" : "FAILURE")
                + " - Synced: "
                + result.getSyncedCount()
                + " faction rivalries, Final DB count: "
                + finalRivalries.size())
        .isGreaterThan(initialRivalryCount);
  }

  @Test
  @DisplayName("Should sync faction rivalries from Notion to database successfully")
  void shouldSyncFactionRivalriesFromNotionToDatabaseSuccessfully() {
    // Given - Real integration test with actual Notion API
    int initialRivalryCount = factionRivalryRepository.findAll().size();

    // When - Sync faction rivalries from real Notion database
    NotionSyncService.SyncResult result =
        notionSyncService.syncFactionRivalries("test-operation-faction-rivalry-123");

    // Then - Verify sync completed successfully (regardless of rivalry count)
    assertThat(result).isNotNull();

    // Integration test should succeed if:
    // 1. No errors occurred during sync, OR
    // 2. Sync completed with some rivalries processed
    boolean syncSuccessful =
        result.isSuccess()
            || (result.getErrorMessage() != null
                && result.getErrorMessage().contains("No faction rivalries found"));

    assertThat(syncSuccessful).isTrue();

    // Verify database state is consistent
    List<FactionRivalry> finalRivalries = factionRivalryRepository.findAll();
    assertThat(finalRivalries.size())
        .withFailMessage(
            "Integration test completed: "
                + (result.isSuccess() ? "SUCCESS" : "FAILURE")
                + " - Synced: "
                + result.getSyncedCount()
                + " faction rivalries, Final DB count: "
                + finalRivalries.size())
        .isGreaterThan(initialRivalryCount);
  }
}
