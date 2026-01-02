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
import com.github.javydreamercsw.base.ai.notion.TitlePage;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.model.pages.Page;

@Slf4j
public class TitleNotionHandler {

  private final NotionHandler notionHandler;

  public TitleNotionHandler(NotionHandler notionHandler) {
    this.notionHandler = notionHandler;
  }

  /**
   * Loads all titles from the Notion Titles database.
   *
   * @return List of all TitlePage objects from the Titles database
   */
  public List<TitlePage> loadAllTitles() {
    log.debug("Loading all titles from Titles database");

    if (!notionHandler.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String titleDbId = notionHandler.getDatabaseId("Championships");
    if (titleDbId == null) {
      log.warn("Championships database not found in workspace");
      return new ArrayList<>();
    }

    return notionHandler
        .createNotionClient()
        .map(
            client -> {
              try {
                return notionHandler.loadAllEntitiesFromDatabase(
                    client, titleDbId, "Title", this::mapPageToTitlePage);
              } catch (Exception e) {
                log.error("Failed to load all titles", e);
                throw new RuntimeException(
                    "Failed to load titles from Notion: " + e.getMessage(), e);
              }
            })
        .orElse(new ArrayList<>());
  }

  /** Maps a Notion page to a TitlePage object. */
  private TitlePage mapPageToTitlePage(@NonNull Page pageData, @NonNull String titleName) {
    return notionHandler.mapPageToGenericEntity(
        pageData, titleName, "Title", TitlePage::new, TitlePage.NotionParent::new);
  }
}
