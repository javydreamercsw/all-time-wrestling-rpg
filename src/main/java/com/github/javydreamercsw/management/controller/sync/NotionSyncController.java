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

import com.github.javydreamercsw.base.config.NotionSyncProperties;
import com.github.javydreamercsw.management.service.sync.EntityDependencyAnalyzer;
import com.github.javydreamercsw.management.service.sync.NotionSyncScheduler;
import com.github.javydreamercsw.management.service.sync.NotionSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing Notion synchronization operations. Provides endpoints to manually
 * trigger sync operations, check sync status, and manage synchronization configuration.
 */
@RestController
@RequestMapping("/api/sync/notion")
@Slf4j
@Tag(name = "Notion Sync", description = "Notion synchronization management endpoints")
@ConditionalOnProperty(name = "notion.sync.enabled", havingValue = "true", matchIfMissing = false)
public class NotionSyncController {

  private final NotionSyncService notionSyncService;
  private final NotionSyncScheduler notionSyncScheduler;
  private final NotionSyncProperties syncProperties;
  private final EntityDependencyAnalyzer dependencyAnalyzer;

  /** Constructor with optional NotionSyncScheduler for integration tests. */
  public NotionSyncController(
      NotionSyncService notionSyncService,
      @Autowired(required = false) NotionSyncScheduler notionSyncScheduler,
      NotionSyncProperties syncProperties,
      EntityDependencyAnalyzer dependencyAnalyzer) {
    this.notionSyncService = notionSyncService;
    this.notionSyncScheduler = notionSyncScheduler;
    this.syncProperties = syncProperties;
    this.dependencyAnalyzer = dependencyAnalyzer;
  }

