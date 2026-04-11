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

import com.github.javydreamercsw.base.ai.notion.InjuryPage;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.model.pages.Page;

@Slf4j
public class InjuryNotionHandler {

  private final NotionHandler notionHandler;

  public InjuryNotionHandler(NotionHandler notionHandler) {
    this.notionHandler = notionHandler;
  }

  /**
   * Loads all injuries from the Notion Injuries database.
   *
   * @return List of all InjuryPage objects from the Injuries database
   */
  public List<InjuryPage> loadAllInjuries() {
    log.debug("Loading all injuries from Injuries database");

    if (!notionHandler.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String injuryDbId = notionHandler.getDatabaseId("Injuries");
    if (injuryDbId == null) {
      log.warn("Injuries database not found in workspace");
      return new ArrayList<>();
    }

    return notionHandler
        .createNotionClient()
        .map(
            client -> {
              try {
                return notionHandler.loadAllEntitiesFromDatabase(
                    client, injuryDbId, "Injury", this::mapPageToInjuryPage);
              } catch (Exception e) {
                log.error("Failed to load all injuries", e);
                throw new RuntimeException(
                    "Failed to load injuries from Notion: " + e.getMessage(), e);
              }
            })
        .orElse(new ArrayList<>());
  }

  /** Maps a Notion page to an InjuryPage object. */
  private InjuryPage mapPageToInjuryPage(@NonNull Page pageData, @NonNull String injuryName) {
    return notionHandler.mapPageToGenericEntity(
        pageData, injuryName, "Injury", InjuryPage::new, InjuryPage.NotionParent::new);
  }
}
