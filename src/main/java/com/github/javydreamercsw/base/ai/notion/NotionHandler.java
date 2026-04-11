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
package com.github.javydreamercsw.base.ai.notion;

import com.github.javydreamercsw.base.ai.notion.handler.FactionNotionHandler;
import com.github.javydreamercsw.base.ai.notion.handler.FactionRivalryNotionHandler;
import com.github.javydreamercsw.base.ai.notion.handler.HeatNotionHandler;
import com.github.javydreamercsw.base.ai.notion.handler.InjuryNotionHandler;
import com.github.javydreamercsw.base.ai.notion.handler.NpcNotionHandler;
import com.github.javydreamercsw.base.ai.notion.handler.RivalryNotionHandler;
import com.github.javydreamercsw.base.ai.notion.handler.SeasonNotionHandler;
import com.github.javydreamercsw.base.ai.notion.handler.SegmentNotionHandler;
import com.github.javydreamercsw.base.ai.notion.handler.ShowNotionHandler;
import com.github.javydreamercsw.base.ai.notion.handler.ShowTemplateNotionHandler;
import com.github.javydreamercsw.base.ai.notion.handler.ShowTypeNotionHandler;
import com.github.javydreamercsw.base.ai.notion.handler.TeamNotionHandler;
import com.github.javydreamercsw.base.ai.notion.handler.TitleNotionHandler;
import com.github.javydreamercsw.base.ai.notion.handler.TitleReignNotionHandler;
import com.github.javydreamercsw.base.ai.notion.handler.WrestlerNotionHandler;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.base.util.NotionBlocksRetriever;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.NotionClient;
import notion.api.v1.http.OkHttp4Client;
import notion.api.v1.model.databases.QueryResults;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageProperty;
import notion.api.v1.model.search.DatabaseSearchResult;
import notion.api.v1.model.search.SearchResults;
import notion.api.v1.request.databases.QueryDatabaseRequest;
import notion.api.v1.request.search.SearchRequest;
import org.springframework.stereotype.Service;

@Getter
@Slf4j
@Service
public class NotionHandler {

  /**
   * Checks if the Notion token is available in the environment.
   *
   * @return true if the token is available, false otherwise.
   */
  public boolean isNotionTokenAvailable() {
    return EnvironmentVariableUtil.isNotionTokenAvailable();
  }

  private final Map<String, String> databaseMap = new HashMap<>();
  private final WrestlerNotionHandler wrestlerNotionHandler;
  private final ShowNotionHandler showNotionHandler;
  private final SegmentNotionHandler segmentNotionHandler;
  private final HeatNotionHandler heatNotionHandler;
  private final TeamNotionHandler teamNotionHandler;
  private final SeasonNotionHandler seasonNotionHandler;
  private final FactionNotionHandler factionNotionHandler;
  private final TitleNotionHandler titleNotionHandler;
  private final NpcNotionHandler npcNotionHandler;
  private final InjuryNotionHandler injuryNotionHandler;
  private final RivalryNotionHandler rivalryNotionHandler;
  private final FactionRivalryNotionHandler factionRivalryNotionHandler;
  private final ShowTemplateNotionHandler showTemplateNotionHandler;
  private final TitleReignNotionHandler titleReignNotionHandler;
  private final ShowTypeNotionHandler showTypeNotionHandler;

  public NotionHandler() {
    // Only initialize databases if NOTION_TOKEN is available
    if (isNotionTokenAvailable()) {
      try {
        initializeDatabases();
      } catch (Exception e) {
        log.warn("Failed to initialize Notion databases: {}", e.getMessage());
      }
    } else {
      log.info("NOTION_TOKEN not available, skipping database initialization");
    }
    this.wrestlerNotionHandler =
        new com.github.javydreamercsw.base.ai.notion.handler.WrestlerNotionHandler(this);
    this.showNotionHandler =
        new com.github.javydreamercsw.base.ai.notion.handler.ShowNotionHandler(this);
    this.segmentNotionHandler = new SegmentNotionHandler(this);
    this.heatNotionHandler = new HeatNotionHandler(this);
    this.teamNotionHandler = new TeamNotionHandler(this);
    this.seasonNotionHandler = new SeasonNotionHandler(this);
    this.factionNotionHandler = new FactionNotionHandler(this);
    this.titleNotionHandler = new TitleNotionHandler(this);
    this.npcNotionHandler = new NpcNotionHandler(this);
    this.injuryNotionHandler = new InjuryNotionHandler(this);
    this.rivalryNotionHandler = new RivalryNotionHandler(this);
    this.factionRivalryNotionHandler = new FactionRivalryNotionHandler(this);
    this.showTemplateNotionHandler = new ShowTemplateNotionHandler(this);
    this.titleReignNotionHandler = new TitleReignNotionHandler(this);
    this.showTypeNotionHandler = new ShowTypeNotionHandler(this);
  }

