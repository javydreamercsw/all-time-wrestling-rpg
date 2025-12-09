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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.WrestlerPage;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.sync.AbstractSyncTest;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.SyncSessionManager;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/**
 * Unit tests for WrestlerSyncService covering wrestler synchronization including stats,
 * relationships, and error handling.
 */
class WrestlerSyncServiceTest extends AbstractSyncTest {

  private WrestlerSyncService wrestlerSyncService;
  @Mock private NotionHandler notionHandler;
  @Mock private SyncServiceDependencies syncServiceDependencies;
  @Mock private SyncSessionManager syncSessionManager;

  @BeforeEach
  @Override
  protected void setUp() {
    super.setUp(); // Call parent setup first
    lenient().when(notionApiExecutor.getSyncProperties()).thenReturn(syncProperties);
    lenient().when(notionApiExecutor.getNotionHandler()).thenReturn(notionHandler);
    lenient()
        .when(notionApiExecutor.executeWithRateLimit(any()))
        .thenAnswer(
            invocation -> invocation.getArgument(0, java.util.function.Supplier.class).get());
    lenient().when(syncServiceDependencies.getNotionHandler()).thenReturn(notionHandler);
    lenient().when(syncServiceDependencies.getSyncSessionManager()).thenReturn(syncSessionManager);
    lenient().when(syncServiceDependencies.getProgressTracker()).thenReturn(progressTracker);
    lenient().when(syncServiceDependencies.getHealthMonitor()).thenReturn(healthMonitor);
    lenient().when(syncProperties.isEntityEnabled("wrestlers")).thenReturn(true);
    wrestlerSyncService =
        new WrestlerSyncService(
            objectMapper,
            syncServiceDependencies,
            wrestlerRepository,
            notionApiExecutor,
            notionPageDataExtractor);
  }

  @Test
  void syncWrestlers_WhenSuccessful_ShouldReturnCorrectResult() {
    // Given
    List<WrestlerPage> mockPages = createMockWrestlerPages();
    when(notionHandler.loadAllWrestlers()).thenReturn(mockPages);
    when(wrestlerRepository.saveAndFlush(any(Wrestler.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    SyncResult result = wrestlerSyncService.syncWrestlers("test-operation");

    // Then
    assertTrue(result.isSuccess());
    assertEquals("Wrestlers", result.getEntityType());
    verify(wrestlerRepository, times(2)).saveAndFlush(any(Wrestler.class));
    verify(healthMonitor).recordSuccess(eq("Wrestlers"), anyLong(), anyInt());
  }

  @Test
  void syncWrestlers_WhenDisabled_ShouldSkipSync() {
    // Given
    when(syncProperties.isEntityEnabled("wrestlers")).thenReturn(false);

    // When
    SyncResult result = wrestlerSyncService.syncWrestlers("test-operation");

    // Then
    assertTrue(result.isSuccess());
    verify(notionHandler, never()).loadAllWrestlers();
  }

  @Test
  void syncWrestlers_WhenNoWrestlersFound_ShouldReturnSuccess() {
    // Given
    when(notionHandler.loadAllWrestlers()).thenReturn(Collections.emptyList());

    // When
    SyncResult result = wrestlerSyncService.syncWrestlers("test-operation");

    // Then
    assertTrue(result.isSuccess());
    verify(wrestlerRepository, never()).save(any(Wrestler.class));
  }

  private List<WrestlerPage> createMockWrestlerPages() {
    WrestlerPage wrestler1 = createMockWrestlerPage("wrestler-1", "John Cena", 85, 90, 95);
    WrestlerPage wrestler2 = createMockWrestlerPage("wrestler-2", "The Rock", 90, 95, 85);
    return Arrays.asList(wrestler1, wrestler2);
  }

  private WrestlerPage createMockWrestlerPage(
      String id, String name, int health, int stamina, int charisma) {
    WrestlerPage page = mock(WrestlerPage.class);
    lenient().when(page.getId()).thenReturn(id);

    Map<String, Object> properties = new HashMap<>();
    properties.put("Name", Map.of("title", List.of(Map.of("text", Map.of("content", name)))));
    properties.put("Health", health);
    properties.put("Stamina", stamina);
    properties.put("Charisma", charisma);
    lenient().when(page.getRawProperties()).thenReturn(properties);

    return page;
  }
}
