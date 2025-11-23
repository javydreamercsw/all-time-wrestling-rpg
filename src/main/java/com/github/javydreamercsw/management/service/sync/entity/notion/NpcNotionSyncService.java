package com.github.javydreamercsw.management.service.sync.entity.notion;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import notion.api.v1.NotionClient;
import notion.api.v1.model.common.PropertyType;
import notion.api.v1.model.databases.DatabaseProperty;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageParent;
import notion.api.v1.model.pages.PageProperty;
import notion.api.v1.request.pages.CreatePageRequest;
import notion.api.v1.request.pages.UpdatePageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NpcNotionSyncService implements NotionSyncService<Npc> {

  private final NpcRepository npcRepository;

  @Override
  public void syncToNotion(@NonNull Npc entity) {
    Optional<NotionHandler> handlerOptional = NotionHandler.getInstance();
    if (handlerOptional.isPresent()) {
      NotionHandler handler = handlerOptional.get();
      Optional<NotionClient> clientOptional = handler.createNotionClient();
      if (clientOptional.isPresent()) {
        try (NotionClient client = clientOptional.get()) {
          String databaseId =
              handler.getDatabaseId("NPCs"); // Assuming a Notion database named "NPCs"
          if (databaseId != null) {
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
            } else {
              // Create new page
              CreatePageRequest createPageRequest =
                  new CreatePageRequest(new PageParent(null, databaseId), properties, null, null);
              Page page = handler.executeWithRetry(() -> client.createPage(createPageRequest));
              entity.setExternalId(page.getId());
            }
            entity.setLastSync(Instant.now());
            npcRepository.save(entity);
          }
        }
      }
    }
  }
}
