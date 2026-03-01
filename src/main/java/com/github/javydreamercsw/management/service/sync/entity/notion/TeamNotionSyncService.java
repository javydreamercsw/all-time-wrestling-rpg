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
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import notion.api.v1.model.pages.PageProperty;
import org.springframework.stereotype.Service;

@Service
public class TeamNotionSyncService extends BaseNotionSyncService<Team> {

  public TeamNotionSyncService(
      TeamRepository repository,
      SyncServiceDependencies syncServiceDependencies,
      NotionApiExecutor notionApiExecutor) {
    super(repository, syncServiceDependencies, notionApiExecutor);
  }

  @Override
  protected Map<String, PageProperty> getProperties(@NonNull Team entity) {
    Map<String, PageProperty> properties = new HashMap<>();
    properties.put("Name", NotionPropertyBuilder.createTitleProperty(entity.getName()));

    if (entity.getDescription() != null && !entity.getDescription().isBlank()) {
      properties.put(
          "Description", NotionPropertyBuilder.createRichTextProperty(entity.getDescription()));
    }

    if (entity.getWrestler1() != null && entity.getWrestler1().getExternalId() != null) {
      properties.put(
          "Member 1",
          NotionPropertyBuilder.createRelationProperty(entity.getWrestler1().getExternalId()));
    }

    if (entity.getWrestler2() != null && entity.getWrestler2().getExternalId() != null) {
      properties.put(
          "Member 2",
          NotionPropertyBuilder.createRelationProperty(entity.getWrestler2().getExternalId()));
    }

    properties.put(
        "Manager",
        NotionPropertyBuilder.createRelationProperty(
            (entity.getManager() != null && entity.getManager().getExternalId() != null)
                ? java.util.Collections.singletonList(entity.getManager().getExternalId())
                : java.util.Collections.emptyList()));

    properties.put(
        "Faction",
        NotionPropertyBuilder.createRelationProperty(
            (entity.getFaction() != null && entity.getFaction().getExternalId() != null)
                ? java.util.Collections.singletonList(entity.getFaction().getExternalId())
                : java.util.Collections.emptyList()));

    if (entity.getThemeSong() != null) {
      properties.put(
          "Theme Song", NotionPropertyBuilder.createRichTextProperty(entity.getThemeSong()));
    }

    if (entity.getArtist() != null) {
      properties.put("Artist", NotionPropertyBuilder.createRichTextProperty(entity.getArtist()));
    }

    if (entity.getTeamFinisher() != null) {
      properties.put(
          "Team Finisher", NotionPropertyBuilder.createRichTextProperty(entity.getTeamFinisher()));
    }

    properties.put("Status", NotionPropertyBuilder.createCheckboxProperty(entity.isActive()));

    return properties;
  }

  @Override
  protected String getDatabaseName() {
    return "Teams";
  }

  @Override
  protected String getEntityName() {
    return "Team";
  }
}
