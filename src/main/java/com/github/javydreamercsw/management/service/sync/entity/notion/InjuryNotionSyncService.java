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
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
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
public class InjuryNotionSyncService implements INotionSyncService {

  private final InjuryRepository injuryRepository;
  private final NotionHandler notionHandler;
  private final SyncProgressTracker progressTracker;

  @Autowired
  public InjuryNotionSyncService(
      InjuryRepository injuryRepository,
      NotionHandler notionHandler,
      SyncProgressTracker progressTracker) {
    this.injuryRepository = injuryRepository;
    this.notionHandler = notionHandler;
    this.progressTracker = progressTracker;
  }

  @Override
  @Transactional
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
          progressTracker.startOperation(operationId, "Sync Injuries", 1);
          List<Injury> injuries = injuryRepository.findAll();
          for (Injury entity : injuries) {
            if (processedCount % 5 == 0) {
              progressTracker.updateProgress(
                  operationId,
                  1,
                  String.format(
                      "Saving injuries to Notion... (%d/%d processed)",
                      processedCount, injuries.size()));
            }
            try {
              Map<String, PageProperty> properties = new HashMap<>();

              // Name (Title property)
              properties.put("Name", NotionPropertyBuilder.createTitleProperty(entity.getName()));

              // Wrestler (Relation property)
              if (entity.getWrestler() != null && entity.getWrestler().getExternalId() != null) {
                properties.put(
                    "Wrestler",
                    NotionPropertyBuilder.createRelationProperty(
                        entity.getWrestler().getExternalId()));
              }

              // Description (Rich Text property)
              if (entity.getDescription() != null && !entity.getDescription().isBlank()) {
                properties.put(
                    "Description",
                    NotionPropertyBuilder.createRichTextProperty(entity.getDescription()));
              }

              // Severity (Select property)
              if (entity.getSeverity() != null) {
                properties.put(
                    "Severity",
                    NotionPropertyBuilder.createSelectProperty(entity.getSeverity().name()));
              }

              // Health Penalty (Number property)
              properties.put(
                  "Health Penalty",
                  NotionPropertyBuilder.createNumberProperty(
                      Integer.valueOf(entity.getHealthPenalty()).doubleValue()));

              // Is Active (Checkbox property)
              properties.put(
                  "Active", NotionPropertyBuilder.createCheckboxProperty(entity.getIsActive()));

              // Injury Date (Date property)
              if (entity.getInjuryDate() != null) {
                properties.put(
                    "Injury Date",
                    NotionPropertyBuilder.createDateProperty(entity.getInjuryDate().toString()));
              }

              // Healed Date (Date property)
              if (entity.getHealedDate() != null) {
                properties.put(
                    "Healed Date",
                    NotionPropertyBuilder.createDateProperty(entity.getHealedDate().toString()));
              }

              // Healing Cost (Number property)
              properties.put(
                  "Healing Cost",
                  NotionPropertyBuilder.createNumberProperty(
                      entity.getHealingCost().doubleValue()));

              // Injury Notes (Rich Text property)
              if (entity.getInjuryNotes() != null && !entity.getInjuryNotes().isBlank()) {
                properties.put(
                    "Injury Notes",
                    NotionPropertyBuilder.createRichTextProperty(entity.getInjuryNotes()));
              }

              if (entity.getExternalId() != null && !entity.getExternalId().isBlank()) {
                log.debug("Updating existing injury page: {}", entity.getName());
                UpdatePageRequest updatePageRequest =
                    new UpdatePageRequest(entity.getExternalId(), properties, false, null, null);
                notionHandler.executeWithRetry(() -> client.updatePage(updatePageRequest));
                updated++;
              } else {
                log.debug("Creating a new injury page for: {}", entity.getName());
                CreatePageRequest createPageRequest =
                    new CreatePageRequest(new PageParent(null, databaseId), properties, null, null);
                Page page =
                    notionHandler.executeWithRetry(() -> client.createPage(createPageRequest));
                entity.setExternalId(page.getId());
                created++;
              }
              entity.setLastSync(Instant.now());
              injuryRepository.save(entity);
              processedCount++;
            } catch (Exception ex) {
              errors++;
              processedCount++;
              log.error("Error syncing injury: " + entity.getName(), ex);
            }
          }
          progressTracker.updateProgress(
              operationId,
              1,
              String.format(
                  "✅ Completed database save: %d injuries saved/updated, %d errors",
                  created + updated, errors));
          return errors > 0
              ? BaseSyncService.SyncResult.failure("injuries", "Error syncing injuries!")
              : BaseSyncService.SyncResult.success("injuries", created, updated, errors);
        }
      }
    }
    progressTracker.failOperation(operationId, "Error syncing injuries!");
    return BaseSyncService.SyncResult.failure("injuries", "Error syncing injuries!");
  }
}
