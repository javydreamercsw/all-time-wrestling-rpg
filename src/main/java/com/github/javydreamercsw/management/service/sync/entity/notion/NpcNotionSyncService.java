package com.github.javydreamercsw.management.service.sync.entity.notion;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.NotionClient;
import notion.api.v1.model.common.PropertyType;
import notion.api.v1.model.databases.DatabaseProperty;
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
public class NpcNotionSyncService implements NotionSyncService {

  private final NpcRepository npcRepository;
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
          String databaseId =
              handler.getDatabaseId("NPCs"); // Assuming a Notion database named "NPCs"
          if (databaseId != null) {
            int processedCount = 0;
            int created = 0;
            int updated = 0;
            int errors = 0;
            progressTracker.startOperation(operationId, "Sync NPCs", 1);
            List<Npc> npcs = npcRepository.findAll();
            for (Npc entity : npcs) {
              // Update progress every 5 entities
              if (processedCount % 5 == 0) {
                progressTracker.updateProgress(
                    operationId,
                    1,
                    String.format(
                        "Saving NPCs to Notion... (%d/%d processedCount)",
                        processedCount, npcs.size()));
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
                        null));

                // Map NpcType
                if (entity.getNpcType() != null && !entity.getNpcType().isBlank()) {
                  properties.put(
                      "Role",
                      new PageProperty(
                          UUID.randomUUID().toString(),
                          PropertyType.Select,
                          null,
                          null,
                          new DatabaseProperty.Select.Option(null, entity.getNpcType(), null, null),
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

                if (entity.getExternalId() != null && !entity.getExternalId().isBlank()) {
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
                npcRepository.save(entity);
                processedCount++;
              } catch (Exception ex) {
                log.error("Error processing NPC: " + entity.getName(), ex);
                errors++;
                processedCount++;
              }
            }
            // Final progress update
            progressTracker.updateProgress(
                operationId,
                1,
                String.format(
                    "✅ Completed Notion sync: %d NPCs saved/updated, %d errors",
                    created + updated, errors));
            return errors > 0
                ? BaseSyncService.SyncResult.failure("NPCs", "Error syncing NPCs!")
                : BaseSyncService.SyncResult.success("NPCs", created, updated, errors);
          }
        }
      }
    }
    progressTracker.failOperation(operationId, "Error syncing NPCs!");
    return BaseSyncService.SyncResult.failure("NPCs", "Error syncing NPCs!");
  }
}
