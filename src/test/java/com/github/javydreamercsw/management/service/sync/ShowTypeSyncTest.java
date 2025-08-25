package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.ShowPage;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.match.MatchResultService;
import com.github.javydreamercsw.management.service.match.type.MatchTypeService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.sync.NotionSyncService.SyncResult;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("Show Type Sync Tests")
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "notion.sync.enabled=true",
      "notion.sync.entities=show_types",
      "notion.sync.scheduler.enabled=true"
    })
class ShowTypeSyncTest {

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
  @Mock private ShowService showService;
  @Mock private ShowTypeService showTypeService;
  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private SeasonService seasonService;
  @Mock private ShowTemplateService showTemplateService;
  @Mock private FactionRepository factionRepository;
  @Mock private TeamService teamService;
  @Mock private TeamRepository teamRepository;
  @Mock private MatchResultService matchResultService;
  @Mock private MatchTypeService matchTypeService;

  private NotionSyncService syncService;

  @BeforeEach
  void setUp() {
    // Create a NotionSyncService instance for testing with all required mocks
    syncService =
        new NotionSyncService(
            objectMapper,
            notionHandler,
            syncProperties,
            progressTracker,
            healthMonitor,
            retryService,
            circuitBreakerService,
            validationService,
            syncTransactionManager,
            integrityChecker,
            showService,
            showTypeService,
            wrestlerService,
            wrestlerRepository,
            seasonService,
            showTemplateService,
            factionRepository,
            teamService,
            teamRepository,
            matchResultService,
            matchTypeService);
  }

  @Test
  @DisplayName("Should extract show types from Notion and create them in database")
  void shouldExtractAndCreateShowTypesFromNotion() {
    // Given
    List<ShowPage> mockShowPages = createMockShowPages();
    when(notionHandler.loadAllShowsForSync()).thenReturn(mockShowPages);
    when(showTypeService.findByName("Weekly")).thenReturn(Optional.empty());
    when(showTypeService.findByName("Premium Live Event")).thenReturn(Optional.empty());
    when(showTypeService.findAll()).thenReturn(List.of()); // No existing show types
    when(showTypeService.save(any(ShowType.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    SyncResult result = syncService.syncShowTypes("test-operation-id");

    // Then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntityType()).isEqualTo("ShowTypes");
    assertThat(result.getSyncedCount()).isGreaterThan(0);
  }

  @Test
  @DisplayName("Should create default show types when no Notion show types found")
  void shouldCreateDefaultShowTypesWhenNotionEmpty() {
    // Given
    when(notionHandler.loadAllShowsForSync()).thenReturn(new ArrayList<>());
    when(showTypeService.findAll()).thenReturn(List.of()); // No existing show types
    when(showTypeService.save(any(ShowType.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    SyncResult result = syncService.syncShowTypes("test-operation-id");

    // Then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntityType()).isEqualTo("ShowTypes");
    assertThat(result.getSyncedCount()).isGreaterThan(0);
  }

  @Test
  @DisplayName("Should skip existing show types and not create duplicates")
  void shouldSkipExistingShowTypes() {
    // Given
    List<ShowPage> mockShowPages = createMockShowPages();
    when(notionHandler.loadAllShowsForSync()).thenReturn(mockShowPages);

    // Mock existing show types
    ShowType existingWeekly = new ShowType();
    existingWeekly.setName("Weekly");
    when(showTypeService.findByName("Weekly")).thenReturn(Optional.of(existingWeekly));

    ShowType existingPLE = new ShowType();
    existingPLE.setName("Premium Live Event");
    when(showTypeService.findByName("Premium Live Event")).thenReturn(Optional.of(existingPLE));

    when(showTypeService.findAll()).thenReturn(List.of(existingWeekly, existingPLE));

    // When
    SyncResult result = syncService.syncShowTypes("test-operation-id");

    // Then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntityType()).isEqualTo("ShowTypes");
    // Should have 0 created since all already exist
    assertThat(result.getSyncedCount()).isEqualTo(0);
  }

  private List<ShowPage> createMockShowPages() {
    List<ShowPage> showPages = new ArrayList<>();

    // Create mock show page with Weekly show type
    ShowPage weeklyShow = new ShowPage();
    Map<String, Object> weeklyProps = new HashMap<>();
    weeklyProps.put("Show Type", "Weekly");
    ReflectionTestUtils.setField(weeklyShow, "rawProperties", weeklyProps);
    showPages.add(weeklyShow);

    // Create mock show page with PLE show type
    ShowPage pleShow = new ShowPage();
    Map<String, Object> pleProps = new HashMap<>();
    pleProps.put("Show Type", "Premium Live Event");
    ReflectionTestUtils.setField(pleShow, "rawProperties", pleProps);
    showPages.add(pleShow);

    // Create mock show page with duplicate Weekly show type (should be deduplicated)
    ShowPage anotherWeeklyShow = new ShowPage();
    Map<String, Object> anotherWeeklyProps = new HashMap<>();
    anotherWeeklyProps.put("Show Type", "Weekly");
    ReflectionTestUtils.setField(anotherWeeklyShow, "rawProperties", anotherWeeklyProps);
    showPages.add(anotherWeeklyShow);

    return showPages;
  }
}
