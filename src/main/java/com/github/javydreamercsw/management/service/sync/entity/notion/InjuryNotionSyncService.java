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
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
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
public class InjuryNotionSyncService implements NotionSyncService {

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
                      null));

              // Wrestler (Relation property)
              if (entity.getWrestler() != null && entity.getWrestler().getExternalId() != null) {
                PageProperty wrestlerProperty = new PageProperty();
                wrestlerProperty.setType(PropertyType.Relation);
                wrestlerProperty.setRelation(
                    Collections.singletonList(
                        new PageProperty.PageReference(entity.getWrestler().getExternalId())));
                properties.put("Wrestler", wrestlerProperty);
              }

              // Description (Rich Text property)
              if (entity.getDescription() != null && !entity.getDescription().isBlank()) {
                PageProperty descriptionProperty = new PageProperty();
                descriptionProperty.setType(PropertyType.RichText);
                descriptionProperty.setRichText(
                    Collections.singletonList(
                        new PageProperty.RichText(
                            RichTextType.Text,
                            new PageProperty.RichText.Text(entity.getDescription()),
                            null,
                            null,
                            null,
                            null,
                            null)));
                properties.put("Description", descriptionProperty);
              }

              // Severity (Select property)
              if (entity.getSeverity() != null) {
                properties.put(
                    "Severity",
                    new PageProperty(
                        UUID.randomUUID().toString(),
                        PropertyType.Select,
                        null,
                        null,
                        new DatabaseProperty.Select.Option(
                            null, entity.getSeverity().name(), null, null),
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

              // Health Penalty (Number property)
              PageProperty healthPenaltyProperty = new PageProperty();
              healthPenaltyProperty.setType(PropertyType.Number);
              healthPenaltyProperty.setNumber(
                  Integer.valueOf(entity.getHealthPenalty()).doubleValue());
              properties.put("Health Penalty", healthPenaltyProperty);

              // Is Active (Checkbox property)
              PageProperty isActiveProperty = new PageProperty();
              isActiveProperty.setType(PropertyType.Checkbox);
              isActiveProperty.setCheckbox(entity.getIsActive());
              properties.put("Active", isActiveProperty);

              // Injury Date (Date property)
              if (entity.getInjuryDate() != null) {
                PageProperty injuryDateProperty = new PageProperty();
                injuryDateProperty.setType(PropertyType.Date);
                injuryDateProperty.setDate(
                    new PageProperty.Date(entity.getInjuryDate().toString(), null));
                properties.put("Injury Date", injuryDateProperty);
              }

              // Healed Date (Date property)
              if (entity.getHealedDate() != null) {
                PageProperty healedDateProperty = new PageProperty();
                healedDateProperty.setType(PropertyType.Date);
                healedDateProperty.setDate(
                    new PageProperty.Date(entity.getHealedDate().toString(), null));
                properties.put("Healed Date", healedDateProperty);
              }

              // Healing Cost (Number property)
              PageProperty healingCostProperty = new PageProperty();
              healingCostProperty.setType(PropertyType.Number);
              healingCostProperty.setNumber(entity.getHealingCost().doubleValue());
              properties.put("Healing Cost", healingCostProperty);

              // Injury Notes (Rich Text property)
              if (entity.getInjuryNotes() != null && !entity.getInjuryNotes().isBlank()) {
                PageProperty injuryNotesProperty = new PageProperty();
                injuryNotesProperty.setType(PropertyType.RichText);
                injuryNotesProperty.setRichText(
                    Collections.singletonList(
                        new PageProperty.RichText(
                            RichTextType.Text,
                            new PageProperty.RichText.Text(entity.getInjuryNotes()),
                            null,
                            null,
                            null,
                            null,
                            null)));
                properties.put("Injury Notes", injuryNotesProperty);
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
