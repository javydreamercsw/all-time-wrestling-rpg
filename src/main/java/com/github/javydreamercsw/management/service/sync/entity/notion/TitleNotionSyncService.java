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
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
public class TitleNotionSyncService implements NotionSyncService {

  private final TitleRepository titleRepository;
  private final NotionHandler notionHandler;
  // Enhanced sync infrastructure services - autowired
  @Autowired public SyncProgressTracker progressTracker;

  public TitleNotionSyncService(TitleRepository titleRepository, NotionHandler notionHandler) {
    this.titleRepository = titleRepository;
    this.notionHandler = notionHandler;
  }

  @Override
  public BaseSyncService.SyncResult syncToNotion(@NonNull String operationId) {
    if (notionHandler != null) {
      Optional<NotionClient> clientOptional = notionHandler.createNotionClient();
      if (clientOptional.isPresent()) {
        try (NotionClient client = clientOptional.get()) {
          String databaseId =
              notionHandler.getDatabaseId(
                  "Championships"); // Assuming a Notion database named "Championships"
          if (databaseId != null) {
            int updated = 0;
            int errors = 0;
            int created = 0;
            int processedCount = 0;
            progressTracker.startOperation(operationId, "Sync Titles", 1);
            List<Title> titles = titleRepository.findAll();
            for (Title entity : titles) {
              // Update progress every 5 entities
              if (processedCount % 5 == 0) {
                progressTracker.updateProgress(
                    operationId,
                    1,
                    String.format(
                        "Saving titles to Notion... (%d/%d processedCount)",
                        processedCount, titles.size()));
              }
              try {
                Map<String, PageProperty> properties = new HashMap<>();
                properties.put(
                    "Name", // Assuming Notion property is "Name"
                    NotionPropertyBuilder.createTitleProperty(entity.getName()));

                // Map Tier (Select)
                if (entity.getTier() != null) {
                  properties.put(
                      "Tier", // Assuming Notion property is "Tier"
                      NotionPropertyBuilder.createSelectProperty(
                          entity.getTier().getDisplayName()));
                }

                // Map Gender (Select)
                if (entity.getGender() != null) {
                  properties.put(
                      "Gender", // Assuming Notion property is "Gender"
                      NotionPropertyBuilder.createSelectProperty(entity.getGender().name()));
                }

                // Map Championship Type (Select)
                if (entity.getChampionshipType() != null) {
                  properties.put(
                      "Category",
                      NotionPropertyBuilder.createSelectProperty(
                          entity.getChampionshipType().name()));
                }

                // Map Is Active (Checkbox)
                if (entity.getIsActive() != null) {
                  properties.put(
                      "Active", // Assuming Notion property is "Active"
                      NotionPropertyBuilder.createCheckboxProperty(entity.getIsActive()));
                }

                // Map Champion (Relation)
                if (entity.getCurrentChampions() != null
                    && !entity.getCurrentChampions().isEmpty()) {
                  List<String> externalIds =
                      entity.getCurrentChampions().stream()
                          .map(wrestler -> wrestler.getExternalId())
                          .filter(java.util.Objects::nonNull)
                          .collect(Collectors.toList());
                  if (!externalIds.isEmpty()) {
                    properties.put(
                        "Champion", // Assuming Notion property is "Champion"
                        NotionPropertyBuilder.createRelationProperty(externalIds));
                  }
                }

                // Map Challengers (Relation)
                if (entity.getChallengers() != null && !entity.getChallengers().isEmpty()) {
                  List<String> externalIds =
                      entity.getChallengers().stream()
                          .map(wrestler -> wrestler.getExternalId())
                          .filter(java.util.Objects::nonNull)
                          .collect(Collectors.toList());
                  if (!externalIds.isEmpty()) {
                    properties.put(
                        "Challengers", // Assuming Notion property is "Challengers"
                        NotionPropertyBuilder.createRelationProperty(externalIds));
                  }
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
                titleRepository.save(entity);
                processedCount++;
              } catch (Exception ex) {
                log.error("Error processing title: " + entity.getName(), ex);
                errors++;
                processedCount++;
              }
            }
            // Final progress update
            progressTracker.updateProgress(
                operationId,
                1,
                String.format(
                    "✅ Completed Notion sync: %d titles saved/updated, %d errors",
                    created + updated, errors));
            return errors > 0
                ? BaseSyncService.SyncResult.failure("titles", "Error syncing titles!")
                : BaseSyncService.SyncResult.success("titles", created, updated, errors);
          }
        }
      }
    }
    progressTracker.failOperation(operationId, "Error syncing titles!");
    return BaseSyncService.SyncResult.failure("titles", "Unable to sync!");
  }
}
