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

import com.github.javydreamercsw.base.test.BaseTest;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.service.sync.EntityDependencyAnalyzer;
import com.github.javydreamercsw.management.service.sync.NotionSyncScheduler;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotionSyncSchedulerTest extends BaseTest {

  @Mock private NotionSyncService notionSyncService;

  @Mock private NotionSyncProperties syncProperties;
  @Mock private EntityDependencyAnalyzer dependencyAnalyzer;

  private NotionSyncScheduler notionSyncScheduler;

  @BeforeEach
  void setUp() {
    notionSyncScheduler =
        new NotionSyncScheduler(notionSyncService, syncProperties, dependencyAnalyzer);
  }

  @Test
  @DisplayName("Should skip scheduled sync when scheduler is disabled")
  void shouldSkipScheduledSyncWhenSchedulerDisabled() {
    // Given
    when(syncProperties.isSchedulerEnabled()).thenReturn(false);

    // When
    notionSyncScheduler.performScheduledSync();

    // Then
    verify(syncProperties).isSchedulerEnabled();
    verifyNoInteractions(notionSyncService);
  }

  @Test
  @DisplayName("Should perform sync for all automatically determined entities")
  void shouldPerformSyncForAllConfiguredEntities() {
    // Given
    when(syncProperties.isSchedulerEnabled()).thenReturn(true);
    when(dependencyAnalyzer.getAutomaticSyncOrder()).thenReturn(List.of("shows", "wrestlers"));
    when(notionSyncService.syncShows(anyString(), any(SyncDirection.class)))
        .thenReturn(BaseSyncService.SyncResult.success("Shows", 5, 0, 0));
    when(notionSyncService.syncWrestlers(anyString(), any(SyncDirection.class)))
        .thenReturn(BaseSyncService.SyncResult.success("Wrestlers", 3, 0, 0));

    // When
    notionSyncScheduler.performScheduledSync();

    // Then
    verify(notionSyncService)
        .syncShows(anyString(), any(SyncDirection.class)); // Now accepts operation ID
    verify(notionSyncService)
        .syncWrestlers(anyString(), any(SyncDirection.class)); // Now accepts operation ID
  }

  @Test
  @DisplayName("Should handle sync failures gracefully")
  void shouldHandleSyncFailuresGracefully() {
    // Given
    when(syncProperties.isSchedulerEnabled()).thenReturn(true);
    when(dependencyAnalyzer.getAutomaticSyncOrder()).thenReturn(List.of("shows"));
    when(notionSyncService.syncShows(anyString(), any(SyncDirection.class)))
        .thenReturn(BaseSyncService.SyncResult.failure("Shows", "Connection failed"));

    // When & Then - Should not throw exception
    assertDoesNotThrow(() -> notionSyncScheduler.performScheduledSync());

    verify(notionSyncService)
        .syncShows(anyString(), any(SyncDirection.class)); // Now accepts operation ID
  }

  @Test
  @DisplayName("Should trigger manual sync for all entities")
  void shouldTriggerManualSyncForAllEntities() {
    // Given
    when(dependencyAnalyzer.getAutomaticSyncOrder()).thenReturn(List.of("shows"));
    when(notionSyncService.syncShows(anyString(), any(SyncDirection.class)))
        .thenReturn(BaseSyncService.SyncResult.success("Shows", 10, 0, 0));

    // When
    List<BaseSyncService.SyncResult> results = notionSyncScheduler.triggerManualSync();

    // Then
    assertNotNull(results);
    assertEquals(1, results.size());
    assertTrue(results.get(0).isSuccess());
    assertEquals("Shows", results.get(0).getEntityType());
    assertEquals(10, results.get(0).getSyncedCount());
  }

  @Test
  @DisplayName("Should trigger sync for specific entity")
  void shouldTriggerSyncForSpecificEntity() {
    // Given
    when(notionSyncService.syncShows(anyString(), any(SyncDirection.class)))
        .thenReturn(BaseSyncService.SyncResult.success("Shows", 7, 0, 0));

    // When
    BaseSyncService.SyncResult result = notionSyncScheduler.triggerEntitySync("shows");

    // Then
    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals("Shows", result.getEntityType());
    assertEquals(7, result.getSyncedCount());
  }

  @Test
  @DisplayName("Should handle unknown entity type")
  void shouldHandleUnknownEntityType() {
    // When
    BaseSyncService.SyncResult result = notionSyncScheduler.triggerEntitySync("unknown");

    // Then
    assertNotNull(result);
    assertFalse(result.isSuccess());
    assertEquals("unknown", result.getEntityType());
    assertEquals("Unknown entity type", result.getErrorMessage());
  }

  @Test
  @DisplayName("Should trigger sync to notion for wrestlers and return success")
  void shouldTriggerSyncToNotionForWrestlersAndReturnSuccess() {
    // Given
    String operationId = UUID.randomUUID().toString();
    when(notionSyncService.syncWrestlers(operationId, SyncDirection.OUTBOUND))
        .thenReturn(BaseSyncService.SyncResult.success("wrestlers", 1, 0, 0));

    // When
    BaseSyncService.SyncResult result =
        notionSyncScheduler.triggerEntitySyncToNotion("wrestlers", operationId);

    // Then
    verify(notionSyncService).syncWrestlers(operationId, SyncDirection.OUTBOUND);
    verify(syncProperties).setLastSyncTime(eq("wrestlers"), any(LocalDateTime.class));
    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals("wrestlers", result.getEntityType());
    assertEquals(1, result.getSyncedCount());
  }

  @Test
  @DisplayName("Should handle unknown entity type for sync to Notion")
  void shouldHandleUnknownEntityTypeForSyncToNotion() {
    // When
    BaseSyncService.SyncResult result =
        notionSyncScheduler.triggerEntitySyncToNotion("unknown", UUID.randomUUID().toString());

    // Then
    assertNotNull(result);
    assertFalse(result.isSuccess());
    assertEquals("unknown", result.getEntityType());
    assertEquals("Unknown entity type for sync to Notion", result.getErrorMessage());
    verifyNoInteractions(notionSyncService);
  }

  @Test
  @DisplayName("Should return sync status information")
  void shouldReturnSyncStatusInformation() {
    // Given
    when(syncProperties.isEnabled()).thenReturn(true);
    when(syncProperties.isSchedulerEnabled()).thenReturn(true);
    when(syncProperties.getScheduler()).thenReturn(createMockScheduler());
    when(dependencyAnalyzer.getAutomaticSyncOrder()).thenReturn(List.of("shows", "wrestlers"));
    when(syncProperties.isBackupEnabled()).thenReturn(true);
    when(syncProperties.getBackup()).thenReturn(createMockBackup());

    // When
    String status = notionSyncScheduler.getSyncStatus();

    // Then
    assertNotNull(status);
    assertTrue(status.contains("Notion Sync Status:"));
    assertTrue(status.contains("Sync Enabled: true"));
    assertTrue(status.contains("Scheduler Enabled: true"));
    assertTrue(status.contains("Entities: shows, wrestlers"));
    assertTrue(status.contains("Backup Enabled: true"));
  }

  private NotionSyncProperties.Scheduler createMockScheduler() {
    NotionSyncProperties.Scheduler scheduler = new NotionSyncProperties.Scheduler();
    scheduler.setEnabled(true);
    scheduler.setInterval(3600000L);
    return scheduler;
  }

  private NotionSyncProperties.Backup createMockBackup() {
    NotionSyncProperties.Backup backup = new NotionSyncProperties.Backup();
    backup.setEnabled(true);
    backup.setDirectory("backups/notion-sync");
    backup.setMaxFiles(10);
    return backup;
  }
}
