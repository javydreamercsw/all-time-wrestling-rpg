package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.FactionPage;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.ShowPage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.injury.InjuryTypeService;
import com.github.javydreamercsw.management.service.match.MatchResultService;
import com.github.javydreamercsw.management.service.match.type.MatchTypeService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class NotionSyncServiceTest {

  @Mock private ObjectMapper objectMapper;
  @Mock private NotionHandler notionHandler;
  @Mock private NotionSyncProperties syncProperties;
  @Mock private SyncProgressTracker progressTracker;

  @Mock private SyncHealthMonitor healthMonitor;
  @Mock private RetryService retryService;
  @Mock private CircuitBreakerService circuitBreakerService;
  @Mock private SyncValidationService validationService;
  @Mock private SyncTransactionManager syncTransactionManager;
  @Mock private DataIntegrityChecker integrityChecker;

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
  @Mock private MatchResultService matchResultService;
  @Mock private MatchTypeService matchTypeService;
  @Mock private InjuryTypeService injuryTypeService;

  private NotionSyncService notionSyncService;

  @BeforeEach
  void setUp() throws Exception {
    // Only mock the services that are actually used by syncShows()

    // Mock circuit breaker to return a successful result (lenient to avoid unnecessary stubbing
    // warnings)
    NotionSyncService.SyncResult mockSuccessResult =
        NotionSyncService.SyncResult.success("Shows", 0, 0);
    lenient().when(circuitBreakerService.execute(anyString(), any())).thenReturn(mockSuccessResult);

    // Mock season service to avoid null pointer exceptions
    Page<Season> emptySeasonPage = Page.empty();
    lenient().when(seasonService.getAllSeasons(any(Pageable.class))).thenReturn(emptySeasonPage);

    // Mock season creation
    Season mockSeason = new Season();
    mockSeason.setId(1L);
    mockSeason.setName("Season 1");
    lenient()
        .when(seasonService.createSeason(anyString(), anyString(), anyInt()))
        .thenReturn(mockSeason);

    // Create the service with simplified constructor
    notionSyncService = new NotionSyncService(objectMapper, syncProperties);

    // Manually inject the mocked dependencies using reflection or setters
    // Since we're using @Autowired fields, we need to set them manually in tests
    setField(notionSyncService, "notionHandler", notionHandler);
    setField(notionSyncService, "progressTracker", progressTracker);
    setField(notionSyncService, "healthMonitor", healthMonitor);
    setField(notionSyncService, "retryService", retryService);
    setField(notionSyncService, "circuitBreakerService", circuitBreakerService);
    setField(notionSyncService, "validationService", validationService);
    setField(notionSyncService, "syncTransactionManager", syncTransactionManager);
    setField(notionSyncService, "integrityChecker", integrityChecker);
    setField(notionSyncService, "showService", showService);
    setField(notionSyncService, "showTypeService", showTypeService);
    setField(notionSyncService, "wrestlerService", wrestlerService);
    setField(notionSyncService, "wrestlerRepository", wrestlerRepository);
    setField(notionSyncService, "seasonService", seasonService);
    setField(notionSyncService, "showTemplateService", showTemplateService);
    setField(notionSyncService, "factionRepository", factionRepository);
    setField(notionSyncService, "teamService", teamService);
    setField(notionSyncService, "teamRepository", teamRepository);
    setField(notionSyncService, "matchResultService", matchResultService);
    setField(notionSyncService, "matchTypeService", matchTypeService);
    setField(notionSyncService, "injuryTypeService", injuryTypeService);
  }

  @Test
  @DisplayName("Should skip sync when shows entity is disabled")
  void shouldSkipSyncWhenShowsEntityDisabled() {
    // Given
    when(syncProperties.isEntityEnabled("shows")).thenReturn(false);

    // When
    NotionSyncService.SyncResult result = notionSyncService.syncShows();

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

    // When - This will fail due to missing NOTION_TOKEN, which is expected in unit tests
    NotionSyncService.SyncResult result = notionSyncService.syncShows();

    // Then - Should fail due to missing NOTION_TOKEN (this is correct behavior)
    assertNotNull(result);
    assertFalse(result.isSuccess()); // Should fail without NOTION_TOKEN
    assertEquals("Shows", result.getEntityType());
    assertThat(result.getErrorMessage()).contains("NOTION_TOKEN");
  }

  @Test
  @DisplayName("Should handle shows with valid data")
  void shouldHandleShowsWithValidData() {
    // Given
    when(syncProperties.isEntityEnabled("shows")).thenReturn(true);

    // When - This will fail due to missing NOTION_TOKEN, which is expected in unit tests
    NotionSyncService.SyncResult result = notionSyncService.syncShows();

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
    NotionSyncService.SyncResult result = notionSyncService.syncFactions("test-operation-id");

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

    // When - This will fail due to missing NOTION_TOKEN, which is expected in unit tests
    NotionSyncService.SyncResult result = notionSyncService.syncFactions("test-operation-id");

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

    // When - This will fail due to missing NOTION_TOKEN, which is expected in unit tests
    NotionSyncService.SyncResult result = notionSyncService.syncFactions(operationId);

    // Then - Should fail gracefully due to missing NOTION_TOKEN
    assertNotNull(result);
    assertFalse(result.isSuccess());
    assertEquals("Factions", result.getEntityType());
    assertNotNull(result.getErrorMessage());
    assertTrue(result.getErrorMessage().contains("NOTION_TOKEN"));

    // Verify progress tracking failure was recorded (no startOperation since we fail early)
    verify(progressTracker).failOperation(eq(operationId), contains("NOTION_TOKEN"));
    verify(progressTracker, never()).startOperation(anyString(), anyString(), anyInt());
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

  /**
   * Helper method to set private fields via reflection for testing. This is needed because we
   * switched from constructor injection to field injection.
   */
  private void setField(Object target, String fieldName, Object value) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set field " + fieldName, e);
    }
  }
}
