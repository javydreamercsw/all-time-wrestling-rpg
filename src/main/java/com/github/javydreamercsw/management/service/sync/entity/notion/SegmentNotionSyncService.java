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
import com.github.javydreamercsw.base.domain.AbstractEntity;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
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
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SegmentNotionSyncService implements NotionEntitySyncService {

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
                  NotionPropertyBuilder.createTitleProperty(
                      entity.getSegmentType().getName() + " - " + entity.getShow().getName()));

              // Show (Relation property)
              if (entity.getShow() != null && entity.getShow().getExternalId() != null) {
                properties.put(
                    "Shows",
                    NotionPropertyBuilder.createRelationProperty(entity.getShow().getExternalId()));
              }

              // Segment Type (Relation property)
              if (entity.getSegmentType() != null
                  && entity.getSegmentType().getExternalId() != null) {
                properties.put(
                    "Segment Type",
                    NotionPropertyBuilder.createRelationProperty(
                        entity.getSegmentType().getExternalId()));
              }

              // Segment Date (Date property)
              if (entity.getSegmentDate() != null) {
                properties.put(
                    "Date",
                    NotionPropertyBuilder.createDateProperty(entity.getSegmentDate().toString()));
              }

              // Segment Rules (Multi-relation)
              if (entity.getSegmentRules() != null && !entity.getSegmentRules().isEmpty()) {
                properties.put(
                    "Rules",
                    NotionPropertyBuilder.createRelationProperty(
                        entity.getSegmentRules().stream()
                            .map(AbstractEntity::getExternalId)
                            .filter(externalId -> externalId != null)
                            .collect(Collectors.toList())));
              }

              // Narration (Rich Text property)
              if (entity.getNarration() != null && !entity.getNarration().isBlank()) {
                properties.put(
                    "Description",
                    NotionPropertyBuilder.createRichTextProperty(entity.getNarration()));
              }

              // Summary (Rich Text property)
              if (entity.getSummary() != null && !entity.getSummary().isBlank()) {
                properties.put(
                    "Summary", NotionPropertyBuilder.createRichTextProperty(entity.getSummary()));
              }

              // Participants (Multi-relation to Wrestler - via SegmentParticipant)
              if (entity.getParticipants() != null && !entity.getParticipants().isEmpty()) {
                properties.put(
                    "Participants",
                    NotionPropertyBuilder.createRelationProperty(
                        entity.getParticipants().stream()
                            .filter(p -> p.getWrestler().getExternalId() != null)
                            .map(p -> p.getWrestler().getExternalId())
                            .collect(Collectors.toList())));
              }

              // Titles (Multi-relation to Title)
              if (entity.getTitles() != null && !entity.getTitles().isEmpty()) {
                properties.put(
                    "Titles",
                    NotionPropertyBuilder.createRelationProperty(
                        entity.getTitles().stream()
                            .map(AbstractEntity::getExternalId)
                            .filter(externalId -> externalId != null)
                            .collect(Collectors.toList())));
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
