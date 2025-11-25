package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.test.BaseTest;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.EntityDependencyAnalyzer;
import com.github.javydreamercsw.management.service.sync.NotionSyncScheduler;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.ui.view.sync.SyncDirection;
import java.time.LocalDateTime;
import java.util.Collections;
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
  @Mock private WrestlerNotionSyncService wrestlerNotionSyncService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private TitleNotionSyncService titleNotionSyncService;

  private NotionSyncScheduler notionSyncScheduler;

  @BeforeEach
  void setUp() {
    notionSyncScheduler =
        new NotionSyncScheduler(
            notionSyncService,
            syncProperties,
            dependencyAnalyzer,
            wrestlerNotionSyncService,
            titleNotionSyncService);
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
        .thenReturn(NotionSyncService.SyncResult.success("Shows", 5, 0, 0));
    when(notionSyncService.syncWrestlers(anyString(), SyncDirection.OUTBOUND))
        .thenReturn(NotionSyncService.SyncResult.success("Wrestlers", 3, 0, 0));

    // When
    notionSyncScheduler.performScheduledSync();

    // Then
    verify(notionSyncService).syncShows(anyString()); // Now accepts operation ID
    verify(notionSyncService)
        .syncWrestlers(anyString(), SyncDirection.OUTBOUND); // Now accepts operation ID
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
        .thenReturn(NotionSyncService.SyncResult.success("Shows", 10, 0, 0));

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
    when(notionSyncService.syncShows(anyString()))
        .thenReturn(NotionSyncService.SyncResult.success("Shows", 7, 0, 0));

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
    Wrestler wrestler = new Wrestler();
    wrestler.setName("Test Wrestler");
    List<Wrestler> wrestlers = Collections.singletonList(wrestler);
    when(wrestlerRepository.findAll()).thenReturn(wrestlers);

    // When
    NotionSyncService.SyncResult result =
        notionSyncScheduler.triggerEntitySyncToNotion("wrestlers", UUID.randomUUID().toString());

    // Then
    verify(wrestlerRepository).findAll();
    verify(wrestlerNotionSyncService).syncToNotion(anyString());
    verify(syncProperties)
        .setLastSyncTime(
            eq("wrestlers"), any(LocalDateTime.class)); // Verify with any(LocalDateTime.class)
    assertNotNull(result);
    assertTrue(result.isSuccess());
    assertEquals("wrestlers", result.getEntityType());
    assertEquals(1, result.getSyncedCount());
  }

  @Test
  @DisplayName("Should handle unknown entity type for sync to Notion")
  void shouldHandleUnknownEntityTypeForSyncToNotion() {
    // When
    NotionSyncService.SyncResult result =
        notionSyncScheduler.triggerEntitySyncToNotion("unknown", UUID.randomUUID().toString());

    // Then
    assertNotNull(result);
    assertFalse(result.isSuccess());
    assertEquals("unknown", result.getEntityType());
    assertEquals("Unknown entity type for sync to Notion", result.getErrorMessage());
    verifyNoInteractions(wrestlerRepository, wrestlerNotionSyncService);
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
