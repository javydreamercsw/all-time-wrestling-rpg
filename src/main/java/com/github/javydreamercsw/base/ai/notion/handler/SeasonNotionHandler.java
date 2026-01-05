/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.base.ai.notion.handler;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.SeasonPage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.model.pages.Page;

@Slf4j
public class SeasonNotionHandler {

  private final NotionHandler notionHandler;

  public SeasonNotionHandler(NotionHandler notionHandler) {
    this.notionHandler = notionHandler;
  }

  /** Loads a season from the Notion database by name. */
  public Optional<SeasonPage> loadSeason(@NonNull String seasonName) {
    log.debug("Loading season: '{}'", seasonName);

    String seasonDbId = notionHandler.getDatabaseId("Seasons");
    if (seasonDbId == null) {
      log.warn("Seasons database not found in workspace");
      return Optional.empty();
    }

    return notionHandler
        .createNotionClient()
        .flatMap(
            client -> {
              try {
                return notionHandler.loadEntityFromDatabase(
                    client, seasonDbId, seasonName, "Season", this::mapPageToSeasonPage);
              } catch (Exception e) {
                log.error("Failed to load season: {}", seasonName, e);
                return Optional.empty();
              }
            });
  }

  /**
   * Loads all seasons from the Notion Seasons database.
   *
   * @return List of all SeasonPage objects from the Seasons database
   */
  public List<SeasonPage> loadAllSeasons() {
    log.debug("Loading all seasons from Seasons database");

    if (!notionHandler.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String seasonDbId = notionHandler.getDatabaseId("Seasons");
    if (seasonDbId == null) {
      log.warn("Seasons database not found in workspace");
      return new ArrayList<>();
    }

    return notionHandler
        .createNotionClient()
        .map(
            client -> {
              try {
                return notionHandler.loadAllEntitiesFromDatabase(
                    client, seasonDbId, "Season", this::mapPageToSeasonPage);
              } catch (Exception e) {
                log.error("Failed to load all seasons", e);
                throw new RuntimeException(
                    "Failed to load seasons from Notion: " + e.getMessage(), e);
              }
            })
        .orElse(new ArrayList<>());
  }

  /** Maps a Notion page to a SeasonPage object. */
  private SeasonPage mapPageToSeasonPage(@NonNull Page pageData, @NonNull String seasonName) {
    return notionHandler.mapPageToGenericEntity(
        pageData, seasonName, "Season", SeasonPage::new, SeasonPage.NotionParent::new);
  }
}
