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
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.ai.notion.FactionPage;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionPageDataExtractor;
import com.github.javydreamercsw.base.config.NotionSyncProperties;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.sync.SyncSessionManager;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import java.util.HashMap;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Slf4j
class FactionSyncIT extends ManagementIntegrationTest {

  @Autowired
  private com.github.javydreamercsw.management.service.sync.NotionSyncService notionSyncService;

  @Autowired private FactionService factionService;
  @Autowired private SyncSessionManager syncSessionManager;
  @Autowired private PlatformTransactionManager transactionManager;
  @MockitoBean private NotionHandler notionHandler;
  @MockitoBean private NotionPageDataExtractor notionPageDataExtractor;
  @MockitoBean private NotionSyncProperties syncProperties;
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
    syncSessionManager.clearSyncSession();
    lenient().when(syncProperties.getParallelThreads()).thenReturn(1);
    lenient().when(syncProperties.isEntityEnabled(anyString())).thenReturn(true);
  }

  @Test
  @DisplayName("Should sync factions from Notion")
  void shouldSyncFactionsFromNotion() {
    log.info("🚀 Starting faction sync integration test...");

    // Given
    String leaderName = "Test Leader";
    TransactionStatus status =
        transactionManager.getTransaction(new DefaultTransactionDefinition());
    try {
      Wrestler leader = createTestWrestler(leaderName);
      leader.setExternalId(UUID.randomUUID().toString());
      wrestlerRepository.saveAndFlush(leader);
      transactionManager.commit(status);
    } catch (Exception e) {
      transactionManager.rollback(status);
      throw e;
    }

    String leaderExtId = wrestlerService.findByName(leaderName).get().getExternalId();

    when(factionPage1.getId()).thenReturn(UUID.randomUUID().toString());
    Map<String, Object> props1 = new HashMap<>();
    props1.put("Name", "Test Faction 1");
    props1.put("Status", true);
    props1.put("Leader", List.of(Map.of("id", leaderExtId)));
    when(factionPage1.getRawProperties()).thenReturn(props1);
    when(notionPageDataExtractor.extractNameFromNotionPage(factionPage1))
        .thenReturn("Test Faction 1");

    when(factionPage2.getId()).thenReturn(UUID.randomUUID().toString());
    Map<String, Object> props2 = new HashMap<>();
    props2.put("Name", "Test Faction 2");
    props2.put("Status", false);
    props2.put("Leader", List.of());
    when(factionPage2.getRawProperties()).thenReturn(props2);
    when(notionPageDataExtractor.extractNameFromNotionPage(factionPage2))
        .thenReturn("Test Faction 2");

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
    TransactionStatus status2 =
        transactionManager.getTransaction(new DefaultTransactionDefinition());
    try {
      Optional<Faction> faction1Opt = factionService.getFactionByName("Test Faction 1");
      assertTrue(faction1Opt.isPresent(), "Test Faction 1 should be in the database");
      Faction faction1 = faction1Opt.get();
      assertEquals(factionPage1.getId(), faction1.getExternalId());
      assertTrue(faction1.isActive());
      assertNotNull(faction1.getLeader(), "Leader should not be null");
      assertEquals(leaderName, faction1.getLeader().getName());

      Optional<Faction> faction2Opt = factionService.getFactionByName("Test Faction 2");
      assertTrue(faction2Opt.isPresent(), "Test Faction 2 should be in the database");
      Faction faction2 = faction2Opt.get();
      assertEquals(factionPage2.getId(), faction2.getExternalId());
      assertFalse(faction2.isActive());
      assertNull(faction2.getLeader());
      transactionManager.commit(status2);
    } catch (Exception e) {
      transactionManager.rollback(status2);
      throw e;
    }
  }

  @Test
  @DisplayName("Should sync factions from Notion with missing leader")
  void shouldSyncFactionsFromNotionWithMissingLeader() {
    log.info("🚀 Starting faction sync with missing leader test...");

    // Given
    when(factionPage1.getId()).thenReturn(UUID.randomUUID().toString());
    Map<String, Object> props = new HashMap<>();
    props.put("Name", "Test Faction 1");
    props.put("Status", true);
    props.put("Leader", List.of());
    when(factionPage1.getRawProperties()).thenReturn(props);
    when(notionPageDataExtractor.extractNameFromNotionPage(factionPage1))
        .thenReturn("Test Faction 1");

    when(notionHandler.loadAllFactions()).thenReturn(List.of(factionPage1));

    // When
    BaseSyncService.SyncResult result =
        notionSyncService.syncFactions(
            "integration-test-factions-no-leader", SyncDirection.INBOUND);

    // Then
    assertTrue(result.isSuccess());

    TransactionStatus status =
        transactionManager.getTransaction(new DefaultTransactionDefinition());
    try {
      Optional<Faction> faction1Opt = factionService.getFactionByName("Test Faction 1");
      assertTrue(faction1Opt.isPresent());
      assertNull(faction1Opt.get().getLeader());
      transactionManager.commit(status);
    } catch (Exception e) {
      transactionManager.rollback(status);
      throw e;
    }
  }
}
