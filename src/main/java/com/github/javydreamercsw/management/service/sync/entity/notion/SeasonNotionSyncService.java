package com.github.javydreamercsw.management.service.sync.entity.notion;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import notion.api.v1.NotionClient;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageParent;
import notion.api.v1.model.pages.PageProperty;
import notion.api.v1.model.pages.PageProperty.Date;
import notion.api.v1.request.pages.CreatePageRequest;
import notion.api.v1.request.pages.UpdatePageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SeasonNotionSyncService implements NotionSyncService<Season> {

  private final SeasonRepository seasonRepository;

  @Override
  public void syncToNotion(Season entity) {
    Optional<NotionHandler> handlerOptional = NotionHandler.getInstance();
    if (handlerOptional.isPresent()) {
      NotionHandler handler = handlerOptional.get();
      Optional<NotionClient> clientOptional = handler.createNotionClient();
      if (clientOptional.isPresent()) {
        try (NotionClient client = clientOptional.get()) {
          String databaseId =
              handler.getDatabaseId("Seasons"); // Assuming a Notion database named "Seasons"
          if (databaseId != null) {
            Map<String, PageProperty> properties = new HashMap<>();
            properties.put(
                "Name", // Assuming Notion property is "Name"
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

            // Map Description
            if (entity.getDescription() != null && !entity.getDescription().isBlank()) {
              properties.put(
                  "Description", // Assuming Notion property is "Description"
                  new PageProperty(
                      UUID.randomUUID().toString(),
                      notion.api.v1.model.common.PropertyType.RichText,
                      Collections.singletonList(
                          new PageProperty.RichText(
                              notion.api.v1.model.common.RichTextType.Text,
                              new PageProperty.RichText.Text(entity.getDescription()),
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

            // Map Start Date
            if (entity.getStartDate() != null) {
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
                      new Date(entity.getStartDate().atOffset(ZoneOffset.UTC).toString(), null),
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

            // Map End Date
            if (entity.getEndDate() != null) {
              properties.put(
                  "End Date", // Assuming Notion property is "End Date"
                  new PageProperty(
                      UUID.randomUUID().toString(),
                      notion.api.v1.model.common.PropertyType.Date,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      new Date(entity.getEndDate().atOffset(ZoneOffset.UTC).toString(), null),
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
            } else {
              // If end date is null, ensure it's removed from Notion to reflect active status if it
              // was previously set
              // Notion API update: set date property to null means clearing the date
              properties.put(
                  "End Date",
                  new PageProperty(
                      UUID.randomUUID().toString(),
                      notion.api.v1.model.common.PropertyType.Date,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null, // Set date to null to clear it
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
            } else {
              // Create new page
              CreatePageRequest createPageRequest =
                  new CreatePageRequest(new PageParent(null, databaseId), properties, null, null);
              Page page = handler.executeWithRetry(() -> client.createPage(createPageRequest));
              entity.setExternalId(page.getId());
            }
            entity.setLastSync(Instant.now());
            seasonRepository.save(entity);
          }
        }
      }
    }
  }
}
