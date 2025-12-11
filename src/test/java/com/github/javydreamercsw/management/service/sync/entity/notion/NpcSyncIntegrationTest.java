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

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NpcPage;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.sync.SyncSessionManager;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Slf4j
class NpcSyncIntegrationTest extends ManagementIntegrationTest {

  @Autowired
  private com.github.javydreamercsw.management.service.sync.NotionSyncService notionSyncService;

  @Autowired private NpcService npcService;
  @Autowired private NpcSyncService npcSyncService;
  @Autowired private SyncSessionManager syncSessionManager;
  @MockitoBean private NotionHandler notionHandler;

  private NpcPage npcPage1;
  private NpcPage npcPage2;

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
    npcPage1 = Mockito.mock(NpcPage.class);
    npcPage2 = Mockito.mock(NpcPage.class);
  }

  @Test
  @DisplayName("Should sync NPCs from Notion")
  void shouldSyncNpcsFromNotion() {
    log.info("🚀 Starting real NPC sync integration test...");

    // Given
    String npc1Id = UUID.randomUUID().toString();
    when(npcPage1.getId()).thenReturn(npc1Id);
    when(npcPage1.getRawProperties())
        .thenReturn(Map.of("Name", "Test NPC 1", "Role", "Interviewer"));

    String npc2Id = UUID.randomUUID().toString();
    when(npcPage2.getId()).thenReturn(npc2Id);
    when(npcPage2.getRawProperties())
        .thenReturn(Map.of("Name", "Test NPC 2", "Role", "General Manager"));

    when(notionHandler.loadAllNpcs()).thenReturn(List.of(npcPage1));

    // When - Perform real sync with real services
    BaseSyncService.SyncResult result =
        notionSyncService.syncNpcs("test-operation", SyncDirection.INBOUND);

    // Then - Verify the sync result
    assertNotNull(result, "Sync result should not be null");
    assertEquals("NPCs", result.getEntityType(), "Entity type should be 'NPCs'");
    assertTrue(result.isSuccess(), "Sync should be successful");
    assertEquals(1, result.getSyncedCount(), "Should have synced 1 NPCs");

    // Verify NPCs in database
    Npc npc1 = npcService.findByName("Test NPC 1");
    assertNotNull(npc1);
    assertEquals(npc1Id, npc1.getExternalId());
    assertEquals("Interviewer", npc1.getNpcType());

    // Run sync again to test updates and no duplicates
    syncSessionManager.clearSyncSession(); // Reset session to allow second sync
    when(npcPage1.getRawProperties())
        .thenReturn(Map.of("Name", "Test NPC 1 Updated", "Role", "Announcer"));
    when(notionHandler.loadAllNpcs()).thenReturn(List.of(npcPage1));
    notionSyncService.syncNpcs("second-sync-operation", SyncDirection.INBOUND);

    assertEquals(1, npcService.findAll().size());

    Npc updatedNpc1 = npcService.findByExternalId(npc1Id);
    assertNotNull(updatedNpc1);
    assertEquals("Test NPC 1 Updated", updatedNpc1.getName());
    assertEquals("Announcer", updatedNpc1.getNpcType());
  }
}
