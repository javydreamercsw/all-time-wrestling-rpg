package com.github.javydreamercsw.base.service;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import notion.api.v1.NotionClient;
import notion.api.v1.model.common.PropertyType;
import notion.api.v1.model.common.RichTextType;
import notion.api.v1.model.databases.DatabaseProperty;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageParent;
import notion.api.v1.model.pages.PageProperty;
import notion.api.v1.request.pages.CreatePageRequest;
import notion.api.v1.request.pages.UpdatePageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WrestlerNotionSyncService implements NotionSyncService<Wrestler> {

  private final WrestlerRepository wrestlerRepository;

  @Override
  public void syncToNotion(Wrestler entity) {
    Optional<NotionHandler> handlerOptional = NotionHandler.getInstance();
    if (handlerOptional.isPresent()) {
      NotionHandler handler = handlerOptional.get();
      Optional<NotionClient> clientOptional = handler.createNotionClient();
      if (clientOptional.isPresent()) {
        try (NotionClient client = clientOptional.get()) {
          String databaseId = handler.getDatabaseId("Wrestlers");
          if (databaseId != null) {
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
            wrestlerRepository.save(entity);
          }
        }
      }
    }
  }
}
