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
import com.github.javydreamercsw.management.domain.injury.InjuryType;
import com.github.javydreamercsw.management.domain.injury.InjuryTypeRepository;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.NotionClient;
import notion.api.v1.model.common.PropertyType;
import notion.api.v1.model.common.RichTextType;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageParent;
import notion.api.v1.model.pages.PageProperty;
import notion.api.v1.request.pages.CreatePageRequest;
import notion.api.v1.request.pages.UpdatePageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InjuryNotionSyncService implements NotionSyncService {

  private final InjuryTypeRepository injuryTypeRepository;
  private final NotionHandler notionHandler;
  // Enhanced sync infrastructure services - autowired
  @Autowired public SyncProgressTracker progressTracker;

  public InjuryNotionSyncService(
      InjuryTypeRepository injuryTypeRepository, NotionHandler notionHandler) {
    this.injuryTypeRepository = injuryTypeRepository;
    this.notionHandler = notionHandler;
  }

  @Override
  public BaseSyncService.SyncResult syncToNotion(@NonNull String operationId) {
    if (notionHandler != null) {
      Optional<NotionClient> clientOptional = notionHandler.createNotionClient();
      if (clientOptional.isPresent()) {
        try (NotionClient client = clientOptional.get()) {
          String databaseId = notionHandler.getDatabaseId("Injuries");
          if (databaseId != null) {
            int processedCount = 0;
            int created = 0;
            int updated = 0;
            int errors = 0;
            progressTracker.startOperation(operationId, "Sync Injury Types", 1);
            List<InjuryType> entities = injuryTypeRepository.findAll();
            for (InjuryType entity : entities) {
              try {
                // Update progress every 5 entities
                if (processedCount % 5 == 0) {
                  progressTracker.updateProgress(
                      operationId,
                      1,
                      String.format(
                          "Saving injury types to Notion... (%d/%d processedCount)",
                          processedCount, entities.size()));
                }
                Map<String, PageProperty> properties = new HashMap<>();
                properties.put(
                    "Name",
                    new PageProperty(
                        UUID.randomUUID().toString(),
                        PropertyType.Title,
                        Collections.singletonList(
                            new PageProperty.RichText(
                                RichTextType.Text,
                                new PageProperty.RichText.Text(entity.getInjuryName()),
                                null,
                                null,
                                null,
                                null,
                                null)),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));

                // Map Health Effect
                if (entity.getHealthEffect() != null) {
                  properties.put(
                      "Health Effect",
                      new PageProperty(
                          UUID.randomUUID().toString(),
                          PropertyType.Number,
                          null,
                          null,
                          null,
                          null,
                          null,
                          entity.getHealthEffect(),
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null));
                }

                // Map Stamina Effect
                if (entity.getStaminaEffect() != null) {
                  properties.put(
                      "Stamina Effect",
                      new PageProperty(
                          UUID.randomUUID().toString(),
                          PropertyType.Number,
                          null,
                          null,
                          null,
                          null,
                          null,
                          entity.getStaminaEffect(),
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null));
                }

                // Map Card Effect
                if (entity.getCardEffect() != null) {
                  properties.put(
                      "Card Effect",
                      new PageProperty(
                          UUID.randomUUID().toString(),
                          PropertyType.Number,
                          null,
                          null,
                          null,
                          null,
                          null,
                          entity.getCardEffect(),
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null));
                }

                // Map Special Effects
                if (entity.getSpecialEffects() != null) {
                  properties.put(
                      "Special Effects",
                      new PageProperty(
                          UUID.randomUUID().toString(),
                          PropertyType.RichText,
                          null,
                          Collections.singletonList(
                              new PageProperty.RichText(
                                  RichTextType.Text,
                                  new PageProperty.RichText.Text(entity.getSpecialEffects()),
                                  null,
                                  null,
                                  null,
                                  null,
                                  null)),
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null));
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
                injuryTypeRepository.save(entity);
                processedCount++;
              } catch (Exception ex) {
                errors++;
                processedCount++;
                log.error("Error syncing injuries!", ex);
              }
            }
            // Final progress update
            progressTracker.updateProgress(
                operationId,
                1,
                String.format(
                    "✅ Completed Notion sync: %d injury types saved/updated, %d errors",
                    created + updated, errors));
            return errors > 0
                ? BaseSyncService.SyncResult.failure("injuries", "Error syncing injuries!")
                : BaseSyncService.SyncResult.success("injuries", created, updated, errors);
          }
        }
      }
    }
    progressTracker.failOperation(operationId, "Error syncing injuries!");
    return BaseSyncService.SyncResult.failure("injuries", "Error syncing injuries!");
  }
}
