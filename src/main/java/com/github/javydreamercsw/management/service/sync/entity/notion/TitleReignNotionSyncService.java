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
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import notion.api.v1.model.pages.PageProperty;
import org.springframework.stereotype.Service;

@Service
public class TitleReignNotionSyncService extends BaseNotionSyncService<TitleReign> {

  public TitleReignNotionSyncService(
      TitleReignRepository repository,
      SyncServiceDependencies syncServiceDependencies,
      NotionApiExecutor notionApiExecutor) {
    super(repository, syncServiceDependencies, notionApiExecutor);
  }

  @Override
  protected Map<String, PageProperty> getProperties(@NonNull TitleReign entity) {
    Map<String, PageProperty> properties = new HashMap<>();

    // Using title name + reign number as the page title
    String name =
        String.format("%s - Reign #%d", entity.getTitle().getName(), entity.getReignNumber());
    properties.put("Name", NotionPropertyBuilder.createTitleProperty(name));

    if (entity.getTitle() != null && entity.getTitle().getExternalId() != null) {
      properties.put(
          "Title", NotionPropertyBuilder.createRelationProperty(entity.getTitle().getExternalId()));
    }

    if (entity.getChampions() != null && !entity.getChampions().isEmpty()) {
      properties.put(
          "Champion",
          NotionPropertyBuilder.createRelationProperty(
              entity.getChampions().stream()
                  .map(Wrestler::getExternalId)
                  .filter(Objects::nonNull)
                  .toList()));
    }

    if (entity.getWonAtSegment() != null && entity.getWonAtSegment().getExternalId() != null) {
      properties.put(
          "Won at Segment",
          NotionPropertyBuilder.createRelationProperty(entity.getWonAtSegment().getExternalId()));
    }

    if (entity.getStartDate() != null) {
      properties.put(
          "Start Date", NotionPropertyBuilder.createDateProperty(entity.getStartDate().toString()));
    }

    if (entity.getEndDate() != null) {
      properties.put(
          "End Date", NotionPropertyBuilder.createDateProperty(entity.getEndDate().toString()));
    }

    properties.put(
        "Reign Number",
        NotionPropertyBuilder.createNumberProperty(entity.getReignNumber().doubleValue()));

    if (entity.getNotes() != null) {
      properties.put("Notes", NotionPropertyBuilder.createRichTextProperty(entity.getNotes()));
    }

    return properties;
  }

  @Override
  protected String getDatabaseName() {
    return "Title Reigns";
  }

  @Override
  protected String getEntityName() {
    return "Title Reign";
  }
}