  /**
   * Initializes the database map by loading all databases from Notion workspace. This method is
   * called automatically on first access to the singleton.
   */
  private void initializeDatabases() {
    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      return;
    }

    log.debug("Initializing NotionHandler - loading databases from workspace");

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        log.warn("NotionClient not available, skipping database initialization.");
        return;
      }
      loadDatabases(client);
      log.debug("NotionHandler initialized successfully with {} databases", databaseMap.size());
    } catch (Exception e) {
      log.error("Failed to initialize NotionHandler", e);
      throw new RuntimeException("Failed to initialize NotionHandler", e);
    }
  }

  /** Loads all databases from the Notion workspace into the internal map. */
  private void loadDatabases(@NonNull NotionClient client) {
    try {
      log.debug("Starting database search in Notion workspace");

      String nextCursor = null;
      boolean hasMore = true;
      databaseMap.clear();

      while (hasMore) {
        final String cursor = nextCursor;
        // Search for all databases in the workspace
        SearchRequest.SearchFilter searchFilter =
            new SearchRequest.SearchFilter("database", "object");
        SearchRequest searchRequest = new SearchRequest("", searchFilter, null, cursor, null);
        SearchResults searchResults =
            NotionUtil.executeWithRetry(() -> client.search(searchRequest));

        log.debug("Found {} database results from Notion API", searchResults.getResults().size());

        searchResults
            .getResults()
            .forEach(
                result -> {
                  DatabaseSearchResult database = result.asDatabase();
                  if (database.getTitle() != null && !database.getTitle().isEmpty()) {
                    String name = database.getTitle().get(0).getPlainText();
                    String id = database.getId();

                    // Store in map for later reference
                    databaseMap.put(name, id);

                    log.debug("Loaded database: '{}' -> {}", name, id);
                  }
                });

        hasMore = searchResults.getHasMore();
        nextCursor = searchResults.getNextCursor();
      }

      log.debug("Database loading completed. Total databases loaded: {}", databaseMap.size());

    } catch (Exception e) {
      log.error("Error loading databases from Notion", e);
      throw e;
    }
  }

  /**
   * Queries a specific database and prints all page properties. This is the original functionality
   * from the main method.
   */
  public void querySpecificDatabase(@NonNull String databaseId) {
    log.debug("Querying specific database: {}", databaseId);

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        log.warn("NotionClient not available, skipping database query.");
        return;
      }
      querySpecificDatabase(client, databaseId);
    } catch (Exception e) {
      log.error("Failed to query database: {}", databaseId, e);
      throw new RuntimeException("Failed to query database: " + databaseId, e);
    }
  }

  /** Internal method to query a specific database with an existing client. */
  private void querySpecificDatabase(@NonNull NotionClient client, String databaseId) {
    // PrintStream originalOut = System.out;
    try {
      log.debug("Creating query request for database: {}", databaseId);

      // Create an empty query request (no filters, returns all results)
      QueryDatabaseRequest queryRequest = new QueryDatabaseRequest(databaseId);

      // Send query request
      List<Page> results =
          NotionUtil.executeWithRetry(() -> client.queryDatabase(queryRequest)).getResults();

      log.debug("Found {} pages in database {}", results.size(), databaseId);

      results.forEach(
          page -> {
            log.debug("Processing page: {}", page.getId());

            Page pageData =
                NotionUtil.executeWithRetry(
                    () -> client.retrievePage(page.getId(), Collections.emptyList()));

            pageData
                .getProperties()
                .forEach(
                    (key, value) -> {
                      String valueStr = NotionUtil.getValue(client, value);
                      log.debug("Page {}: {} = {}", page.getId(), key, valueStr);
                    });
          });

      log.debug("Completed querying database: {}", databaseId);

    } catch (Exception e) {
      log.error("Error querying database: {}", databaseId, e);
      throw e;
    }
  }

  /**
   * Gets the database ID for a given database name.
   *
   * @param databaseName The name of the database
   * @return The database ID, or null if not found
   */
  public String getDatabaseId(@NonNull String databaseName) {
    if (databaseMap.isEmpty() && isNotionTokenAvailable()) {
      initializeDatabases();
    }
    log.debug("Looking up database ID for: '{}'", databaseName);
    String id = databaseMap.get(databaseName);
    if (id != null) {
      log.debug("Found database ID: {} for '{}'", id, databaseName);
    } else {
      log.debug("Database '{}' not found in loaded databases", databaseName);
    }
    return id;
  }

  /**
   * Gets all page IDs from a given database.
   *
   * @param databaseName The name of the database
   * @return List of page IDs
   */
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.NOTION_QUERIES_CACHE,
      key = "#databaseName")
  public List<String> getDatabasePageIds(@NonNull String databaseName) {
    log.debug("Loading all page IDs from database: '{}'", databaseName);

    String dbId = getDatabaseId(databaseName);
    if (dbId == null) {
      log.warn("'{}' database not found in workspace", databaseName);
      return new ArrayList<>();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        return new ArrayList<>();
      }
      return loadAllEntitiesFromDatabase(client, dbId, databaseName, (page, name) -> page.getId());
    } catch (Exception e) {
      log.error("Failed to load all page IDs from database '{}'", databaseName, e);
      return new ArrayList<>();
    }
  }

  /**
   * Loads a wrestler from the Notion database by name.
   *
   * @param wrestlerName The name of the wrestler to load (e.g., "Rob Van Dam")
   * @return Optional containing the WrestlerPage object if found, empty otherwise
   */
  public Optional<WrestlerPage> loadWrestler(@NonNull String wrestlerName) {
    return wrestlerNotionHandler.loadByName(wrestlerName);
  }

  /**
   * Loads all wrestlers from the Notion Wrestlers database for sync operations. This method is
   * optimized for bulk operations and extracts only essential properties.
   *
   * @return List of WrestlerPage objects with basic properties populated
   */
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.NOTION_SYNC_CACHE,
      key = "'wrestlers'")
  public List<WrestlerPage> loadAllWrestlers() {
    return wrestlerNotionHandler.loadAll();
  }

  /**
   * Loads all wrestlers from the Notion Wrestlers database.
   *
   * @param syncMode If true, loads minimal data for sync operations (faster). If false, loads full
   *     data with all relationships (slower).
   * @return List of WrestlerPage objects from the Wrestlers database
   */
  public List<WrestlerPage> loadAllWrestlers(boolean syncMode) {
    return wrestlerNotionHandler.loadAll(syncMode);
  }

  // ==================== SHOW LOADING METHODS ====================

  /**
   * Loads a show from the Notion database by ID.
   *
   * @param showId The ID of the show to load.
   * @return Optional containing the ShowPage object if found, empty otherwise.
   */
  public Optional<ShowPage> loadShowById(@NonNull String showId) {
    return showNotionHandler.loadById(showId);
  }

  /**
   * Loads all shows from the Notion Shows database with minimal processing for sync operations.
   * This method is optimized for bulk operations and extracts only essential properties.
   *
   * @return List of ShowPage objects with basic properties populated
   */
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.NOTION_SYNC_CACHE,
      key = "'shows'")
  public List<ShowPage> loadAllShowsForSync() {
    return showNotionHandler.loadAll();
  }

  public Optional<ShowTemplatePage> loadShowTemplate(@NonNull String templateName) {
    return showTemplateNotionHandler.loadShowTemplate(templateName);
  }

  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.NOTION_SYNC_CACHE,
      key = "'showTemplates'")
  public List<ShowTemplatePage> loadAllShowTemplates() {
    return showTemplateNotionHandler.loadAllShowTemplates();
  }

  public Map<String, ShowTemplatePage> retrieveShowTemplateData(
      @NonNull List<String> templateNames) {
    return showTemplateNotionHandler.retrieveShowTemplateData(templateNames);
  }

  // ==================== MATCH LOADING METHODS ====================

  public Optional<SegmentPage> loadSegment(@NonNull String segmentName) {
    return segmentNotionHandler.loadSegment(segmentName);
  }

  public Optional<SegmentPage> loadSegmentById(@NonNull String segment) {
    return segmentNotionHandler.loadSegmentById(segment);
  }

  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.NOTION_SYNC_CACHE,
      key = "'segments'")
  public List<SegmentPage> loadAllSegments() {
    return segmentNotionHandler.loadAllSegments();
  }

  public Optional<HeatPage> loadHeat(@NonNull String heatName) {
    return heatNotionHandler.loadHeat(heatName);
  }

  public Optional<TeamPage> loadTeam(@NonNull String teamName) {
    return teamNotionHandler.loadTeam(teamName);
  }

  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.NOTION_SYNC_CACHE,
      key = "'teams'")
  public List<TeamPage> loadAllTeams() {
    return teamNotionHandler.loadAllTeams();
  }

  public Optional<SeasonPage> loadSeason(@NonNull String seasonName) {
    return seasonNotionHandler.loadSeason(seasonName);
  }

  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.NOTION_SYNC_CACHE,
      key = "'seasons'")
  public List<SeasonPage> loadAllSeasons() {
    return seasonNotionHandler.loadAllSeasons();
  }

  public Optional<FactionPage> loadFaction(@NonNull String factionName) {
    return factionNotionHandler.loadFaction(factionName);
  }

  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.NOTION_SYNC_CACHE,
      key = "'factions'")
  public List<FactionPage> loadAllFactions() {
    return factionNotionHandler.loadAllFactions();
  }

  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.NOTION_SYNC_CACHE,
      key = "'titles'")
  public List<TitlePage> loadAllTitles() {
    return titleNotionHandler.loadAllTitles();
  }

  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.NOTION_SYNC_CACHE,
      key = "'npcs'")
  public List<NpcPage> loadAllNpcs() {
    return npcNotionHandler.loadAllNpcs();
  }

  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.NOTION_SYNC_CACHE,
      key = "'injuries'")
  public List<InjuryPage> loadAllInjuries() {
    return injuryNotionHandler.loadAllInjuries();
  }

  // ==================== GENERIC HELPER METHODS ====================

  /**
   * Loads a Notion page by its ID.
   *
   * @param pageId The ID of the page to load.
   * @return Optional containing the Page object if found, empty otherwise.
   */
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.NOTION_PAGES_CACHE,
      key = "#pageId")
  public Optional<Page> loadPage(@NonNull String pageId) {
    log.debug("Loading page with ID: '{}'", pageId);
    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        return Optional.empty();
      }
      return Optional.of(
          NotionUtil.executeWithRetry(
              () -> {
                synchronized (client) {
                  return client.retrievePage(pageId, Collections.emptyList());
                }
              }));
    } catch (Exception e) {
      log.error("Failed to load page with ID: {}", pageId, e);
      return Optional.empty();
    }
  }

  /**
   * Creates a NotionClient safely, checking for token availability.
   *
   * @return NotionClient instance, or null if token is not available
   */
  public Optional<NotionClient> createNotionClient() {
    String notionToken = EnvironmentVariableUtil.getNotionToken();
    if (notionToken == null || notionToken.trim().isEmpty()) {
      log.warn("NOTION_TOKEN not available. Cannot create NotionClient.");
      return Optional.empty();
    }
    NotionClient client = new NotionClient(notionToken);
    client.setHttpClient(new OkHttp4Client(60_000, 60_000, 60_000));
    client.setLogger(new notion.api.v1.logging.Slf4jLogger());
    return Optional.of(client);
  }

  /** Optimized method to load all entities for sync operations with minimal processing. */
  public List<ShowPage> loadAllEntitiesForSync(
      @NonNull NotionClient client, @NonNull String databaseId, @NonNull String entityType) {

    List<ShowPage> entities = new ArrayList<>();
    try {
      log.debug("Loading all {} entities for sync from database {}", entityType, databaseId);

      QueryDatabaseRequest queryRequest = new QueryDatabaseRequest(databaseId);

      List<Page> results =
          NotionUtil.executeWithRetry(() -> client.queryDatabase(queryRequest)).getResults();

      log.debug("Found {} pages in {} database for sync", results.size(), entityType);

      // Process pages in batches to avoid overwhelming the API
      int batchSize = 10;
      for (int i = 0; i < results.size(); i += batchSize) {
        int endIndex = Math.min(i + batchSize, results.size());
        List<Page> batch = results.subList(i, endIndex);

        log.debug("Processing batch {}-{} of {} pages", i + 1, endIndex, results.size());

        for (Page page : batch) {
          try {
            // Create ShowPage with minimal processing - just extract basic properties
            ShowPage showPage = createMinimalShowPage(page);
            entities.add(showPage);

          } catch (Exception e) {
            log.warn(
                "Failed to process {} entity from page {}: {}",
                entityType,
                page.getId(),
                e.getMessage());
          }
        }

        // Small delay between batches to be respectful to the API
        if (endIndex < results.size()) {
          try {
            Thread.sleep(100); // 100ms delay between batches
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }

      log.debug("Successfully loaded {} {} entities for sync", entities.size(), entityType);
      return entities;

    } catch (Exception e) {
      log.error(
          "Error loading all {} entities for sync from database: {}", entityType, databaseId, e);
      return entities; // Return partial results
    }
  }

  /** Generic method to load all entities from a database. */
  public <T> List<T> loadAllEntitiesFromDatabase(
      @NonNull NotionClient client,
      @NonNull String databaseId,
      @NonNull String entityType,
      @NonNull java.util.function.BiFunction<Page, String, T> mapper) {

    List<T> entities = new ArrayList<>();
    try {
      log.debug("Loading all {} entities from database {}", entityType, databaseId);
      String nextCursor = null;
      boolean hasMore;

      List<Page> results = new ArrayList<>();

      do {
        QueryDatabaseRequest queryRequest = new QueryDatabaseRequest(databaseId);
        queryRequest.setStartCursor(nextCursor);

        QueryResults queryResults =
            NotionUtil.executeWithRetry(() -> client.queryDatabase(queryRequest));
        results.addAll(queryResults.getResults());
        hasMore = queryResults.getHasMore();
        nextCursor = queryResults.getNextCursor();

        log.debug(
            "Found {} pages in {} database (hasMore: {}, nextCursor: {})",
            results.size(),
            entityType,
            hasMore,
            nextCursor);
      } while (hasMore);

      // Parallelize the mapping of pages to entities to improve performance
      entities =
          results.parallelStream()
              .map(page -> mapPageToEntity(page, client, entityType, mapper))
              .filter(java.util.Objects::nonNull)
              .collect(java.util.stream.Collectors.toList());

      log.debug("Successfully loaded {} {} entities from database", entities.size(), entityType);
      return entities;

    } catch (Exception e) {
      log.error("Error loading all {} entities from database: {}", entityType, databaseId, e);
      return entities; // Return partial results
    }
  }

  /**
   * Generic method to load all entity IDs from a database.
   *
   * @param client The NotionClient instance.
   * @param databaseId The ID of the database.
   * @param entityType The type of entity being loaded (for logging).
   * @return A list of entity IDs (Strings).
   */
  public List<String> loadAllEntityIdsFromDatabase(
      @NonNull NotionClient client, @NonNull String databaseId, @NonNull String entityType) {
    List<String> entityIds = new ArrayList<>();
    try {
      log.debug("Loading all {} entity IDs from database {}", entityType, databaseId);
      String nextCursor = null;
      boolean hasMore;

      List<Page> results = new ArrayList<>();

      do {
        QueryDatabaseRequest queryRequest = new QueryDatabaseRequest(databaseId);
        queryRequest.setStartCursor(nextCursor);

        QueryResults queryResults =
            NotionUtil.executeWithRetry(() -> client.queryDatabase(queryRequest));
        results.addAll(queryResults.getResults());
        hasMore = queryResults.getHasMore();
        nextCursor = queryResults.getNextCursor();

        log.debug(
            "Found {} pages in {} database (hasMore: {}, nextCursor: {})",
            results.size(),
            entityType,
            hasMore,
            nextCursor);
      } while (hasMore);

      entityIds =
          results.parallelStream().map(Page::getId).collect(java.util.stream.Collectors.toList());

      log.debug("Successfully loaded {} {} entity IDs from database", entityIds.size(), entityType);
      return entityIds;

    } catch (Exception e) {
      log.error("Error loading all {} entity IDs from database: {}", entityType, databaseId, e);
      return new ArrayList<>(); // Return partial results
    }
  }

  public <T> T mapPageToEntity(
      Page page,
      NotionClient client,
      String entityType,
      java.util.function.BiFunction<Page, String, T> mapper) {
    try {
      Page pageData =
          NotionUtil.executeWithRetry(
              () -> client.retrievePage(page.getId(), Collections.emptyList()));

      // Get the name property for logging
      String entityName = "Unknown";
      PageProperty nameProperty = pageData.getProperties().get("Name");
      if (nameProperty != null
          && nameProperty.getTitle() != null
          && !nameProperty.getTitle().isEmpty()) {
        entityName = nameProperty.getTitle().get(0).getPlainText();
      }

      T entity = mapper.apply(pageData, entityName);
      log.debug("Loaded {} entity: {}", entityType, entityName);
      return entity;

    } catch (Exception e) {
      log.warn(
          "Failed to load {} entity from page {}: {}", entityType, page.getId(), e.getMessage());
      return null;
    }
  }

  /** Generic method to load any entity from a database by name. */
  public <T> Optional<T> loadEntityFromDatabase(
      @NonNull NotionClient client,
      @NonNull String databaseId,
      @NonNull String entityName,
      @NonNull String entityType,
      @NonNull java.util.function.BiFunction<Page, String, T> mapper) {

    // PrintStream originalOut = System.out;
    try {
      log.debug("Searching for {} '{}' in database {}", entityType, entityName, databaseId);

      QueryDatabaseRequest queryRequest = new QueryDatabaseRequest(databaseId);

      List<Page> results =
          NotionUtil.executeWithRetry(() -> client.queryDatabase(queryRequest)).getResults();

      log.debug("Found {} pages in {} database", results.size(), entityType);

      // Search for the entity by name
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

          if (entityName.equalsIgnoreCase(pageName)) {
            log.debug("Found matching {} page: {}", entityType, pageName);
            return Optional.of(mapper.apply(pageData, entityName));
          }
        }
      }

      log.debug("{} '{}' not found in database", entityType, entityName);
      return Optional.empty();

    } catch (Exception e) {
      log.error("Error loading {} '{}' from database: {}", entityType, entityName, databaseId, e);
      throw e;
    }
  }

  // ==================== MAPPING METHODS ====================

  /** Creates a minimal ShowPage object for sync operations without detailed property extraction. */
  private ShowPage createMinimalShowPage(@NonNull Page page) {
    ShowPage showPage = new ShowPage(this);

    // Set basic page properties
    showPage.setId(page.getId());
    showPage.setCreated_time(page.getCreatedTime());
    showPage.setLast_edited_time(page.getLastEditedTime());
    showPage.setArchived(Boolean.TRUE.equals(page.getArchived()));
    showPage.setIn_trash(Boolean.TRUE.equals(page.getInTrash()));
    showPage.setUrl(page.getUrl());
    showPage.setPublic_url(page.getPublicUrl());

    // Extract only essential properties for sync without detailed processing
    Map<String, Object> rawProperties = new HashMap<>();
    Map<String, PageProperty> pageProperties = page.getProperties();

    // Extract key properties with minimal processing
    pageProperties.forEach(
        (key, value) -> {
          try {
            String simpleValue = extractSimplePropertyValue(value);
            if (simpleValue != null) {
              rawProperties.put(key, simpleValue);
            } else {
              log.debug("Property {} extracted as null, skipping", key);
            }
          } catch (Exception e) {
            log.debug("Failed to extract property {}: {}", key, e.getMessage());
            // Don't put "N/A" - just skip the property if extraction fails
          }
        });

    showPage.setRawProperties(rawProperties);
    return showPage;
  }

  /** Extracts a simple string value from a PageProperty without complex processing. */
  private String extractSimplePropertyValue(PageProperty property) {
    if (property == null) return null; // Return null instead of "N/A" for missing properties

    try {
      // Handle different property types with minimal processing
      if (property.getTitle() != null && !property.getTitle().isEmpty()) {
        return property.getTitle().get(0).getPlainText();
      }
      if (property.getRichText() != null && !property.getRichText().isEmpty()) {
        return property.getRichText().get(0).getPlainText();
      }
      if (property.getSelect() != null) {
        return property.getSelect().getName();
      }
      if (property.getDate() != null) {
        return property.getDate().getStart();
      }
      if (property.getNumber() != null) {
        return property.getNumber().toString();
      }
      if (property.getRelation() != null && !property.getRelation().isEmpty()) {
        return property.getRelation().get(0).getId();
      }

      // Log when we can't extract a value instead of defaulting to "N/A"
      log.debug("Unable to extract value from property type: {}", property.getType());
      return null;

    } catch (Exception e) {
      log.debug("Error extracting property value: {}", e.getMessage());
      return null; // Return null instead of "N/A" for extraction errors
    }
  }

  public Optional<SegmentPage> getSegmentPage(@NonNull String segmentId) {
    return segmentNotionHandler.loadSegmentById(segmentId);
  }

  public List<ShowTypePage> getShowTypePages() {
    return showTypeNotionHandler.getShowTypePages();
  }

  public List<String> getSegmentIds() {
    return segmentNotionHandler.getSegmentIds();
  }

  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.NOTION_SYNC_CACHE,
      key = "'titleReigns'")
  public List<TitleReignPage> loadAllTitleReigns() {
    return titleReignNotionHandler.loadAllTitleReigns();
  }

  /** Generic mapping method for all entity types with full relationship resolution. */
  public <T, P> T mapPageToGenericEntity(
      @NonNull Page pageData,
      @NonNull String entityName,
      @NonNull String entityType,
      @NonNull java.util.function.Supplier<T> entityConstructor,
      @NonNull java.util.function.Supplier<P> parentConstructor) {
    return mapPageToGenericEntity(
        pageData, entityName, entityType, entityConstructor, parentConstructor, true);
  }

  /** Generic mapping method for all entity types with optional relationship resolution. */
  public <T, P> T mapPageToGenericEntity(
      @NonNull Page pageData,
      @NonNull String entityName,
      @NonNull String entityType,
      @NonNull java.util.function.Supplier<T> entityConstructor,
      @NonNull java.util.function.Supplier<P> parentConstructor,
      boolean resolveRelationships) {

    log.debug("Mapping Notion page to {} object for: {}", entityType, entityName);

    T entityPage = entityConstructor.get();

    // Set basic page information using reflection-like approach
    setBasicPageInfo(entityPage, pageData);

    // Set parent information
    P parent = parentConstructor.get();
    setParentInfo(parent, pageData);
    setParent(entityPage, parent);

    // Extract and log all property values, then set them on the entity page
    Map<String, Object> processedProperties =
        extractAndLogProperties(pageData, entityName, entityType, resolveRelationships);
    setRawProperties(entityPage, processedProperties);

    log.debug("Mapped {}Page for: {} with ID: {}", entityType, entityName, pageData.getId());

    return entityPage;
  }

  public void setBasicPageInfo(@NonNull Object entityPage, @NonNull Page pageData) {
    try {
      entityPage.getClass().getMethod("setObject", String.class).invoke(entityPage, "page");
      entityPage.getClass().getMethod("setId", String.class).invoke(entityPage, pageData.getId());
      entityPage
          .getClass()
          .getMethod("setCreated_time", String.class)
          .invoke(entityPage, pageData.getCreatedTime());
      entityPage
          .getClass()
          .getMethod("setLast_edited_time", String.class)
          .invoke(entityPage, pageData.getLastEditedTime());
      entityPage
          .getClass()
          .getMethod("setArchived", boolean.class)
          .invoke(entityPage, pageData.getArchived());
      entityPage.getClass().getMethod("setIn_trash", boolean.class).invoke(entityPage, false);
      entityPage.getClass().getMethod("setUrl", String.class).invoke(entityPage, pageData.getUrl());
      entityPage
          .getClass()
          .getMethod("setPublic_url", String.class)
          .invoke(entityPage, pageData.getPublicUrl());
    } catch (Exception e) {
      log.warn("Failed to set basic page info: {}", e.getMessage());
    }
  }

  /** Helper method to set parent information. */
  private void setParentInfo(@NonNull Object parent, @NonNull Page pageData) {
    try {
      parent.getClass().getMethod("setType", String.class).invoke(parent, "database_id");
      assert pageData.getParent() != null;
      parent
          .getClass()
          .getMethod("setDatabase_id", String.class)
          .invoke(parent, pageData.getParent().getDatabaseId());
    } catch (Exception e) {
      log.warn("Failed to set parent info: {}", e.getMessage());
    }
  }

  /** Helper method to set parent on entity page. */
  private void setParent(@NonNull Object entityPage, @NonNull Object parent) {
    try {
      entityPage
          .getClass()
          .getMethod("setParent", parent.getClass().getSuperclass())
          .invoke(entityPage, parent);
    } catch (Exception e) {
      // This is not critical for sync functionality, so just log at debug level
      log.debug(
          "Failed to set parent on {}: {} (this is not critical)",
          entityPage.getClass().getSimpleName(),
          e.getMessage());
    }
  }

  public void setRawProperties(
      @NonNull Object entityPage, @NonNull Map<String, Object> properties) {
    try {
      entityPage.getClass().getMethod("setRawProperties", Map.class).invoke(entityPage, properties);
      log.debug("Set {} raw properties on entity page", properties.size());
    } catch (Exception e) {
      log.warn("Failed to set raw properties: {}", e.getMessage());
    }
  }

  /** Helper method to extract and log properties with optional relationship resolution. */
  private Map<String, Object> extractAndLogProperties(
      @NonNull Page pageData,
      @NonNull String entityName,
      @NonNull String entityType,
      boolean resolveRelationships) {
    Optional<NotionClient> clientOptional = createNotionClient();
    if (clientOptional.isPresent()) {
      try (NotionClient client = clientOptional.get()) {
        Map<String, PageProperty> properties = pageData.getProperties();
        log.debug("Extracting {} properties for {}: {}", properties.size(), entityType, entityName);

        Map<String, Object> processedProperties = new HashMap<>();

        properties.forEach(
            (key, value) -> {
              try {
                String valueStr = NotionUtil.getValue(client, value);
                if (valueStr != null) {
                  processedProperties.put(key, valueStr);
                }
              } catch (Exception e) {
                log.warn(
                    "Failed to extract property {} for {}: {}", key, entityName, e.getMessage());
              }
            });
        return processedProperties;
      } catch (Exception e) {
        log.error("Error extracting properties for {}: {}", entityName, e.getMessage());
        return new HashMap<>();
      }
    } else {
      log.warn(
          "NotionClient not available, returning empty properties for {}: {}",
          entityType,
          entityName);
      return new HashMap<>();
    }
  }

  /**
   * Extracts the first segment name from a show's matches relation property.
   *
   * @param showPage The ShowPage object to extract matches from
   * @return The name of the first segment, or null if no matches found
   */
  public String extractFirstMatchFromShow(@NonNull ShowPage showPage) {
    log.debug("Extracting first segment from show: {}", showPage.getId());

    Optional<NotionClient> clientOptional = createNotionClient();
    if (clientOptional.isEmpty()) {
      log.warn(
          "NotionClient not available, returning null for first match from show: {}",
          showPage.getId());
      return null;
    }

    try (NotionClient client = clientOptional.get()) {
      try {
        Page pageData =
            NotionUtil.executeWithRetry(
                () -> client.retrievePage(showPage.getId(), Collections.emptyList()));

        Map<String, PageProperty> properties = pageData.getProperties();

        // Look for a "Segments" relation property
        PageProperty matchesProperty = properties.get("Segments");
        if (matchesProperty != null
            && matchesProperty.getRelation() != null
            && !matchesProperty.getRelation().isEmpty()) {
          // Get the first related segment - using generic approach since Relation type may not be
          // accessible
          Object firstMatch = matchesProperty.getRelation().get(0);
          String matchId = null;

          // Use reflection to get the ID since we can't access the Relation type directly
          try {
            matchId = (String) firstMatch.getClass().getMethod("getId").invoke(firstMatch);
            return matchId;
          } catch (Exception e) {
            log.error("Failed to extract match ID from relation object: {}", e.getMessage());
          }
        }

      } catch (Exception e) {
        log.error("Error extracting first match from show: {}", e.getMessage());
      }
    } catch (Exception e) {
      log.error("Error processing with NotionClient for show: {}", e.getMessage());
    }

    return null;
  }

  /**
   * Retrieves the plain text content of a Notion page by its ID. This method fetches all blocks
   * within the page and extracts their plain text.
   *
   * @param pageId The ID of the Notion page.
   * @return The concatenated plain text content of the page, or an empty string if no content.
   */
  public String getPageContentPlainText(@NonNull String pageId) {
    log.debug("Retrieving content for page: {}", pageId);
    String notionToken = EnvironmentVariableUtil.getNotionToken();
    if (notionToken == null || notionToken.trim().isEmpty()) {
      log.warn("NOTION_TOKEN not available. Cannot retrieve page content.");
      return "";
    }
    NotionBlocksRetriever retriever = new NotionBlocksRetriever(notionToken);
    return retriever.retrievePageContent(pageId);
  }

  /**
   * Loads all rivalries from the Notion Heat database.
   *
   * @return List of all RivalryPage objects from the Heat database
   */
  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.NOTION_SYNC_CACHE,
      key = "'rivalries'")
  public List<RivalryPage> loadAllRivalries() {
    return rivalryNotionHandler.loadAllRivalries();
  }

  @org.springframework.cache.annotation.Cacheable(
      value = com.github.javydreamercsw.management.config.CacheConfig.NOTION_SYNC_CACHE,
      key = "'factionRivalries'")
  public List<FactionRivalryPage> loadAllFactionRivalries() {
    return factionRivalryNotionHandler.loadAllFactionRivalries();
  }

  public <T> T executeWithRetry(@NonNull Supplier<T> action) {
    return NotionUtil.executeWithRetry(action);
  }
}
