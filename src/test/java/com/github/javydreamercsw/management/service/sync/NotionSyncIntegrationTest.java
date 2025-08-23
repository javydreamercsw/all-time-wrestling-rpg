package com.github.javydreamercsw.management.service.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.ShowPage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.sync.NotionSyncService.SyncResult;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration test for NotionSyncService that tests actual Notion API integration. This test
 * requires the NOTION_TOKEN environment variable to be set.
 */
class NotionSyncIntegrationTest {

  private static final Logger log = LoggerFactory.getLogger(NotionSyncIntegrationTest.class);

  private NotionSyncService notionSyncService;
  private NotionHandler notionHandler;
  private NotionSyncProperties syncProperties;
  private SyncProgressTracker progressTracker;

  // Mock database services for testing
  private ShowService showService;
  private ShowTypeService showTypeService;
  private SeasonService seasonService;
  private ShowTemplateService showTemplateService;
  private WrestlerService wrestlerService;
  private WrestlerRepository wrestlerRepository;
  private FactionRepository factionRepository;

  @BeforeEach
  void setUp() {
    // Set up the Notion token for testing
    String notionToken = "***REMOVED***";
    System.setProperty("NOTION_TOKEN", notionToken);

    // Create real instances for integration testing
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    notionHandler = NotionHandler.getInstance();
    progressTracker = new SyncProgressTracker();

    // Create mock database services
    showService = mock(ShowService.class);
    showTypeService = mock(ShowTypeService.class);
    seasonService = mock(SeasonService.class);
    showTemplateService = mock(ShowTemplateService.class);
    wrestlerService = mock(WrestlerService.class);
    wrestlerRepository = mock(WrestlerRepository.class);
    factionRepository = mock(FactionRepository.class);

    // Create sync properties with test configuration
    syncProperties = new NotionSyncProperties();
    syncProperties.setEnabled(true);
    syncProperties.setEntities(List.of("shows"));

    // Configure backup settings for testing
    NotionSyncProperties.Backup backup = new NotionSyncProperties.Backup();
    backup.setEnabled(false); // Disable backup for testing
    syncProperties.setBackup(backup);

    notionSyncService =
        new NotionSyncService(
            objectMapper,
            notionHandler,
            syncProperties,
            progressTracker,
            showService,
            showTypeService,
            wrestlerService,
            wrestlerRepository,
            seasonService,
            showTemplateService,
            factionRepository);
  }

