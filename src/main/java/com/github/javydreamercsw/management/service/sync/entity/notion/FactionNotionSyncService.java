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
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import notion.api.v1.model.pages.PageProperty;
import org.springframework.stereotype.Service;

@Service
public class FactionNotionSyncService extends BaseNotionSyncService<Faction> {

  public FactionNotionSyncService(
      FactionRepository repository,
      SyncServiceDependencies syncServiceDependencies,
      NotionApiExecutor notionApiExecutor) {
    super(repository, syncServiceDependencies, notionApiExecutor);
  }

  @Override
  protected Map<String, PageProperty> getProperties(@NonNull Faction entity) {
    Map<String, PageProperty> properties = new HashMap<>();
    properties.put("Name", NotionPropertyBuilder.createTitleProperty(entity.getName()));
    properties.put("Active", NotionPropertyBuilder.createCheckboxProperty(entity.isActive()));

    if (entity.getLeader() != null && entity.getLeader().getExternalId() != null) {
      properties.put(
          "Leader",
          NotionPropertyBuilder.createRelationProperty(entity.getLeader().getExternalId()));
    }

    properties.put(
        "Members",
        NotionPropertyBuilder.createRelationProperty(
            entity.getMembers() == null
                ? java.util.Collections.emptyList()
                : entity.getMembers().stream()
                    .map(
                        com.github.javydreamercsw.management.domain.wrestler.Wrestler
                            ::getExternalId)
                    .filter(Objects::nonNull)
                    .toList()));

    properties.put(
        "Teams",
        NotionPropertyBuilder.createRelationProperty(
            entity.getTeams() == null
                ? java.util.Collections.emptyList()
                : entity.getTeams().stream()
                    .map(com.github.javydreamercsw.management.domain.team.Team::getExternalId)
                    .filter(Objects::nonNull)
                    .toList()));

    return properties;
  }

  @Override
  protected String getDatabaseName() {
    return "Factions";
  }

  @Override
  protected String getEntityName() {
    return "Faction";
  }
}
