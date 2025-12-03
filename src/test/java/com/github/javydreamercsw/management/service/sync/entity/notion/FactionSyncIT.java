package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.FactionPage;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
class FactionSyncIT extends ManagementIntegrationTest {

  @Autowired
  private com.github.javydreamercsw.management.service.sync.NotionSyncService notionSyncService;

  @Autowired private FactionService factionService;
  @MockitoBean private NotionHandler notionHandler;
  @Mock private FactionPage factionPage1;
  @Mock private FactionPage factionPage2;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
  }

  @Test
  @DisplayName("Should sync factions from Notion")
  @Transactional
  void shouldSyncFactionsFromNotion() {
    log.info("🚀 Starting faction sync integration test...");

    // Given
    String leaderName = "Test Leader";
    wrestlerRepository.saveAndFlush(createTestWrestler(leaderName));

    when(factionPage1.getId()).thenReturn(UUID.randomUUID().toString());
    when(factionPage1.getRawProperties())
        .thenReturn(Map.of("Name", "Test Faction 1", "Active", true, "Leader", leaderName));

    when(factionPage2.getId()).thenReturn(UUID.randomUUID().toString());
    when(factionPage2.getRawProperties())
        .thenReturn(Map.of("Name", "Test Faction 2", "Active", false, "Leader", ""));

    when(notionHandler.loadAllFactions()).thenReturn(List.of(factionPage1, factionPage2));

    // When - Perform sync with mocked services
    BaseSyncService.SyncResult result =
        notionSyncService.syncFactions("integration-test-factions", SyncDirection.INBOUND);

    // Then - Verify the sync result
    assertNotNull(result, "Sync result should not be null");
    assertTrue(result.isSuccess(), "Sync should be successful");
    assertEquals("Factions", result.getEntityType(), "Entity type should be 'Factions'");
    assertEquals(2, result.getSyncedCount(), "Should have synced 2 factions");

    // Verify factions in the database
    Optional<Faction> faction1Opt = factionService.getFactionByName("Test Faction 1");
    assertTrue(faction1Opt.isPresent(), "Test Faction 1 should be in the database");
    Faction faction1 = faction1Opt.get();
    assertEquals(factionPage1.getId(), faction1.getExternalId());
    assertTrue(faction1.getIsActive());
    assertNotNull(faction1.getLeader());
    assertEquals(leaderName, faction1.getLeader().getName());

    Optional<Faction> faction2Opt = factionService.getFactionByName("Test Faction 2");
    assertTrue(faction2Opt.isPresent(), "Test Faction 2 should be in the database");
    Faction faction2 = faction2Opt.get();
    assertEquals(factionPage2.getId(), faction2.getExternalId());
    assertFalse(faction2.getIsActive());
    assertNull(faction2.getLeader());
  }

  @Test
  @DisplayName("Should validate faction sync operation ID handling")
  void shouldValidateFactionSyncOperationIdHandling() {
    log.info("🧪 Testing faction sync with specific operation ID...");

    String operationId = "test-faction-sync-" + System.currentTimeMillis();

    when(notionHandler.loadAllFactions()).thenReturn(List.of());

    // When - Perform sync with specific operation ID
    BaseSyncService.SyncResult result =
        notionSyncService.syncFactions(operationId, SyncDirection.INBOUND);

    // Then - Verify the result structure
    assertNotNull(result, "Sync result should not be null");
    assertEquals("Factions", result.getEntityType(), "Entity type should be 'Factions'");

    log.info("✅ Faction sync operation ID handling validated");
    log.info("   Operation ID: {}", operationId);
    log.info("   Result: {}", result.isSuccess() ? "SUCCESS" : "FAILURE");
  }
}
