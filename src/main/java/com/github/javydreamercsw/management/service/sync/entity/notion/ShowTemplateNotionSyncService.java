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
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import notion.api.v1.model.pages.PageProperty;
import org.springframework.stereotype.Service;

@Service
public class ShowTemplateNotionSyncService extends BaseNotionSyncService<ShowTemplate> {

  public ShowTemplateNotionSyncService(
      ShowTemplateRepository repository,
      SyncServiceDependencies syncServiceDependencies,
      NotionApiExecutor notionApiExecutor) {
    super(repository, syncServiceDependencies, notionApiExecutor);
  }

  @Override
  protected Map<String, PageProperty> getProperties(@NonNull ShowTemplate entity) {
    Map<String, PageProperty> properties = new HashMap<>();
    properties.put("Name", NotionPropertyBuilder.createTitleProperty(entity.getName()));

    if (entity.getShowType() != null && entity.getShowType().getExternalId() != null) {
      properties.put(
          "Show Type",
          NotionPropertyBuilder.createRelationProperty(entity.getShowType().getExternalId()));
    }

    if (entity.getDescription() != null) {
      properties.put(
          "Description", NotionPropertyBuilder.createRichTextProperty(entity.getDescription()));
    }

    if (entity.getDayOfWeek() != null) {
      properties.put(
          "Day of Week", NotionPropertyBuilder.createSelectProperty(entity.getDayOfWeek().name()));
    }

    if (entity.getWeekOfMonth() != null) {
      properties.put(
          "Week of Month",
          NotionPropertyBuilder.createNumberProperty(entity.getWeekOfMonth().doubleValue()));
    }

    return properties;
  }

  @Override
  protected String getDatabaseName() {
    return "Show Templates";
  }

  @Override
  protected String getEntityName() {
    return "Show Template";
  }
}
