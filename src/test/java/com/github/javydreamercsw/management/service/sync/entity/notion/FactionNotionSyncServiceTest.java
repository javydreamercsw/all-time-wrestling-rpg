/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.service.sync.AbstractSyncTest;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import notion.api.v1.NotionClient;
import notion.api.v1.model.pages.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

class FactionNotionSyncServiceTest extends AbstractSyncTest {

  private FactionNotionSyncService factionNotionSyncService;
  @Captor private ArgumentCaptor<Faction> factionCaptor;

  @BeforeEach
  public void setUp() {
    super.setUp();
    factionNotionSyncService =
        new FactionNotionSyncService(factionRepository, syncServiceDependencies, notionApiExecutor);
  }

  @Test
  @DisplayName("Sync new faction creates Notion page and saves external ID")
  void syncToNotion_newFaction_createsPageAndSavesExternalId() {
    String operationId = UUID.randomUUID().toString();

    Faction faction = new Faction();
    faction.setName("The Order");
    faction.setAlignment("HEEL");
    faction.setDescription("Villainous stable");

    NotionClient client = mock(NotionClient.class);
    when(factionRepository.findAll()).thenReturn(List.of(faction));
    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("db_factions");

    Page page = mock(Page.class);
    when(page.getId()).thenReturn(UUID.randomUUID().toString());
    when(notionHandler.executeWithRetry(any())).thenReturn(page);

    var result = factionNotionSyncService.syncToNotion(operationId);

    assertNotNull(result);
    assertEquals(1, result.getCreatedCount());
    assertEquals(0, result.getErrorCount());
    verify(factionRepository, times(1)).saveAndFlush(factionCaptor.capture());
    assertNotNull(factionCaptor.getValue().getExternalId());
  }

  @Test
  @DisplayName("Sync with no factions returns zero counts")
  void syncToNotion_empty_returnsZeroCounts() {
    String operationId = UUID.randomUUID().toString();

    NotionClient client = mock(NotionClient.class);
    when(factionRepository.findAll()).thenReturn(Collections.emptyList());
    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("db_factions");

    var result = factionNotionSyncService.syncToNotion(operationId);

    assertNotNull(result);
    assertEquals(0, result.getCreatedCount());
    assertEquals(0, result.getUpdatedCount());
    assertEquals(0, result.getErrorCount());
  }

  @Test
  @DisplayName("Faction with optional leader relation synced without error")
  void syncToNotion_factionWithLeader_syncedSuccessfully() {
    String operationId = UUID.randomUUID().toString();

    com.github.javydreamercsw.management.domain.wrestler.Wrestler leader =
        new com.github.javydreamercsw.management.domain.wrestler.Wrestler();
    leader.setName("Faction Leader");
    leader.setExternalId(UUID.randomUUID().toString());

    Faction faction = new Faction();
    faction.setName("The Coalition");
    faction.setAlignment("FACE");
    faction.setLeader(leader);

    NotionClient client = mock(NotionClient.class);
    when(factionRepository.findAll()).thenReturn(List.of(faction));
    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("db_factions");

    Page page = mock(Page.class);
    when(page.getId()).thenReturn(UUID.randomUUID().toString());
    when(notionHandler.executeWithRetry(any())).thenReturn(page);

    var result = factionNotionSyncService.syncToNotion(operationId);

    assertNotNull(result);
    assertEquals(1, result.getCreatedCount());
  }
}
