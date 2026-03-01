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
import com.github.javydreamercsw.management.domain.faction.FactionRivalry;
import com.github.javydreamercsw.management.service.sync.AbstractSyncTest;
import java.time.Instant;
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

class FactionRivalryNotionSyncServiceTest extends AbstractSyncTest {
  private FactionRivalryNotionSyncService factionRivalryNotionSyncService;
  @Captor private ArgumentCaptor<FactionRivalry> factionRivalryCaptor;

  @BeforeEach
  protected void setUp() {
    super.setUp();
    factionRivalryNotionSyncService =
        new FactionRivalryNotionSyncService(
            factionRivalryRepository, notionHandler, progressTracker);
  }

  @Test
  @DisplayName("Test Sync to Notion with a single new Faction Rivalry")
  void testSyncToNotionSingleNew() {
    // Given
    String operationId = UUID.randomUUID().toString();

    Faction f1 = new Faction();
    f1.setName("Faction 1");
    f1.setExternalId(UUID.randomUUID().toString());

    Faction f2 = new Faction();
    f2.setName("Faction 2");
    f2.setExternalId(UUID.randomUUID().toString());

    FactionRivalry rivalry = new FactionRivalry();
    rivalry.setFaction1(f1);
    rivalry.setFaction2(f2);
    rivalry.setHeat(25);
    rivalry.setIsActive(true);
    rivalry.setStartedDate(Instant.now());
    rivalry.setStorylineNotes("Faction war");

    NotionClient client = mock(NotionClient.class);

    when(factionRivalryRepository.findAll()).thenReturn(List.of(rivalry));
    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("test_db_id");

    Page page = mock(Page.class);

    when(page.getId()).thenReturn(UUID.randomUUID().toString());
    when(notionHandler.executeWithRetry(any())).thenReturn(page);

    // When
    var result = factionRivalryNotionSyncService.syncToNotion(operationId);

    // Then
    assertNotNull(result);
    assertEquals(1, result.getCreatedCount());
    assertEquals(0, result.getUpdatedCount());
    assertEquals(0, result.getErrorCount());
    verify(factionRivalryRepository, times(1)).save(factionRivalryCaptor.capture());
    FactionRivalry savedRivalry = factionRivalryCaptor.getValue();
    assertNotNull(savedRivalry.getExternalId());
    assertNotNull(savedRivalry.getLastSync());
  }

  @Test
  @DisplayName("Test Sync to Notion with no Faction Rivalries")
  void testSyncToNotionEmpty() {
    // Given
    String operationId = UUID.randomUUID().toString();

    when(factionRivalryRepository.findAll()).thenReturn(Collections.emptyList());

    NotionClient client = mock(NotionClient.class);

    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("test_db_id");

    // When
    var result = factionRivalryNotionSyncService.syncToNotion(operationId);

    // Then
    assertNotNull(result);
    assertEquals(0, result.getCreatedCount());
    assertEquals(0, result.getUpdatedCount());
    assertEquals(0, result.getErrorCount());
    verify(factionRivalryRepository, times(0)).save(any());
  }
}
