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

import com.github.javydreamercsw.base.ai.notion.NotionApiExecutor;
import com.github.javydreamercsw.base.ai.notion.NotionPropertyBuilder;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import notion.api.v1.model.pages.PageProperty;
import org.springframework.stereotype.Service;

@Service
public class RivalryNotionSyncService extends BaseNotionSyncService<Rivalry> {

  public RivalryNotionSyncService(
      RivalryRepository repository,
      SyncServiceDependencies syncServiceDependencies,
      NotionApiExecutor notionApiExecutor) {
    super(repository, syncServiceDependencies, notionApiExecutor);
  }

  @Override
  protected Map<String, PageProperty> getProperties(@NonNull Rivalry entity) {
    Map<String, PageProperty> properties = new HashMap<>();
    properties.put("Name", NotionPropertyBuilder.createTitleProperty(entity.getDisplayName()));

    if (entity.getWrestler1() != null && entity.getWrestler1().getExternalId() != null) {
      properties.put(
          "Wrestler 1",
          NotionPropertyBuilder.createRelationProperty(entity.getWrestler1().getExternalId()));
    }

    if (entity.getWrestler2() != null && entity.getWrestler2().getExternalId() != null) {
      properties.put(
          "Wrestler 2",
          NotionPropertyBuilder.createRelationProperty(entity.getWrestler2().getExternalId()));
    }

    properties.put(
        "Heat", NotionPropertyBuilder.createNumberProperty(entity.getHeat().doubleValue()));

    properties.put(
        "Priority", NotionPropertyBuilder.createNumberProperty(entity.getPriority().doubleValue()));

    properties.put(
        "Status",
        NotionPropertyBuilder.createSelectProperty(entity.getIsActive() ? "Active" : "Innactive"));

    if (entity.getStartedDate() != null) {
      properties.put(
          "Start Date",
          NotionPropertyBuilder.createDateProperty(
              entity.getStartedDate().atOffset(ZoneOffset.UTC).toLocalDateTime().toString()));
    }

    if (entity.getEndedDate() != null) {
      properties.put(
          "End Date",
          NotionPropertyBuilder.createDateProperty(
              entity.getEndedDate().atOffset(ZoneOffset.UTC).toLocalDateTime().toString()));
    }

    if (entity.getStorylineNotes() != null && !entity.getStorylineNotes().isBlank()) {
      properties.put(
          "Storyline Notes",
          NotionPropertyBuilder.createRichTextProperty(entity.getStorylineNotes()));
    }

    return properties;
  }

  @Override
  protected String getDatabaseName() {
    return "Heat";
  }

  @Override
  protected String getEntityName() {
    return "Rivalry";
  }
}
