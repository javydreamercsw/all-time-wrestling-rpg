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

import com.github.javydreamercsw.base.ai.notion.FactionPage;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.model.pages.Page;

@Slf4j
public class FactionNotionHandler {

  private final NotionHandler notionHandler;

  public FactionNotionHandler(NotionHandler notionHandler) {
    this.notionHandler = notionHandler;
  }

  /** Loads a faction from the Notion database by name. */
  public Optional<FactionPage> loadFaction(@NonNull String factionName) {
    log.debug("Loading faction: '{}'", factionName);

    String factionDbId = notionHandler.getDatabaseId("Factions");
    if (factionDbId == null) {
      log.warn("Factions database not found in workspace");
      return Optional.empty();
    }

    return notionHandler
        .createNotionClient()
        .flatMap(
            client -> {
              try {
                return notionHandler.loadEntityFromDatabase(
                    client, factionDbId, factionName, "Faction", this::mapPageToFactionPage);
              } catch (Exception e) {
                log.error("Failed to load faction: {}", factionName, e);
                return Optional.empty();
              }
            });
  }

  /**
   * Loads all factions from the Notion Factions database.
   *
   * @return List of all FactionPage objects from the Factions database
   */
  public List<FactionPage> loadAllFactions() {
    log.debug("Loading all factions from Factions database");

    if (!notionHandler.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String factionDbId = notionHandler.getDatabaseId("Factions");
    if (factionDbId == null) {
      log.warn("Factions database not found in workspace");
      return new ArrayList<>();
    }

    return notionHandler
        .createNotionClient()
        .map(
            client -> {
              try {
                return notionHandler.loadAllEntitiesFromDatabase(
                    client, factionDbId, "Faction", this::mapPageToFactionPage);
              } catch (Exception e) {
                log.error("Failed to load all factions", e);
                throw new RuntimeException(
                    "Failed to load factions from Notion: " + e.getMessage(), e);
              }
            })
        .orElse(new ArrayList<>());
  }

  /** Maps a Notion page to a FactionPage object. */
  private FactionPage mapPageToFactionPage(@NonNull Page pageData, @NonNull String factionName) {
    return notionHandler.mapPageToGenericEntity(
        pageData, factionName, "Faction", FactionPage::new, FactionPage.NotionParent::new);
  }
}
