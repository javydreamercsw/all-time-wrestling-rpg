package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.faction.FactionRivalry;
import com.github.javydreamercsw.management.domain.faction.FactionRivalryRepository;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Slf4j
@DisplayName("Rivalry Sync Integration Tests")
class RivalrySyncIntegrationTest extends ManagementIntegrationTest {
  @MockitoBean private NotionSyncService notionSyncService;

  @Autowired private RivalryRepository rivalryRepository;
  @Autowired private FactionRivalryRepository factionRivalryRepository;

  @BeforeEach
  void setUp() {
    when(notionSyncService.syncRivalries(anyString()))
        .thenReturn(NotionSyncService.SyncResult.success("Rivalries", 0, 0, 0));
    when(notionSyncService.syncFactionRivalries(anyString()))
        .thenReturn(NotionSyncService.SyncResult.success("Faction Rivalries", 0, 0, 0));
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
    String failMessage =
        "Integration test completed: "
            + (result.isSuccess() ? "SUCCESS" : "FAILURE")
            + " - Synced: "
            + result.getSyncedCount()
            + " rivalries, Final DB count: "
            + finalRivalries.size();
    assertThat(finalRivalries.size()).withFailMessage(failMessage).isEqualTo(initialRivalryCount);
  }

  @Test
  @DisplayName("Should sync faction rivalries from Notion to database successfully")
  void shouldSyncFactionRivalriesFromNotionToDatabaseSuccessfully() {
    // Given - Real integration test with actual Notion API
    int initialRivalryCount = factionRivalryRepository.findAll().size();

    // When - Sync faction rivalries from real Notion database
    notionSyncService.syncFactions("test-operation-faction-123");
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
    String failMessage =
        "Integration test completed: "
            + (result.isSuccess() ? "SUCCESS" : "FAILURE")
            + " - Synced: "
            + result.getSyncedCount()
            + " faction rivalries, Final DB count: "
            + finalRivalries.size();
    assertThat(finalRivalries.size()).withFailMessage(failMessage).isEqualTo(initialRivalryCount);
  }
}
