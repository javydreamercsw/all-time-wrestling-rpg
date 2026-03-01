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

import com.github.javydreamercsw.base.ai.notion.NotionApiExecutor;
import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.NotionClient;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageParent;
import notion.api.v1.model.pages.PageProperty;
import notion.api.v1.request.pages.CreatePageRequest;
import notion.api.v1.request.pages.UpdatePageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
public abstract class BaseNotionSyncService<T extends AbstractEntity>
    implements NotionEntitySyncService {

  protected final JpaRepository<T, Long> repository;
  protected final SyncServiceDependencies syncServiceDependencies;
  protected final NotionApiExecutor notionApiExecutor;

  protected BaseNotionSyncService(
      JpaRepository<T, Long> repository,
      SyncServiceDependencies syncServiceDependencies,
      NotionApiExecutor notionApiExecutor) {
    this.repository = repository;
    this.syncServiceDependencies = syncServiceDependencies;
    this.notionApiExecutor = notionApiExecutor;
  }

  @Override
  @Transactional
  public BaseSyncService.SyncResult syncToNotion(@NonNull String operationId) {
    return syncToNotion(operationId, null);
  }

  @Override
  @Transactional
  public BaseSyncService.SyncResult syncToNotion(
      @NonNull String operationId, java.util.Collection<Long> ids) {
    Optional<NotionClient> clientOptional =
        notionApiExecutor.getNotionHandler().createNotionClient();
    if (clientOptional.isPresent()) {
      try (NotionClient client = clientOptional.get()) {
        String databaseId = getDatabaseId();
        if (databaseId == null) {
          String errorMsg = "Database '" + getDatabaseName() + "' not found in Notion workspace.";
          log.error(errorMsg);
          syncServiceDependencies.getProgressTracker().failOperation(operationId, errorMsg);
          return BaseSyncService.SyncResult.failure(getEntityName(), errorMsg);
        }

        int processedCount = 0;
        int created = 0;
        int updated = 0;
        int errors = 0;
        List<T> entities =
            (ids == null || ids.isEmpty()) ? repository.findAll() : repository.findAllById(ids);

        syncServiceDependencies
            .getProgressTracker()
            .addLogMessage(
                operationId, "🔍 Checking existing entries in Notion database...", "INFO");

        // Load all existing pages from Notion to avoid duplicates
        Map<String, String> notionLookup =
            notionApiExecutor.executeWithRateLimit(
                operationId,
                () ->
                    notionApiExecutor.getNotionHandler().getDatabaseNamesToIds(getDatabaseName()));

        // Fetch the database schema to know which properties are valid to send
        Set<String> validNotionProperties;
        try {
          validNotionProperties =
              notionApiExecutor.executeWithRateLimit(
                  operationId,
                  () ->
                      notionApiExecutor
                          .getNotionHandler()
                          .executeWithRetry(() -> client.retrieveDatabase(databaseId))
                          .getProperties()
                          .keySet());
        } catch (Exception e) {
          log.warn(
              "Failed to fetch schema for database '{}', falling back to sending all properties:"
                  + " {}",
              getDatabaseName(),
              e.getMessage());
          validNotionProperties = null;
        }

        syncServiceDependencies
            .getProgressTracker()
            .startOperation(operationId, "Sync " + getEntityName(), entities.size());

        for (T entity : entities) {
          // Update progress for each entity
          syncServiceDependencies
              .getProgressTracker()
              .updateProgress(
                  operationId,
                  processedCount,
                  String.format(
                      "Saving %s to Notion... (%d/%d processed)",
                      getEntityName(), processedCount, entities.size()));
          try {
            String entityDisplayName = getEntityDisplayName(entity);
            syncServiceDependencies
                .getProgressTracker()
                .addLogMessage(
                    operationId,
                    "Processing " + getEntityName() + ": " + entityDisplayName,
                    "INFO");

            String externalId = entity.getExternalId();

            // If externalId is missing locally, try to match by name from Notion lookup
            if ((externalId == null || externalId.isBlank())
                && notionLookup.containsKey(entityDisplayName)) {
              externalId = notionLookup.get(entityDisplayName);
              entity.setExternalId(externalId);
              syncServiceDependencies
                  .getProgressTracker()
                  .addLogMessage(
                      operationId,
                      "🔗 Matched existing Notion page for: " + entityDisplayName,
                      "INFO");
            }

            Map<String, PageProperty> allProperties = getProperties(entity);
            // Filter properties to only include those that exist in the Notion database schema
            Map<String, PageProperty> propertiesToSend;
            if (validNotionProperties != null) {
              propertiesToSend = new HashMap<>();
              final Set<String> validProps = validNotionProperties;
              allProperties.forEach(
                  (key, value) -> {
                    if (validProps.contains(key)) {
                      propertiesToSend.put(key, value);
                    } else {
                      log.trace(
                          "Skipping property '{}' as it is not present in Notion database '{}'",
                          key,
                          getDatabaseName());
                    }
                  });
            } else {
              propertiesToSend = allProperties;
            }

            if (externalId != null && !externalId.isBlank()) {
              log.debug("Updating existing page for: {}", getEntityName());
              final String finalId = externalId;
              UpdatePageRequest updatePageRequest =
                  new UpdatePageRequest(finalId, propertiesToSend, false, null, null);
              notionApiExecutor.executeWithRateLimit(
                  operationId,
                  () ->
                      notionApiExecutor
                          .getNotionHandler()
                          .executeWithRetry(() -> client.updatePage(updatePageRequest)));
              updated++;
            } else {
              log.debug("Creating a new page for: {}", getEntityName());
              CreatePageRequest createPageRequest =
                  new CreatePageRequest(
                      new PageParent(null, databaseId), propertiesToSend, null, null);
              Page page =
                  notionApiExecutor.executeWithRateLimit(
                      operationId,
                      () ->
                          notionApiExecutor
                              .getNotionHandler()
                              .executeWithRetry(() -> client.createPage(createPageRequest)));
              if (page != null && page.getId() != null) {
                entity.setExternalId(page.getId());
                created++;
              } else {
                throw new RuntimeException(
                    "Failed to create Notion page: API returned null page or ID");
              }
            }
            entity.setLastSync(Instant.now());
            repository.saveAndFlush(entity);
            processedCount++;
            syncServiceDependencies
                .getProgressTracker()
                .addLogMessage(
                    operationId,
                    "✅ Successfully synced " + getEntityName() + ": " + entityDisplayName,
                    "SUCCESS");
          } catch (Exception ex) {
            errors++;
            processedCount++;
            log.error("Error syncing " + getEntityName(), ex);
            syncServiceDependencies
                .getProgressTracker()
                .addLogMessage(
                    operationId,
                    "❌ Error syncing "
                        + getEntityName()
                        + ": "
                        + getEntityDisplayName(entity)
                        + " - "
                        + ex.getMessage(),
                    "ERROR");
          }
        }
        syncServiceDependencies
            .getProgressTracker()
            .updateProgress(
                operationId,
                processedCount,
                String.format(
                    "✅ Completed database save: %d %s saved/updated, %d errors",
                    created + updated, getEntityName(), errors));
        if (errors > 0) {
          syncServiceDependencies
              .getProgressTracker()
              .failOperation(operationId, "Completed with " + errors + " errors.");
          return BaseSyncService.SyncResult.failure(
              getEntityName(), "Error syncing " + getEntityName() + "!");
        } else {
          syncServiceDependencies
              .getProgressTracker()
              .completeOperation(
                  operationId,
                  true,
                  "Successfully synced " + (created + updated) + " items.",
                  created + updated);
          return BaseSyncService.SyncResult.success(getEntityName(), created, updated, errors);
        }
      } catch (Exception e) {
        log.error("Error during Notion sync: {}", e.getMessage(), e);
        return BaseSyncService.SyncResult.failure(
            getEntityName(), "Error during Notion sync: " + e.getMessage() + "!");
      }
    }
    syncServiceDependencies
        .getProgressTracker()
        .failOperation(operationId, "Error syncing " + getEntityName() + "!");
    return BaseSyncService.SyncResult.failure(
        getEntityName(), "Error syncing " + getEntityName() + "!");
  }

  protected abstract Map<String, PageProperty> getProperties(T entity);

  protected abstract String getDatabaseName();

  protected String getDatabaseId() {
    return syncServiceDependencies.getNotionHandler().getDatabaseId(getDatabaseName());
  }

  protected abstract String getEntityName();

  private String getEntityDisplayName(T entity) {
    try {
      java.lang.reflect.Method getNameMethod = entity.getClass().getMethod("getName");
      return (String) getNameMethod.invoke(entity);
    } catch (Exception e) {
      return entity.toString();
    }
  }
}