  @Operation(
      summary = "Get sync status and configuration",
      description = "Returns the current status and configuration of Notion synchronization")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
        @ApiResponse(responseCode = "503", description = "Sync service unavailable")
      })
  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getSyncStatus() {
    try {
      Map<String, Object> status =
          Map.of(
              "enabled",
              syncProperties.isEnabled(),
              "schedulerEnabled",
              syncProperties.isSchedulerEnabled(),
              "entities",
              dependencyAnalyzer.getAutomaticSyncOrder(),
              "interval",
              syncProperties.getScheduler().getInterval(),
              "backupEnabled",
              syncProperties.isBackupEnabled(),
              "backupDirectory",
              syncProperties.getBackup().getDirectory(),
              "maxBackupFiles",
              syncProperties.getBackup().getMaxFiles(),
              "detailedStatus",
              notionSyncScheduler != null
                  ? notionSyncScheduler.getSyncStatus()
                  : "Scheduler not available");

      return ResponseEntity.ok(status);

    } catch (Exception e) {
      log.error("Failed to get sync status", e);
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("error", "Failed to retrieve sync status: " + e.getMessage()));
    }
  }

  @Operation(
      summary = "Trigger manual sync for all entities",
      description = "Manually triggers synchronization for all configured entities")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Sync completed successfully"),
        @ApiResponse(responseCode = "500", description = "Sync operation failed"),
        @ApiResponse(responseCode = "503", description = "Sync service unavailable")
      })
  @PostMapping("/trigger")
  public ResponseEntity<Map<String, Object>> triggerManualSync() {
    try {
      log.info("Manual sync triggered via REST API");

      if (notionSyncScheduler == null) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("error", "Notion sync scheduler is not available"));
      }

      List<NotionSyncService.SyncResult> results = notionSyncScheduler.triggerManualSync();

      // Calculate summary statistics
      int successCount = (int) results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
      int failureCount = results.size() - successCount;
      int totalSynced =
          results.stream().mapToInt(NotionSyncService.SyncResult::getSyncedCount).sum();

      Map<String, Object> response =
          Map.of(
              "success",
              failureCount == 0,
              "totalEntities",
              results.size(),
              "successfulSyncs",
              successCount,
              "failedSyncs",
              failureCount,
              "totalItemsSynced",
              totalSynced,
              "results",
              results);

      HttpStatus status = failureCount == 0 ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
      return ResponseEntity.status(status).body(response);

    } catch (Exception e) {
      log.error("Failed to trigger manual sync", e);
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("error", "Failed to trigger sync: " + e.getMessage()));
    }
  }

  @Operation(
      summary = "Trigger sync for a specific entity",
      description = "Manually triggers synchronization for a specific entity type")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Entity sync completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid entity name"),
        @ApiResponse(responseCode = "500", description = "Sync operation failed"),
        @ApiResponse(responseCode = "503", description = "Sync service unavailable")
      })
  @PostMapping("/trigger/{entity}")
  public ResponseEntity<Map<String, Object>> triggerEntitySync(
      @Parameter(description = "Entity name to sync (shows, wrestlers, teams, segments, templates)")
          @PathVariable
          String entity) {

    try {
      // Validate entity name against automatically determined entities
      List<String> validEntities = dependencyAnalyzer.getAutomaticSyncOrder();
      if (!validEntities.contains(entity.toLowerCase())) {
        return ResponseEntity.badRequest()
            .body(
                Map.of("error", "Invalid entity name: " + entity, "validEntities", validEntities));
      }

      log.info("Manual {} sync triggered via REST API", entity);

      if (notionSyncScheduler == null) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("error", "Notion sync scheduler is not available"));
      }

      NotionSyncService.SyncResult result = notionSyncScheduler.triggerEntitySync(entity);

      Map<String, Object> response =
          Map.of(
              "success",
              result.isSuccess(),
              "entity",
              result.getEntityType(),
              "itemsSynced",
              result.getSyncedCount(),
              "errorCount",
              result.getErrorCount(),
              "errorMessage",
              result.getErrorMessage() != null ? result.getErrorMessage() : "",
              "result",
              result);

      HttpStatus status = result.isSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
      return ResponseEntity.status(status).body(response);

    } catch (Exception e) {
      log.error("Failed to trigger {} sync", entity, e);
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("error", "Failed to trigger " + entity + " sync: " + e.getMessage()));
    }
  }

  @Operation(
      summary = "Sync shows from Notion",
      description = "Manually triggers synchronization of shows from Notion Shows database")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Shows sync completed successfully"),
        @ApiResponse(responseCode = "500", description = "Shows sync failed"),
        @ApiResponse(responseCode = "503", description = "Sync service unavailable")
      })
  @PostMapping("/shows")
  public ResponseEntity<Map<String, Object>> syncShows() {
    try {
      log.info("Manual shows sync triggered via REST API");

      NotionSyncService.SyncResult result =
          notionSyncService.syncShows(
              "manual-show-sync-" + java.util.UUID.randomUUID(),
              com.github.javydreamercsw.management.service.sync.base.SyncDirection.INBOUND);

      Map<String, Object> response =
          Map.of(
              "success",
              result.isSuccess(),
              "showsSynced",
              result.getSyncedCount(),
              "errorCount",
              result.getErrorCount(),
              "errorMessage",
              result.getErrorMessage() != null ? result.getErrorMessage() : "",
              "result",
              result);

      HttpStatus status = result.isSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
      return ResponseEntity.status(status).body(response);

    } catch (Exception e) {
      log.error("Failed to sync shows", e);
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("error", "Failed to sync shows: " + e.getMessage()));
    }
  }

  @Operation(
      summary = "Get list of supported entities",
      description = "Returns the list of entities that can be synchronized from Notion")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Entity list retrieved successfully")
      })
  @GetMapping("/entities")
  public ResponseEntity<Map<String, Object>> getSupportedEntities() {
    List<String> automaticEntities = dependencyAnalyzer.getAutomaticSyncOrder();
    Map<String, Object> response =
        Map.of(
            "syncEntities",
            automaticEntities,
            "configuredEntities",
            automaticEntities,
            "syncOrder",
            "Automatically determined based on database relationships",
            "description",
            Map.of(
                "templates",
                "Show templates and formats",
                "seasons",
                "Wrestling seasons and periods",
                "injury-types",
                "Injury types for the card game system",
                "shows",
                "Wrestling shows and events",
                "wrestlers",
                "Individual wrestlers and their profiles",
                "factions",
                "Wrestling factions and groups",
                "teams",
                "Wrestling teams and stables",
                "segments",
                "Segment records and results"));

    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Health check for sync service",
      description = "Checks if the Notion sync service is healthy and operational")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Service is healthy"),
        @ApiResponse(responseCode = "503", description = "Service is unhealthy")
      })
  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> healthCheck() {
    try {
      // Basic health check - verify configuration and service availability
      boolean healthy =
          syncProperties.isEnabled() && notionSyncService != null && notionSyncScheduler != null;

      Map<String, Object> health =
          Map.of(
              "status",
              healthy ? "UP" : "DOWN",
              "syncEnabled",
              syncProperties.isEnabled(),
              "schedulerEnabled",
              syncProperties.isSchedulerEnabled(),
              "timestamp",
              System.currentTimeMillis());

      HttpStatus status = healthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
      return ResponseEntity.status(status).body(health);

    } catch (Exception e) {
      log.error("Health check failed", e);
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(
              Map.of(
                  "status",
                  "DOWN",
                  "error",
                  e.getMessage(),
                  "timestamp",
                  System.currentTimeMillis()));
    }
  }
}
