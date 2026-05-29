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
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.model.pages.PageProperty;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ShowNotionSyncService extends BaseNotionSyncService<Show> {

  private final GameSettingService gameSettingService;

  public ShowNotionSyncService(
      final ShowRepository repository,
      final SyncServiceDependencies syncServiceDependencies,
      final NotionApiExecutor notionApiExecutor,
      final GameSettingService gameSettingService) {
    super(repository, syncServiceDependencies, notionApiExecutor);
    this.gameSettingService = gameSettingService;
  }

  @Override
  protected List<Show> getEntitiesToSync(@NonNull final List<Show> all) {
    LocalDate gameDate = gameSettingService.getCurrentGameDate();
    if (gameDate == null) {
      return all;
    }
    List<Show> eligible =
        all.stream()
            .filter(show -> show.getShowDate() == null || !show.getShowDate().isAfter(gameDate))
            .collect(Collectors.toList());
    int excluded = all.size() - eligible.size();
    if (excluded > 0) {
      log.info(
          "⏩ Skipping {} future show(s) (show_date > game date {}) from outbound sync",
          excluded,
          gameDate);
    }
    return eligible;
  }

  @Override
  protected Map<String, PageProperty> getProperties(@NonNull final Show entity) {
    Map<String, PageProperty> properties = new HashMap<>();
    properties.put("Name", NotionPropertyBuilder.createTitleProperty(entity.getName()));

    if (entity.getType() != null && entity.getType().getExternalId() != null) {
      properties.put(
          "Show Type",
          NotionPropertyBuilder.createRelationProperty(entity.getType().getExternalId()));
    }

    if (entity.getSeason() != null && entity.getSeason().getExternalId() != null) {
      properties.put(
          "Season",
          NotionPropertyBuilder.createRelationProperty(entity.getSeason().getExternalId()));
    }

    if (entity.getTemplate() != null && entity.getTemplate().getExternalId() != null) {
      properties.put(
          "Template",
          NotionPropertyBuilder.createRelationProperty(entity.getTemplate().getExternalId()));
    }

    if (entity.getShowDate() != null) {
      properties.put(
          "Date",
          NotionPropertyBuilder.createDateProperty(
              entity.getShowDate().atStartOfDay().atOffset(ZoneOffset.UTC).toString()));
    }

    if (entity.getDescription() != null && !entity.getDescription().isBlank()) {
      properties.put(
          "Description", NotionPropertyBuilder.createRichTextProperty(entity.getDescription()));
    }
    return properties;
  }

  @Override
  protected String getDatabaseName() {
    return "Shows";
  }

  @Override
  protected String getEntityName() {
    return "Show";
  }

  @Override
  protected String getEntityDisplayName(@NonNull final Show entity) {
    if (entity.getShowDate() != null) {
      return "%s (%s)".formatted(entity.getName(), entity.getShowDate());
    }
    return super.getEntityDisplayName(entity);
  }

  @Override
  protected boolean isNameBasedMatchingEnabled() {
    // Shows repeat annually (same name, different year) so name alone is never a unique key.
    // Require external_id to be set before updating a Notion page.
    return false;
  }
}
