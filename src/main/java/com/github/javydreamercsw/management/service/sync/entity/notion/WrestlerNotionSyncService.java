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
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
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
import notion.api.v1.model.databases.DatabaseProperty;
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
public class WrestlerNotionSyncService implements NotionSyncService {

  private final WrestlerRepository wrestlerRepository;
  private final NotionHandler notionHandler;

  // Enhanced sync infrastructure services - autowired
  @Autowired public SyncProgressTracker progressTracker;

  @Autowired
  public WrestlerNotionSyncService(
      WrestlerRepository wrestlerRepository, NotionHandler notionHandler) {
    this.wrestlerRepository = wrestlerRepository;
    this.notionHandler = notionHandler;
  }

  @Override
  @Transactional
  public BaseSyncService.SyncResult syncToNotion(@NonNull String operationId) {
    Optional<NotionClient> clientOptional = notionHandler.createNotionClient();
    if (clientOptional.isPresent()) {
      try (NotionClient client = clientOptional.get()) {
        String databaseId = notionHandler.getDatabaseId("Wrestlers");
        if (databaseId != null) {
          int processedCount = 0;
          int created = 0;
          int updated = 0;
          int errors = 0;
          progressTracker.startOperation(operationId, "Sync Wrestlers", 1);
          List<Wrestler> wrestlers = wrestlerRepository.findAll();
          for (Wrestler entity : wrestlers) {
            // Update progress every 5 entities
            if (processedCount % 5 == 0) {
              progressTracker.updateProgress(
                  operationId,
                  1,
                  String.format(
                      "Saving wrestlers to Notion... (%d/%d processedCount)",
                      processedCount, wrestlers.size()));
            }
            try {
              Map<String, PageProperty> properties = new HashMap<>();
              properties.put(
                  "Name",
                  new PageProperty(
                      UUID.randomUUID().toString(),
                      PropertyType.Title,
                      Collections.singletonList(
                          new PageProperty.RichText(
                              RichTextType.Text,
                              new PageProperty.RichText.Text(entity.getName()),
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
                      null,
                      null,
                      null));
              if (entity.getStartingStamina() != null) {
                properties.put(
                    "Starting Stamina",
                    new PageProperty(
                        UUID.randomUUID().toString(),
                        PropertyType.Number,
                        null,
                        null,
                        null,
                        null,
                        null,
                        entity.getStartingStamina().doubleValue(),
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
              if (entity.getStartingHealth() != null) {
                properties.put(
                    "Starting Health",
                    new PageProperty(
                        UUID.randomUUID().toString(),
                        PropertyType.Number,
                        null,
                        null,
                        null,
                        null,
                        null,
                        entity.getStartingHealth().doubleValue(),
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
              if (entity.getFans() != null) {
                properties.put(
                    "Fans",
                    new PageProperty(
                        UUID.randomUUID().toString(),
                        PropertyType.Number,
                        null,
                        null,
                        null,
                        null,
                        null,
                        entity.getFans().doubleValue(),
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
              if (entity.getTier() != null) {
                properties.put(
                    "Tier",
                    new PageProperty(
                        UUID.randomUUID().toString(),
                        PropertyType.Select,
                        null,
                        null,
                        new DatabaseProperty.Select.Option(
                            null, entity.getTier().getDisplayName(), null, null),
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
                        null,
                        null,
                        null));
              }
              if (entity.getGender() != null) {
                properties.put(
                    "Gender",
                    new PageProperty(
                        UUID.randomUUID().toString(),
                        PropertyType.Select,
                        null,
                        null,
                        new DatabaseProperty.Select.Option(
                            null, entity.getGender().name(), null, null),
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
                        null,
                        null,
                        null));
              }
              if (entity.getBumps() != null) {
                properties.put(
                    "Bumps",
                    new PageProperty(
                        UUID.randomUUID().toString(),
                        PropertyType.Number,
                        null,
                        null,
                        null,
                        null,
                        null,
                        entity.getBumps().doubleValue(),
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
              if (entity.getLowHealth() != null) {
                properties.put(
                    "Low Health",
                    new PageProperty(
                        UUID.randomUUID().toString(),
                        PropertyType.Number,
                        null,
                        null,
                        null,
                        null,
                        null,
                        entity.getLowHealth().doubleValue(),
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
              if (entity.getLowStamina() != null) {
                properties.put(
                    "Low Stamina",
                    new PageProperty(
                        UUID.randomUUID().toString(),
                        PropertyType.Number,
                        null,
                        null,
                        null,
                        null,
                        null,
                        entity.getLowStamina().doubleValue(),
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
              if (entity.getDeckSize() != null) {
                properties.put(
                    "Deck Size",
                    new PageProperty(
                        UUID.randomUUID().toString(),
                        PropertyType.Number,
                        null,
                        null,
                        null,
                        null,
                        null,
                        entity.getDeckSize().doubleValue(),
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
                log.debug("Updating existing wrestler page: {}", entity.getName());
                // Update existing page
                UpdatePageRequest updatePageRequest =
                    new UpdatePageRequest(entity.getExternalId(), properties, false, null, null);
                notionHandler.executeWithRetry(() -> client.updatePage(updatePageRequest));
                updated++;
              } else {
                log.debug("Creating a new wrestler page for: {}", entity.getName());
                // Create new page
                CreatePageRequest createPageRequest =
                    new CreatePageRequest(new PageParent(null, databaseId), properties, null, null);
                Page page =
                    notionHandler.executeWithRetry(() -> client.createPage(createPageRequest));
                entity.setExternalId(page.getId());
                created++;
              }
              entity.setLastSync(Instant.now());
              entity = wrestlerRepository.save(entity);
              processedCount++;
            } catch (Exception ex) {
              errors++;
              processedCount++;
            }
          }
          // Final progress update
          progressTracker.updateProgress(
              operationId,
              1,
              String.format(
                  "✅ Completed database save: %d wrestlers saved/updated, %d errors",
                  created + updated, errors));
          return errors > 0
              ? BaseSyncService.SyncResult.failure("wrestlers", "Error syncing wrestlers!")
              : BaseSyncService.SyncResult.success("wrestlers", created, updated, errors);
        }
      }
    }
    progressTracker.failOperation(operationId, "Error syncing wrestlers!");
    return BaseSyncService.SyncResult.failure("wrestlers", "Error syncing wrestlers!");
  }
}
