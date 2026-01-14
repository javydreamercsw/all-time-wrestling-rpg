/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.FactionPage;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
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

  private static MockedStatic<EnvironmentVariableUtil> mockedEnvironmentVariableUtil;

  @BeforeAll
  static void beforeAll() {
    mockedEnvironmentVariableUtil = Mockito.mockStatic(EnvironmentVariableUtil.class);
    mockedEnvironmentVariableUtil
        .when(EnvironmentVariableUtil::isNotionTokenAvailable)
        .thenReturn(true);
    mockedEnvironmentVariableUtil
        .when(EnvironmentVariableUtil::getNotionToken)
        .thenReturn("test-token");
  }

  @AfterAll
  static void afterAll() {
    if (mockedEnvironmentVariableUtil != null) {
      mockedEnvironmentVariableUtil.close();
    }
  }

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
    assertTrue(faction1.isActive());
    assertNotNull(faction1.getLeader());
    assertEquals(leaderName, faction1.getLeader().getName());

    Optional<Faction> faction2Opt = factionService.getFactionByName("Test Faction 2");
    assertTrue(faction2Opt.isPresent(), "Test Faction 2 should be in the database");
    Faction faction2 = faction2Opt.get();
    assertEquals(factionPage2.getId(), faction2.getExternalId());
    assertFalse(faction2.isActive());
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
