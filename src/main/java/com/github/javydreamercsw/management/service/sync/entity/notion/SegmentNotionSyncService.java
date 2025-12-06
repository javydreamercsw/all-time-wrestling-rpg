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
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
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
public class SegmentNotionSyncService implements NotionSyncService {

  private final SegmentRepository segmentRepository;
  private final NotionHandler notionHandler;
  private final SyncProgressTracker progressTracker;

  @Autowired
  public SegmentNotionSyncService(
      SegmentRepository segmentRepository,
      NotionHandler notionHandler,
      SyncProgressTracker progressTracker) {
    this.segmentRepository = segmentRepository;
    this.notionHandler = notionHandler;
    this.progressTracker = progressTracker;
  }

  @Override
  @Transactional
  public BaseSyncService.SyncResult syncToNotion(@NonNull String operationId) {
    Optional<NotionClient> clientOptional = notionHandler.createNotionClient();
    if (clientOptional.isPresent()) {
      try (NotionClient client = clientOptional.get()) {
        String databaseId = notionHandler.getDatabaseId("Segments");
        if (databaseId != null) {
          int processedCount = 0;
          int created = 0;
          int updated = 0;
          int errors = 0;
          progressTracker.startOperation(operationId, "Sync Segments", 1);
          List<Segment> segments = segmentRepository.findAllWithShow();
          for (Segment entity : segments) {
            if (processedCount % 5 == 0) {
              progressTracker.updateProgress(
                  operationId,
                  1,
                  String.format(
                      "Saving segments to Notion... (%d/%d processed)",
                      processedCount, segments.size()));
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
                              new PageProperty.RichText.Text(
                                  entity.getSegmentType().getName()
                                      + " - "
                                      + entity.getShow().getName()),
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

              // Show (Relation property)
              if (entity.getShow() != null && entity.getShow().getExternalId() != null) {
                PageProperty showProperty = new PageProperty();
                showProperty.setType(PropertyType.Relation);
                showProperty.setRelation(
                    Collections.singletonList(
                        new PageProperty.PageReference(entity.getShow().getExternalId())));
                properties.put("Show", showProperty);
              }

              // Segment Type (Relation property)
              if (entity.getSegmentType() != null
                  && entity.getSegmentType().getExternalId() != null) {
                PageProperty segmentTypeProperty = new PageProperty();
                segmentTypeProperty.setType(PropertyType.Relation);
                segmentTypeProperty.setRelation(
                    Collections.singletonList(
                        new PageProperty.PageReference(entity.getSegmentType().getExternalId())));
                properties.put("Segment Type", segmentTypeProperty);
              }

              // Segment Date (Date property)
              if (entity.getSegmentDate() != null) {
                PageProperty segmentDateProperty = new PageProperty();
                segmentDateProperty.setType(PropertyType.Date);
                segmentDateProperty.setDate(
                    new PageProperty.Date(entity.getSegmentDate().toString(), null));
                properties.put("Date", segmentDateProperty);
              }

              // Status (Select property)
              if (entity.getStatus() != null) {
                PageProperty statusProperty = new PageProperty();
                statusProperty.setType(PropertyType.Select);
                DatabaseProperty.Select.Option selectStatus =
                    new DatabaseProperty.Select.Option(
                        null,
                        entity.getStatus().name(),
                        null,
                        null); // Only set name, Notion fills in ID/color
                statusProperty.setSelect(selectStatus);
                properties.put("Status", statusProperty);
              }

              // Adjudication Status (Select property)
              if (entity.getAdjudicationStatus() != null) {
                PageProperty adjudicationStatusProperty = new PageProperty();
                adjudicationStatusProperty.setType(PropertyType.Select);
                DatabaseProperty.Select.Option selectAdjudicationStatus =
                    new DatabaseProperty.Select.Option(
                        null,
                        entity.getAdjudicationStatus().name(),
                        null,
                        null); // Only set name, Notion fills in ID/color
                adjudicationStatusProperty.setSelect(selectAdjudicationStatus);
                properties.put("Adjudication Status", adjudicationStatusProperty);
              }

              // Segment Rules (Multi-relation)
              if (entity.getSegmentRules() != null && !entity.getSegmentRules().isEmpty()) {
                PageProperty segmentRulesProperty = new PageProperty();
                segmentRulesProperty.setType(PropertyType.Relation);
                segmentRulesProperty.setRelation(
                    entity.getSegmentRules().stream()
                        .filter(rule -> rule.getExternalId() != null)
                        .map(rule -> new PageProperty.PageReference(rule.getExternalId()))
                        .collect(Collectors.toList()));
                properties.put("Rules", segmentRulesProperty);
              }

              // Narration (Rich Text property)
              if (entity.getNarration() != null && !entity.getNarration().isBlank()) {
                PageProperty narrationProperty = new PageProperty();
                narrationProperty.setType(PropertyType.RichText);
                narrationProperty.setRichText(
                    Collections.singletonList(
                        new PageProperty.RichText(
                            RichTextType.Text,
                            new PageProperty.RichText.Text(entity.getNarration()),
                            null,
                            null,
                            null,
                            null,
                            null)));
                properties.put("Narration", narrationProperty);
              }

              // Summary (Rich Text property)
              if (entity.getSummary() != null && !entity.getSummary().isBlank()) {
                PageProperty summaryProperty = new PageProperty();
                summaryProperty.setType(PropertyType.RichText);
                summaryProperty.setRichText(
                    Collections.singletonList(
                        new PageProperty.RichText(
                            RichTextType.Text,
                            new PageProperty.RichText.Text(entity.getSummary()),
                            null,
                            null,
                            null,
                            null,
                            null)));
                properties.put("Summary", summaryProperty);
              }

              // Is Title Segment (Checkbox property)
              PageProperty isTitleSegmentProperty = new PageProperty();
              isTitleSegmentProperty.setType(PropertyType.Checkbox);
              isTitleSegmentProperty.setCheckbox(entity.getIsTitleSegment());
              properties.put("Title Segment", isTitleSegmentProperty);

              // Is NPC Generated (Checkbox property)
              PageProperty isNpcGeneratedProperty = new PageProperty();
              isNpcGeneratedProperty.setType(PropertyType.Checkbox);
              isNpcGeneratedProperty.setCheckbox(entity.getIsNpcGenerated());
              properties.put("NPC Generated", isNpcGeneratedProperty);

              // Segment Order (Number property)
              PageProperty segmentOrderProperty = new PageProperty();
              segmentOrderProperty.setType(PropertyType.Number);
              segmentOrderProperty.setNumber(
                  Integer.valueOf(entity.getSegmentOrder()).doubleValue());
              properties.put("Order", segmentOrderProperty);

              // Is Main Event (Checkbox property)
              PageProperty isMainEventProperty = new PageProperty();
              isMainEventProperty.setType(PropertyType.Checkbox);
              isMainEventProperty.setCheckbox(entity.isMainEvent());
              properties.put("Main Event", isMainEventProperty);

              // Participants (Multi-relation to Wrestler - via SegmentParticipant)
              if (entity.getParticipants() != null && !entity.getParticipants().isEmpty()) {
                PageProperty participantsProperty = new PageProperty();
                participantsProperty.setType(PropertyType.Relation);
                participantsProperty.setRelation(
                    entity.getParticipants().stream()
                        .filter(p -> p.getWrestler().getExternalId() != null)
                        .map(p -> new PageProperty.PageReference(p.getWrestler().getExternalId()))
                        .collect(Collectors.toList()));
                properties.put("Participants", participantsProperty);
              }

              // Titles (Multi-relation to Title)
              if (entity.getTitles() != null && !entity.getTitles().isEmpty()) {
                PageProperty titlesProperty = new PageProperty();
                titlesProperty.setType(PropertyType.Relation);
                titlesProperty.setRelation(
                    entity.getTitles().stream()
                        .filter(title -> title.getExternalId() != null)
                        .map(title -> new PageProperty.PageReference(title.getExternalId()))
                        .collect(Collectors.toList()));
                properties.put("Titles", titlesProperty);
              }

              if (entity.getExternalId() != null && !entity.getExternalId().isBlank()) {
                log.debug("Updating existing segment page: {}", entity.getId());
                UpdatePageRequest updatePageRequest =
                    new UpdatePageRequest(entity.getExternalId(), properties, false, null, null);
                notionHandler.executeWithRetry(() -> client.updatePage(updatePageRequest));
                updated++;
              } else {
                log.debug("Creating a new segment page for: {}", entity.getId());
                CreatePageRequest createPageRequest =
                    new CreatePageRequest(new PageParent(null, databaseId), properties, null, null);
                Page page =
                    notionHandler.executeWithRetry(() -> client.createPage(createPageRequest));
                entity.setExternalId(page.getId());
                created++;
              }
              entity.setLastSync(Instant.now());
              segmentRepository.save(entity);
              processedCount++;
            } catch (Exception ex) {
              errors++;
              processedCount++;
              log.error("Error syncing segment: " + entity.getId(), ex);
            }
          }
          progressTracker.updateProgress(
              operationId,
              1,
              String.format(
                  "✅ Completed database save: %d segments saved/updated, %d errors",
                  created + updated, errors));
          return errors > 0
              ? BaseSyncService.SyncResult.failure("segments", "Error syncing segments!")
              : BaseSyncService.SyncResult.success("segments", created, updated, errors);
        }
      }
    }
    progressTracker.failOperation(operationId, "Error syncing segments!");
    return BaseSyncService.SyncResult.failure("segments", "Error syncing segments!");
  }
}
