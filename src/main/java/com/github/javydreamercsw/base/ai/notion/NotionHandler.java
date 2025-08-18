package com.github.javydreamercsw.base.ai.notion;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import notion.api.v1.NotionClient;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageProperty;
import notion.api.v1.model.search.DatabaseSearchResult;
import notion.api.v1.model.search.SearchResults;
import notion.api.v1.request.databases.QueryDatabaseRequest;
import notion.api.v1.request.search.SearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotionHandler {
  private static final Logger log = LoggerFactory.getLogger(NotionHandler.class);

  // Singleton instance
  private static volatile NotionHandler instance;

  // Map to store database names and their corresponding IDs for later reference
  private final Map<String, String> databaseMap = new HashMap<>();

  // Flag to track if databases have been loaded
  private volatile boolean initialized = false;

  // Private constructor to prevent instantiation
  private NotionHandler() {}

  /** Gets the singleton instance of NotionHandler. Initializes databases on first call. */
  public static NotionHandler getInstance() {
    if (instance == null) {
      synchronized (NotionHandler.class) {
        if (instance == null) {
          instance = new NotionHandler();
          instance.initializeDatabases();
        }
      }
    }
    return instance;
  }

  public static void main(String[] args) {
    // Example usage
    NotionHandler handler = NotionHandler.getInstance();
    log.info("Database map loaded with {} databases", handler.databaseMap.size());
    log.info("You can now reference databases by name using getDatabaseId(name)");

    // Example: Load different entity types

    // Load a wrestler
    Optional<WrestlerPage> wrestlerPage = handler.loadWrestler("Rob Van Dam");
    if (wrestlerPage.isPresent()) {
      WrestlerPage rvd = wrestlerPage.get();
      log.info("Loaded wrestler page: {} with ID: {}", "Rob Van Dam", rvd.getId());
      log.info("Wrestler page as pretty JSON:\n{}", rvd.toPrettyJson());
    } else {
      log.info("Wrestler 'Rob Van Dam' not found in Notion database");
    }

    // Load a show
    Optional<ShowPage> showPage = handler.loadShow("Quantum Quarrel");
    if (showPage.isPresent()) {
      ShowPage show = showPage.get();
      log.info("Loaded show page: {} with ID: {}", "Quantum Quarrel", show.getId());
      log.info("Show page as pretty JSON:\n{}", show.toPrettyJson());

      // Load all matches from the show using the new getMatches() method
      log.info("Loading all matches for show 'Quantum Quarrel'...");
      List<MatchPage> matches = show.getMatches();

      if (!matches.isEmpty()) {
        log.info("Found {} matches in show:", matches.size());
        for (int i = 0; i < matches.size(); i++) {
          MatchPage match = matches.get(i);
          log.info("Match {}: ID = {}", (i + 1), match.getId());
          log.info("Match {} details as JSON:\n{}", (i + 1), match.toPrettyJson());
        }

        // Also demonstrate loading the first match individually for comparison
        if (!matches.isEmpty()) {
          MatchPage firstMatch = matches.get(0);
          log.info("First match from list has ID: {}", firstMatch.getId());
        }
      } else {
        log.info("No matches found in the show, trying fallback approach");
        // Fallback to the original single match loading approach
        String firstMatchName = handler.extractFirstMatchFromShow(show);
        if (firstMatchName != null) {
          Optional<MatchPage> matchPage = handler.loadMatch(firstMatchName);
          if (matchPage.isPresent()) {
            MatchPage match = matchPage.get();
            log.info("Loaded fallback match '{}' with ID: {}", firstMatchName, match.getId());
            log.info("Fallback match as JSON:\n{}", match.toPrettyJson());
          }
        } else {
          log.info("No matches found using any method");
        }
      }
    } else {
      log.info("Show 'Quantum Quarrel' not found in Notion database");
    }

    // Load a team
    Optional<TeamPage> teamPage = handler.loadTeam("Team Extreme");
    if (teamPage.isPresent()) {
      TeamPage team = teamPage.get();
      log.info("Loaded team page: {} with ID: {}", "Team Extreme", team.getId());
      log.info("Team page as pretty JSON:\n{}", team.toPrettyJson());
    } else {
      log.info("Team 'Team Extreme' not found in Notion database");
    }

    // Load a faction
    Optional<FactionPage> factionPage = handler.loadFaction("Desolation's Smile");
    if (factionPage.isPresent()) {
      FactionPage faction = factionPage.get();
      log.info("Loaded faction page: {} with ID: {}", "Desolation's Smile", faction.getId());
      log.info("Faction page as pretty JSON:\n{}", faction.toPrettyJson());
    } else {
      log.info("Faction 'Desolation's Smile' not found in Notion database");
    }
  }

  /**
   * Initializes the database map by loading all databases from Notion workspace. This method is
   * called automatically on first access to the singleton.
   */
  private void initializeDatabases() {
    if (initialized) {
      return;
    }

    log.debug("Initializing NotionHandler - loading databases from workspace");

    try (NotionClient client = new NotionClient(System.getenv("NOTION_TOKEN"))) {
      loadDatabases(client);
      initialized = true;
      log.info("NotionHandler initialized successfully with {} databases", databaseMap.size());
    } catch (Exception e) {
      log.error("Failed to initialize NotionHandler", e);
      throw new RuntimeException("Failed to initialize NotionHandler", e);
    }
  }

  /** Loads all databases from the Notion workspace into the internal map. */
  private void loadDatabases(@NonNull NotionClient client) {
    PrintStream originalOut = System.out;
    try {
      log.debug("Starting database search in Notion workspace");

      // Temporarily redirect System.out to suppress Notion SDK logs
      System.setOut(new PrintStream(new ByteArrayOutputStream()));

      // Search for all databases in the workspace
      SearchRequest.SearchFilter searchFilter =
          new SearchRequest.SearchFilter("database", "object");
      SearchRequest searchRequest = new SearchRequest("", searchFilter);
      SearchResults searchResults = client.search(searchRequest);

      // Restore System.out before logging our results
      System.setOut(originalOut);

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
      // Restore System.out in case of exception
      System.setOut(originalOut);
      log.error("Error loading databases from Notion", e);
      throw e;
    } finally {
      // Ensure System.out is always restored
      System.setOut(originalOut);
    }
  }

  /**
   * Queries a specific database and prints all page properties. This is the original functionality
   * from the main method.
   */
  public void querySpecificDatabase(@NonNull String databaseId) {
    log.debug("Querying specific database: {}", databaseId);
    try (NotionClient client = new NotionClient(System.getenv("NOTION_TOKEN"))) {
      querySpecificDatabase(client, databaseId);
    } catch (Exception e) {
      log.error("Failed to query database: {}", databaseId, e);
      throw new RuntimeException("Failed to query database: " + databaseId, e);
    }
  }

  /** Internal method to query a specific database with an existing client. */
  private void querySpecificDatabase(@NonNull NotionClient client, String databaseId) {
    PrintStream originalOut = System.out;
    try {
      log.debug("Creating query request for database: {}", databaseId);

      // Create an empty query request (no filters, returns all results)
      QueryDatabaseRequest queryRequest = new QueryDatabaseRequest(databaseId);

      // Temporarily redirect System.out to suppress Notion SDK logs
      System.setOut(new PrintStream(new ByteArrayOutputStream()));

      // Send query request
      List<Page> results = client.queryDatabase(queryRequest).getResults();

      // Restore System.out before logging our results
      System.setOut(originalOut);

      log.debug("Found {} pages in database {}", results.size(), databaseId);

      results.forEach(
          page -> {
            log.debug("Processing page: {}", page.getId());

            // Suppress output again for retrievePage calls
            System.setOut(new PrintStream(new ByteArrayOutputStream()));
            Page pageData = client.retrievePage(page.getId(), Collections.emptyList());
            System.setOut(originalOut);

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
      // Restore System.out in case of exception
      System.setOut(originalOut);
      log.error("Error querying database: {}", databaseId, e);
      throw e;
    } finally {
      // Ensure System.out is always restored
      System.setOut(originalOut);
    }
  }

  private String getValue(@NonNull NotionClient client, @NonNull PageProperty value) {
    return switch (value.getType().getValue()) {
      case "formula" -> getFormulaValue(value.getFormula());
      case "people" -> value.getPeople().stream().findFirst().map(p -> p.getName()).orElse("N/A");
      case "created_by" -> value.getCreatedBy().getName();
      case "last_edited_by" -> value.getLastEditedBy().getName();
      case "created_time" -> value.getCreatedTime().toString();
      case "number" -> value.getNumber().toString();
      case "last_edited_time" -> value.getLastEditedTime().toString();
      case "unique_id" -> value.getUniqueId().getPrefix() + "-" + value.getUniqueId().getNumber();
      case "title" -> value.getTitle().get(0).getPlainText();
      case "relation" ->
          value.getRelation().stream()
              .map(
                  relation -> {
                    // Suppress System.out for relation retrievePage calls
                    PrintStream originalOut = System.out;
                    try {
                      System.setOut(new PrintStream(new ByteArrayOutputStream()));
                      Page relatedPage =
                          client.retrievePage(relation.getId(), Collections.emptyList());
                      System.setOut(originalOut);

                      PageProperty titleProperty = relatedPage.getProperties().get("Name");
                      if (titleProperty != null
                          && titleProperty.getTitle() != null
                          && !titleProperty.getTitle().isEmpty()) {
                        return titleProperty.getTitle().get(0).getPlainText();
                      }
                      return relation.getId();
                    } finally {
                      System.setOut(originalOut);
                    }
                  })
              .reduce((a, b) -> a + ", " + b)
              .orElse("N/A");
      default -> value.getType().toString();
    };
  }

  /** Helper method to extract values from formula properties based on their result type. */
  private String getFormulaValue(PageProperty.Formula formula) {
    if (formula == null) {
      return "null";
    }

    // Check the formula result type and extract accordingly
    if (formula.getString() != null) {
      return formula.getString();
    } else if (formula.getNumber() != null) {
      return formula.getNumber().toString();
    } else if (formula.getBoolean() != null) {
      return formula.getBoolean().toString();
    } else if (formula.getDate() != null) {
      return formula.getDate().toString();
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
  public String getDatabaseId(String databaseName) {
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
  public boolean databaseExists(String databaseName) {
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
    return getInstance().getDatabaseId(databaseName);
  }

  /**
   * Static convenience method to get all databases.
   *
   * @return A copy of the database map
   */
  public static Map<String, String> getStaticAllDatabases() {
    return getInstance().getAllDatabases();
  }

  /**
   * Static convenience method to check if database exists.
   *
   * @param databaseName The name of the database
   * @return true if the database exists, false otherwise
   */
  public static boolean staticDatabaseExists(String databaseName) {
    return getInstance().databaseExists(databaseName);
  }

  /**
   * Loads a wrestler from the Notion database by name.
   *
   * @param wrestlerName The name of the wrestler to load (e.g., "Rob Van Dam")
   * @return Optional containing the WrestlerPage object if found, empty otherwise
   */
  public Optional<WrestlerPage> loadWrestler(String wrestlerName) {
    log.debug("Loading wrestler: '{}'", wrestlerName);

    // First, find the wrestlers database
    String wrestlerDbId = getDatabaseId("Wrestlers");
    if (wrestlerDbId == null) {
      log.warn("Wrestlers database not found in workspace");
      return Optional.empty();
    }

    try (NotionClient client = new NotionClient(System.getenv("NOTION_TOKEN"))) {
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
  public static Optional<WrestlerPage> loadWrestlerStatic(String wrestlerName) {
    return getInstance().loadWrestler(wrestlerName);
  }

  /** Internal method to load a wrestler from a specific database. */
  private Optional<WrestlerPage> loadWrestlerFromDatabase(
      @NonNull NotionClient client, String databaseId, String wrestlerName) {
    PrintStream originalOut = System.out;
    try {
      log.debug("Searching for wrestler '{}' in database {}", wrestlerName, databaseId);

      // Create a query request to find the wrestler by name
      QueryDatabaseRequest queryRequest = new QueryDatabaseRequest(databaseId);

      // Temporarily redirect System.out to suppress Notion SDK logs
      System.setOut(new PrintStream(new ByteArrayOutputStream()));

      // Send query request
      List<Page> results = client.queryDatabase(queryRequest).getResults();

      // Restore System.out before logging our results
      System.setOut(originalOut);

      log.debug("Found {} pages in wrestlers database", results.size());

      // Search for the wrestler by name
      for (Page page : results) {
        // Suppress output again for retrievePage calls
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        Page pageData = client.retrievePage(page.getId(), Collections.emptyList());
        System.setOut(originalOut);

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
      // Restore System.out in case of exception
      System.setOut(originalOut);
      log.error("Error loading wrestler '{}' from database: {}", wrestlerName, databaseId, e);
      throw e;
    } finally {
      // Ensure System.out is always restored
      System.setOut(originalOut);
    }
  }

  /** Maps a Notion page to a WrestlerPage object. */
  private WrestlerPage mapPageToWrestlerPage(Page pageData, String wrestlerName) {
    log.debug("Mapping Notion page to WrestlerPage object for: {}", wrestlerName);

    WrestlerPage wrestlerPage = new WrestlerPage();

    // Set basic page information using Lombok-generated setters
    wrestlerPage.setObject("page");
    wrestlerPage.setId(pageData.getId());
    wrestlerPage.setCreated_time(pageData.getCreatedTime().toString());
    wrestlerPage.setLast_edited_time(pageData.getLastEditedTime().toString());
    wrestlerPage.setArchived(pageData.getArchived());
    wrestlerPage.setIn_trash(false); // Default value
    wrestlerPage.setUrl(pageData.getUrl());
    wrestlerPage.setPublic_url(pageData.getPublicUrl());

    // Set parent information
    WrestlerPage.NotionParent parent = new WrestlerPage.NotionParent();
    parent.setType("database_id");
    parent.setDatabase_id(pageData.getParent().getDatabaseId());
    wrestlerPage.setParent(parent);

    // Extract and log all property values using the same logic as querySpecificDatabase
    try (NotionClient client = new NotionClient(System.getenv("NOTION_TOKEN"))) {
      Map<String, PageProperty> properties = pageData.getProperties();
      log.debug("Extracting {} properties for wrestler: {}", properties.size(), wrestlerName);

      // Create a map to store the processed property values
      Map<String, Object> processedProperties = new HashMap<>();

      properties.forEach(
          (key, value) -> {
            String valueStr = getValue(client, value);
            log.info("Wrestler Property - {}: {}", key, valueStr);
            processedProperties.put(key, valueStr);
          });

      // Set the processed properties on the wrestler page
      wrestlerPage.setRawProperties(processedProperties);
    } catch (Exception e) {
      log.error("Error extracting property values for wrestler: {}", wrestlerName, e);
    }

    log.debug("Mapped WrestlerPage for: {} with ID: {}", wrestlerName, wrestlerPage.getId());

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

    try (NotionClient client = new NotionClient(System.getenv("NOTION_TOKEN"))) {
      return loadEntityFromDatabase(client, showDbId, showName, "Show", this::mapPageToShowPage);
    } catch (Exception e) {
      log.error("Failed to load show: {}", showName, e);
      return Optional.empty();
    }
  }

  /** Static convenience method to load a show. */
  public static Optional<ShowPage> loadShowStatic(String showName) {
    return getInstance().loadShow(showName);
  }

  // ==================== MATCH LOADING METHODS ====================

  /** Loads a match from the Notion database by name. */
  public Optional<MatchPage> loadMatch(String matchName) {
    log.debug("Loading match: '{}'", matchName);

    String matchDbId = getDatabaseId("Matches");
    if (matchDbId == null) {
      log.warn("Matches database not found in workspace");
      return Optional.empty();
    }

    try (NotionClient client = new NotionClient(System.getenv("NOTION_TOKEN"))) {
      return loadEntityFromDatabase(
          client, matchDbId, matchName, "Match", this::mapPageToMatchPage);
    } catch (Exception e) {
      log.error("Failed to load match: {}", matchName, e);
      return Optional.empty();
    }
  }

  /** Static convenience method to load a match. */
  public static Optional<MatchPage> loadMatchStatic(String matchName) {
    return getInstance().loadMatch(matchName);
  }

  // ==================== HEAT LOADING METHODS ====================

  /** Loads a heat entry from the Notion database by name. */
  public Optional<HeatPage> loadHeat(String heatName) {
    log.debug("Loading heat: '{}'", heatName);

    String heatDbId = getDatabaseId("Heat");
    if (heatDbId == null) {
      log.warn("Heat database not found in workspace");
      return Optional.empty();
    }

    try (NotionClient client = new NotionClient(System.getenv("NOTION_TOKEN"))) {
      return loadEntityFromDatabase(client, heatDbId, heatName, "Heat", this::mapPageToHeatPage);
    } catch (Exception e) {
      log.error("Failed to load heat: {}", heatName, e);
      return Optional.empty();
    }
  }

  /** Static convenience method to load a heat entry. */
  public static Optional<HeatPage> loadHeatStatic(String heatName) {
    return getInstance().loadHeat(heatName);
  }

  // ==================== TEAM LOADING METHODS ====================

  /** Loads a team from the Notion database by name. */
  public Optional<TeamPage> loadTeam(String teamName) {
    log.debug("Loading team: '{}'", teamName);

    String teamDbId = getDatabaseId("Teams");
    if (teamDbId == null) {
      log.warn("Teams database not found in workspace");
      return Optional.empty();
    }

    try (NotionClient client = new NotionClient(System.getenv("NOTION_TOKEN"))) {
      return loadEntityFromDatabase(client, teamDbId, teamName, "Team", this::mapPageToTeamPage);
    } catch (Exception e) {
      log.error("Failed to load team: {}", teamName, e);
      return Optional.empty();
    }
  }

  /** Static convenience method to load a team. */
  public static Optional<TeamPage> loadTeamStatic(String teamName) {
    return getInstance().loadTeam(teamName);
  }

  // ==================== FACTION LOADING METHODS ====================

  /** Loads a faction from the Notion database by name. */
  public Optional<FactionPage> loadFaction(String factionName) {
    log.debug("Loading faction: '{}'", factionName);

    String factionDbId = getDatabaseId("Factions");
    if (factionDbId == null) {
      log.warn("Factions database not found in workspace");
      return Optional.empty();
    }

    try (NotionClient client = new NotionClient(System.getenv("NOTION_TOKEN"))) {
      return loadEntityFromDatabase(
          client, factionDbId, factionName, "Faction", this::mapPageToFactionPage);
    } catch (Exception e) {
      log.error("Failed to load faction: {}", factionName, e);
      return Optional.empty();
    }
  }

  /** Static convenience method to load a faction. */
  public static Optional<FactionPage> loadFactionStatic(String factionName) {
    return getInstance().loadFaction(factionName);
  }

  // ==================== GENERIC HELPER METHODS ====================

  /** Generic method to load any entity from a database by name. */
  private <T> Optional<T> loadEntityFromDatabase(
      @NonNull NotionClient client,
      String databaseId,
      String entityName,
      String entityType,
      java.util.function.BiFunction<Page, String, T> mapper) {

    PrintStream originalOut = System.out;
    try {
      log.debug("Searching for {} '{}' in database {}", entityType, entityName, databaseId);

      QueryDatabaseRequest queryRequest = new QueryDatabaseRequest(databaseId);

      // Temporarily redirect System.out to suppress Notion SDK logs
      System.setOut(new PrintStream(new ByteArrayOutputStream()));

      List<Page> results = client.queryDatabase(queryRequest).getResults();

      // Restore System.out before logging our results
      System.setOut(originalOut);

      log.debug("Found {} pages in {} database", results.size(), entityType);

      // Search for the entity by name
      for (Page page : results) {
        // Suppress output again for retrievePage calls
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        Page pageData = client.retrievePage(page.getId(), Collections.emptyList());
        System.setOut(originalOut);

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
      // Restore System.out in case of exception
      System.setOut(originalOut);
      log.error("Error loading {} '{}' from database: {}", entityType, entityName, databaseId, e);
      throw e;
    } finally {
      // Ensure System.out is always restored
      System.setOut(originalOut);
    }
  }

  // ==================== MAPPING METHODS ====================

  /** Maps a Notion page to a ShowPage object. */
  private ShowPage mapPageToShowPage(Page pageData, String showName) {
    return mapPageToGenericEntity(
        pageData, showName, "Show", ShowPage::new, ShowPage.NotionParent::new);
  }

  /** Maps a Notion page to a MatchPage object. */
  private MatchPage mapPageToMatchPage(Page pageData, String matchName) {
    return mapPageToGenericEntity(
        pageData, matchName, "Match", MatchPage::new, MatchPage.NotionParent::new);
  }

  /** Maps a Notion page to a HeatPage object. */
  private HeatPage mapPageToHeatPage(Page pageData, String heatName) {
    return mapPageToGenericEntity(
        pageData, heatName, "Heat", HeatPage::new, HeatPage.NotionParent::new);
  }

  /** Maps a Notion page to a TeamPage object. */
  private TeamPage mapPageToTeamPage(Page pageData, String teamName) {
    return mapPageToGenericEntity(
        pageData, teamName, "Team", TeamPage::new, TeamPage.NotionParent::new);
  }

  /** Maps a Notion page to a FactionPage object. */
  private FactionPage mapPageToFactionPage(Page pageData, String factionName) {
    return mapPageToGenericEntity(
        pageData, factionName, "Faction", FactionPage::new, FactionPage.NotionParent::new);
  }

  /** Generic mapping method for all entity types. */
  private <T, P> T mapPageToGenericEntity(
      Page pageData,
      String entityName,
      String entityType,
      java.util.function.Supplier<T> entityConstructor,
      java.util.function.Supplier<P> parentConstructor) {

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
        extractAndLogProperties(pageData, entityName, entityType);
    setRawProperties(entityPage, processedProperties);

    log.debug("Mapped {}Page for: {} with ID: {}", entityType, entityName, pageData.getId());

    return entityPage;
  }

  /** Helper method to set basic page information using reflection. */
  private void setBasicPageInfo(Object entityPage, Page pageData) {
    try {
      entityPage.getClass().getMethod("setObject", String.class).invoke(entityPage, "page");
      entityPage.getClass().getMethod("setId", String.class).invoke(entityPage, pageData.getId());
      entityPage
          .getClass()
          .getMethod("setCreated_time", String.class)
          .invoke(entityPage, pageData.getCreatedTime().toString());
      entityPage
          .getClass()
          .getMethod("setLast_edited_time", String.class)
          .invoke(entityPage, pageData.getLastEditedTime().toString());
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
  private void setParentInfo(Object parent, Page pageData) {
    try {
      parent.getClass().getMethod("setType", String.class).invoke(parent, "database_id");
      parent
          .getClass()
          .getMethod("setDatabase_id", String.class)
          .invoke(parent, pageData.getParent().getDatabaseId());
    } catch (Exception e) {
      log.warn("Failed to set parent info: {}", e.getMessage());
    }
  }

  /** Helper method to set parent on entity page. */
  private void setParent(Object entityPage, Object parent) {
    try {
      entityPage
          .getClass()
          .getMethod("setParent", parent.getClass().getSuperclass())
          .invoke(entityPage, parent);
    } catch (Exception e) {
      log.warn("Failed to set parent: {}", e.getMessage());
    }
  }

  /** Helper method to set raw properties on entity page. */
  private void setRawProperties(Object entityPage, Map<String, Object> properties) {
    try {
      entityPage.getClass().getMethod("setRawProperties", Map.class).invoke(entityPage, properties);
      log.debug("Set {} raw properties on entity page", properties.size());
    } catch (Exception e) {
      log.warn("Failed to set raw properties: {}", e.getMessage());
    }
  }

  /** Helper method to extract and log properties. */
  private Map<String, Object> extractAndLogProperties(
      Page pageData, String entityName, String entityType) {
    try (NotionClient client = new NotionClient(System.getenv("NOTION_TOKEN"))) {
      Map<String, PageProperty> properties = pageData.getProperties();
      log.debug("Extracting {} properties for {}: {}", properties.size(), entityType, entityName);

      Map<String, Object> processedProperties = new HashMap<>();

      properties.forEach(
          (key, value) -> {
            String valueStr = getValue(client, value);
            log.info("{} Property - {}: {}", entityType, key, valueStr);
            processedProperties.put(key, valueStr);
          });

      return processedProperties;
    } catch (Exception e) {
      log.error(
          "Error extracting property values for {} '{}': {}",
          entityType,
          entityName,
          e.getMessage());
      return new HashMap<>();
    }
  }

  /**
   * Extracts the first match name from a show's matches relation property.
   *
   * @param showPage The ShowPage object to extract matches from
   * @return The name of the first match, or null if no matches found
   */
  public String extractFirstMatchFromShow(ShowPage showPage) {
    log.debug("Extracting first match from show: {}", showPage.getId());

    try (NotionClient client = new NotionClient(System.getenv("NOTION_TOKEN"))) {
      // Get the original page data to access properties
      PrintStream originalOut = System.out;
      try {
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        Page pageData = client.retrievePage(showPage.getId(), Collections.emptyList());
        System.setOut(originalOut);

        Map<String, PageProperty> properties = pageData.getProperties();

        // Look for a "Matches" relation property
        PageProperty matchesProperty = properties.get("Matches");
        if (matchesProperty != null
            && matchesProperty.getRelation() != null
            && !matchesProperty.getRelation().isEmpty()) {
          // Get the first related match - using generic approach since Relation type may not be
          // accessible
          Object firstMatch = matchesProperty.getRelation().get(0);
          String matchId = null;

          // Use reflection to get the ID since we can't access the Relation type directly
          try {
            matchId = (String) firstMatch.getClass().getMethod("getId").invoke(firstMatch);
          } catch (Exception e) {
            log.error("Failed to extract match ID from relation: {}", e.getMessage());
            return null;
          }

          log.debug("Found first match ID: {}", matchId);

          // Retrieve the match page to get its name
          System.setOut(new PrintStream(new ByteArrayOutputStream()));
          Page matchPage = client.retrievePage(matchId, Collections.emptyList());
          System.setOut(originalOut);

          // Get the match name from the Name property
          PageProperty nameProperty = matchPage.getProperties().get("Name");
          if (nameProperty != null
              && nameProperty.getTitle() != null
              && !nameProperty.getTitle().isEmpty()) {
            String matchName = nameProperty.getTitle().get(0).getPlainText();
            log.debug("Extracted match name: {}", matchName);
            return matchName;
          }
        }

        log.debug("No matches found in show's Matches property");
        return null;

      } catch (Exception e) {
        System.setOut(originalOut);
        log.error("Error extracting match from show: {}", e.getMessage());
        return null;
      } finally {
        System.setOut(originalOut);
      }
    } catch (Exception e) {
      log.error("Error creating Notion client for match extraction: {}", e.getMessage());
      return null;
    }
  }
}
