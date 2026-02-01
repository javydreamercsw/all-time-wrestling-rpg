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

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.NotionPropertyBuilder;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.time.Instant;
import java.util.HashMap;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ShowTemplateNotionSyncService implements NotionEntitySyncService {

  private final ShowTemplateRepository showTemplateRepository;
  private final NotionHandler notionHandler;
  private final SyncProgressTracker progressTracker;

  @Autowired
  public ShowTemplateNotionSyncService(
      ShowTemplateRepository showTemplateRepository,
      NotionHandler notionHandler,
      SyncProgressTracker progressTracker) {
    this.showTemplateRepository = showTemplateRepository;
    this.notionHandler = notionHandler;
    this.progressTracker = progressTracker;
  }

  @Override
  @Transactional
  public BaseSyncService.SyncResult syncToNotion(@NonNull String operationId) {
    Optional<NotionClient> clientOptional = notionHandler.createNotionClient();
    if (clientOptional.isPresent()) {
      try (NotionClient client = clientOptional.get()) {
        String databaseId = notionHandler.getDatabaseId("Show Templates");
        if (databaseId != null) {
          int processedCount = 0;
          int created = 0;
          int updated = 0;
          int errors = 0;
          progressTracker.startOperation(operationId, "Sync Show Templates", 1);
          List<ShowTemplate> showTemplates = showTemplateRepository.findAll();
          for (ShowTemplate entity : showTemplates) {
            if (processedCount % 5 == 0) {
              progressTracker.updateProgress(
                  operationId,
                  1,
                  String.format(
                      "Saving show templates to Notion... (%d/%d processed)",
                      processedCount, showTemplates.size()));
            }
            try {
              Map<String, PageProperty> properties = new HashMap<>();

              // Name (Title property)
              properties.put("Name", NotionPropertyBuilder.createTitleProperty(entity.getName()));

              // Description (Rich Text property)
              if (entity.getDescription() != null && !entity.getDescription().isBlank()) {
                properties.put(
                    "Description",
                    NotionPropertyBuilder.createRichTextProperty(entity.getDescription()));
              }

              // Show Type (Relation property)
              if (entity.getShowType() != null && entity.getShowType().getExternalId() != null) {
                properties.put(
                    "Show Type",
                    NotionPropertyBuilder.createRelationProperty(
                        entity.getShowType().getExternalId()));
              }

              if (entity.getExternalId() != null && !entity.getExternalId().isBlank()) {
                log.debug("Updating existing show template page: {}", entity.getName());
                UpdatePageRequest updatePageRequest =
                    new UpdatePageRequest(entity.getExternalId(), properties, false, null, null);
                notionHandler.executeWithRetry(() -> client.updatePage(updatePageRequest));
                updated++;
              } else {
                log.debug("Creating a new show template page for: {}", entity.getName());
                CreatePageRequest createPageRequest =
                    new CreatePageRequest(new PageParent(null, databaseId), properties, null, null);
                Page page =
                    notionHandler.executeWithRetry(() -> client.createPage(createPageRequest));
                entity.setExternalId(page.getId());
                created++;
              }
              entity.setLastSync(Instant.now());
              showTemplateRepository.save(entity);
              processedCount++;
            } catch (Exception ex) {
              errors++;
              processedCount++;
              log.error("Error syncing show template: " + entity.getName(), ex);
            }
          }
          progressTracker.updateProgress(
              operationId,
              1,
              String.format(
                  "✅ Completed database save: %d show templates saved/updated, %d errors",
                  created + updated, errors));
          return errors > 0
              ? BaseSyncService.SyncResult.failure(
                  "show templates", "Error syncing show templates!")
              : BaseSyncService.SyncResult.success("show templates", created, updated, errors);
        }
      }
    }
    progressTracker.failOperation(operationId, "Error syncing show templates!");
    return BaseSyncService.SyncResult.failure("show templates", "Error syncing show templates!");
  }
}
