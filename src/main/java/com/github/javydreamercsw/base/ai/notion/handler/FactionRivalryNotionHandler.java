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

import com.github.javydreamercsw.base.ai.notion.FactionRivalryPage;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.NotionClient;
import notion.api.v1.model.pages.Page;

@Slf4j
public class FactionRivalryNotionHandler {

  private final NotionHandler notionHandler;

  public FactionRivalryNotionHandler(NotionHandler notionHandler) {
    this.notionHandler = notionHandler;
  }

  /**
   * Loads all faction rivalries from the Notion Faction Heat database.
   *
   * @return List of all FactionRivalryPage objects from the Faction Heat database
   */
  public List<FactionRivalryPage> loadAllFactionRivalries() {
    log.debug("Loading all faction rivalries from Faction Heat database");

    if (!notionHandler.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String dbId = notionHandler.getDatabaseId("Faction Heat");
    if (dbId == null) {
      log.warn("Faction Heat database not found in workspace");
      return new ArrayList<>();
    }

    Optional<NotionClient> clientOptional = notionHandler.createNotionClient();
    if (clientOptional.isPresent()) {
      try (NotionClient client = clientOptional.get()) {
        return notionHandler.loadAllEntitiesFromDatabase(
            client, dbId, "Faction Rivalry", this::mapPageToFactionRivalryPage);
      } catch (Exception e) {
        log.error("Failed to load all faction rivalries", e);
        throw new RuntimeException(
            "Failed to load faction rivalries from Notion: " + e.getMessage(), e);
      }
    } else {
      log.warn("NotionClient not available, returning empty list for faction rivalries.");
      return new ArrayList<>();
    }
  }

  /** Maps a Notion page to a FactionRivalryPage object. */
  private FactionRivalryPage mapPageToFactionRivalryPage(
      @NonNull Page pageData, @NonNull String entityName) {
    return notionHandler.mapPageToGenericEntity(
        pageData,
        entityName,
        "Faction Rivalry",
        FactionRivalryPage::new,
        FactionRivalryPage.NotionParent::new);
  }
}
