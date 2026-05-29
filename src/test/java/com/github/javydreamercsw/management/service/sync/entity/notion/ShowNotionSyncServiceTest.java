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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.sync.AbstractSyncTest;
import java.time.LocalDate;
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
import org.mockito.Mock;

class ShowNotionSyncServiceTest extends AbstractSyncTest {
  private ShowNotionSyncService showNotionSyncService;
  @Captor private ArgumentCaptor<Show> showCaptor;
  @Mock private GameSettingService gameSettingService;

  @BeforeEach
  public void setUp() {
    super.setUp();
    showNotionSyncService =
        new ShowNotionSyncService(
            showRepository, syncServiceDependencies, notionApiExecutor, gameSettingService);
  }

  @Test
  @DisplayName("Test Sync to Notion with a single new Show")
  void testSyncToNotionSingleNew() {
    // Given
    String operationId = UUID.randomUUID().toString();
    Show show = new Show();
    show.setName("Test Show");
    show.setDescription("Test Description");
    show.setShowDate(LocalDate.now());

    NotionClient client = mock(NotionClient.class);

    when(showRepository.findAll()).thenReturn(List.of(show));
    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("test_db_id");

    Page page = mock(Page.class);

    when(page.getId()).thenReturn(UUID.randomUUID().toString());
    when(notionHandler.executeWithRetry(any())).thenReturn(page);

    // When
    var result = showNotionSyncService.syncToNotion(operationId);

    // Then
    assertNotNull(result);
    assertEquals(1, result.getCreatedCount());
    assertEquals(0, result.getUpdatedCount());
    assertEquals(0, result.getErrorCount());
    verify(showRepository, times(1)).saveAndFlush(showCaptor.capture());
    Show savedShow = showCaptor.getValue();
    assertNotNull(savedShow.getExternalId());
    assertNotNull(savedShow.getLastSync());
  }

  @Test
  @DisplayName(
      "Name-based page matching is disabled for shows to prevent annual recurrences overwriting"
          + " each other")
  void testNameBasedMatchingDisabled() {
    assertFalse(
        showNotionSyncService.isNameBasedMatchingEnabled(),
        "Shows repeat annually; name-based matching must be disabled to prevent date corruption");
  }

  @Test
  @DisplayName("Test Sync to Notion with no Shows")
  void testSyncToNotionEmpty() {
    // Given
    String operationId = UUID.randomUUID().toString();

    when(showRepository.findAll()).thenReturn(Collections.emptyList());

    NotionClient client = mock(NotionClient.class);

    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("test_db_id");

    // When
    var result = showNotionSyncService.syncToNotion(operationId);

    // Then
    assertNotNull(result);
    assertEquals(0, result.getCreatedCount());
    assertEquals(0, result.getUpdatedCount());
    assertEquals(0, result.getErrorCount());
    verify(showRepository, times(0)).save(any());
  }

  @Test
  @DisplayName("Future shows are excluded from outbound sync when a game date is set")
  void testFutureShowsExcludedFromOutboundSync() {
    // Given
    LocalDate gameDate = LocalDate.of(2026, 1, 15);
    when(gameSettingService.getCurrentGameDate()).thenReturn(gameDate);

    Show pastShow = new Show();
    pastShow.setName("Past Show");
    pastShow.setShowDate(LocalDate.of(2026, 1, 10));

    Show todayShow = new Show();
    todayShow.setName("Today Show");
    todayShow.setShowDate(gameDate);

    Show futureShow = new Show();
    futureShow.setName("Future Show");
    futureShow.setShowDate(LocalDate.of(2026, 1, 20));

    NotionClient client = mock(NotionClient.class);
    when(showRepository.findAll()).thenReturn(List.of(pastShow, todayShow, futureShow));
    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("test_db_id");
    Page page = mock(Page.class);
    when(page.getId()).thenReturn(UUID.randomUUID().toString());
    when(notionHandler.executeWithRetry(any())).thenReturn(page);

    // When
    var result = showNotionSyncService.syncToNotion(UUID.randomUUID().toString());

    // Then — only pastShow and todayShow synced; futureShow skipped
    assertNotNull(result);
    assertEquals(2, result.getCreatedCount());
    assertEquals(0, result.getErrorCount());
  }

  @Test
  @DisplayName("Shows with no date and shows on or before game date are all synced")
  void testShowsWithNullDateAlwaysIncluded() {
    // Given
    LocalDate gameDate = LocalDate.of(2026, 1, 15);
    when(gameSettingService.getCurrentGameDate()).thenReturn(gameDate);

    Show nullDateShow = new Show();
    nullDateShow.setName("Undated Show");
    nullDateShow.setShowDate(null);

    Show pastShow = new Show();
    pastShow.setName("Past Show");
    pastShow.setShowDate(LocalDate.of(2025, 12, 1));

    NotionClient client = mock(NotionClient.class);
    when(showRepository.findAll()).thenReturn(List.of(nullDateShow, pastShow));
    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("test_db_id");
    Page page = mock(Page.class);
    when(page.getId()).thenReturn(UUID.randomUUID().toString());
    when(notionHandler.executeWithRetry(any())).thenReturn(page);

    // When
    var result = showNotionSyncService.syncToNotion(UUID.randomUUID().toString());

    // Then — both shows included
    assertNotNull(result);
    assertEquals(2, result.getCreatedCount());
    assertEquals(0, result.getErrorCount());
  }

  @Test
  @DisplayName("All shows synced when no game date is configured")
  void testAllShowsSyncedWhenNoGameDate() {
    // Given — game date not set
    when(gameSettingService.getCurrentGameDate()).thenReturn(null);

    Show futureShow = new Show();
    futureShow.setName("Future Show");
    futureShow.setShowDate(LocalDate.now().plusYears(1));

    NotionClient client = mock(NotionClient.class);
    when(showRepository.findAll()).thenReturn(List.of(futureShow));
    when(notionHandler.createNotionClient()).thenReturn(Optional.of(client));
    when(notionHandler.getDatabaseId(anyString())).thenReturn("test_db_id");
    Page page = mock(Page.class);
    when(page.getId()).thenReturn(UUID.randomUUID().toString());
    when(notionHandler.executeWithRetry(any())).thenReturn(page);

    // When
    var result = showNotionSyncService.syncToNotion(UUID.randomUUID().toString());

    // Then — future show still synced because no game date is configured
    assertNotNull(result);
    assertEquals(1, result.getCreatedCount());
    assertEquals(0, result.getErrorCount());
  }
}
