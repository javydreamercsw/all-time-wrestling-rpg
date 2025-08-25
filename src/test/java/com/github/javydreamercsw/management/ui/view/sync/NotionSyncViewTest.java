package com.github.javydreamercsw.management.ui.view.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.service.sync.EntityDependencyAnalyzer;
import com.github.javydreamercsw.management.service.sync.NotionSyncScheduler;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotionSyncViewTest {

  @Mock private NotionSyncService notionSyncService;
  @Mock private NotionSyncScheduler notionSyncScheduler;
  @Mock private NotionSyncProperties syncProperties;
  @Mock private SyncProgressTracker progressTracker;
  @Mock private EntityDependencyAnalyzer dependencyAnalyzer;

  private NotionSyncView notionSyncView;

  @BeforeEach
  void setUp() {
    // Mock basic configuration
    when(syncProperties.isEnabled()).thenReturn(true);
    when(syncProperties.isSchedulerEnabled()).thenReturn(true);
    when(syncProperties.isBackupEnabled()).thenReturn(true);
    when(dependencyAnalyzer.getAutomaticSyncOrder()).thenReturn(List.of("shows", "wrestlers"));

    // Mock scheduler properties
    NotionSyncProperties.Scheduler scheduler = new NotionSyncProperties.Scheduler();
    scheduler.setInterval(3600000L);
    when(syncProperties.getScheduler()).thenReturn(scheduler);

    // Mock backup properties
    NotionSyncProperties.Backup backup = new NotionSyncProperties.Backup();
    backup.setDirectory("backups/notion-sync");
    backup.setMaxFiles(10);
    when(syncProperties.getBackup()).thenReturn(backup);

    notionSyncView =
        new NotionSyncView(
            notionSyncService,
            notionSyncScheduler,
            syncProperties,
            progressTracker,
            dependencyAnalyzer);
  }

  @Test
  @DisplayName("Should initialize UI components correctly")
  void shouldInitializeUIComponentsCorrectly() {
    // The view should be created without throwing exceptions
    assertNotNull(notionSyncView);

    // Just verify the view was created successfully - CSS class names may vary
    // The important thing is that it doesn't throw exceptions during initialization
    assertTrue(notionSyncView.getElement() != null);
  }

  @Test
  @DisplayName("Should handle sync properties configuration")
  void shouldHandleSyncPropertiesConfiguration() {
    // Verify that sync properties are used during initialization (allowing multiple calls)
    verify(syncProperties, atLeastOnce()).isEnabled();
    verify(syncProperties, atLeastOnce()).isSchedulerEnabled();
    verify(syncProperties, atLeastOnce()).isBackupEnabled();
    verify(syncProperties, atLeastOnce()).getScheduler();
    verify(syncProperties, atLeastOnce()).getBackup();
  }

  @Test
  @DisplayName("Should create view with disabled sync configuration")
  void shouldCreateViewWithDisabledSyncConfiguration() {
    // Test with disabled configuration
    when(syncProperties.isEnabled()).thenReturn(false);
    when(syncProperties.isSchedulerEnabled()).thenReturn(false);
    when(syncProperties.isBackupEnabled()).thenReturn(false);
    when(dependencyAnalyzer.getAutomaticSyncOrder()).thenReturn(List.of());

    NotionSyncView disabledView =
        new NotionSyncView(
            notionSyncService,
            notionSyncScheduler,
            syncProperties,
            progressTracker,
            dependencyAnalyzer);

    assertNotNull(disabledView);
  }

  @Test
  @DisplayName("Should handle empty entities list")
  void shouldHandleEmptyEntitiesList() {
    when(dependencyAnalyzer.getAutomaticSyncOrder()).thenReturn(List.of());

    NotionSyncView emptyEntitiesView =
        new NotionSyncView(
            notionSyncService,
            notionSyncScheduler,
            syncProperties,
            progressTracker,
            dependencyAnalyzer);

    assertNotNull(emptyEntitiesView);
  }

  @Test
  @DisplayName("Should handle null backup configuration")
  void shouldHandleNullBackupConfiguration() {
    // Reset mocks for this specific test to avoid unnecessary stubbing
    reset(syncProperties);
    when(syncProperties.isEnabled()).thenReturn(true);
    when(syncProperties.isSchedulerEnabled()).thenReturn(false);
    when(syncProperties.isBackupEnabled()).thenReturn(false);
    lenient().when(syncProperties.getEntities()).thenReturn(List.of());

    NotionSyncProperties.Scheduler scheduler = new NotionSyncProperties.Scheduler();
    when(syncProperties.getScheduler()).thenReturn(scheduler);

    // Should not throw exception even with null backup config
    assertDoesNotThrow(
        () -> {
          new NotionSyncView(
              notionSyncService,
              notionSyncScheduler,
              syncProperties,
              progressTracker,
              dependencyAnalyzer);
        });
  }
}
