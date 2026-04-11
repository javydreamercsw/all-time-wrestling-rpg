/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
import com.github.javydreamercsw.management.domain.injury.InjuryType;
import com.github.javydreamercsw.management.domain.injury.InjuryTypeRepository;
import com.github.javydreamercsw.management.service.sync.SyncEntityType;
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

@Service
@Slf4j
public class InjuryTypeNotionSyncService implements NotionEntitySyncService {
  @Autowired private InjuryTypeRepository injuryTypeRepository;

  private final NotionHandler notionHandler;
  // Enhanced sync infrastructure services - autowired
  @Autowired public SyncProgressTracker progressTracker;

  @Autowired
  public InjuryTypeNotionSyncService(
      InjuryTypeRepository injuryTypeRepository,
      NotionHandler notionHandler,
      SyncProgressTracker progressTracker) {
    this.injuryTypeRepository = injuryTypeRepository;
    this.notionHandler = notionHandler;
    this.progressTracker = progressTracker;
  }

  @Override
  public BaseSyncService.SyncResult syncToNotion(@NonNull String operationId) {
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
          List<InjuryType> types = injuryTypeRepository.findAll();
          for (InjuryType type : types) {
            if (processedCount % 5 == 0) {
              progressTracker.updateProgress(
                  operationId,
                  1,
                  String.format(
                      "Saving injury types to Notion... (%d/%d processed)",
                      processedCount, types.size()));
            }
            try {
              Map<String, PageProperty> properties = new HashMap<>();

              // Name (Title property)
              properties.put(
                  "Name", NotionPropertyBuilder.createTitleProperty(type.getInjuryName()));

              // Health Effect (Number property)
              properties.put(
                  "Health Effect",
                  NotionPropertyBuilder.createNumberProperty(type.getHealthEffect().doubleValue()));

              // Stamina Effect (Number property)
              properties.put(
                  "Stamina Effect",
                  NotionPropertyBuilder.createNumberProperty(
                      type.getStaminaEffect().doubleValue()));

              // Card Effect (Number property)
              properties.put(
                  "Card Effect",
                  NotionPropertyBuilder.createNumberProperty(type.getCardEffect().doubleValue()));

              // Description (Rich Text property)
              if (type.getSpecialEffects() != null && !type.getSpecialEffects().isBlank()) {
                properties.put(
                    "Description",
                    NotionPropertyBuilder.createRichTextProperty(type.getSpecialEffects()));
              }

              if (type.getExternalId() != null && !type.getExternalId().isBlank()) {
                log.debug("Updating existing injury type page: {}", type.getInjuryName());
                UpdatePageRequest updatePageRequest =
                    new UpdatePageRequest(type.getExternalId(), properties, false, null, null);
                notionHandler.executeWithRetry(() -> client.updatePage(updatePageRequest));
                updated++;
              } else {
                log.debug("Creating a new injury type page for: {}", type.getInjuryName());
                CreatePageRequest createPageRequest =
                    new CreatePageRequest(new PageParent(null, databaseId), properties, null, null);
                Page page =
                    notionHandler.executeWithRetry(() -> client.createPage(createPageRequest));
                type.setExternalId(page.getId());
                created++;
              }
              type.setLastSync(Instant.now());
              injuryTypeRepository.save(type);
              processedCount++;
            } catch (Exception ex) {
              errors++;
              processedCount++;
              log.error("Error syncing injury type: {}", type.getInjuryName(), ex);
            }
          }
          progressTracker.updateProgress(
              operationId,
              1,
              String.format(
                  "✅ Completed database save: %d injury types saved/updated, %d errors",
                  created + updated, errors));
          return errors > 0
              ? BaseSyncService.SyncResult.failure(
                  SyncEntityType.INJURY_TYPES.getKey(), "Error syncing injury types!")
              : BaseSyncService.SyncResult.success(
                  SyncEntityType.INJURY_TYPES.getKey(), created, updated, errors);
        }
      }
    }
    return null;
  }
}
