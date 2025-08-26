package com.github.javydreamercsw.management.service.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.config.NotionSyncProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotionSyncSchedulerTest {

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
    when(notionSyncService.syncShows(anyString()))
        .thenReturn(NotionSyncService.SyncResult.success("Shows", 5, 0));
    when(notionSyncService.syncWrestlers(anyString()))
        .thenReturn(NotionSyncService.SyncResult.success("Wrestlers", 3, 0));

    // When
    notionSyncScheduler.performScheduledSync();

    // Then
    verify(notionSyncService).syncShows(anyString()); // Now accepts operation ID
    verify(notionSyncService).syncWrestlers(anyString()); // Now accepts operation ID
  }

  @Test
  @DisplayName("Should handle sync failures gracefully")
  void shouldHandleSyncFailuresGracefully() {
    // Given
    when(syncProperties.isSchedulerEnabled()).thenReturn(true);
    when(dependencyAnalyzer.getAutomaticSyncOrder()).thenReturn(List.of("shows"));
    when(notionSyncService.syncShows(anyString()))
        .thenReturn(NotionSyncService.SyncResult.failure("Shows", "Connection failed"));

    // When & Then - Should not throw exception
    assertDoesNotThrow(() -> notionSyncScheduler.performScheduledSync());

    verify(notionSyncService).syncShows(anyString()); // Now accepts operation ID
  }

  @Test
  @DisplayName("Should trigger manual sync for all entities")
  void shouldTriggerManualSyncForAllEntities() {
    // Given
    when(dependencyAnalyzer.getAutomaticSyncOrder()).thenReturn(List.of("shows"));
    when(notionSyncService.syncShows(anyString()))
        .thenReturn(NotionSyncService.SyncResult.success("Shows", 10, 0));

    // When
    List<NotionSyncService.SyncResult> results = notionSyncScheduler.triggerManualSync();

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
    when(notionSyncService.syncShows(anyString()))
        .thenReturn(NotionSyncService.SyncResult.success("Shows", 7, 0));

    // When
    NotionSyncService.SyncResult result = notionSyncScheduler.triggerEntitySync("shows");

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
    NotionSyncService.SyncResult result = notionSyncScheduler.triggerEntitySync("unknown");

    // Then
    assertNotNull(result);
    assertFalse(result.isSuccess());
    assertEquals("unknown", result.getEntityType());
    assertEquals("Unknown entity type", result.getErrorMessage());
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
