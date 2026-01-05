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
import com.github.javydreamercsw.base.ai.notion.ShowTypePage;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.model.pages.Page;

@Slf4j
public class ShowTypeNotionHandler {

  private final NotionHandler notionHandler;

  public ShowTypeNotionHandler(NotionHandler notionHandler) {
    this.notionHandler = notionHandler;
  }

  /**
   * Loads all show types from the Notion Show Types database.
   *
   * @return List of all ShowTypePage objects from the Show Types database
   */
  public List<ShowTypePage> getShowTypePages() {
    log.debug("Loading all show types from Show Types database");

    if (!notionHandler.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String dbId = notionHandler.getDatabaseId("Show Types");
    if (dbId == null) {
      log.warn("Show Types database not found in workspace");
      return new ArrayList<>();
    }

    return notionHandler
        .createNotionClient()
        .map(
            client -> {
              try {
                return notionHandler.loadAllEntitiesFromDatabase(
                    client, dbId, "Show Type", this::mapPageToShowTypePage);
              } catch (Exception e) {
                log.error("Failed to load all show types", e);
                throw new RuntimeException(
                    "Failed to load show types from Notion: " + e.getMessage(), e);
              }
            })
        .orElse(new ArrayList<>());
  }

  /** Maps a Notion page to a ShowTypePage object. */
  private ShowTypePage mapPageToShowTypePage(@NonNull Page pageData, @NonNull String showTypeName) {
    return notionHandler.mapPageToGenericEntity(
        pageData, showTypeName, "Show Type", ShowTypePage::new, ShowTypePage.NotionParent::new);
  }
}
