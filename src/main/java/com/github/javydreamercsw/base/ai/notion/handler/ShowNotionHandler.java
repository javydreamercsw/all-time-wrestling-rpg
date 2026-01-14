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
package com.github.javydreamercsw.base.ai.notion.handler;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.ShowPage;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.NotionClient;
import notion.api.v1.model.pages.Page;

@Slf4j
public class ShowNotionHandler implements NotionEntityHandler<ShowPage> {

  private final NotionHandler notionHandler;

  public ShowNotionHandler(NotionHandler notionHandler) {
    this.notionHandler = notionHandler;
  }

  @Override
  public String getDatabaseName() {
    return "Shows";
  }

  @Override
  public Optional<ShowPage> loadById(@NonNull String id) {
    log.debug("Loading show with ID: '{}'", id);
    return notionHandler.loadPage(id).map(page -> mapPageToShowPage(page, ""));
  }

  @Override
  public Optional<ShowPage> loadByName(@NonNull String name) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public List<ShowPage> loadAll() {
    return loadAll(true);
  }

  @Override
  public List<ShowPage> loadAll(boolean syncMode) {
    log.debug("Loading all shows for sync operation (optimized)");

    // Check if NOTION_TOKEN is available first
    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String showDbId = notionHandler.getDatabaseId(getDatabaseName());
    if (showDbId == null) {
      log.warn("Shows database not found in workspace");
      return new ArrayList<>();
    }

    try (NotionClient client = notionHandler.createNotionClient().orElse(null)) {
      if (client == null) {
        log.warn("NotionClient not available, returning empty list for shows for sync.");
        return new ArrayList<>();
      }
      return notionHandler.loadAllEntitiesFromDatabase(
          client, showDbId, "Show", this::mapPageToShowPageForSync);
    } catch (Exception e) {
      log.error("Failed to load all shows for sync", e);
      throw new RuntimeException("Failed to load shows from Notion: " + e.getMessage(), e);
    }
  }

  /** Maps a Notion page to a ShowPage object with full relationship resolution. */
  private ShowPage mapPageToShowPage(@NonNull Page pageData, @NonNull String showName) {
    ShowPage showPage =
        notionHandler.mapPageToGenericEntity(
            pageData,
            showName,
            "Show",
            () -> new ShowPage(notionHandler),
            ShowPage.NotionParent::new,
            true);
    showPage.setNotionHandler(notionHandler);
    return showPage;
  }

  /** Maps a Notion page to a ShowPage object with minimal relationship resolution for sync. */
  private ShowPage mapPageToShowPageForSync(@NonNull Page pageData, @NonNull String showName) {
    ShowPage showPage =
        notionHandler.mapPageToGenericEntity(
            pageData,
            showName,
            "Show",
            () -> new ShowPage(notionHandler),
            ShowPage.NotionParent::new,
            true);
    showPage.setNotionHandler(notionHandler);
    return showPage;
  }
}
