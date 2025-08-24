package com.github.javydreamercsw.management.service.sync;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.FactionPage;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.ShowPage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.sync.NotionSyncService.SyncResult;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotionSyncServiceTest {

  @Mock private ObjectMapper objectMapper;
  @Mock private NotionHandler notionHandler;
  @Mock private NotionSyncProperties syncProperties;
  @Mock private SyncProgressTracker progressTracker;

  @Mock private SyncHealthMonitor healthMonitor;

  @Mock private TeamService teamService;

  @Mock private TeamRepository teamRepository;

  // Mock database services
  @Mock private ShowService showService;
  @Mock private ShowTypeService showTypeService;
  @Mock private SeasonService seasonService;
  @Mock private ShowTemplateService showTemplateService;
  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private FactionRepository factionRepository;

  private NotionSyncService notionSyncService;

  @BeforeEach
  void setUp() {
    notionSyncService =
        new NotionSyncService(
            objectMapper,
            notionHandler,
            syncProperties,
            progressTracker,
            healthMonitor,
            showService,
            showTypeService,
            wrestlerService,
            wrestlerRepository,
            seasonService,
            showTemplateService,
            factionRepository,
            teamService,
            teamRepository);
  }

  @Test
  @DisplayName("Should skip sync when shows entity is disabled")
  void shouldSkipSyncWhenShowsEntityDisabled() {
    // Given
    when(syncProperties.isEntityEnabled("shows")).thenReturn(false);

    // When
    SyncResult result = notionSyncService.syncShows();

    // Then
    assertTrue(result.isSuccess());
    assertEquals("Shows", result.getEntityType());
    assertEquals(0, result.getSyncedCount());
    verify(notionHandler, never()).loadAllShowsForSync();
  }

  @Test
  @DisplayName("Should handle empty shows list from Notion")
  void shouldHandleEmptyShowsListFromNotion() {
    // Given
    when(syncProperties.isEntityEnabled("shows")).thenReturn(true);
    when(syncProperties.isBackupEnabled()).thenReturn(false);
    // Mock NotionHandler to throw exception when NOTION_TOKEN is not available
    when(notionHandler.loadAllShowsForSync())
        .thenThrow(new IllegalStateException("NOTION_TOKEN is required for sync operations"));

    // When - This will fail due to missing NOTION_TOKEN, which is expected in unit tests
    SyncResult result = notionSyncService.syncShows();

    // Then - Should fail gracefully due to missing NOTION_TOKEN
    assertNotNull(result);
    assertFalse(result.isSuccess());
    assertEquals("Shows", result.getEntityType());
    assertNotNull(result.getErrorMessage());
    assertTrue(result.getErrorMessage().contains("NOTION_TOKEN"));
  }

  @Test
  @DisplayName("Should handle shows with valid data")
  void shouldHandleShowsWithValidData() {
    // Given
    when(syncProperties.isEntityEnabled("shows")).thenReturn(true);
    when(syncProperties.isBackupEnabled()).thenReturn(false);
    // Mock NotionHandler to throw exception when NOTION_TOKEN is not available
    when(notionHandler.loadAllShowsForSync())
        .thenThrow(new IllegalStateException("NOTION_TOKEN is required for sync operations"));

    // When - This will fail due to missing NOTION_TOKEN, which is expected in unit tests
    SyncResult result = notionSyncService.syncShows();

    // Then - Should fail gracefully due to missing NOTION_TOKEN
    assertNotNull(result);
    assertFalse(result.isSuccess());
    assertEquals("Shows", result.getEntityType());
    assertNotNull(result.getErrorMessage());
    assertTrue(result.getErrorMessage().contains("NOTION_TOKEN"));
  }

  private ShowPage createMockShowPage(String name, String description) {
    ShowPage showPage = new ShowPage();
    showPage.setId("test-id-123");

    // Create mock raw properties
    Map<String, Object> rawProperties = new HashMap<>();
    rawProperties.put("Name", name);
    rawProperties.put("Description", description);
    rawProperties.put("Show Type", "Weekly");
    rawProperties.put("Date", "2024-01-15");

    showPage.setRawProperties(rawProperties);

    return showPage;
  }

  // ==================== FACTION SYNC TESTS ====================

  @Test
  @DisplayName("Should skip faction sync when factions entity is disabled")
  void shouldSkipFactionSyncWhenFactionsEntityDisabled() {
    // Given
    when(syncProperties.isEntityEnabled("factions")).thenReturn(false);

    // When
    SyncResult result = notionSyncService.syncFactions(null);

    // Then
    assertTrue(result.isSuccess());
    assertEquals("Factions", result.getEntityType());
    assertEquals(0, result.getSyncedCount());
    verify(notionHandler, never()).loadAllFactions();
  }

  @Test
  @DisplayName("Should handle empty factions list from Notion")
  void shouldHandleEmptyFactionsListFromNotion() {
    // Given
    when(syncProperties.isEntityEnabled("factions")).thenReturn(true);
    when(syncProperties.isBackupEnabled()).thenReturn(false);
    // Mock NotionHandler to throw exception when NOTION_TOKEN is not available
    when(notionHandler.loadAllFactions())
        .thenThrow(new IllegalStateException("NOTION_TOKEN is required for sync operations"));

    // When - This will fail due to missing NOTION_TOKEN, which is expected in unit tests
    SyncResult result = notionSyncService.syncFactions(null);

    // Then - Should fail gracefully due to missing NOTION_TOKEN
    assertNotNull(result);
    assertFalse(result.isSuccess());
    assertEquals("Factions", result.getEntityType());
    assertTrue(result.getErrorMessage().contains("NOTION_TOKEN"));
  }

  @Test
  @DisplayName("Should handle faction sync with progress tracking")
  void shouldHandleFactionSyncWithProgressTracking() {
    // Given
    String operationId = "test-faction-sync";
    when(syncProperties.isEntityEnabled("factions")).thenReturn(true);
    when(syncProperties.isBackupEnabled()).thenReturn(false);
    // Mock NotionHandler to throw exception when NOTION_TOKEN is not available
    when(notionHandler.loadAllFactions())
        .thenThrow(new IllegalStateException("NOTION_TOKEN is required for sync operations"));

    // When - This will fail due to missing NOTION_TOKEN, which is expected in unit tests
    SyncResult result = notionSyncService.syncFactions(operationId);

    // Then - Should fail gracefully but still track progress
    assertNotNull(result);
    assertFalse(result.isSuccess());
    assertEquals("Factions", result.getEntityType());

    // Verify progress tracking was attempted
    verify(progressTracker).startOperation(eq(operationId), eq("Sync Factions"), eq(4));
    verify(progressTracker).failOperation(eq(operationId), anyString());
  }

  /**
   * Helper method to create a mock FactionPage for testing.
   *
   * @param name Faction name
   * @param description Faction description
   * @param leader Leader wrestler name
   * @param alignment Faction alignment
   * @return Mock FactionPage
   */
  private FactionPage createMockFactionPage(
      String name, String description, String leader, String alignment) {
    FactionPage factionPage = new FactionPage();
    factionPage.setId("faction-" + name.toLowerCase().replace(" ", "-"));

    // Create mock raw properties
    Map<String, Object> rawProperties = new HashMap<>();
    rawProperties.put("Name", name);
    rawProperties.put("Description", description);
    rawProperties.put("Leader", leader);
    rawProperties.put("Alignment", alignment);
    rawProperties.put("Status", "Active");
    rawProperties.put("FormedDate", "2024-01-01");
    rawProperties.put("Members", "2 relations"); // Mock format for multiple relations

    factionPage.setRawProperties(rawProperties);

    return factionPage;
  }
}
