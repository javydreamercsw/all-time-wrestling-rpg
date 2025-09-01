package com.github.javydreamercsw.management.controller.sync;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.service.sync.EntityDependencyAnalyzer;
import com.github.javydreamercsw.management.service.sync.NotionSyncScheduler;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotionSyncController.class)
@TestPropertySource(properties = {"notion.sync.enabled=true"})
class NotionSyncControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private NotionSyncService notionSyncService;
  @MockitoBean private NotionSyncScheduler notionSyncScheduler;
  @MockitoBean private NotionSyncProperties syncProperties;
  @MockitoBean private EntityDependencyAnalyzer dependencyAnalyzer;

  @Test
  @DisplayName("Should return sync status")
  void shouldReturnSyncStatus() throws Exception {
    // Given
    when(syncProperties.isEnabled()).thenReturn(true);
    when(syncProperties.isSchedulerEnabled()).thenReturn(true);
    when(syncProperties.isBackupEnabled()).thenReturn(true);
    when(dependencyAnalyzer.getAutomaticSyncOrder()).thenReturn(List.of("shows", "wrestlers"));
    when(syncProperties.getScheduler()).thenReturn(createMockScheduler());
    when(syncProperties.getBackup()).thenReturn(createMockBackup());
    when(notionSyncScheduler.getSyncStatus()).thenReturn("Mock status");

    // When & Then
    mockMvc
        .perform(get("/api/sync/notion/status"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.enabled").value(true))
        .andExpect(jsonPath("$.schedulerEnabled").value(true))
        .andExpect(jsonPath("$.entities[0]").value("shows"))
        .andExpect(jsonPath("$.entities[1]").value("wrestlers"))
        .andExpect(jsonPath("$.backupEnabled").value(true));
  }

  @Test
  @DisplayName("Should trigger manual sync successfully")
  void shouldTriggerManualSyncSuccessfully() throws Exception {
    // Given
    List<NotionSyncService.SyncResult> results =
        List.of(NotionSyncService.SyncResult.success("Shows", 5, 0));
    when(notionSyncScheduler.triggerManualSync()).thenReturn(results);

    // When & Then
    mockMvc
        .perform(post("/api/sync/notion/trigger"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.totalEntities").value(1))
        .andExpect(jsonPath("$.successfulSyncs").value(1))
        .andExpect(jsonPath("$.failedSyncs").value(0))
        .andExpect(jsonPath("$.totalItemsSynced").value(5));
  }

  @Test
  @DisplayName("Should handle manual sync failures")
  void shouldHandleManualSyncFailures() throws Exception {
    // Given
    List<NotionSyncService.SyncResult> results =
        List.of(
            NotionSyncService.SyncResult.success("Shows", 3, 0),
            NotionSyncService.SyncResult.failure("Wrestlers", "Connection error"));
    when(notionSyncScheduler.triggerManualSync()).thenReturn(results);

    // When & Then
    mockMvc
        .perform(post("/api/sync/notion/trigger"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.totalEntities").value(2))
        .andExpect(jsonPath("$.successfulSyncs").value(1))
        .andExpect(jsonPath("$.failedSyncs").value(1))
        .andExpect(jsonPath("$.totalItemsSynced").value(3));
  }

  @Test
  @DisplayName("Should trigger entity sync successfully")
  void shouldTriggerEntitySyncSuccessfully() throws Exception {
    // Given
    when(dependencyAnalyzer.getAutomaticSyncOrder()).thenReturn(List.of("shows", "wrestlers"));
    when(notionSyncScheduler.triggerEntitySync("shows"))
        .thenReturn(NotionSyncService.SyncResult.success("Shows", 8, 0));

    // When & Then
    mockMvc
        .perform(post("/api/sync/notion/trigger/shows"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.entity").value("Shows"))
        .andExpect(jsonPath("$.itemsSynced").value(8))
        .andExpect(jsonPath("$.errorCount").value(0));
  }

  @Test
  @DisplayName("Should reject invalid entity name")
  void shouldRejectInvalidEntityName() throws Exception {
    // Given
    when(dependencyAnalyzer.getAutomaticSyncOrder()).thenReturn(List.of("shows", "wrestlers"));

    // When & Then
    mockMvc
        .perform(post("/api/sync/notion/trigger/invalid"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("Invalid entity name: invalid"))
        .andExpect(jsonPath("$.validEntities[0]").value("shows"))
        .andExpect(jsonPath("$.validEntities[1]").value("wrestlers"));
  }

  @Test
  @DisplayName("Should sync shows successfully")
  void shouldSyncShowsSuccessfully() throws Exception {
    // Given
    when(notionSyncService.syncShows())
        .thenReturn(NotionSyncService.SyncResult.success("Shows", 12, 0));

    // When & Then
    mockMvc
        .perform(post("/api/sync/notion/shows"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.showsSynced").value(12))
        .andExpect(jsonPath("$.errorCount").value(0));
  }

  @Test
  @DisplayName("Should handle shows sync failure")
  void shouldHandleShowsSyncFailure() throws Exception {
    // Given
    when(notionSyncService.syncShows())
        .thenReturn(NotionSyncService.SyncResult.failure("Shows", "Database connection failed"));

    // When & Then
    mockMvc
        .perform(post("/api/sync/notion/shows"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorMessage").value("Database connection failed"));
  }

  @Test
  @DisplayName("Should return supported entities")
  void shouldReturnSupportedEntities() throws Exception {
    // Given
    when(dependencyAnalyzer.getAutomaticSyncOrder()).thenReturn(List.of("shows", "wrestlers"));

    // When & Then
    mockMvc
        .perform(get("/api/sync/notion/entities"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.configuredEntities[0]").value("shows"))
        .andExpect(jsonPath("$.configuredEntities[1]").value("wrestlers"))
        .andExpect(jsonPath("$.syncEntities").isArray())
        .andExpect(jsonPath("$.description").isMap());
  }

  @Test
  @DisplayName("Should return health status")
  void shouldReturnHealthStatus() throws Exception {
    // Given
    when(syncProperties.isEnabled()).thenReturn(true);
    when(syncProperties.isSchedulerEnabled()).thenReturn(false);

    // When & Then
    mockMvc
        .perform(get("/api/sync/notion/health"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.syncEnabled").value(true))
        .andExpect(jsonPath("$.schedulerEnabled").value(false))
        .andExpect(jsonPath("$.timestamp").isNumber());
  }

  private NotionSyncProperties.Scheduler createMockScheduler() {
    NotionSyncProperties.Scheduler scheduler = new NotionSyncProperties.Scheduler();
    scheduler.setInterval(3600000L);
    return scheduler;
  }

  private NotionSyncProperties.Backup createMockBackup() {
    NotionSyncProperties.Backup backup = new NotionSyncProperties.Backup();
    backup.setDirectory("backups/notion-sync");
    backup.setMaxFiles(10);
    return backup;
  }
}
