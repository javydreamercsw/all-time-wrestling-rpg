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

import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.base.util.NotionBlocksRetriever;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.NotionClient;
import notion.api.v1.exception.NotionAPIError;
import notion.api.v1.http.OkHttp4Client;
import notion.api.v1.model.databases.DatabaseProperty;
import notion.api.v1.model.databases.QueryResults;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageProperty;
import notion.api.v1.model.search.DatabaseSearchResult;
import notion.api.v1.model.search.SearchResults;
import notion.api.v1.model.users.User;
import notion.api.v1.request.databases.QueryDatabaseRequest;
import notion.api.v1.request.search.SearchRequest;

@Slf4j
public class NotionHandler {

  // Singleton instance
  private static volatile NotionHandler instance;

  // Map to store database names and their corresponding IDs for later reference
  protected final Map<String, String> databaseMap = new HashMap<>();

  // Flag to track if databases have been loaded
  private volatile boolean initialized = false;

  // Private constructor to prevent instantiation
  private NotionHandler() {}

  /** Constructor for testing purposes. */
  public NotionHandler(boolean test) {
    // Do nothing
  }

  /** Gets the singleton instance of NotionHandler. Initializes databases on first call. */
  public static Optional<NotionHandler> getInstance() {
    if (instance == null) {
      synchronized (NotionHandler.class) {
        if (instance == null) {
          NotionHandler tempInstance = new NotionHandler();
          if (tempInstance.initializeDatabases()) {
            instance = tempInstance;
          } else {
            return Optional.empty();
          }
        }
      }
    }
    return Optional.ofNullable(instance);
  }

  public static void main(String[] args) {
    // Example usage
    Optional<NotionHandler> handlerOptional = NotionHandler.getInstance();
    if (handlerOptional.isPresent()) {
      NotionHandler handler = handlerOptional.get();
      log.debug("Database map loaded with {} databases", handler.databaseMap.size());
      log.debug("You can now reference databases by name using getDatabaseId(name)");

      handler.databaseMap.forEach((key, value) -> log.debug("{}: {}", key, value));
    } else {
      log.error("NotionHandler could not be initialized. Check NOTION_TOKEN.");
    }
  }

