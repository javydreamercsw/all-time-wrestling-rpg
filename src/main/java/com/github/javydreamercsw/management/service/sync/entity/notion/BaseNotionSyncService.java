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
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    Optional<NotionClient> clientOptional =
        notionApiExecutor.getNotionHandler().createNotionClient();
    if (clientOptional.isPresent()) {
      try (NotionClient client = clientOptional.get()) {
        String databaseId = getDatabaseId();
        if (databaseId != null) {
          int processedCount = 0;
          int created = 0;
          int updated = 0;
          int errors = 0;
          syncServiceDependencies
              .getProgressTracker()
              .startOperation(operationId, "Sync " + getEntityName(), 1);
          List<T> entities = repository.findAll();
          for (T entity : entities) {
            if (processedCount % 5 == 0) {
              syncServiceDependencies
                  .getProgressTracker()
                  .updateProgress(
                      operationId,
                      1,
                      String.format(
                          "Saving %s to Notion... (%d/%d processed)",
                          getEntityName(), processedCount, entities.size()));
            }
            try {
              Map<String, PageProperty> properties = getProperties(entity);
              if (entity.getExternalId() != null && !entity.getExternalId().isBlank()) {
                log.debug("Updating existing page for: {}", getEntityName());
                UpdatePageRequest updatePageRequest =
                    new UpdatePageRequest(entity.getExternalId(), properties, false, null, null);
                notionApiExecutor.executeWithRateLimit(
                    () ->
                        notionApiExecutor
                            .getNotionHandler()
                            .executeWithRetry(() -> client.updatePage(updatePageRequest)));
                updated++;
              } else {
                log.debug("Creating a new page for: {}", getEntityName());
                CreatePageRequest createPageRequest =
                    new CreatePageRequest(new PageParent(null, databaseId), properties, null, null);
                Page page =
                    notionApiExecutor.executeWithRateLimit(
                        () ->
                            notionApiExecutor
                                .getNotionHandler()
                                .executeWithRetry(() -> client.createPage(createPageRequest)));
                entity.setExternalId(page.getId());
                created++;
              }
              entity.setLastSync(Instant.now());
              repository.save(entity);
              processedCount++;
            } catch (Exception ex) {
              errors++;
              processedCount++;
              log.error("Error syncing " + getEntityName(), ex);
            }
          }
          syncServiceDependencies
              .getProgressTracker()
              .updateProgress(
                  operationId,
                  1,
                  String.format(
                      "✅ Completed database save: %d %s saved/updated, %d errors",
                      created + updated, getEntityName(), errors));
          return errors > 0
              ? BaseSyncService.SyncResult.failure(
                  getEntityName(), "Error syncing " + getEntityName() + "!")
              : BaseSyncService.SyncResult.success(getEntityName(), created, updated, errors);
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

  protected abstract String getDatabaseId();

  protected abstract String getEntityName();
}
