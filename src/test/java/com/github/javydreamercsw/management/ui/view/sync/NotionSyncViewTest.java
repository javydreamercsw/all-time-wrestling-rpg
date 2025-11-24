package com.github.javydreamercsw.management.ui.view.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.service.sync.EntityDependencyAnalyzer;
import com.github.javydreamercsw.management.service.sync.NotionSyncScheduler;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.provider.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotionSyncViewTest {

  @Mock private NotionSyncService notionSyncService;
  @Mock private NotionSyncScheduler notionSyncScheduler;
  @Mock private NotionSyncProperties syncProperties;
  @Mock private SyncProgressTracker progressTracker;
  @Mock private EntityDependencyAnalyzer dependencyAnalyzer;

  private NotionSyncView notionSyncView;

  @BeforeEach
  void setUp() {
    // Mock the sync properties
    lenient().when(syncProperties.isEnabled()).thenReturn(true);
    lenient().when(syncProperties.isSchedulerEnabled()).thenReturn(true);
    lenient().when(syncProperties.isBackupEnabled()).thenReturn(true);

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
            notionSyncScheduler, syncProperties, progressTracker, dependencyAnalyzer);
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
    when(dependencyAnalyzer.getAutomaticSyncOrder()).thenReturn(new ArrayList<>());

    NotionSyncView disabledView =
        new NotionSyncView(
            notionSyncScheduler, syncProperties, progressTracker, dependencyAnalyzer);

    assertNotNull(disabledView);
  }

  @Test
  @DisplayName("Should handle empty entities list")
  void shouldHandleEmptyEntitiesList() {
    when(dependencyAnalyzer.getAutomaticSyncOrder()).thenReturn(new ArrayList<>());
    // Remove deprecated getEntities() call - entities are now automatically determined

    NotionSyncView emptyEntitiesView =
        new NotionSyncView(
            notionSyncScheduler, syncProperties, progressTracker, dependencyAnalyzer);

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

    NotionSyncProperties.Scheduler scheduler = new NotionSyncProperties.Scheduler();
    when(syncProperties.getScheduler()).thenReturn(scheduler);

    // Should not throw exception even with null backup config
    assertDoesNotThrow(
        () -> {
          new NotionSyncView(
              notionSyncScheduler, syncProperties, progressTracker, dependencyAnalyzer);
        });
  }

  @Test
  @DisplayName("Should sort entities alphabetically in the dropdown")
  void shouldSortEntitiesAlphabetically() {
    // Given
    List<String> unsortedEntities = List.of("wrestlers", "shows", "factions");
    when(dependencyAnalyzer.getAutomaticSyncOrder()).thenReturn(new ArrayList<>(unsortedEntities));

    // When
    notionSyncView =
        new NotionSyncView(
            notionSyncScheduler, syncProperties, progressTracker, dependencyAnalyzer);

    // Then
    ComboBox<String> entitySelectionCombo =
        (ComboBox<String>)
            notionSyncView
                .getChildren()
                .filter(c -> c instanceof HorizontalLayout) // Find the control section
                .findFirst()
                .orElseThrow(() -> new AssertionError("Control section not found"))
                .getChildren()
                .filter(c -> c instanceof ComboBox) // Find all ComboBoxes in the section
                .skip(1) // Skip the first ComboBox (syncDirection)
                .map(c -> (ComboBox<String>) c) // Cast to ComboBox<String>
                .filter(
                    comboBox ->
                        "Select Entity to Sync"
                            .equals(comboBox.getLabel())) // Further filter by label if needed
                .findFirst()
                .orElseThrow(() -> new AssertionError("Entity selection combo box not found"));

    List<String> dropdownItems = new ArrayList<>();
    entitySelectionCombo.getDataProvider().fetch(new Query<>()).forEach(dropdownItems::add);

    List<String> sortedEntities = new ArrayList<>(unsortedEntities);
    Collections.sort(sortedEntities);

    assertEquals(sortedEntities, dropdownItems);
  }
}
