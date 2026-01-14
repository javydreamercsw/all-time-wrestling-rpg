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

import com.github.javydreamercsw.base.ai.notion.HeatPage;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.model.pages.Page;

@Slf4j
public class HeatNotionHandler {

  private final NotionHandler notionHandler;

  public HeatNotionHandler(NotionHandler notionHandler) {
    this.notionHandler = notionHandler;
  }

  /** Loads a heat entry from the Notion database by name. */
  public Optional<HeatPage> loadHeat(@NonNull String heatName) {
    log.debug("Loading heat: '{}'", heatName);

    String heatDbId = notionHandler.getDatabaseId("Heat");
    if (heatDbId == null) {
      log.warn("Heat database not found in workspace");
      return Optional.empty();
    }

    return notionHandler
        .createNotionClient()
        .flatMap(
            client -> {
              try {
                return notionHandler.loadEntityFromDatabase(
                    client, heatDbId, heatName, "Heat", this::mapPageToHeatPage);
              } catch (Exception e) {
                log.error("Failed to load heat: {}", heatName, e);
                return Optional.empty();
              }
            });
  }

  /** Maps a Notion page to a HeatPage object. */
  private HeatPage mapPageToHeatPage(@NonNull Page pageData, @NonNull String heatName) {
    return notionHandler.mapPageToGenericEntity(
        pageData, heatName, "Heat", HeatPage::new, HeatPage.NotionParent::new);
  }
}
