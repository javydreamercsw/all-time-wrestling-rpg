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

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
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

class WrestlerNotionSyncServiceTest extends AbstractSyncTest {

  private WrestlerNotionSyncService wrestlerNotionSyncService;
  @Captor private ArgumentCaptor<Wrestler> wrestlerCaptor;

  @BeforeEach
  public void setUp() {
    super.setUp();
    wrestlerNotionSyncService =
        new WrestlerNotionSyncService(
            wrestlerRepository, syncServiceDependencies, notionApiExecutor);
  }

  @Test
  @DisplayName("Sync new wrestler creates Notion page and saves external ID")
  void syncToNotion_newWrestler_createsPageAndSavesExternalId() {
    String operationId = UUID.randomUUID().toString();

    Wrestler wrestler = new Wrestler();
    wrestler.setName("Test Wrestler");
    wrestler.setGender(Gender.MALE);
    wrestler.setStartingHealth(100);
    wrestler.setStartingStamina(100);

    NotionClient client = mock(NotionClient.class);
    when(wrestlerRepository.findAll()).thenReturn(List.of(wrestler));
    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("db_wrestlers");

    Page page = mock(Page.class);
    when(page.getId()).thenReturn(UUID.randomUUID().toString());
    when(notionHandler.executeWithRetry(any())).thenReturn(page);

    var result = wrestlerNotionSyncService.syncToNotion(operationId);

    assertNotNull(result);
    assertEquals(1, result.getCreatedCount());
    assertEquals(0, result.getErrorCount());
    verify(wrestlerRepository, times(1)).saveAndFlush(wrestlerCaptor.capture());
    assertNotNull(wrestlerCaptor.getValue().getExternalId());
  }

  @Test
  @DisplayName("Sync with no wrestlers returns zero counts")
  void syncToNotion_empty_returnsZeroCounts() {
    String operationId = UUID.randomUUID().toString();

    NotionClient client = mock(NotionClient.class);
    when(wrestlerRepository.findAll()).thenReturn(Collections.emptyList());
    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("db_wrestlers");

    var result = wrestlerNotionSyncService.syncToNotion(operationId);

    assertNotNull(result);
    assertEquals(0, result.getCreatedCount());
    assertEquals(0, result.getUpdatedCount());
    assertEquals(0, result.getErrorCount());
  }

  @Test
  @DisplayName("Wrestler with optional stats synced without error")
  void syncToNotion_wrestlerWithOptionalStats_syncedSuccessfully() {
    String operationId = UUID.randomUUID().toString();

    Wrestler wrestler = new Wrestler();
    wrestler.setName("Brawler");
    wrestler.setGender(Gender.MALE);
    wrestler.setBrawl(80);
    wrestler.setCharisma(60);
    wrestler.setDrive(70);
    wrestler.setResilience(75);

    NotionClient client = mock(NotionClient.class);
    when(wrestlerRepository.findAll()).thenReturn(List.of(wrestler));
    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("db_wrestlers");

    Page page = mock(Page.class);
    when(page.getId()).thenReturn(UUID.randomUUID().toString());
    when(notionHandler.executeWithRetry(any())).thenReturn(page);

    var result = wrestlerNotionSyncService.syncToNotion(operationId);

    assertNotNull(result);
    assertEquals(1, result.getCreatedCount());
  }
}
