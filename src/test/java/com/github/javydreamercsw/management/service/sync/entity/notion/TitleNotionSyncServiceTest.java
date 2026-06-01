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

import com.github.javydreamercsw.management.domain.title.Title;
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

class TitleNotionSyncServiceTest extends AbstractSyncTest {

  private TitleNotionSyncService titleNotionSyncService;
  @Captor private ArgumentCaptor<Title> titleCaptor;

  @BeforeEach
  public void setUp() {
    super.setUp();
    titleNotionSyncService =
        new TitleNotionSyncService(titleRepository, syncServiceDependencies, notionApiExecutor);
  }

  @Test
  @DisplayName("Sync new title creates Notion page and saves external ID")
  void syncToNotion_newTitle_createsPageAndSavesExternalId() {
    String operationId = UUID.randomUUID().toString();

    Title title = new Title();
    title.setName("World Championship");
    title.setIsActive(true);

    NotionClient client = mock(NotionClient.class);
    when(titleRepository.findAll()).thenReturn(List.of(title));
    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("db_championships");

    Page page = mock(Page.class);
    when(page.getId()).thenReturn(UUID.randomUUID().toString());
    when(notionHandler.executeWithRetry(any())).thenReturn(page);

    var result = titleNotionSyncService.syncToNotion(operationId);

    assertNotNull(result);
    assertEquals(1, result.getCreatedCount());
    assertEquals(0, result.getErrorCount());
    verify(titleRepository, times(1)).saveAndFlush(titleCaptor.capture());
    assertNotNull(titleCaptor.getValue().getExternalId());
  }

  @Test
  @DisplayName("Sync with no titles returns zero counts")
  void syncToNotion_empty_returnsZeroCounts() {
    String operationId = UUID.randomUUID().toString();

    NotionClient client = mock(NotionClient.class);
    when(titleRepository.findAll()).thenReturn(Collections.emptyList());
    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("db_championships");

    var result = titleNotionSyncService.syncToNotion(operationId);

    assertNotNull(result);
    assertEquals(0, result.getCreatedCount());
    assertEquals(0, result.getUpdatedCount());
    assertEquals(0, result.getErrorCount());
  }

  @Test
  @DisplayName("Title with optional fields synced without error")
  void syncToNotion_titleWithOptionalFields_syncedSuccessfully() {
    String operationId = UUID.randomUUID().toString();

    Title title = new Title();
    title.setName("Intercontinental Championship");
    title.setIsActive(true);
    title.setDescription("Mid-card title");
    title.setDefenseFrequencyType(
        com.github.javydreamercsw.management.domain.title.DefenseFrequencyType.BI_WEEKLY);

    NotionClient client = mock(NotionClient.class);
    when(titleRepository.findAll()).thenReturn(List.of(title));
    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("db_championships");

    Page page = mock(Page.class);
    when(page.getId()).thenReturn(UUID.randomUUID().toString());
    when(notionHandler.executeWithRetry(any())).thenReturn(page);

    var result = titleNotionSyncService.syncToNotion(operationId);

    assertNotNull(result);
    assertEquals(1, result.getCreatedCount());
  }
}
