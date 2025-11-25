package com.github.javydreamercsw.management.service.sync.entity.notion;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@Slf4j
public class RivalryNotionSyncService implements NotionSyncService {

  private final RivalryRepository rivalryRepository;
  // Enhanced sync infrastructure services - autowired
  @Autowired public SyncProgressTracker progressTracker;

  @Override
  public BaseSyncService.SyncResult syncToNotion(@NonNull String operationId) {
    Optional<NotionHandler> handlerOptional = NotionHandler.getInstance();
    if (handlerOptional.isPresent()) {
      NotionHandler handler = handlerOptional.get();
      Optional<NotionClient> clientOptional = handler.createNotionClient();
      if (clientOptional.isPresent()) {
        try (NotionClient client = clientOptional.get()) {
          String databaseId = handler.getDatabaseId("Heat");
          if (databaseId != null) {
            int processedCount = 0;
            int created = 0;
            int updated = 0;
            int errors = 0;
            progressTracker.startOperation(operationId, "Sync Rivalries", 1);
            List<Rivalry> rivalries = rivalryRepository.findAll();
            for (Rivalry entity : rivalries) {
              // Update progress every 5 entities
              if (processedCount % 5 == 0) {
                progressTracker.updateProgress(
                    operationId,
                    1,
                    String.format(
                        "Saving rivalries to Notion... (%d/%d processedCount)",
                        processedCount, rivalries.size()));
              }
              try {
                Map<String, PageProperty> properties = new HashMap<>();
                properties.put(
                    "Name",
                    new PageProperty(
                        UUID.randomUUID().toString(),
                        notion.api.v1.model.common.PropertyType.Title,
                        Collections.singletonList(
                            new PageProperty.RichText(
                                notion.api.v1.model.common.RichTextType.Text,
                                new PageProperty.RichText.Text(entity.getDisplayName()),
                                null,
                                null,
                                null,
                                null,
                                null))));

                if (entity.getWrestler1() != null) {
                  List<PageProperty.PageReference> relations = new ArrayList<>();
                  relations.add(
                      new PageProperty.PageReference(entity.getWrestler1().getExternalId()));
                  properties.put(
                      "Wrestler 1",
                      new PageProperty(
                          UUID.randomUUID().toString(),
                          notion.api.v1.model.common.PropertyType.Relation,
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
                          relations,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null));
                }
                if (entity.getWrestler2() != null) {
                  List<PageProperty.PageReference> relations = new ArrayList<>();
                  relations.add(
                      new PageProperty.PageReference(entity.getWrestler2().getExternalId()));
                  properties.put(
                      "Wrestler 2",
                      new PageProperty(
                          UUID.randomUUID().toString(),
                          notion.api.v1.model.common.PropertyType.Relation,
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
                          relations,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null));
                }
                if (entity.getHeat() != null) {
                  properties.put(
                      "Heat",
                      new PageProperty(
                          UUID.randomUUID().toString(),
                          notion.api.v1.model.common.PropertyType.Number,
                          null,
                          null,
                          null,
                          null,
                          null,
                          entity.getHeat().doubleValue(),
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

                // Map startedDate
                if (entity.getStartedDate() != null) {
                  properties.put(
                      "Start Date", // Assuming Notion property is "Start Date"
                      new PageProperty(
                          UUID.randomUUID().toString(),
                          notion.api.v1.model.common.PropertyType.Date,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          new PageProperty.Date(entity.getStartedDate().toString(), null),
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

                // Map endedDate
                if (entity.getEndedDate() != null) {
                  properties.put(
                      "Ended Date", // Assuming Notion property is "Ended Date"
                      new PageProperty(
                          UUID.randomUUID().toString(),
                          notion.api.v1.model.common.PropertyType.Date,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          new PageProperty.Date(entity.getEndedDate().toString(), null),
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

                // Map storylineNotes
                if (entity.getStorylineNotes() != null && !entity.getStorylineNotes().isBlank()) {
                  properties.put(
                      "Storyline Notes", // Assuming Notion property is "Storyline Notes"
                      new PageProperty(
                          UUID.randomUUID().toString(),
                          notion.api.v1.model.common.PropertyType.RichText,
                          Collections.singletonList(
                              new PageProperty.RichText(
                                  notion.api.v1.model.common.RichTextType.Text,
                                  new PageProperty.RichText.Text(entity.getStorylineNotes()),
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
                }

                if (!entity.getExternalId().isBlank()) {
                  // Update existing page
                  UpdatePageRequest updatePageRequest =
                      new UpdatePageRequest(entity.getExternalId(), properties, false, null, null);
                  handler.executeWithRetry(() -> client.updatePage(updatePageRequest));
                  updated++;
                } else {
                  // Create new page
                  CreatePageRequest createPageRequest =
                      new CreatePageRequest(
                          new PageParent(null, databaseId), properties, null, null);
                  Page page = handler.executeWithRetry(() -> client.createPage(createPageRequest));
                  entity.setExternalId(page.getId());
                  created++;
                }
                entity.setLastSync(Instant.now());
                rivalryRepository.save(entity);
                processedCount++;
              } catch (Exception ex) {
                log.error("Error processing rivalry: " + entity.getDisplayName(), ex);
                errors++;
                processedCount++;
              }
            }
            // Final progress update
            progressTracker.updateProgress(
                operationId,
                1,
                String.format(
                    "✅ Completed Notion sync: %d rivalries saved/updated, %d errors",
                    created + updated, errors));
            return errors > 0
                ? BaseSyncService.SyncResult.failure("rivalries", "Error syncing rivalries!")
                : BaseSyncService.SyncResult.success("rivalries", created, updated, errors);
          }
        }
      }
    }
    progressTracker.failOperation(operationId, "Error syncing rivalries!");
    return BaseSyncService.SyncResult.failure("rivalries", "Error syncing rivalries!");
  }
}
