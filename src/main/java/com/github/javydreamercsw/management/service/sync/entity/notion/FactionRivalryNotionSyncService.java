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
import com.github.javydreamercsw.management.domain.faction.FactionRivalry;
import com.github.javydreamercsw.management.domain.faction.FactionRivalryRepository;
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
public class FactionRivalryNotionSyncService implements NotionSyncService {

  private final FactionRivalryRepository factionRivalryRepository;
  private final NotionHandler notionHandler;
  private final SyncProgressTracker progressTracker;

  @Autowired
  public FactionRivalryNotionSyncService(
      FactionRivalryRepository factionRivalryRepository,
      NotionHandler notionHandler,
      SyncProgressTracker progressTracker) {
    this.factionRivalryRepository = factionRivalryRepository;
    this.notionHandler = notionHandler;
    this.progressTracker = progressTracker;
  }

  @Override
  @Transactional
  public BaseSyncService.SyncResult syncToNotion(@NonNull String operationId) {
    Optional<NotionClient> clientOptional = notionHandler.createNotionClient();
    if (clientOptional.isPresent()) {
      try (NotionClient client = clientOptional.get()) {
        String databaseId = notionHandler.getDatabaseId("Faction Rivalries");
        if (databaseId != null) {
          int processedCount = 0;
          int created = 0;
          int updated = 0;
          int errors = 0;
          progressTracker.startOperation(operationId, "Sync Faction Rivalries", 1);
          List<FactionRivalry> rivalries = factionRivalryRepository.findAll();
          for (FactionRivalry entity : rivalries) {
            if (processedCount % 5 == 0) {
              progressTracker.updateProgress(
                  operationId,
                  1,
                  String.format(
                      "Saving faction rivalries to Notion... (%d/%d processed)",
                      processedCount, rivalries.size()));
            }
            try {
              Map<String, PageProperty> properties = new HashMap<>();
              properties.put(
                  "Name", NotionPropertyBuilder.createTitleProperty(entity.getDisplayName()));

              properties.put(
                  "Faction 1",
                  NotionPropertyBuilder.createRelationProperty(
                      entity.getFaction1().getExternalId()));

              // Faction 2
              properties.put(
                  "Faction 2",
                  NotionPropertyBuilder.createRelationProperty(
                      entity.getFaction2().getExternalId()));

              // Heat
              properties.put(
                  "Heat",
                  NotionPropertyBuilder.createNumberProperty(entity.getHeat().doubleValue()));

              // Active
              properties.put(
                  "Active", NotionPropertyBuilder.createCheckboxProperty(entity.getIsActive()));

              // Intensity
              properties.put(
                  "Intensity",
                  NotionPropertyBuilder.createSelectProperty(
                      entity.getIntensity().getDisplayName()));

              if (entity.getExternalId() != null && !entity.getExternalId().isBlank()) {
                log.debug("Updating existing faction rivalry page: {}", entity.getDisplayName());
                UpdatePageRequest updatePageRequest =
                    new UpdatePageRequest(entity.getExternalId(), properties, false, null, null);
                notionHandler.executeWithRetry(() -> client.updatePage(updatePageRequest));
                updated++;
              } else {
                log.debug("Creating a new faction rivalry page for: {}", entity.getDisplayName());
                CreatePageRequest createPageRequest =
                    new CreatePageRequest(new PageParent(null, databaseId), properties, null, null);
                Page page =
                    notionHandler.executeWithRetry(() -> client.createPage(createPageRequest));
                entity.setExternalId(page.getId());
                created++;
              }
              entity.setLastSync(Instant.now());
              factionRivalryRepository.save(entity);
              processedCount++;
            } catch (Exception ex) {
              errors++;
              processedCount++;
              log.error("Error syncing faction rivalry: " + entity.getDisplayName(), ex);
            }
          }
          progressTracker.updateProgress(
              operationId,
              1,
              String.format(
                  "✅ Completed database save: %d faction rivalries saved/updated, %d errors",
                  created + updated, errors));
          return errors > 0
              ? BaseSyncService.SyncResult.failure(
                  "faction rivalries", "Error syncing faction rivalries!")
              : BaseSyncService.SyncResult.success("faction rivalries", created, updated, errors);
        }
      }
    }
    progressTracker.failOperation(operationId, "Error syncing faction rivalries!");
    return BaseSyncService.SyncResult.failure(
        "faction rivalries", "Error syncing faction rivalries!");
  }
}