  /**
   * Initializes the database map by loading all databases from Notion workspace. This method is
   * called automatically on first access to the singleton.
   */
  private boolean initializeDatabases() {
    if (initialized || !EnvironmentVariableUtil.isNotionTokenAvailable()) {
      return false;
    }

    log.debug("Initializing NotionHandler - loading databases from workspace");

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        log.warn("NotionClient not available, skipping database initialization.");
        return false;
      }
      loadDatabases(client);
      initialized = true;
      log.debug("NotionHandler initialized successfully with {} databases", databaseMap.size());
      return true;
    } catch (Exception e) {
      log.error("Failed to initialize NotionHandler", e);
      throw new RuntimeException("Failed to initialize NotionHandler", e);
    }
  }

  /** Loads all databases from the Notion workspace into the internal map. */
  private void loadDatabases(@NonNull NotionClient client) {
    try {
      log.debug("Starting database search in Notion workspace");

      // Search for all databases in the workspace
      SearchRequest.SearchFilter searchFilter =
          new SearchRequest.SearchFilter("database", "object");
      SearchRequest searchRequest = new SearchRequest("", searchFilter);
      SearchResults searchResults = executeWithRetry(() -> client.search(searchRequest));

      log.debug("Found {} database results from Notion API", searchResults.getResults().size());

      // Clear the map before populating it
      databaseMap.clear();

      searchResults
          .getResults()
          .forEach(
              result -> {
                DatabaseSearchResult database = result.asDatabase();
                String name = database.getTitle().get(0).getPlainText();
                String id = database.getId();

                // Store in map for later reference
                databaseMap.put(name, id);

                log.debug("Loaded database: '{}' -> {}", name, id);
              });

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
      List<Page> results = executeWithRetry(() -> client.queryDatabase(queryRequest)).getResults();

      log.debug("Found {} pages in database {}", results.size(), databaseId);

      results.forEach(
          page -> {
            log.debug("Processing page: {}", page.getId());

            Page pageData =
                executeWithRetry(() -> client.retrievePage(page.getId(), Collections.emptyList()));

            pageData
                .getProperties()
                .forEach(
                    (key, value) -> {
                      String valueStr = getValue(client, value);
                      log.debug("Page {}: {} = {}", page.getId(), key, valueStr);
                    });
          });

      log.debug("Completed querying database: {}", databaseId);

    } catch (Exception e) {
      log.error("Error querying database: {}", databaseId, e);
      throw e;
    }
  }

  public <T> T executeWithRetry(java.util.function.Supplier<T> action) {
    RetryPolicy<T> rateLimitPolicy =
        RetryPolicy.<T>builder()
            .handleIf(
                e -> {
                  if (e instanceof NotionAPIError notionError) {
                    String message = notionError.getMessage().toLowerCase();
                    if (message.contains("429")
                        || message.contains("rate limit")
                        || message.contains("too many requests")
                        || message.contains("rate_limited")) {
                      return true;
                    }
                    return notionError.getError().getStatus() == 429
                        || "rate_limited".equals(notionError.getError().getCode());
                  }
                  return false;
                })
            .withBackoff(1, 5, java.time.temporal.ChronoUnit.SECONDS)
            .withMaxRetries(3)
            .onRetry(e -> log.warn("Rate limited by Notion API. Retrying...", e.getLastException()))
            .build();

    RetryPolicy<T> serverRetryPolicy =
        RetryPolicy.<T>builder()
            .handleIf(
                e ->
                    e instanceof NotionAPIError
                        && ((NotionAPIError) e).getError().getStatus() >= 500)
            .withBackoff(1, 5, java.time.temporal.ChronoUnit.SECONDS)
            .withMaxRetries(3)
            .onRetry(e -> log.warn("Server side error. Retrying...", e.getLastException()))
            .build();

    return Failsafe.with(rateLimitPolicy, serverRetryPolicy).get(action::get);
  }

  public String getValue(@NonNull NotionClient client, @NonNull PageProperty value) {
    return getValue(client, value, true);
  }

  public String getValue(
      @NonNull NotionClient client, @NonNull PageProperty value, boolean resolveRelationships) {
    try {
      // Handle cases where type is null but we can infer the type from populated fields
      String propertyType;

      if (value.getType() != null) {
        propertyType = value.getType().getValue();
      } else {
        // Infer property type from populated fields when type is null
        propertyType = inferPropertyType(value);
        if (propertyType == null) {
          log.debug("Property type is null and cannot be inferred for property: {}", value);
          return "N/A";
        }
        log.debug("Inferred property type '{}' for property with null type", propertyType);
      }

      return switch (propertyType) {
        case "formula" -> value.getFormula() != null ? getFormulaValue(value.getFormula()) : "N/A";
        case "people" ->
            value.getPeople() != null && !value.getPeople().isEmpty()
                ? value.getPeople().stream().findFirst().map(User::getName).orElse("N/A")
                : "N/A";
        case "created_by" -> value.getCreatedBy() != null ? value.getCreatedBy().getName() : "N/A";
        case "last_edited_by" ->
            value.getLastEditedBy() != null ? value.getLastEditedBy().getName() : "N/A";
        case "created_time" -> value.getCreatedTime() != null ? value.getCreatedTime() : "N/A";
        case "number" -> value.getNumber() != null ? value.getNumber().toString() : "N/A";
        case "last_edited_time" ->
            value.getLastEditedTime() != null ? value.getLastEditedTime() : "N/A";
        case "unique_id" ->
            value.getUniqueId() != null
                ? value.getUniqueId().getPrefix() + "-" + value.getUniqueId().getNumber()
                : "N/A";
        case "title" ->
            value.getTitle() != null && !value.getTitle().isEmpty()
                ? value.getTitle().get(0).getPlainText()
                : "N/A";
        case "rich_text" -> {
          // Handle rich_text properties (commonly used for text fields in Notion)
          if (value.getRichText() != null && !value.getRichText().isEmpty()) {
            yield value.getRichText().stream()
                .map(PageProperty.RichText::getPlainText)
                .filter(text -> text != null && !text.trim().isEmpty())
                .reduce((a, b) -> a + " " + b)
                .orElse("N/A");
          } else {
            yield "N/A";
          }
        }
        case "relation" -> {
          if (value.getRelation() == null || value.getRelation().isEmpty()) {
            yield "N/A";
          } else if (!resolveRelationships) {
            // Fast mode: return comma-separated IDs without resolving names
            yield value.getRelation().stream()
                .map(PageProperty.PageReference::getId)
                .collect(Collectors.joining(", "));
          } else {
            // Full mode: resolve relationship names (expensive)
            yield value.getRelation().stream()
                .map(
                    relation -> {
                      Page relatedPage =
                          executeWithRetry(
                              () -> client.retrievePage(relation.getId(), Collections.emptyList()));

                      // Try multiple common title property names
                      String[] titlePropertyNames = {
                        "Name", "Title", "Title Name", "Championship", "name", "title"
                      };
                      for (String propertyName : titlePropertyNames) {
                        PageProperty titleProperty = relatedPage.getProperties().get(propertyName);
                        if (titleProperty != null
                            && titleProperty.getTitle() != null
                            && !titleProperty.getTitle().isEmpty()) {
                          return titleProperty.getTitle().get(0).getPlainText();
                        }
                      }

                      // If no title property found, log available properties for debugging
                      log.debug(
                          "No title property found for relation {}. Available properties: {}",
                          relation.getId(),
                          relatedPage.getProperties().keySet());
                      return relation.getId();
                    })
                .reduce((a, b) -> a + ", " + b)
                .orElse("N/A");
          }
        }
        case "select" -> {
          // Handle select properties (dropdown with single selection)
          if (value.getSelect() != null) {
            yield value.getSelect().getName();
          } else {
            yield "N/A";
          }
        }
        case "status" -> {
          // Handle status properties (workflow status)
          if (value.getStatus() != null) {
            yield value.getStatus().getName();
          } else {
            yield "N/A";
          }
        }
        case "multi_select" -> {
          // Handle multi_select properties (dropdown with multiple selections)
          if (value.getMultiSelect() != null && !value.getMultiSelect().isEmpty()) {
            yield value.getMultiSelect().stream()
                .map(DatabaseProperty.MultiSelect.Option::getName)
                .filter(name -> name != null && !name.trim().isEmpty())
                .reduce((a, b) -> a + ", " + b)
                .orElse("N/A");
          } else {
            yield "N/A";
          }
        }
        case "date" -> {
          // Handle date properties
          if (value.getDate() != null && value.getDate().getStart() != null) {
            String dateStr = value.getDate().getStart();
            // The Notion API sometimes prefixes dates with @, so we remove it.
            if (dateStr.startsWith("@")) {
              yield dateStr.substring(1);
            }
            yield dateStr;
          } else {
            yield "N/A";
          }
        }
        case "checkbox" -> {
          // Handle checkbox properties
          if (value.getCheckbox() != null) {
            yield value.getCheckbox().toString();
          } else {
            yield "false";
          }
        }
        case "url" -> {
          // Handle URL properties
          if (value.getUrl() != null && !value.getUrl().trim().isEmpty()) {
            yield value.getUrl();
          } else {
            yield "N/A";
          }
        }
        case "email" -> {
          // Handle email properties
          if (value.getEmail() != null && !value.getEmail().trim().isEmpty()) {
            yield value.getEmail();
          } else {
            yield "N/A";
          }
        }
        case "phone_number" -> {
          // Handle phone number properties
          if (value.getPhoneNumber() != null && !value.getPhoneNumber().trim().isEmpty()) {
            yield value.getPhoneNumber();
          } else {
            yield "N/A";
          }
        }
        default -> {
          // Log unhandled property types for debugging
          log.warn("Unhandled property type '{}' for property: {}", propertyType, value);
          yield "N/A";
        }
      };
    } catch (Exception e) {
      log.debug("Exception in getValue method: {}", e.getMessage());
      return "N/A";
    }
  }

  /**
   * Infers the property type from populated fields when the type field is null. This is a fallback
   * mechanism for cases where the Notion API doesn't properly set the type.
   */
  private String inferPropertyType(@NonNull PageProperty value) {
    // Check each possible property type by looking at which field is populated
    if (value.getTitle() != null && !value.getTitle().isEmpty()) {
      return "title";
    }
    if (value.getRichText() != null && !value.getRichText().isEmpty()) {
      return "rich_text";
    }
    if (value.getSelect() != null) {
      return "select";
    }
    if (value.getStatus() != null) {
      return "status";
    }
    if (value.getMultiSelect() != null && !value.getMultiSelect().isEmpty()) {
      return "multi_select";
    }
    if (value.getDate() != null) {
      return "date";
    }
    if (value.getCheckbox() != null) {
      return "checkbox";
    }
    if (value.getNumber() != null) {
      return "number";
    }
    if (value.getUrl() != null) {
      return "url";
    }
    if (value.getEmail() != null) {
      return "email";
    }
    if (value.getPhoneNumber() != null) {
      return "phone_number";
    }
    if (value.getPeople() != null && !value.getPeople().isEmpty()) {
      return "people";
    }
    if (value.getRelation() != null && !value.getRelation().isEmpty()) {
      return "relation";
    }
    if (value.getFormula() != null) {
      return "formula";
    }
    if (value.getCreatedBy() != null) {
      return "created_by";
    }
    if (value.getLastEditedBy() != null) {
      return "last_edited_by";
    }
    if (value.getCreatedTime() != null) {
      return "created_time";
    }
    if (value.getLastEditedTime() != null) {
      return "last_edited_time";
    }
    if (value.getUniqueId() != null) {
      return "unique_id";
    }

    // Could not infer type
    return null;
  }

  /** Helper method to extract values from formula properties based on their result type. */
  private String getFormulaValue(@NonNull PageProperty.Formula formula) {

    // Check the formula result type and extract accordingly
    if (formula.getString() != null) {
      return formula.getString();
    } else if (formula.getNumber() != null) {
      return formula.getNumber().toString();
    } else if (formula.getBoolean() != null) {
      return formula.getBoolean().toString();
    } else if (formula.getDate() != null) {
      // Handle formula date - extract the start date and format it properly
      if (formula.getDate().getStart() != null) {
        String dateStr = formula.getDate().getStart();
        // The Notion API sometimes prefixes dates with @, so we remove it.
        if (dateStr.startsWith("@")) {
          return dateStr.substring(1);
        }
        return dateStr;
      } else {
        return "N/A";
      }
    } else {
      return "null";
    }
  }

  /**
   * Gets the database ID for a given database name.
   *
   * @param databaseName The name of the database
   * @return The database ID, or null if not found
   */
  public String getDatabaseId(@NonNull String databaseName) {
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
      return loadAllEntitiesFromDatabase(
          client, dbId, databaseName, (page, name) -> page.getId(), false);
    } catch (Exception e) {
      log.error("Failed to load all page IDs from database '{}'", databaseName, e);
      return new ArrayList<>();
    }
  }

  /**
   * Gets all loaded databases as a map of name -> ID.
   *
   * @return A copy of the database map
   */
  public Map<String, String> getAllDatabases() {
    log.debug("Returning copy of all {} loaded databases", databaseMap.size());
    return new HashMap<>(databaseMap);
  }

  /**
   * Checks if a database with the given name exists.
   *
   * @param databaseName The name of the database
   * @return true if the database exists, false otherwise
   */
  public boolean databaseExists(@NonNull String databaseName) {
    boolean exists = databaseMap.containsKey(databaseName);
    log.debug("Database '{}' exists: {}", databaseName, exists);
    return exists;
  }

  // Static convenience methods for backward compatibility
  /**
   * Static convenience method to get database ID.
   *
   * @param databaseName The name of the database
   * @return The database ID, or null if not found
   */
  public static String getStaticDatabaseId(String databaseName) {
    return getInstance().map(handler -> handler.getDatabaseId(databaseName)).orElse(null);
  }

  /**
   * Static convenience method to get all databases.
   *
   * @return A copy of the database map
   */
  public static Map<String, String> getStaticAllDatabases() {
    return getInstance().map(NotionHandler::getAllDatabases).orElse(Collections.emptyMap());
  }

  /**
   * Static convenience method to check if database exists.
   *
   * @param databaseName The name of the database
   * @return true if the database exists, false otherwise
   */
  public static boolean staticDatabaseExists(@NonNull String databaseName) {
    return getInstance().map(handler -> handler.databaseExists(databaseName)).orElse(false);
  }

  /**
   * Loads a wrestler from the Notion database by name.
   *
   * @param wrestlerName The name of the wrestler to load (e.g., "Rob Van Dam")
   * @return Optional containing the WrestlerPage object if found, empty otherwise
   */
  public Optional<WrestlerPage> loadWrestler(@NonNull String wrestlerName) {
    log.debug("Loading wrestler: '{}'", wrestlerName);

    // First, find the wrestlers database
    String wrestlerDbId = getDatabaseId("Wrestlers");
    if (wrestlerDbId == null) {
      log.warn("Wrestlers database not found in workspace");
      return Optional.empty();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        return Optional.empty();
      }
      return loadWrestlerFromDatabase(client, wrestlerDbId, wrestlerName);
    } catch (Exception e) {
      log.error("Failed to load wrestler: {}", wrestlerName, e);
      return Optional.empty();
    }
  }

  /**
   * Static convenience method to load a wrestler.
   *
   * @param wrestlerName The name of the wrestler to load
   * @return Optional containing the WrestlerPage object if found, empty otherwise
   */
  public static Optional<WrestlerPage> loadWrestlerStatic(@NonNull String wrestlerName) {
    return getInstance().flatMap(handler -> handler.loadWrestler(wrestlerName));
  }

  /**
   * Loads all wrestlers from the Notion Wrestlers database for sync operations. This method is
   * optimized for bulk operations and extracts only essential properties.
   *
   * @return List of WrestlerPage objects with basic properties populated
   */
  public List<WrestlerPage> loadAllWrestlers() {
    return loadAllWrestlers(true); // Default to sync mode for performance
  }

  /**
   * Loads all wrestlers from the Notion Wrestlers database.
   *
   * @param syncMode If true, loads minimal data for sync operations (faster). If false, loads full
   *     data with all relationships (slower).
   * @return List of WrestlerPage objects from the Wrestlers database
   */
  public List<WrestlerPage> loadAllWrestlers(boolean syncMode) {
    log.debug("Loading all wrestlers from Wrestlers database (syncMode: {})", syncMode);

    String wrestlerDbId = getDatabaseId("Wrestlers");
    if (wrestlerDbId == null) {
      log.warn("Wrestlers database not found in workspace");
      return new ArrayList<>();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        return new ArrayList<>();
      }
      if (syncMode) {
        return loadAllEntitiesFromDatabase(
            client, wrestlerDbId, "Wrestler", this::mapPageToWrestlerPageSyncMode, false);
      } else {
        return loadAllEntitiesFromDatabase(
            client, wrestlerDbId, "Wrestler", this::mapPageToWrestlerPage, true);
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
      List<Page> results = executeWithRetry(() -> client.queryDatabase(queryRequest)).getResults();

      log.debug("Found {} pages in wrestlers database", results.size());

      // Search for the wrestler by name
      for (Page page : results) {
        Page pageData =
            executeWithRetry(() -> client.retrievePage(page.getId(), Collections.emptyList()));

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
    Optional<NotionClient> clientOptional = createNotionClient();
    Map<String, PageProperty> properties = pageData.getProperties();
    log.debug("Extracting {} properties for wrestler: {}", properties.size(), wrestlerName);

    // Create a map to store the processed property values
    Map<String, Object> processedProperties = new HashMap<>();

    clientOptional.ifPresentOrElse(
        client -> {
          try (client) {
            properties.forEach(
                (key, value) -> {
                  String valueStr = getValue(client, value);
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
    setBasicPageInfo(wrestlerPage, pageData);

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
          String formulaValue = getFormulaValue(property.getFormula());
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
    setRawProperties(wrestlerPage, minimalProperties);

    log.debug(
        "Mapped WrestlerPage in sync mode for: {} with ID: {}", wrestlerName, wrestlerPage.getId());
    return wrestlerPage;
  }

  /** Helper method to extract integer values from Notion properties. */
  private Integer getIntegerProperty(
      Map<String, PageProperty> properties, String propertyName, Integer defaultValue) {
    PageProperty property = properties.get(propertyName);
    if (property != null && property.getNumber() != null) {
      Number number = property.getNumber();
      log.debug("Found property '{}': {}", propertyName, number);
      return number.intValue();
    }
    log.debug("Property '{}' not found or null, using default: {}", propertyName, defaultValue);
    return defaultValue;
  }

  // ==================== SHOW LOADING METHODS ====================

  /**
   * Loads a show from the Notion database by name.
   *
   * @param showName The name of the show to load (e.g., "WrestleMania 40")
   * @return Optional containing the ShowPage object if found, empty otherwise
   */
  public Optional<ShowPage> loadShow(String showName) {
    log.debug("Loading show: '{}'", showName);

    String showDbId = getDatabaseId("Shows");
    if (showDbId == null) {
      log.warn("Shows database not found in workspace");
      return Optional.empty();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        log.warn("NotionClient not available, returning empty Optional for show: {}", showName);
        return Optional.empty();
      }
      return loadEntityFromDatabase(client, showDbId, showName, "Show", this::mapPageToShowPage);
    } catch (Exception e) {
      log.error("Failed to load show: {}", showName, e);
      return Optional.empty();
    }
  }

  /**
   * Loads a show from the Notion database by ID.
   *
   * @param showId The ID of the show to load.
   * @return Optional containing the ShowPage object if found, empty otherwise.
   */
  public Optional<ShowPage> loadShowById(@NonNull String showId) {
    log.debug("Loading show with ID: '{}'", showId);
    return loadPage(showId).map(page -> mapPageToShowPage(page, ""));
  }

  /** Static convenience method to load a show. */
  public static Optional<ShowPage> loadShowStatic(String showName) {
    return getInstance().flatMap(handler -> handler.loadShow(showName));
  }

  /**
   * Loads all shows from the Notion Shows database.
   *
   * @return List of all ShowPage objects from the Shows database
   */
  public List<ShowPage> loadAllShows() {
    log.debug("Loading all shows from Shows database");

    String showDbId = getDatabaseId("Shows");
    if (showDbId == null) {
      log.warn("Shows database not found in workspace");
      return new ArrayList<>();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        return new ArrayList<>();
      }
      return loadAllEntitiesFromDatabase(client, showDbId, "Show", this::mapPageToShowPage, true);
    } catch (Exception e) {
      log.error("Failed to load all shows", e);
      return new ArrayList<>();
    }
  }

  /**
   * Static convenience method to load all shows.
   *
   * @return List of all ShowPage objects from the Shows database
   */
  public static List<ShowPage> loadAllShowsStatic() {
    return getInstance().map(NotionHandler::loadAllShows).orElse(Collections.emptyList());
  }

  /**
   * Loads all shows from the Notion Shows database with minimal processing for sync operations.
   * This method is optimized for bulk operations and extracts only essential properties.
   *
   * @return List of ShowPage objects with basic properties populated
   */
  public List<ShowPage> loadAllShowsForSync() {
    log.debug("Loading all shows for sync operation (optimized)");

    // Check if NOTION_TOKEN is available first
    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String showDbId = getDatabaseId("Shows");
    if (showDbId == null) {
      log.warn("Shows database not found in workspace");
      return new ArrayList<>();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        log.warn("NotionClient not available, returning empty list for shows for sync.");
        return new ArrayList<>();
      }
      return loadAllEntitiesFromDatabase(
          client, showDbId, "Show", this::mapPageToShowPageForSync, false);
    } catch (Exception e) {
      log.error("Failed to load all shows for sync", e);
      throw new RuntimeException("Failed to load shows from Notion: " + e.getMessage(), e);
    }
  }

  // ==================== SHOW TEMPLATE LOADING METHODS ====================

  /**
   * Loads a show template from the Notion database by name.
   *
   * @param templateName The name of the show template to load
   * @return Optional containing the ShowTemplatePage object if found, empty otherwise
   */
  public Optional<ShowTemplatePage> loadShowTemplate(@NonNull String templateName) {
    log.debug("Loading show template: '{}'", templateName);

    String templateDbId = getDatabaseId("Show Templates");
    if (templateDbId == null) {
      log.warn("Show Templates database not found in workspace");
      return Optional.empty();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        log.warn(
            "NotionClient not available, returning empty Optional for show template: {}",
            templateName);
        return Optional.empty();
      }
      return loadEntityFromDatabase(
          client,
          templateDbId,
          templateName,
          "Show Template",
          (pageData, entityName) ->
              mapPageToGenericEntity(
                  pageData,
                  entityName,
                  "ShowTemplate",
                  ShowTemplatePage::new,
                  NotionPage.NotionParent::new));
    } catch (Exception e) {
      log.error("Failed to load show template: {}", templateName, e);
      return Optional.empty();
    }
  }

  /** Static convenience method to load a show template. */
  public static Optional<ShowTemplatePage> loadShowTemplateStatic(@NonNull String templateName) {
    return getInstance().flatMap(handler -> handler.loadShowTemplate(templateName));
  }

  /**
   * Loads all show templates from the Notion Show Templates database for sync operations. This
   * method is optimized for bulk operations and extracts only essential properties.
   *
   * @return List of ShowTemplatePage objects with basic properties populated
   */
  public List<ShowTemplatePage> loadAllShowTemplates() {
    log.debug("Loading all show templates for sync operation");

    String templateDbId = getDatabaseId("Show Templates");
    if (templateDbId == null) {
      log.warn("Show Templates database not found in workspace");
      return new ArrayList<>();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        return new ArrayList<>();
      }
      return loadAllEntitiesFromDatabase(
          client,
          templateDbId,
          "Show Template",
          (pageData, entityName) ->
              mapPageToGenericEntity(
                  pageData,
                  entityName,
                  "ShowTemplate",
                  ShowTemplatePage::new,
                  NotionPage.NotionParent::new),
          false);
    } catch (Exception e) {
      log.error("Failed to load all show templates for sync", e);
      return new ArrayList<>();
    }
  }

  /**
   * Retrieves show template data from Notion for all specified template names.
   *
   * @param templateNames List of template names to retrieve
   * @return Map of template name to show template data
   */
  public Map<String, ShowTemplatePage> retrieveShowTemplateData(
      @NonNull List<String> templateNames) {
    log.debug("Retrieving show template data for {} templates", templateNames.size());
    Map<String, ShowTemplatePage> templateData = new HashMap<>();

    for (String templateName : templateNames) {
      log.debug("Loading template: {}", templateName);
      Optional<ShowTemplatePage> templatePageOpt = loadShowTemplate(templateName);

      if (templatePageOpt.isPresent()) {
        ShowTemplatePage templatePage = templatePageOpt.get();
        templateData.put(templateName, templatePage);
        log.debug("Successfully loaded template: {}", templateName);

        // Log the template data for inspection
        logShowTemplateData(templateName, templatePage);
      } else {
        log.warn("Template '{}' not found in Notion database", templateName);
      }
    }

    log.debug("Retrieved {} out of {} templates", templateData.size(), templateNames.size());
    return templateData;
  }

  /** Logs detailed information about a show template for inspection. */
  private void logShowTemplateData(
      @NonNull String templateName, @NonNull ShowTemplatePage templatePage) {
    log.debug("=== TEMPLATE DATA FOR {} ===", templateName);

    // Log raw properties for complete data inspection
    if (templatePage.getRawProperties() != null && !templatePage.getRawProperties().isEmpty()) {
      log.debug("Raw properties:");
      templatePage
          .getRawProperties()
          .forEach(
              (key, value) -> {
                log.debug("  {}: {}", key, value);
              });
    }

    log.debug("=== END TEMPLATE DATA FOR {} ===", templateName);
  }

  // ==================== MATCH LOADING METHODS ====================

  /** Loads a segment from the Notion database by name. */
  public Optional<SegmentPage> loadSegment(@NonNull String segmentName) {
    log.debug("Loading segment: '{}'", segmentName);

    String matchDbId = getDatabaseId("Segments");
    if (matchDbId == null) {
      log.warn("Segments database not found in workspace");
      return Optional.empty();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        log.warn(
            "NotionClient not available, returning empty Optional for segment: {}", segmentName);
        return Optional.empty();
      }
      return loadEntityFromDatabase(
          client, matchDbId, segmentName, "Segment", this::mapPageToSegmentPage);
    } catch (Exception e) {
      log.error("Failed to load segment: {}", segmentName, e);
      return Optional.empty();
    }
  }

  /**
   * Loads a segment from the Notion database by ID.
   *
   * @param segment The ID of the segment to load.
   * @return Optional containing the MatchPage object if found, empty otherwise.
   */
  public Optional<SegmentPage> loadSegmentById(@NonNull String segment) {
    log.debug("Loading segment with ID: '{}'", segment);
    return loadPage(segment).map(page -> mapPageToSegmentPage(page, ""));
  }

  /**
   * Loads all matches from the Notion Matches database.
   *
   * @return List of all MatchPage objects from the Matches database
   */
  public List<SegmentPage> loadAllSegments() {
    log.debug("Loading all matches from Matches database");

    // Check if NOTION_TOKEN is available first
    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String matchDbId = getDatabaseId("Segments");
    if (matchDbId == null) {
      log.warn("Segment database not found in workspace");
      return new ArrayList<>();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        return new ArrayList<>();
      }
      return loadAllEntitiesFromDatabase(
          client, matchDbId, "Segment", this::mapPageToSegmentPage, false);
    } catch (Exception e) {
      log.error("Failed to load all matches", e);
      throw new RuntimeException("Failed to load matches from Notion: " + e.getMessage(), e);
    }
  }

  // ==================== HEAT LOADING METHODS ====================

  /** Loads a heat entry from the Notion database by name. */
  public Optional<HeatPage> loadHeat(@NonNull String heatName) {
    log.debug("Loading heat: '{}'", heatName);

    String heatDbId = getDatabaseId("Heat");
    if (heatDbId == null) {
      log.warn("Heat database not found in workspace");
      return Optional.empty();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        log.warn("NotionClient not available, returning empty Optional for heat: {}", heatName);
        return Optional.empty();
      }
      return loadEntityFromDatabase(client, heatDbId, heatName, "Heat", this::mapPageToHeatPage);
    } catch (Exception e) {
      log.error("Failed to load heat: {}", heatName, e);
      return Optional.empty();
    }
  }

  /** Static convenience method to load a heat entry. */
  public static Optional<HeatPage> loadHeatStatic(@NonNull String heatName) {
    return getInstance().flatMap(handler -> handler.loadHeat(heatName));
  }

  // ==================== TEAM LOADING METHODS ====================

  /** Loads a team from the Notion database by name. */
  public Optional<TeamPage> loadTeam(@NonNull String teamName) {
    log.debug("Loading team: '{}'", teamName);

    String teamDbId = getDatabaseId("Teams");
    if (teamDbId == null) {
      log.warn("Teams database not found in workspace");
      return Optional.empty();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        log.warn("NotionClient not available, returning empty Optional for team: {}", teamName);
        return Optional.empty();
      }
      return loadEntityFromDatabase(client, teamDbId, teamName, "Team", this::mapPageToTeamPage);
    } catch (Exception e) {
      log.error("Failed to load team: {}", teamName, e);
      return Optional.empty();
    }
  }

  /**
   * Loads all teams from the Notion Teams database.
   *
   * @return List of all TeamPage objects from the Teams database
   */
  public List<TeamPage> loadAllTeams() {
    log.debug("Loading all teams from Teams database");

    // Check if NOTION_TOKEN is available first
    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String teamDbId = getDatabaseId("Teams");
    if (teamDbId == null) {
      log.warn("Teams database not found in workspace");
      return new ArrayList<>();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        return new ArrayList<>();
      }
      return loadAllEntitiesFromDatabase(client, teamDbId, "Team", this::mapPageToTeamPage, false);
    } catch (Exception e) {
      log.error("Failed to load all teams", e);
      throw new RuntimeException("Failed to load teams from Notion: " + e.getMessage(), e);
    }
  }

  /** Static convenience method to load a team. */
  public static Optional<TeamPage> loadTeamStatic(@NonNull String teamName) {
    return getInstance().flatMap(handler -> handler.loadTeam(teamName));
  }

  // ==================== SEASON LOADING METHODS ====================

  /** Loads a season from the Notion database by name. */
  public Optional<SeasonPage> loadSeason(@NonNull String seasonName) {
    log.debug("Loading season: '{}'", seasonName);

    String seasonDbId = getDatabaseId("Seasons");
    if (seasonDbId == null) {
      log.warn("Seasons database not found in workspace");
      return Optional.empty();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        log.warn("NotionClient not available, returning empty Optional for season: {}", seasonName);
        return Optional.empty();
      }
      return loadEntityFromDatabase(
          client, seasonDbId, seasonName, "Season", this::mapPageToSeasonPage);
    } catch (Exception e) {
      log.error("Failed to load season: {}", seasonName, e);
      return Optional.empty();
    }
  }

  /**
   * Loads all seasons from the Notion Seasons database.
   *
   * @return List of all SeasonPage objects from the Seasons database
   */
  public List<SeasonPage> loadAllSeasons() {
    log.debug("Loading all seasons from Seasons database");

    // Check if NOTION_TOKEN is available first
    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String seasonDbId = getDatabaseId("Seasons");
    if (seasonDbId == null) {
      log.warn("Seasons database not found in workspace");
      return new ArrayList<>();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        return new ArrayList<>();
      }
      return loadAllEntitiesFromDatabase(
          client, seasonDbId, "Season", this::mapPageToSeasonPage, false);
    } catch (Exception e) {
      log.error("Failed to load all seasons", e);
      throw new RuntimeException("Failed to load seasons from Notion: " + e.getMessage(), e);
    }
  }

  /** Static convenience method to load a season. */
  public static Optional<SeasonPage> loadSeasonStatic(@NonNull String seasonName) {
    return getInstance().flatMap(handler -> handler.loadSeason(seasonName));
  }

  // ==================== FACTION LOADING METHODS ====================

  /** Loads a faction from the Notion database by name. */
  public Optional<FactionPage> loadFaction(@NonNull String factionName) {
    log.debug("Loading faction: '{}'", factionName);

    String factionDbId = getDatabaseId("Factions");
    if (factionDbId == null) {
      log.warn("Factions database not found in workspace");
      return Optional.empty();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        log.warn(
            "NotionClient not available, returning empty Optional for faction: {}", factionName);
        return Optional.empty();
      }
      return loadEntityFromDatabase(
          client, factionDbId, factionName, "Faction", this::mapPageToFactionPage);
    } catch (Exception e) {
      log.error("Failed to load faction: {}", factionName, e);
      return Optional.empty();
    }
  }

  /**
   * Loads all factions from the Notion Factions database.
   *
   * @return List of all FactionPage objects from the Factions database
   */
  public List<FactionPage> loadAllFactions() {
    log.debug("Loading all factions from Factions database");

    // Check if NOTION_TOKEN is available first
    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String factionDbId = getDatabaseId("Factions");
    if (factionDbId == null) {
      log.warn("Factions database not found in workspace");
      return new ArrayList<>();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        return new ArrayList<>();
      }
      return loadAllEntitiesFromDatabase(
          client, factionDbId, "Faction", this::mapPageToFactionPage, false);
    } catch (Exception e) {
      log.error("Failed to load all factions", e);
      throw new RuntimeException("Failed to load factions from Notion: " + e.getMessage(), e);
    }
  }

  /** Static convenience method to load a faction. */
  public static Optional<FactionPage> loadFactionStatic(@NonNull String factionName) {
    return getInstance().flatMap(handler -> handler.loadFaction(factionName));
  }

  // ==================== TITLE LOADING METHODS ====================

  /**
   * Loads all titles from the Notion Titles database.
   *
   * @return List of all TitlePage objects from the Titles database
   */
  public List<TitlePage> loadAllTitles() {
    log.debug("Loading all titles from Titles database");

    // Check if NOTION_TOKEN is available first
    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String titleDbId = getDatabaseId("Championships");
    if (titleDbId == null) {
      log.warn("Championships database not found in workspace");
      return new ArrayList<>();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        return new ArrayList<>();
      }
      return loadAllEntitiesFromDatabase(
          client, titleDbId, "Title", this::mapPageToTitlePage, false);
    } catch (Exception e) {
      log.error("Failed to load all titles", e);
      throw new RuntimeException("Failed to load titles from Notion: " + e.getMessage(), e);
    }
  }

  /** Maps a Notion page to a TitlePage object. */
  private TitlePage mapPageToTitlePage(@NonNull Page pageData, @NonNull String titleName) {
    return mapPageToGenericEntity(
        pageData, titleName, "Title", TitlePage::new, TitlePage.NotionParent::new);
  }

  // ==================== NPC LOADING METHODS ====================

  /**
   * Loads all NPCs from the Notion NPCs database.
   *
   * @return List of all NpcPage objects from the NPCs database
   */
  public List<NpcPage> loadAllNpcs() {
    log.debug("Loading all NPCs from NPCs database");

    // Check if NOTION_TOKEN is available first
    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String npcDbId = getDatabaseId("NPCs");
    if (npcDbId == null) {
      log.warn("NPCs database not found in workspace");
      return new ArrayList<>();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        return new ArrayList<>();
      }
      return loadAllEntitiesFromDatabase(client, npcDbId, "NPC", this::mapPageToNpcPage, false);
    } catch (Exception e) {
      log.error("Failed to load all NPCs", e);
      throw new RuntimeException("Failed to load NPCs from Notion: " + e.getMessage(), e);
    }
  }

  /** Maps a Notion page to a NpcPage object. */
  private NpcPage mapPageToNpcPage(@NonNull Page pageData, @NonNull String npcName) {
    return mapPageToGenericEntity(
        pageData, npcName, "NPC", NpcPage::new, NpcPage.NotionParent::new);
  }

  // ==================== INJURY LOADING METHODS ====================

  /**
   * Loads all injuries from the Notion Injuries database.
   *
   * @return List of all InjuryPage objects from the Injuries database
   */
  public List<InjuryPage> loadAllInjuries() {
    log.debug("Loading all injuries from Injuries database");

    // Check if NOTION_TOKEN is available first
    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String injuryDbId = getDatabaseId("Injuries");
    if (injuryDbId == null) {
      log.warn("Injuries database not found in workspace");
      return new ArrayList<>();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        return new ArrayList<>();
      }
      return loadAllEntitiesFromDatabase(
          client, injuryDbId, "Injury", this::mapPageToInjuryPage, false);
    } catch (Exception e) {
      log.error("Failed to load all injuries", e);
      throw new RuntimeException("Failed to load injuries from Notion: " + e.getMessage(), e);
    }
  }

  // ==================== GENERIC HELPER METHODS ====================

  /**
   * Loads a Notion page by its ID.
   *
   * @param pageId The ID of the page to load.
   * @return Optional containing the Page object if found, empty otherwise.
   */
  public Optional<Page> loadPage(@NonNull String pageId) {
    log.debug("Loading page with ID: '{}'", pageId);
    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        return Optional.empty();
      }
      return Optional.of(
          executeWithRetry(
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
  private List<ShowPage> loadAllEntitiesForSync(
      @NonNull NotionClient client, @NonNull String databaseId, @NonNull String entityType) {

    List<ShowPage> entities = new ArrayList<>();
    try {
      log.debug("Loading all {} entities for sync from database {}", entityType, databaseId);

      QueryDatabaseRequest queryRequest = new QueryDatabaseRequest(databaseId);

      List<Page> results = executeWithRetry(() -> client.queryDatabase(queryRequest)).getResults();

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
  private <T> List<T> loadAllEntitiesFromDatabase(
      @NonNull NotionClient client,
      @NonNull String databaseId,
      @NonNull String entityType,
      @NonNull java.util.function.BiFunction<Page, String, T> mapper,
      boolean resolveRelationships) {

    List<T> entities = new ArrayList<>();
    try {
      log.debug("Loading all {} entities from database {}", entityType, databaseId);
      String nextCursor = null;
      boolean hasMore;

      List<Page> results = new ArrayList<>();

      do {
        QueryDatabaseRequest queryRequest = new QueryDatabaseRequest(databaseId);
        queryRequest.setStartCursor(nextCursor);

        QueryResults queryResults = executeWithRetry(() -> client.queryDatabase(queryRequest));
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
              .map(page -> mapPageToEntity(page, client, entityType, mapper, resolveRelationships))
              .filter(java.util.Objects::nonNull)
              .collect(java.util.stream.Collectors.toList());

      log.debug("Successfully loaded {} {} entities from database", entities.size(), entityType);
      return entities;

    } catch (Exception e) {
      log.error("Error loading all {} entities from database: {}", entityType, databaseId, e);
      return entities; // Return partial results
    }
  }

  private <T> T mapPageToEntity(
      Page page,
      NotionClient client,
      String entityType,
      java.util.function.BiFunction<Page, String, T> mapper,
      boolean resolveRelationships) {
    try {
      Page pageData =
          executeWithRetry(() -> client.retrievePage(page.getId(), Collections.emptyList()));

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
  private <T> Optional<T> loadEntityFromDatabase(
      @NonNull NotionClient client,
      @NonNull String databaseId,
      @NonNull String entityName,
      @NonNull String entityType,
      @NonNull java.util.function.BiFunction<Page, String, T> mapper) {

    // PrintStream originalOut = System.out;
    try {
      log.debug("Searching for {} '{}' in database {}", entityType, entityName, databaseId);

      QueryDatabaseRequest queryRequest = new QueryDatabaseRequest(databaseId);

      List<Page> results = executeWithRetry(() -> client.queryDatabase(queryRequest)).getResults();

      log.debug("Found {} pages in {} database", results.size(), entityType);

      // Search for the entity by name
      for (Page page : results) {
        Page pageData =
            executeWithRetry(() -> client.retrievePage(page.getId(), Collections.emptyList()));

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
    ShowPage showPage = new ShowPage();

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

  /**
   * Loads a segment from the Notion database by ID.
   *
   * @param segmentId The ID of the segment to load.
   * @return Optional containing the MatchPage object if found, empty otherwise.
   */
  public Optional<SegmentPage> getSegmentPage(@NonNull String segmentId) {
    log.debug("Loading segment with ID: '{}'", segmentId);
    return loadPage(segmentId).map(page -> mapPageToSegmentPage(page, ""));
  }

  /**
   * Loads all show types from the Notion Show Types database.
   *
   * @return List of all ShowTypePage objects from the Show Types database
   */
  public List<ShowTypePage> getShowTypePages() {
    log.debug("Loading all show types from Show Types database");

    // Check if NOTION_TOKEN is available first
    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String dbId = getDatabaseId("Show Types");
    if (dbId == null) {
      log.warn("Show Types database not found in workspace");
      return new ArrayList<>();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        return new ArrayList<>();
      }
      return loadAllEntitiesFromDatabase(
          client, dbId, "Show Type", this::mapPageToShowTypePage, false);
    } catch (Exception e) {
      log.error("Failed to load all show types", e);
      throw new RuntimeException("Failed to load show types from Notion: " + e.getMessage(), e);
    }
  }

  /**
   * Gets all segment IDs from the Notion database.
   *
   * @return List of segment IDs.
   */
  public List<String> getSegmentIds() {
    log.debug("Loading all segment IDs from Segments database");

    String dbId = getDatabaseId("Segments");
    if (dbId == null) {
      log.warn("'Segments' database not found in workspace");
      return new ArrayList<>();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        return new ArrayList<>();
      }
      return loadAllEntitiesFromDatabase(
          client, dbId, "Segments", (page, name) -> page.getId(), false);
    } catch (Exception e) {
      log.error("Failed to load all segment IDs from database 'Segments'", e);
      return new ArrayList<>();
    }
  }

  /** Maps a Notion page to a ShowPage object with full relationship resolution. */
  private ShowPage mapPageToShowPage(@NonNull Page pageData, @NonNull String showName) {
    return mapPageToGenericEntity(
        pageData, showName, "Show", ShowPage::new, ShowPage.NotionParent::new, true);
  }

  /** Maps a Notion page to a ShowTypePage object. */
  private ShowTypePage mapPageToShowTypePage(@NonNull Page pageData, @NonNull String showTypeName) {
    return mapPageToGenericEntity(
        pageData, showTypeName, "Show Type", ShowTypePage::new, ShowTypePage.NotionParent::new);
  }

  /** Maps a Notion page to a ShowPage object with minimal relationship resolution for sync. */
  private ShowPage mapPageToShowPageForSync(@NonNull Page pageData, @NonNull String showName) {
    return mapPageToGenericEntity(
        pageData, showName, "Show", ShowPage::new, ShowPage.NotionParent::new, true);
  }

  /** Maps a Notion page to a MatchPage object. */
  private SegmentPage mapPageToSegmentPage(@NonNull Page pageData, @NonNull String matchName) {
    SegmentPage matchPage =
        mapPageToGenericEntity(
            pageData, matchName, "Segment", SegmentPage::new, SegmentPage.NotionParent::new);

    // Extract and set specific MatchPage properties
    SegmentPage.NotionProperties properties = new SegmentPage.NotionProperties();
    Map<String, PageProperty> notionProperties = pageData.getProperties();

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        log.warn("NotionClient not available, cannot map segment page properties.");
      } else {
        properties.setParticipants(createProperty(notionProperties, "Participants", client));
        properties.setWinners(createProperty(notionProperties, "Winners", client));
        properties.setShows(createProperty(notionProperties, "Shows", client));
        properties.setSegment_Type(createProperty(notionProperties, "Segment Type", client));
        properties.setReferee_s(createProperty(notionProperties, "Referee(s)", client));
        properties.setRules(createProperty(notionProperties, "Rules", client));
        properties.setTitle_s(createProperty(notionProperties, "Title(s)", client));
        properties.setNotes(createProperty(notionProperties, "Notes", client));
        properties.setDate(createProperty(notionProperties, "Date", client));
      }
    } catch (Exception e) {
      log.error("Error mapping MatchPage properties for {}: {}", matchName, e.getMessage());
    }

    matchPage.setProperties(properties);
    return matchPage;
  }

  // Helper method to create a NotionPage.Property from a PageProperty
  private NotionPage.Property createProperty(
      Map<String, PageProperty> notionProperties, String propertyName, NotionClient client) {
    PageProperty pageProperty = notionProperties.get(propertyName);
    if (pageProperty == null) {
      return null;
    }
    NotionPage.Property property = new NotionPage.Property();
    property.setId(pageProperty.getId());
    property.setType(pageProperty.getType() != null ? pageProperty.getType().getValue() : null);
    property.setTitle(pageProperty.getTitle());
    property.setRich_text(pageProperty.getRichText());
    property.setDate(pageProperty.getDate());
    property.setSelect(pageProperty.getSelect());
    // Convert Notion API's PageReference list to NotionPage.Relation list
    if (pageProperty.getRelation() != null) {
      List<NotionPage.Relation> relations =
          pageProperty.getRelation().stream()
              .map(
                  pageReference -> {
                    NotionPage.Relation newRelation = new NotionPage.Relation();
                    newRelation.setId(pageReference.getId());
                    return newRelation;
                  })
              .collect(Collectors.toList());
      property.setRelation(relations);
    }
    property.setPeople(pageProperty.getPeople());
    property.setNumber(pageProperty.getNumber());
    property.setCreated_time(pageProperty.getCreatedTime());
    property.setLast_edited_time(pageProperty.getLastEditedTime());
    property.setCreated_by(pageProperty.getCreatedBy());
    property.setLast_edited_by(pageProperty.getLastEditedBy());
    property.setUnique_id(pageProperty.getUniqueId());
    property.setFormula(pageProperty.getFormula());
    property.setHas_more(pageProperty.getHasMore());
    return property;
  }

  /** Maps a Notion page to a HeatPage object. */
  private HeatPage mapPageToHeatPage(@NonNull Page pageData, @NonNull String heatName) {
    return mapPageToGenericEntity(
        pageData, heatName, "Heat", HeatPage::new, HeatPage.NotionParent::new);
  }

  /** Maps a Notion page to a TeamPage object. */
  private TeamPage mapPageToTeamPage(@NonNull Page pageData, @NonNull String teamName) {
    return mapPageToGenericEntity(
        pageData, teamName, "Team", TeamPage::new, TeamPage.NotionParent::new, true);
  }

  /** Maps a Notion page to a SeasonPage object. */
  private SeasonPage mapPageToSeasonPage(@NonNull Page pageData, @NonNull String seasonName) {
    return mapPageToGenericEntity(
        pageData, seasonName, "Season", SeasonPage::new, SeasonPage.NotionParent::new);
  }

  /** Maps a Notion page to a FactionPage object. */
  private FactionPage mapPageToFactionPage(@NonNull Page pageData, @NonNull String factionName) {
    return mapPageToGenericEntity(
        pageData, factionName, "Faction", FactionPage::new, FactionPage.NotionParent::new);
  }

  /**
   * Loads all title reigns from the Notion Title Reigns database.
   *
   * @return List of all TitleReignPage objects from the Title Reigns database
   */
  public List<TitleReignPage> loadAllTitleReigns() {
    log.debug("Loading all title reigns from Title Reigns database");

    // Check if NOTION_TOKEN is available first
    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String dbId = getDatabaseId("Title Reigns");
    if (dbId == null) {
      log.warn("Title Reigns database not found in workspace");
      return new ArrayList<>();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        return new ArrayList<>();
      }
      return loadAllEntitiesFromDatabase(
          client, dbId, "Title Reign", this::mapPageToTitleReignPage, false);
    } catch (Exception e) {
      log.error("Failed to load all title reigns", e);
      throw new RuntimeException("Failed to load title reigns from Notion: " + e.getMessage(), e);
    }
  }

  /** Maps a Notion page to an InjuryPage object. */
  private InjuryPage mapPageToInjuryPage(@NonNull Page pageData, @NonNull String injuryName) {
    return mapPageToGenericEntity(
        pageData, injuryName, "Injury", InjuryPage::new, InjuryPage.NotionParent::new);
  }

  /** Maps a Notion page to a TitleReignPage object. */
  private TitleReignPage mapPageToTitleReignPage(
      @NonNull Page pageData, @NonNull String entityName) {
    return mapPageToGenericEntity(
        pageData, entityName, "Title Reign", TitleReignPage::new, TitleReignPage.NotionParent::new);
  }

  /** Generic mapping method for all entity types with full relationship resolution. */
  private <T, P> T mapPageToGenericEntity(
      @NonNull Page pageData,
      @NonNull String entityName,
      @NonNull String entityType,
      @NonNull java.util.function.Supplier<T> entityConstructor,
      @NonNull java.util.function.Supplier<P> parentConstructor) {
    return mapPageToGenericEntity(
        pageData, entityName, entityType, entityConstructor, parentConstructor, true);
  }

  /** Generic mapping method for all entity types with optional relationship resolution. */
  private <T, P> T mapPageToGenericEntity(
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

    // If entity is TeamPage, set typed properties using original PageProperty map
    if (entityPage instanceof TeamPage teamPage) {
      Map<String, PageProperty> notionProperties = pageData.getProperties();
      TeamPage.NotionProperties props = mapPagePropertiesToNotionProperties(notionProperties);
      teamPage.setProperties(props);
    }

    log.debug("Mapped {}Page for: {} with ID: {}", entityType, entityName, pageData.getId());

    return entityPage;
  }

  /** Helper method to set basic page information using reflection. */
  private void setBasicPageInfo(@NonNull Object entityPage, @NonNull Page pageData) {
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

  /** Helper method to set raw properties on entity page. */
  private void setRawProperties(
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
                String valueStr = getValue(client, value, resolveRelationships);
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
            executeWithRetry(() -> client.retrievePage(showPage.getId(), Collections.emptyList()));

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

  /** Maps raw properties to TeamPage.NotionProperties. */
  private TeamPage.NotionProperties mapPagePropertiesToNotionProperties(
      Map<String, notion.api.v1.model.pages.PageProperty> notionProperties) {
    TeamPage.NotionProperties props = new TeamPage.NotionProperties();
    if (notionProperties == null) return props;
    if (notionProperties.containsKey("Member 1"))
      props.setMembers(toProperty(notionProperties.get("Member 1")));
    if (notionProperties.containsKey("Leader"))
      props.setLeader(toProperty(notionProperties.get("Leader")));
    if (notionProperties.containsKey("TeamType"))
      props.setTeamType(toProperty(notionProperties.get("TeamType")));
    if (notionProperties.containsKey("Status"))
      props.setStatus(toProperty(notionProperties.get("Status")));
    if (notionProperties.containsKey("FormedDate"))
      props.setFormedDate(toProperty(notionProperties.get("FormedDate")));
    if (notionProperties.containsKey("DisbandedDate"))
      props.setDisbandedDate(toProperty(notionProperties.get("DisbandedDate")));
    if (notionProperties.containsKey("Faction"))
      props.setFaction(toProperty(notionProperties.get("Faction")));
    return props;
  }

  /** Converts a Notion PageProperty to a TeamPage.Property. */
  private TeamPage.Property toProperty(notion.api.v1.model.pages.PageProperty pageProperty) {
    if (pageProperty == null) return null;
    TeamPage.Property property = new TeamPage.Property();
    property.setId(pageProperty.getId());
    property.setType(pageProperty.getType() != null ? pageProperty.getType().getValue() : null);
    property.setTitle(pageProperty.getTitle());
    property.setRich_text(pageProperty.getRichText());
    property.setDate(pageProperty.getDate());
    property.setSelect(pageProperty.getSelect());
    // Map relations
    if (pageProperty.getRelation() != null) {
      List<TeamPage.Relation> relations = new java.util.ArrayList<>();
      pageProperty
          .getRelation()
          .forEach(
              ref -> {
                TeamPage.Relation rel = new TeamPage.Relation();
                rel.setId(ref.getId());
                relations.add(rel);
              });
      property.setRelation(relations);
    }
    property.setPeople(pageProperty.getPeople());
    property.setNumber(pageProperty.getNumber());
    property.setCreated_time(pageProperty.getCreatedTime());
    property.setLast_edited_time(pageProperty.getLastEditedTime());
    property.setCreated_by(pageProperty.getCreatedBy());
    property.setLast_edited_by(pageProperty.getLastEditedBy());
    property.setUnique_id(pageProperty.getUniqueId());
    property.setFormula(pageProperty.getFormula());
    property.setHas_more(pageProperty.getHasMore());
    return property;
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
  public List<RivalryPage> loadAllRivalries() {
    log.debug("Loading all rivalries from Heat database");

    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String dbId = getDatabaseId("Heat");
    if (dbId == null) {
      log.warn("Heat database not found in workspace");
      return new ArrayList<>();
    }

    try (NotionClient client = createNotionClient().orElse(null)) {
      if (client == null) {
        return new ArrayList<>();
      }
      return loadAllEntitiesFromDatabase(
          client, dbId, "Rivalry", this::mapPageToRivalryPage, false);
    } catch (Exception e) {
      log.error("Failed to load all rivalries", e);
      throw new RuntimeException("Failed to load rivalries from Notion: " + e.getMessage(), e);
    }
  }

  /**
   * Loads all faction rivalries from the Notion Faction Heat database.
   *
   * @return List of all FactionRivalryPage objects from the Faction Heat database
   */
  public List<FactionRivalryPage> loadAllFactionRivalries() {
    log.debug("Loading all faction rivalries from Faction Heat database");

    if (!EnvironmentVariableUtil.isNotionTokenAvailable()) {
      throw new IllegalStateException("NOTION_TOKEN is required for sync operations");
    }

    String dbId = getDatabaseId("Faction Heat");
    if (dbId == null) {
      log.warn("Faction Heat database not found in workspace");
      return new ArrayList<>();
    }

    Optional<NotionClient> clientOptional = createNotionClient();
    if (clientOptional.isPresent()) {
      try (NotionClient client = clientOptional.get()) {
        return loadAllEntitiesFromDatabase(
            client, dbId, "Faction Rivalry", this::mapPageToFactionRivalryPage, false);
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

  /** Maps a Notion page to a RivalryPage object. */
  private RivalryPage mapPageToRivalryPage(@NonNull Page pageData, @NonNull String entityName) {
    return mapPageToGenericEntity(
        pageData, entityName, "Rivalry", RivalryPage::new, RivalryPage.NotionParent::new);
  }

  /** Maps a Notion page to a FactionRivalryPage object. */
  private FactionRivalryPage mapPageToFactionRivalryPage(
      @NonNull Page pageData, @NonNull String entityName) {
    return mapPageToGenericEntity(
        pageData,
        entityName,
        "Faction Rivalry",
        FactionRivalryPage::new,
        FactionRivalryPage.NotionParent::new);
  }
}
