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
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import notion.api.v1.model.pages.PageProperty;
import org.springframework.stereotype.Service;

@Service
public class TitleNotionSyncService extends BaseNotionSyncService<Title> {

  public TitleNotionSyncService(
      TitleRepository repository,
      SyncServiceDependencies syncServiceDependencies,
      NotionApiExecutor notionApiExecutor) {
    super(repository, syncServiceDependencies, notionApiExecutor);
  }

  @Override
  protected Map<String, PageProperty> getProperties(@NonNull Title entity) {
    Map<String, PageProperty> properties = new HashMap<>();
    properties.put("Name", NotionPropertyBuilder.createTitleProperty(entity.getName()));

    if (entity.getTier() != null) {
      properties.put(
          "Tier", NotionPropertyBuilder.createSelectProperty(entity.getTier().getDisplayName()));
    }

    if (entity.getGender() != null) {
      properties.put(
          "Gender", NotionPropertyBuilder.createSelectProperty(entity.getGender().name()));
    }

    if (entity.getChampionshipType() != null) {
      properties.put(
          "Title Type",
          NotionPropertyBuilder.createSelectProperty(entity.getChampionshipType().name()));
    }

    properties.put("Active", NotionPropertyBuilder.createCheckboxProperty(entity.getIsActive()));

    if (entity.getDefenseFrequency() != null) {
      properties.put(
          "Defense Cadence (weeks)",
          NotionPropertyBuilder.createNumberProperty(entity.getDefenseFrequency().doubleValue()));
    }

    // Current Champion relation
    properties.put(
        "Current Champion",
        NotionPropertyBuilder.createRelationProperty(
            entity.getChampion() == null
                ? java.util.Collections.emptyList()
                : entity.getChampion().stream()
                    .map(Wrestler::getExternalId)
                    .filter(Objects::nonNull)
                    .toList()));

    // #1 Contender relation
    properties.put(
        "#1 Contender",
        NotionPropertyBuilder.createRelationProperty(
            entity.getChallengers() == null
                ? java.util.Collections.emptyList()
                : entity.getChallengers().stream()
                    .map(Wrestler::getExternalId)
                    .filter(Objects::nonNull)
                    .toList()));

    return properties;
  }

  @Override
  protected String getDatabaseName() {
    return "Championships";
  }

  @Override
  protected String getEntityName() {
    return "Title";
  }
}