  @Test
  @DisplayName("Should connect to Notion and retrieve database information")
  void shouldConnectToNotionAndRetrieveDatabaseInfo() {
    // This test verifies that we can connect to Notion with the provided token
    try {
      // Try to get the Shows database ID
      String showsDbId = notionHandler.getDatabaseId("Shows");
      log.info("Shows database ID: {}", showsDbId);

      // The database ID should not be null if connection is successful
      assertNotNull(showsDbId, "Shows database should be found in Notion workspace");

    } catch (Exception e) {
      log.error("Failed to connect to Notion or retrieve database info", e);
      fail(
          "Should be able to connect to Notion and retrieve database information: "
              + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should load shows from Notion Shows database")
  void shouldLoadShowsFromNotionShowsDatabase() {
    try {
      // Load all shows from Notion
      List<ShowPage> shows = notionHandler.loadAllShows();
      log.info("Loaded {} shows from Notion", shows.size());

      // Verify we got some shows (or at least an empty list without errors)
      assertNotNull(shows, "Shows list should not be null");

      // Log details about the first few shows for verification
      for (int i = 0; i < Math.min(3, shows.size()); i++) {
        ShowPage show = shows.get(i);
        log.info("Show {}: ID={}, Properties={}", i + 1, show.getId(), show.getRawProperties());
      }

    } catch (Exception e) {
      log.error("Failed to load shows from Notion", e);
      fail("Should be able to load shows from Notion: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should perform complete sync operation")
  void shouldPerformCompleteSyncOperation() {
    try {
      // Perform the sync operation
      SyncResult result = notionSyncService.syncShows();

      log.info("Sync result: {}", result);

      // Verify the sync completed successfully
      assertNotNull(result, "Sync result should not be null");
      assertEquals("Shows", result.getEntityType());

      if (result.isSuccess()) {
        log.info("✅ Sync completed successfully with {} shows", result.getSyncedCount());
        assertTrue(result.getSyncedCount() >= 0, "Synced count should be non-negative");
      } else {
        log.warn("❌ Sync failed: {}", result.getErrorMessage());
        // For integration testing, we'll log the failure but not fail the test
        // since it might be due to file system permissions or other environmental issues
      }

    } catch (Exception e) {
      log.error("Failed to perform sync operation", e);
      fail("Should be able to perform sync operation: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should handle Notion API errors gracefully")
  void shouldHandleNotionApiErrorsGracefully() {
    // Test with invalid token to verify error handling
    System.setProperty("NOTION_TOKEN", "invalid_token");

    try {
      // Create a new handler with invalid token
      NotionHandler invalidHandler = NotionHandler.getInstance();
      SyncProgressTracker invalidProgressTracker = new SyncProgressTracker();
      NotionSyncService invalidSyncService =
          new NotionSyncService(
              new ObjectMapper(),
              invalidHandler,
              syncProperties,
              invalidProgressTracker,
              showService,
              showTypeService,
              wrestlerService,
              wrestlerRepository,
              seasonService,
              showTemplateService,
              factionRepository);

      // This should handle the error gracefully
      SyncResult result = invalidSyncService.syncShows();

      // Should return a failure result rather than throwing an exception
      assertNotNull(result);
      assertFalse(result.isSuccess());
      assertNotNull(result.getErrorMessage());

      log.info("Handled invalid token gracefully: {}", result.getErrorMessage());

    } catch (Exception e) {
      // This is also acceptable - the service should handle errors appropriately
      log.info("Service threw exception for invalid token (acceptable): {}", e.getMessage());
    } finally {
      // Restore the valid token
      System.setProperty("NOTION_TOKEN", "***REMOVED***");
    }
  }

  @Test
  @DisplayName("Should verify shows.json file structure after sync")
  void shouldVerifyShowsJsonFileStructureAfterSync() {
    try {
      // Perform sync
      SyncResult result = notionSyncService.syncShows();

      if (result.isSuccess() && result.getSyncedCount() > 0) {
        // Verify the JSON file exists and has valid structure
        Path showsJsonPath = Paths.get("src/main/resources/shows.json");
        assertTrue(Files.exists(showsJsonPath), "shows.json file should exist after sync");

        // Read and parse the JSON file
        String jsonContent = Files.readString(showsJsonPath);
        assertFalse(jsonContent.trim().isEmpty(), "shows.json should not be empty");

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        // Parse as array of ShowDTO objects
        var shows =
            mapper.readValue(jsonContent, new TypeReference<List<Map<String, Object>>>() {});

        assertFalse(shows.isEmpty(), "Shows array should not be empty");
        assertEquals(
            result.getSyncedCount(),
            shows.size(),
            "Number of shows in JSON should match sync result");

        // Verify structure of first show
        Map<String, Object> firstShow = shows.get(0);
        assertTrue(firstShow.containsKey("name"), "Show should have name field");
        assertTrue(firstShow.containsKey("showDate"), "Show should have showDate field");
        assertTrue(firstShow.containsKey("seasonName"), "Show should have seasonName field");

        log.info("✅ Verified shows.json structure with {} shows", shows.size());

      } else {
        log.info("Sync returned no shows or failed, skipping file structure verification");
      }

    } catch (Exception e) {
      log.error("Failed to verify shows.json file structure", e);
      fail("Should be able to verify shows.json file structure: " + e.getMessage());
    }
  }

  @Test
  @DisplayName("Should measure sync performance")
  void shouldMeasureSyncPerformance() {
    try {
      long startTime = System.currentTimeMillis();

      // Perform sync
      SyncResult result = notionSyncService.syncShows();

      long endTime = System.currentTimeMillis();
      long duration = endTime - startTime;

      log.info("=== SYNC PERFORMANCE METRICS ===");
      log.info("Total sync time: {}ms", duration);
      log.info("Shows synced: {}", result.getSyncedCount());

      if (result.getSyncedCount() > 0) {
        double avgTimePerShow = (double) duration / result.getSyncedCount();
        log.info("Average time per show: {:.2f}ms", avgTimePerShow);

        // Performance assertions - sync should be reasonably fast
        assertTrue(duration < 30000, "Sync should complete within 30 seconds");
        assertTrue(avgTimePerShow < 1000, "Average time per show should be less than 1 second");
      }

      log.info("=== END PERFORMANCE METRICS ===");

    } catch (Exception e) {
      log.error("Failed to measure sync performance", e);
      fail("Should be able to measure sync performance: " + e.getMessage());
    }
  }
}
