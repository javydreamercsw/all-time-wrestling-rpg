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
import com.github.javydreamercsw.base.ai.notion.NotionUtil;
import com.github.javydreamercsw.base.ai.notion.WrestlerPage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.NotionClient;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageProperty;
import notion.api.v1.request.databases.QueryDatabaseRequest;

@Slf4j
public class WrestlerNotionHandler implements NotionEntityHandler<WrestlerPage> {

  private final NotionHandler notionHandler;

  public WrestlerNotionHandler(NotionHandler notionHandler) {
    this.notionHandler = notionHandler;
  }

  @Override
  public String getDatabaseName() {
    return "Wrestlers";
  }

  @Override
  public Optional<WrestlerPage> loadById(@NonNull String id) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Optional<WrestlerPage> loadByName(@NonNull String name) {
    log.debug("Loading wrestler: '{}'", name);

    // First, find the wrestlers database
    String wrestlerDbId = notionHandler.getDatabaseId(getDatabaseName());
    if (wrestlerDbId == null) {
      log.warn("Wrestlers database not found in workspace");
      return Optional.empty();
    }

    try (NotionClient client = notionHandler.createNotionClient().orElse(null)) {
      if (client == null) {
        return Optional.empty();
      }
      return loadWrestlerFromDatabase(client, wrestlerDbId, name);
    } catch (Exception e) {
      log.error("Failed to load wrestler: {}", name, e);
      return Optional.empty();
    }
  }

  @Override
  public List<WrestlerPage> loadAll() {
    return loadAll(true);
  }

  @Override
  public List<WrestlerPage> loadAll(boolean syncMode) {
    log.debug("Loading all wrestlers from Wrestlers database (syncMode: {})", syncMode);

    String wrestlerDbId = notionHandler.getDatabaseId(getDatabaseName());
    if (wrestlerDbId == null) {
      log.warn("Wrestlers database not found in workspace");
      return new ArrayList<>();
    }

    try (NotionClient client = notionHandler.createNotionClient().orElse(null)) {
      if (client == null) {
        return new ArrayList<>();
      }
      if (syncMode) {
        return notionHandler.loadAllEntitiesFromDatabase(
            client, wrestlerDbId, "Wrestler", this::mapPageToWrestlerPageSyncMode);
      } else {
        return notionHandler.loadAllEntitiesFromDatabase(
            client, wrestlerDbId, "Wrestler", this::mapPageToWrestlerPage);
      }
    } catch (Exception e) {
      log.error("Failed to load all wrestlers for sync", e);
      return new ArrayList<>();
    }
  }

  /** Internal method to load a wrestler from a specific database. */
  private Optional<WrestlerPage> loadWrestlerFromDatabase(
      @NonNull NotionClient client, @NonNull String databaseId, @NonNull String wrestlerName) {
    try {
      log.debug("Searching for wrestler '{}' in database {}", wrestlerName, databaseId);

      // Create a query request to find the wrestler by name
      QueryDatabaseRequest queryRequest = new QueryDatabaseRequest(databaseId);

      // Send query request
      List<Page> results =
          NotionUtil.executeWithRetry(() -> client.queryDatabase(queryRequest)).getResults();

      log.debug("Found {} pages in wrestlers database", results.size());

      // Search for the wrestler by name
      for (Page page : results) {
        Page pageData =
            NotionUtil.executeWithRetry(
                () -> client.retrievePage(page.getId(), Collections.emptyList()));

        // Get the name property
        PageProperty nameProperty = pageData.getProperties().get("Name");
        if (nameProperty != null
            && nameProperty.getTitle() != null
            && !nameProperty.getTitle().isEmpty()) {
          String pageName = nameProperty.getTitle().get(0).getPlainText();

          if (wrestlerName.equalsIgnoreCase(pageName)) {
            log.debug("Found matching wrestler page: {}", pageName);
            return Optional.of(mapPageToWrestlerPage(pageData, wrestlerName));
          }
        }
      }

      log.debug("Wrestler '{}' not found in database", wrestlerName);
      return Optional.empty();

    } catch (Exception e) {
      log.error("Error loading wrestler '{}' from database: {}", wrestlerName, databaseId, e);
      throw e;
    }
  }

