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
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
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
public class ShowTypeNotionSyncService implements INotionSyncService {

  private final ShowTypeRepository showTypeRepository;
  private final NotionHandler notionHandler;
  private final SyncProgressTracker progressTracker;

  @Autowired
  public ShowTypeNotionSyncService(
      ShowTypeRepository showTypeRepository,
      NotionHandler notionHandler,
      SyncProgressTracker progressTracker) {
    this.showTypeRepository = showTypeRepository;
    this.notionHandler = notionHandler;
    this.progressTracker = progressTracker;
  }

  @Override
  @Transactional
  public BaseSyncService.SyncResult syncToNotion(@NonNull String operationId) {
    Optional<NotionClient> clientOptional = notionHandler.createNotionClient();
    if (clientOptional.isPresent()) {
      try (NotionClient client = clientOptional.get()) {
        String databaseId = notionHandler.getDatabaseId("Show Types");
        if (databaseId != null) {
          int processedCount = 0;
          int created = 0;
          int updated = 0;
          int errors = 0;
          progressTracker.startOperation(operationId, "Sync Show Types", 1);
          List<ShowType> showTypes = showTypeRepository.findAll();
          for (ShowType entity : showTypes) {
            if (processedCount % 5 == 0) {
              progressTracker.updateProgress(
                  operationId,
                  1,
                  String.format(
                      "Saving show types to Notion... (%d/%d processed)",
                      processedCount, showTypes.size()));
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

              // Expected Matches (Number property)
              properties.put(
                  "Expected Matches",
                  NotionPropertyBuilder.createNumberProperty(
                      Integer.valueOf(entity.getExpectedMatches()).doubleValue()));

              // Expected Promos (Number property)
              properties.put(
                  "Expected Promos",
                  NotionPropertyBuilder.createNumberProperty(
                      Integer.valueOf(entity.getExpectedPromos()).doubleValue()));

              if (entity.getExternalId() != null && !entity.getExternalId().isBlank()) {
                log.debug("Updating existing show type page: {}", entity.getName());
                UpdatePageRequest updatePageRequest =
                    new UpdatePageRequest(entity.getExternalId(), properties, false, null, null);
                notionHandler.executeWithRetry(() -> client.updatePage(updatePageRequest));
                updated++;
              } else {
                log.debug("Creating a new show type page for: {}", entity.getName());
                CreatePageRequest createPageRequest =
                    new CreatePageRequest(new PageParent(null, databaseId), properties, null, null);
                Page page =
                    notionHandler.executeWithRetry(() -> client.createPage(createPageRequest));
                entity.setExternalId(page.getId());
                created++;
              }
              entity.setLastSync(Instant.now());
              showTypeRepository.save(entity);
              processedCount++;
            } catch (Exception ex) {
              errors++;
              processedCount++;
              log.error("Error syncing show type: " + entity.getName(), ex);
            }
          }
          progressTracker.updateProgress(
              operationId,
              1,
              String.format(
                  "✅ Completed database save: %d show types saved/updated, %d errors",
                  created + updated, errors));
          return errors > 0
              ? BaseSyncService.SyncResult.failure("show types", "Error syncing show types!")
              : BaseSyncService.SyncResult.success("show types", created, updated, errors);
        }
      }
    }
    progressTracker.failOperation(operationId, "Error syncing show types!");
    return BaseSyncService.SyncResult.failure("show types", "Error syncing show types!");
  }
}
