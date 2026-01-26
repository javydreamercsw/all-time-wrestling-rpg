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
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.service.sync.SyncEntityType;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.time.Instant;
import java.time.ZoneOffset;
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

@Service
@Slf4j
public class ShowNotionSyncService implements NotionSyncService {

  private final ShowRepository showRepository;
  private final NotionHandler notionHandler;
  // Enhanced sync infrastructure services - autowired
  @Autowired public SyncProgressTracker progressTracker;

  public ShowNotionSyncService(ShowRepository showRepository, NotionHandler notionHandler) {
    this.showRepository = showRepository;
    this.notionHandler = notionHandler;
  }

  @Override
  public BaseSyncService.SyncResult syncToNotion(@NonNull String operationId) {
    if (notionHandler != null) {
      Optional<NotionClient> clientOptional = notionHandler.createNotionClient();
      if (clientOptional.isPresent()) {
        try (NotionClient client = clientOptional.get()) {
          String databaseId =
              notionHandler.getDatabaseId("Shows"); // Assuming a Notion database named "Shows"
          if (databaseId != null) {
            int processedCount = 0;
            int created = 0;
            int updated = 0;
            int errors = 0;
            progressTracker.startOperation(operationId, "Sync Shows", 1);
            List<Show> shows = showRepository.findAll();
            for (Show entity : shows) {
              if (processedCount % 5 == 0) {
                progressTracker.updateProgress(
                    operationId,
                    1,
                    String.format(
                        "Saving shows to Notion... (%d/%d processedCount)",
                        processedCount, shows.size()));
              }
              try {
                Map<String, PageProperty> properties = new HashMap<>();
                properties.put(
                    "Name", // Assuming Notion property is "Name"
                    NotionPropertyBuilder.createTitleProperty(entity.getName()));

                // Map Show Type (Relation)
                if (entity.getType() != null) {
                  if (entity.getType().getExternalId() != null) {
                    properties.put(
                        "Show Type",
                        NotionPropertyBuilder.createRelationProperty(
                            entity.getType().getExternalId()));
                  }
                }

                // Map Season (Relation)
                if (entity.getSeason() != null) {
                  properties.put(
                      "Season", // Assuming Notion property is "Season"
                      NotionPropertyBuilder.createRelationProperty(
                          entity.getSeason().getExternalId()));
                }

                // Map Template (Relation)
                if (entity.getTemplate() != null) {
                  properties.put(
                      "Template", // Assuming Notion property is "Template"
                      NotionPropertyBuilder.createRelationProperty(
                          entity.getTemplate().getExternalId()));
                }

                // Map Show Date
                if (entity.getShowDate() != null) {
                  properties.put(
                      "Date", // Assuming Notion property is "Date"
                      NotionPropertyBuilder.createDateProperty(
                          entity.getShowDate().atStartOfDay().atOffset(ZoneOffset.UTC).toString()));
                }

                if (entity.getExternalId() != null && !entity.getExternalId().isBlank()) {
                  // Update existing page
                  UpdatePageRequest updatePageRequest =
                      new UpdatePageRequest(entity.getExternalId(), properties, false, null, null);
                  notionHandler.executeWithRetry(() -> client.updatePage(updatePageRequest));
                  updated++;
                } else {
                  // Create new page
                  CreatePageRequest createPageRequest =
                      new CreatePageRequest(
                          new PageParent(null, databaseId), properties, null, null);
                  Page page =
                      notionHandler.executeWithRetry(() -> client.createPage(createPageRequest));
                  entity.setExternalId(page.getId());
                  created++;
                }
                entity.setLastSync(Instant.now());
                showRepository.save(entity);
                processedCount++;
              } catch (Exception ex) {
                log.error("Error processing show: {}", entity.getName(), ex);
                errors++;
                processedCount++;
              }
            }
            // Final progress update
            progressTracker.updateProgress(
                operationId,
                1,
                String.format(
                    "✅ Completed Notion sync: %d shows saved/updated, %d errors",
                    created + updated, errors));
            return errors > 0
                ? BaseSyncService.SyncResult.failure(
                    SyncEntityType.SHOWS.getKey(), "Error syncing shows!")
                : BaseSyncService.SyncResult.success(
                    SyncEntityType.SHOWS.getKey(), created, updated, errors);
          }
        }
      }
    }
    progressTracker.failOperation(operationId, "Error syncing shows!");
    return BaseSyncService.SyncResult.failure(
        SyncEntityType.SHOWS.getKey(), "Error syncing shows!");
  }
}
