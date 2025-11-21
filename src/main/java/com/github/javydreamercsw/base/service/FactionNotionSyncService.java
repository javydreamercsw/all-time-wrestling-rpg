package com.github.javydreamercsw.base.service;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import notion.api.v1.NotionClient;
import notion.api.v1.model.common.PropertyType;
import notion.api.v1.model.common.RichTextType;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageParent;
import notion.api.v1.model.pages.PageProperty;
import notion.api.v1.request.pages.CreatePageRequest;
import notion.api.v1.request.pages.UpdatePageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FactionNotionSyncService implements NotionSyncService<Faction> {

  private final FactionRepository factionRepository;

  @Override
  public void syncToNotion(Faction entity) {
    Optional<NotionHandler> handlerOptional = NotionHandler.getInstance();
    if (handlerOptional.isPresent()) {
      NotionHandler handler = handlerOptional.get();
      Optional<NotionClient> clientOptional = handler.createNotionClient();
      if (clientOptional.isPresent()) {
        try (NotionClient client = clientOptional.get()) {
          String databaseId = handler.getDatabaseId("Factions");
          if (databaseId != null) {
            Map<String, PageProperty> properties = new HashMap<>();
            properties.put(
                "Name",
                new PageProperty(
                    PropertyType.TITLE,
                    Collections.singletonList(
                        new PageProperty.RichText(
                            RichTextType.TEXT,
                            new PageProperty.RichText.Text(entity.getName())))));
            if (entity.getIsActive() != null) {
              properties.put(
                  "Active", new PageProperty(PropertyType.CHECKBOX, entity.getIsActive()));
            }
            if (entity.getLeader() != null && entity.getLeader().getExternalId() != null) {
              List<PageProperty.PageReference> relations = new ArrayList<>();
              relations.add(new PageProperty.PageReference(entity.getLeader().getExternalId()));
              properties.put("Leader", new PageProperty(PropertyType.RELATION, relations));
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
            factionRepository.save(entity);
          }
        }
      }
    }
  }
}