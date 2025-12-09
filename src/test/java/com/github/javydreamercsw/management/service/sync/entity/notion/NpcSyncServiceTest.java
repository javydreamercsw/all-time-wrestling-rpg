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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionPageDataExtractor;
import com.github.javydreamercsw.base.ai.notion.NpcPage;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.sync.AbstractSyncTest;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.SyncSessionManager;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class NpcSyncServiceTest extends AbstractSyncTest {

  @Mock private NpcService npcService;
  @Mock private NotionHandler notionHandler;
  @Mock private SyncServiceDependencies syncServiceDependencies;
  @Mock private SyncSessionManager syncSessionManager;
  @Mock private NotionPageDataExtractor notionPageDataExtractor;

  private NpcSyncService npcSyncService;

  @BeforeEach
  @Override
  public void setUp() {
    super.setUp(); // Call parent setup first
    lenient().when(syncServiceDependencies.getNotionHandler()).thenReturn(notionHandler);
    lenient().when(syncServiceDependencies.getSyncSessionManager()).thenReturn(syncSessionManager);
    lenient().when(syncServiceDependencies.getNotionSyncProperties()).thenReturn(syncProperties);
    lenient()
        .when(syncServiceDependencies.getNotionPageDataExtractor())
        .thenReturn(notionPageDataExtractor);
    lenient().when(syncServiceDependencies.getRateLimitService()).thenReturn(rateLimitService);
    npcSyncService =
        new NpcSyncService(objectMapper, syncServiceDependencies, npcService, notionApiExecutor);
  }

  @Test
  void testSyncNpcs() throws InterruptedException {
    // Given
    when(syncProperties.isEntityEnabled("npcs")).thenReturn(true);

    List<NpcPage> npcPages = new ArrayList<>();
    NpcPage npcPage = new NpcPage();
    npcPage.setId("test-id");
    Map<String, Object> properties = new HashMap<>();
    properties.put("Name", "Test NPC");
    properties.put("Role", "Referee");
    npcPage.setRawProperties(properties);
    npcPages.add(npcPage);

    when(notionHandler.loadAllNpcs()).thenReturn(npcPages);
    when(notionPageDataExtractor.extractNameFromNotionPage(any(NpcPage.class)))
        .thenReturn("Test NPC");

    when(npcService.findByExternalId("test-id")).thenReturn(null);
    when(npcService.findByName("Test NPC")).thenReturn(null);
    doNothing().when(rateLimitService).acquirePermit();

    // When
    SyncResult result =
        npcSyncService.syncNpcs(
            "test-operation",
            com.github.javydreamercsw.management.service.sync.base.SyncDirection.INBOUND);

    // Then
    assertTrue(result.isSuccess());
    assertEquals(1, result.getSyncedCount());
    verify(npcService, times(1)).save(any(Npc.class));
  }
}
