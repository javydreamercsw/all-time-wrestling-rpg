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
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
public class TeamNotionSyncService implements NotionEntitySyncService {

  private final TeamRepository teamRepository;
  private final NotionHandler notionHandler;

  // Enhanced sync infrastructure services - autowired
  @Autowired public SyncProgressTracker progressTracker;

  @Autowired
  public TeamNotionSyncService(TeamRepository teamRepository, NotionHandler notionHandler) {
    this.teamRepository = teamRepository;
    this.notionHandler = notionHandler;
  }

  @Override
  @Transactional
  public BaseSyncService.SyncResult syncToNotion(@NonNull String operationId) {
    Optional<NotionClient> clientOptional = notionHandler.createNotionClient();
    if (clientOptional.isPresent()) {
      try (NotionClient client = clientOptional.get()) {
        String databaseId =
            notionHandler.getDatabaseId("Teams"); // Assuming a Notion database named "Teams"
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
                  NotionPropertyBuilder.createTitleProperty(entity.getName()));

              // Map Description
              if (entity.getDescription() != null && !entity.getDescription().isBlank()) {
                properties.put(
                    "Description", // Assuming Notion property is "Description"
                    NotionPropertyBuilder.createRichTextProperty(entity.getDescription()));
              }

              // Map Members (Relation)
              if (entity.getWrestler1() != null && entity.getWrestler1().getExternalId() != null) {
                properties.put(
                    "Member 1",
                    NotionPropertyBuilder.createRelationProperty(
                        entity.getWrestler1().getExternalId()));
              }
              if (entity.getWrestler2() != null && entity.getWrestler2().getExternalId() != null) {
                properties.put(
                    "Member 2",
                    NotionPropertyBuilder.createRelationProperty(
                        entity.getWrestler2().getExternalId()));
              }

              // Map Faction (Relation)
              if (entity.getFaction() != null) {
                properties.put(
                    "Faction", // Assuming Notion property is "Faction"
                    NotionPropertyBuilder.createRelationProperty(
                        entity.getFaction().getExternalId()));
              }

              // Map Status (Checkbox)
              if (entity.getStatus() != null) {
                properties.put(
                    "Status", // Assuming Notion property is "Status"
                    NotionPropertyBuilder.createCheckboxProperty(entity.isActive()));
              }

              if (entity.getExternalId() != null && !entity.getExternalId().isBlank()) {
                // Update existing page
                UpdatePageRequest updatePageRequest =
                    new UpdatePageRequest(entity.getExternalId(), properties, false, null, null);
                notionHandler.executeWithRetry(() -> client.updatePage(updatePageRequest));
                updated++;
              } else {
                // Create new page
                CreatePageRequest createPageRequest =
                    new CreatePageRequest(new PageParent(null, databaseId), properties, null, null);
                Page page =
                    notionHandler.executeWithRetry(() -> client.createPage(createPageRequest));
                entity.setExternalId(page.getId());
                created++;
              }
              entity.setLastSync(Instant.now());
              teamRepository.save(entity);
              processedCount++;
            } catch (Exception ex) {
              log.error("Error syncing team: " + entity.getName(), ex);
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
    return BaseSyncService.SyncResult.failure("teams", "Error syncing teams!");
  }
}
