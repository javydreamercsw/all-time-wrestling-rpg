package com.github.javydreamercsw.management.service.sync.entity.notion;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import notion.api.v1.NotionClient;
import notion.api.v1.model.common.PropertyType;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageParent;
import notion.api.v1.model.pages.PageProperty;
import notion.api.v1.model.pages.PageProperty.Date;
import notion.api.v1.request.pages.CreatePageRequest;
import notion.api.v1.request.pages.UpdatePageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeamNotionSyncService implements NotionSyncService {

  private final TeamRepository teamRepository;
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
              handler.getDatabaseId("Teams"); // Assuming a Notion database named "Teams"
          if (databaseId != null) {
            int processedCount = 0;
            int created = 0;
            int updated = 0;
            int errors = 0;
            progressTracker.startOperation(operationId, "Sync Teams", 1);
            List<Team> teams = teamRepository.findAll();
            for (Team entity : teamRepository.findAll()) {
              try {
                // Update progress every 5 entities
                if (processedCount % 5 == 0) {
                  progressTracker.updateProgress(
                      operationId,
                      1,
                      String.format(
                          "Saving teams to Notion... (%d/%d processedCount)",
                          processedCount, teams.size()));
                }
                Map<String, PageProperty> properties = new HashMap<>();
                properties.put(
                    "Name", // Assuming Notion property is "Name"
                    new PageProperty(
                        UUID.randomUUID().toString(),
                        PropertyType.Title,
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
                          PropertyType.RichText,
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

                // Map Wrestler 1 (Relation)
                if (entity.getWrestler1() != null) {
                  List<PageProperty.PageReference> relations =
                      Collections.singletonList(
                          new PageProperty.PageReference(entity.getWrestler1().getExternalId()));
                  properties.put(
                      "Wrestler 1", // Assuming Notion property is "Wrestler 1"
                      new PageProperty(
                          UUID.randomUUID().toString(),
                          PropertyType.Relation,
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

                // Map Wrestler 2 (Relation)
                if (entity.getWrestler2() != null) {
                  List<PageProperty.PageReference> relations =
                      Collections.singletonList(
                          new PageProperty.PageReference(entity.getWrestler2().getExternalId()));
                  properties.put(
                      "Wrestler 2", // Assuming Notion property is "Wrestler 2"
                      new PageProperty(
                          UUID.randomUUID().toString(),
                          PropertyType.Relation,
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

                // Map Faction (Relation)
                if (entity.getFaction() != null) {
                  List<PageProperty.PageReference> relations =
                      Collections.singletonList(
                          new PageProperty.PageReference(entity.getFaction().getExternalId()));
                  properties.put(
                      "Faction", // Assuming Notion property is "Faction"
                      new PageProperty(
                          UUID.randomUUID().toString(),
                          PropertyType.Relation,
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

                // Map Status (Checkbox)
                if (entity.getStatus() != null) {
                  properties.put(
                      "Status", // Assuming Notion property is "Status"
                      new PageProperty(
                          UUID.randomUUID().toString(),
                          PropertyType.Checkbox, // Changed to Checkbox
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          entity.isActive(), // Use entity.isActive() for boolean value
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

                // Map Formed Date
                if (entity.getFormedDate() != null) {
                  properties.put(
                      "Formed Date", // Assuming Notion property is "Formed Date"
                      new PageProperty(
                          UUID.randomUUID().toString(),
                          PropertyType.Date,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          new Date(
                              entity.getFormedDate().atOffset(ZoneOffset.UTC).toString(), null),
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

                // Map Disbanded Date
                if (entity.getDisbandedDate() != null) {
                  properties.put(
                      "Disbanded Date", // Assuming Notion property is "Disbanded Date"
                      new PageProperty(
                          UUID.randomUUID().toString(),
                          PropertyType.Date,
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          new Date(
                              entity.getDisbandedDate().atOffset(ZoneOffset.UTC).toString(), null),
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
                  // If disbanded date is null, ensure it's removed from Notion to reflect active
                  // status
                  // if it was previously set
                  properties.put(
                      "Disbanded Date",
                      new PageProperty(
                          UUID.randomUUID().toString(),
                          PropertyType.Date,
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
                teamRepository.save(entity);
                processedCount++;
              } catch (Exception ex) {
                errors++;
                processedCount++;
              }
            }
            return errors > 0
                ? BaseSyncService.SyncResult.failure("teams", "Error syncing teams!")
                : BaseSyncService.SyncResult.success("teams", created, updated, errors);
          }
        }
      }
    }
    return BaseSyncService.SyncResult.failure("teams", "Error syncing teams!");
  }
}