  /** Maps a Notion page to a WrestlerPage object. */
  private WrestlerPage mapPageToWrestlerPage(@NonNull Page pageData, @NonNull String wrestlerName) {
    log.debug("Mapping Notion page to WrestlerPage object for: {}", wrestlerName);

    WrestlerPage wrestlerPage = new WrestlerPage();

    // Set basic page information using Lombok-generated setters
    wrestlerPage.setObject("page");
    wrestlerPage.setId(pageData.getId());
    wrestlerPage.setCreated_time(pageData.getCreatedTime());
    wrestlerPage.setLast_edited_time(pageData.getLastEditedTime());
    wrestlerPage.setArchived(Boolean.TRUE.equals(pageData.getArchived()));
    wrestlerPage.setIn_trash(false); // Default value
    wrestlerPage.setUrl(pageData.getUrl());
    wrestlerPage.setPublic_url(pageData.getPublicUrl());

    // Set parent information
    WrestlerPage.NotionParent parent = new WrestlerPage.NotionParent();
    parent.setType("database_id");
    assert pageData.getParent() != null;
    parent.setDatabase_id(pageData.getParent().getDatabaseId());
    wrestlerPage.setParent(parent);

    // Extract and log all property values using the same logic as querySpecificDatabase
    Optional<NotionClient> clientOptional = notionHandler.createNotionClient();
    Map<String, PageProperty> properties = pageData.getProperties();
    log.debug("Extracting {} properties for wrestler: {}", properties.size(), wrestlerName);

    // Create a map to store the processed property values
    Map<String, Object> processedProperties = new HashMap<>();

    clientOptional.ifPresentOrElse(
        client -> {
          try (client) {
            properties.forEach(
                (key, value) -> {
                  String valueStr = NotionUtil.getValue(client, value);
                  log.debug("Wrestler Property - {}: {}", key, valueStr);
                  processedProperties.put(key, valueStr);
                });
          } catch (Exception e) {
            log.error(
                "Error processing properties with NotionClient for wrestler: {}", wrestlerName, e);
          }
        },
        () -> {
          log.warn(
              "NotionClient not available, returning empty properties for wrestler: {}",
              wrestlerName);
        });

    // Set the processed properties on the wrestler page
    wrestlerPage.setRawProperties(processedProperties);

    log.debug("Mapped WrestlerPage for: {} with ID: {}", wrestlerName, wrestlerPage.getId());

    return wrestlerPage;
  }

  /** Maps a Notion page to a WrestlerPage object in sync mode (minimal processing). */
  private WrestlerPage mapPageToWrestlerPageSyncMode(
      @NonNull Page pageData, @NonNull String wrestlerName) {
    log.debug("Mapping Notion page to WrestlerPage object in sync mode for: {}", wrestlerName);

    WrestlerPage wrestlerPage = new WrestlerPage();

    // Set basic page information using the base class methods
    notionHandler.setBasicPageInfo(wrestlerPage, pageData);

    // Extract only essential properties for sync (no complex relationships)
    Map<String, Object> minimalProperties = new HashMap<>();
    Map<String, PageProperty> pageProperties = pageData.getProperties();

    // Extract only simple properties needed for sync
    for (Map.Entry<String, PageProperty> entry : pageProperties.entrySet()) {
      String key = entry.getKey();
      PageProperty property = entry.getValue();

      try {
        // Only extract simple properties, skip complex relationships like Matches, Heat, etc.
        if (property.getTitle() != null && !property.getTitle().isEmpty()) {
          minimalProperties.put(key, property.getTitle().get(0).getPlainText());
          log.debug(
              "Wrestler Property (sync mode) - {}: {}",
              key,
              property.getTitle().get(0).getPlainText());
        } else if (property.getRichText() != null && !property.getRichText().isEmpty()) {
          minimalProperties.put(key, property.getRichText().get(0).getPlainText());
          log.debug(
              "Wrestler Property (sync mode) - {}: {}",
              key,
              property.getRichText().get(0).getPlainText());
        } else if (property.getNumber() != null) {
          minimalProperties.put(key, property.getNumber());
          log.debug("Wrestler Property (sync mode) - {}: {}", key, property.getNumber());
        } else if (property.getSelect() != null) {
          minimalProperties.put(key, property.getSelect().getName());
          log.debug("Wrestler Property (sync mode) - {}: {}", key, property.getSelect().getName());
        } else if (property.getRelation() != null && !property.getRelation().isEmpty()) {
          // For complex properties, just store the count of relations without resolution
          int relationCount = property.getRelation().size();
          minimalProperties.put(key, relationCount + " relations");
          log.debug(
              "Wrestler Property (sync mode - count only) - {}: {} relations", key, relationCount);
        } else if (property.getPeople() != null && !property.getPeople().isEmpty()) {
          int peopleCount = property.getPeople().size();
          minimalProperties.put(key, peopleCount + " people");
          log.debug("Wrestler Property (sync mode - count only) - {}: {} people", key, peopleCount);
        } else if (property.getFormula() != null) {
          String formulaValue = NotionUtil.getFormulaValue(property.getFormula());
          minimalProperties.put(key, formulaValue);
          log.debug("Wrestler Property (sync mode) - {}: {}", key, formulaValue);
        } else if (property.getCreatedTime() != null) {
          minimalProperties.put(key, property.getCreatedTime());
          log.debug("Wrestler Property (sync mode) - {}: {}", key, property.getCreatedTime());
        } else if (property.getLastEditedTime() != null) {
          minimalProperties.put(key, property.getLastEditedTime());
          log.debug("Wrestler Property (sync mode) - {}: {}", key, property.getLastEditedTime());
        }
      } catch (Exception e) {
        log.debug(
            "Skipped property {} for wrestler {} in sync mode: {}",
            key,
            wrestlerName,
            e.getMessage());
      }
    }

    // Set the minimal properties
    notionHandler.setRawProperties(wrestlerPage, minimalProperties);

    log.debug(
        "Mapped WrestlerPage in sync mode for: {} with ID: {}", wrestlerName, wrestlerPage.getId());
    return wrestlerPage;
  }
}
