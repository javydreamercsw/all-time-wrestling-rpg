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
import com.github.javydreamercsw.base.ai.notion.TitleReignPage;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.model.pages.Page;

@Slf4j
public class TitleReignNotionHandler {

  private final NotionHandler notionHandler;

  public TitleReignNotionHandler(NotionHandler notionHandler) {
    this.notionHandler = notionHandler;
  }

  /**
   * Loads all title reigns from the Notion Title Reigns database.
   *
   * @return List of all TitleReignPage objects from the Title Reigns database
   */
  public List<TitleReignPage> loadAllTitleReigns() {
    log.debug("Loading all title reigns from Title Reigns database");

    if (!notionHandler.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String dbId = notionHandler.getDatabaseId("Title Reigns");
    if (dbId == null) {
      log.warn("Title Reigns database not found in workspace");
      return new ArrayList<>();
    }

    return notionHandler
        .createNotionClient()
        .map(
            client -> {
              try {
                return notionHandler.loadAllEntitiesFromDatabase(
                    client, dbId, "Title Reign", this::mapPageToTitleReignPage);
              } catch (Exception e) {
                log.error("Failed to load all title reigns", e);
                throw new RuntimeException(
                    "Failed to load title reigns from Notion: " + e.getMessage(), e);
              }
            })
        .orElse(new ArrayList<>());
  }

  /** Maps a Notion page to a TitleReignPage object. */
  private TitleReignPage mapPageToTitleReignPage(
      @NonNull Page pageData, @NonNull String entityName) {
    return notionHandler.mapPageToGenericEntity(
        pageData, entityName, "Title Reign", TitleReignPage::new, TitleReignPage.NotionParent::new);
  }
}
