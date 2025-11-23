package com.github.javydreamercsw.management.service.sync.entity.notion;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
public class ShowNotionSyncService implements NotionSyncService<Show> {

  private final ShowRepository showRepository;

  @Override
  public void syncToNotion(Show entity) {
    Optional<NotionHandler> handlerOptional = NotionHandler.getInstance();
    if (handlerOptional.isPresent()) {
      NotionHandler handler = handlerOptional.get();
      Optional<NotionClient> clientOptional = handler.createNotionClient();
      if (clientOptional.isPresent()) {
        try (NotionClient client = clientOptional.get()) {
          String databaseId =
              handler.getDatabaseId("Shows"); // Assuming a Notion database named "Shows"
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

            // Map Show Type (Relation)
            if (entity.getType() != null) {
              List<PageProperty.PageReference> relations = new ArrayList<>();
              relations.add(new PageProperty.PageReference(entity.getType().getExternalId()));
              properties.put(
                  "Show Type", // Assuming Notion property is "Show Type"
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

            // Map Season (Relation)
            if (entity.getSeason() != null) {
              List<PageProperty.PageReference> relations = new ArrayList<>();
              relations.add(new PageProperty.PageReference(entity.getSeason().getExternalId()));
              properties.put(
                  "Season", // Assuming Notion property is "Season"
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

            // Map Template (Relation)
            if (entity.getTemplate() != null) {
              List<PageProperty.PageReference> relations = new ArrayList<>();
              relations.add(new PageProperty.PageReference(entity.getTemplate().getExternalId()));
              properties.put(
                  "Template", // Assuming Notion property is "Template"
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

            // Map Show Date
            if (entity.getShowDate() != null) {
              properties.put(
                  "Date", // Assuming Notion property is "Date"
                  new PageProperty(
                      UUID.randomUUID().toString(),
                      notion.api.v1.model.common.PropertyType.Date,
                      null,
                      null,
                      null,
                      null,
                      null,
                      null,
                      new Date(
                          entity.getShowDate().atStartOfDay().atOffset(ZoneOffset.UTC).toString(),
                          null),
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
            } else {
              // Create new page
              CreatePageRequest createPageRequest =
                  new CreatePageRequest(new PageParent(null, databaseId), properties, null, null);
              Page page = handler.executeWithRetry(() -> client.createPage(createPageRequest));
              entity.setExternalId(page.getId());
            }
            entity.setLastSync(Instant.now());
            showRepository.save(entity);
          }
        }
      }
    }
  }
}
