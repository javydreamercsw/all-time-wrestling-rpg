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
package com.github.javydreamercsw.management.controller.sync;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.github.javydreamercsw.base.config.NotionSyncProperties;
import com.github.javydreamercsw.management.controller.AbstractControllerTest;
import com.github.javydreamercsw.management.service.sync.EntityDependencyAnalyzer;
import com.github.javydreamercsw.management.service.sync.NotionSyncScheduler;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import com.github.javydreamercsw.management.service.sync.SyncEntityType;
import com.github.javydreamercsw.management.service.sync.base.SyncDirection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NotionSyncControllerTest extends AbstractControllerTest {

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
    when(dependencyAnalyzer.getAutomaticSyncOrder())
        .thenReturn(List.of(SyncEntityType.SHOWS, SyncEntityType.WRESTLERS));
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
        .andExpect(jsonPath("$.entities[0]").value(SyncEntityType.SHOWS.getKey()))
        .andExpect(jsonPath("$.entities[1]").value(SyncEntityType.WRESTLERS.getKey()))
        .andExpect(jsonPath("$.backupEnabled").value(true));
  }

  @Test
  @DisplayName("Should trigger manual sync successfully")
  @WithMockUser(roles = "ADMIN")
  void shouldTriggerManualSyncSuccessfully() throws Exception {
    // Given
    List<NotionSyncService.SyncResult> results =
        List.of(NotionSyncService.SyncResult.success(SyncEntityType.SHOWS.getKey(), 5, 0, 0));
    when(notionSyncScheduler.triggerManualSync()).thenReturn(results);

    // When & Then
    mockMvc
        .perform(post("/api/sync/notion/trigger").with(csrf()))
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
  @WithMockUser(roles = "ADMIN")
  void shouldHandleManualSyncFailures() throws Exception {
    // Given
    List<NotionSyncService.SyncResult> results =
        List.of(
            NotionSyncService.SyncResult.success(SyncEntityType.SHOWS.getKey(), 3, 0, 0),
            NotionSyncService.SyncResult.failure(
                SyncEntityType.WRESTLERS.getKey(), "Connection error"));
    when(notionSyncScheduler.triggerManualSync()).thenReturn(results);

    // When & Then
    mockMvc
        .perform(post("/api/sync/notion/trigger").with(csrf()))
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
  @WithMockUser(roles = "ADMIN")
  void shouldTriggerEntitySyncSuccessfully() throws Exception {
    // Given
    when(dependencyAnalyzer.getAutomaticSyncOrder())
        .thenReturn(List.of(SyncEntityType.SHOWS, SyncEntityType.WRESTLERS));
    when(notionSyncScheduler.triggerEntitySync(SyncEntityType.SHOWS.getKey()))
        .thenReturn(NotionSyncService.SyncResult.success(SyncEntityType.SHOWS.getKey(), 8, 0, 0));

    // When & Then
    mockMvc
        .perform(post("/api/sync/notion/trigger/Shows").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.entity").value(SyncEntityType.SHOWS.getKey()))
        .andExpect(jsonPath("$.itemsSynced").value(8))
        .andExpect(jsonPath("$.errorCount").value(0));
  }

  @Test
  @DisplayName("Should reject invalid entity name")
  @WithMockUser(roles = "ADMIN")
  void shouldRejectInvalidEntityName() throws Exception {
    // Given
    when(dependencyAnalyzer.getAutomaticSyncOrder())
        .thenReturn(
            List.of(
                com.github.javydreamercsw.management.service.sync.SyncEntityType.SHOWS,
                com.github.javydreamercsw.management.service.sync.SyncEntityType.WRESTLERS));

    // When & Then
    mockMvc
        .perform(post("/api/sync/notion/trigger/invalid").with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("Invalid entity name: invalid"))
        .andExpect(jsonPath("$.validEntities[0]").value(SyncEntityType.SHOWS.getKey()))
        .andExpect(jsonPath("$.validEntities[1]").value(SyncEntityType.WRESTLERS.getKey()));
  }

  @Test
  @DisplayName("Should sync shows successfully")
  @WithMockUser(roles = "ADMIN")
  void shouldSyncShowsSuccessfully() throws Exception {
    // Given
    when(notionSyncService.syncShows(anyString(), any(SyncDirection.class)))
        .thenReturn(NotionSyncService.SyncResult.success(SyncEntityType.SHOWS.getKey(), 12, 0, 0));

    // When & Then
    mockMvc
        .perform(post("/api/sync/notion/shows").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.showsSynced").value(12))
        .andExpect(jsonPath("$.errorCount").value(0));
  }

  @Test
  @DisplayName("Should handle shows sync failure")
  @WithMockUser(roles = "ADMIN")
  void shouldHandleShowsSyncFailure() throws Exception {
    // Given
    when(notionSyncService.syncShows(anyString(), any(SyncDirection.class)))
        .thenReturn(NotionSyncService.SyncResult.failure("Shows", "Database connection failed"));

    // When & Then
    mockMvc
        .perform(post("/api/sync/notion/shows").with(csrf()))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorMessage").value("Database connection failed"));
  }

  @Test
  @DisplayName("Should return supported entities")
  @WithMockUser(roles = "ADMIN")
  void shouldReturnSupportedEntities() throws Exception {
    // Given
    when(dependencyAnalyzer.getAutomaticSyncOrder())
        .thenReturn(
            List.of(
                com.github.javydreamercsw.management.service.sync.SyncEntityType.SHOWS,
                com.github.javydreamercsw.management.service.sync.SyncEntityType.WRESTLERS));

    // When & Then
    mockMvc
        .perform(get("/api/sync/notion/entities"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.configuredEntities[0]").value("Shows"))
        .andExpect(jsonPath("$.configuredEntities[1]").value("Wrestlers"))
        .andExpect(jsonPath("$.syncEntities").isArray())
        .andExpect(jsonPath("$.description").isMap());
  }

  @Test
  @DisplayName("Should return health status")
  @WithMockUser(roles = "ADMIN")
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
