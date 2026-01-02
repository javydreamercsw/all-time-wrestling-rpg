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
import com.github.javydreamercsw.base.ai.notion.RivalryPage;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.model.pages.Page;

@Slf4j
public class RivalryNotionHandler {

  private final NotionHandler notionHandler;

  public RivalryNotionHandler(NotionHandler notionHandler) {
    this.notionHandler = notionHandler;
  }

  /**
   * Loads all rivalries from the Notion Heat database.
   *
   * @return List of all RivalryPage objects from the Heat database
   */
  public List<RivalryPage> loadAllRivalries() {
    log.debug("Loading all rivalries from Heat database");

    if (!notionHandler.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String dbId = notionHandler.getDatabaseId("Heat");
    if (dbId == null) {
      log.warn("Heat database not found in workspace");
      return new ArrayList<>();
    }

    return notionHandler
        .createNotionClient()
        .map(
            client -> {
              try {
                return notionHandler.loadAllEntitiesFromDatabase(
                    client, dbId, "Rivalry", this::mapPageToRivalryPage);
              } catch (Exception e) {
                log.error("Failed to load all rivalries", e);
                throw new RuntimeException(
                    "Failed to load rivalries from Notion: " + e.getMessage(), e);
              }
            })
        .orElse(new ArrayList<>());
  }

  /** Maps a Notion page to a RivalryPage object. */
  private RivalryPage mapPageToRivalryPage(@NonNull Page pageData, @NonNull String entityName) {
    return notionHandler.mapPageToGenericEntity(
        pageData, entityName, "Rivalry", RivalryPage::new, RivalryPage.NotionParent::new);
  }
}
