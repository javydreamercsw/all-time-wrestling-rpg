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
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import notion.api.v1.model.pages.PageProperty;
import org.springframework.stereotype.Service;

@Service
public class InjuryNotionSyncService extends BaseNotionSyncService<Injury> {

  public InjuryNotionSyncService(
      InjuryRepository repository,
      SyncServiceDependencies syncServiceDependencies,
      NotionApiExecutor notionApiExecutor) {
    super(repository, syncServiceDependencies, notionApiExecutor);
  }

  @Override
  protected Map<String, PageProperty> getProperties(@NonNull Injury entity) {
    Map<String, PageProperty> properties = new HashMap<>();
    properties.put("Name", NotionPropertyBuilder.createTitleProperty(entity.getName()));

    if (entity.getWrestler() != null && entity.getWrestler().getExternalId() != null) {
      properties.put(
          "Wrestler",
          NotionPropertyBuilder.createRelationProperty(entity.getWrestler().getExternalId()));
    }

    if (entity.getDescription() != null && !entity.getDescription().isBlank()) {
      properties.put(
          "Description", NotionPropertyBuilder.createRichTextProperty(entity.getDescription()));
    }

    if (entity.getSeverity() != null) {
      properties.put(
          "Severity", NotionPropertyBuilder.createSelectProperty(entity.getSeverity().name()));
    }

    properties.put(
        "Health Penalty",
        NotionPropertyBuilder.createNumberProperty(
            Integer.valueOf(entity.getHealthPenalty()).doubleValue()));

    properties.put("Active", NotionPropertyBuilder.createCheckboxProperty(entity.getIsActive()));

    if (entity.getInjuryDate() != null) {
      properties.put(
          "Injury Date",
          NotionPropertyBuilder.createDateProperty(entity.getInjuryDate().toString()));
    }

    if (entity.getHealedDate() != null) {
      properties.put(
          "Healed Date",
          NotionPropertyBuilder.createDateProperty(entity.getHealedDate().toString()));
    }

    properties.put(
        "Healing Cost",
        NotionPropertyBuilder.createNumberProperty(entity.getHealingCost().doubleValue()));

    if (entity.getInjuryNotes() != null && !entity.getInjuryNotes().isBlank()) {
      properties.put(
          "Injury Notes", NotionPropertyBuilder.createRichTextProperty(entity.getInjuryNotes()));
    }

    return properties;
  }

  @Override
  protected String getDatabaseName() {
    return "Injuries";
  }

  @Override
  protected String getEntityName() {
    return "Injury";
  }
}
