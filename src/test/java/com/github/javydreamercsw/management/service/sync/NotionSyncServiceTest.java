package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.test.BaseTest;
import com.github.javydreamercsw.management.config.EntitySyncConfiguration;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.sync.entity.FactionSyncService;
import com.github.javydreamercsw.management.service.sync.entity.InjurySyncService;
import com.github.javydreamercsw.management.service.sync.entity.MatchSyncService;
import com.github.javydreamercsw.management.service.sync.entity.SeasonSyncService;
import com.github.javydreamercsw.management.service.sync.entity.ShowSyncService;
import com.github.javydreamercsw.management.service.sync.entity.ShowTemplateSyncService;
import com.github.javydreamercsw.management.service.sync.entity.ShowTypeSyncService;
import com.github.javydreamercsw.management.service.sync.entity.TeamSyncService;
import com.github.javydreamercsw.management.service.sync.entity.WrestlerSyncService;
import com.github.javydreamercsw.management.service.sync.parallel.ParallelSyncOrchestrator;
import com.github.javydreamercsw.management.service.sync.parallel.ParallelSyncOrchestrator.ParallelSyncResult;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@EnabledIf("isNotionTokenAvailable")
class NotionSyncServiceTest extends BaseTest {

  @Mock private ObjectMapper objectMapper;
  @Mock private NotionSyncProperties syncProperties;

  // Mock entity-specific sync services
  @Mock private ShowSyncService showSyncService;
  @Mock private WrestlerSyncService wrestlerSyncService;
  @Mock private FactionSyncService factionSyncService;
  @Mock private TeamSyncService teamSyncService;
  @Mock private MatchSyncService matchSyncService;
  @Mock private SeasonSyncService seasonSyncService;
  @Mock private ShowTypeSyncService showTypeSyncService;
  @Mock private ShowTemplateSyncService showTemplateSyncService;
  @Mock private InjurySyncService injurySyncService;

  // Mock parallel sync components
  @Mock private ParallelSyncOrchestrator parallelSyncOrchestrator;
  @Mock private EntitySyncConfiguration entitySyncConfiguration;

  private NotionSyncService notionSyncService;

  @BeforeEach
  void setUp() throws Exception {
    // Create the service with constructor injection
    notionSyncService = new NotionSyncService(objectMapper, syncProperties);

    // Inject the mocked sync services using reflection
    setField(notionSyncService, "showSyncService", showSyncService);
    setField(notionSyncService, "wrestlerSyncService", wrestlerSyncService);
    setField(notionSyncService, "factionSyncService", factionSyncService);
    setField(notionSyncService, "teamSyncService", teamSyncService);
    setField(notionSyncService, "matchSyncService", matchSyncService);
    setField(notionSyncService, "seasonSyncService", seasonSyncService);
    setField(notionSyncService, "showTypeSyncService", showTypeSyncService);
    setField(notionSyncService, "showTemplateSyncService", showTemplateSyncService);
    setField(notionSyncService, "injurySyncService", injurySyncService);
    setField(notionSyncService, "parallelSyncOrchestrator", parallelSyncOrchestrator);
    setField(notionSyncService, "entitySyncConfiguration", entitySyncConfiguration);
  }

  // ==================== PARALLEL SYNC TESTS ====================

  @Test
  @DisplayName("Should execute parallel sync for all entities")
  void shouldExecuteParallelSyncForAllEntities() {
    // Given
    ParallelSyncResult mockResult = mock(ParallelSyncResult.class);
    when(parallelSyncOrchestrator.executeParallelSync()).thenReturn(mockResult);

    // When
    ParallelSyncResult result = notionSyncService.syncAllEntitiesParallel();

    // Then
    assertThat(result).isEqualTo(mockResult);
    verify(parallelSyncOrchestrator).executeParallelSync();
  }

  @Test
  @DisplayName("Should execute parallel sync with operation ID")
  void shouldExecuteParallelSyncWithOperationId() {
    // Given
    String operationId = "test-parallel-sync";
    ParallelSyncResult mockResult = mock(ParallelSyncResult.class);
    when(parallelSyncOrchestrator.executeParallelSync(operationId)).thenReturn(mockResult);

    // When
    ParallelSyncResult result = notionSyncService.syncAllEntitiesParallel(operationId);

    // Then
    assertThat(result).isEqualTo(mockResult);
    verify(parallelSyncOrchestrator).executeParallelSync(operationId);
  }

  // ==================== SHOWS SYNC TESTS ====================

  @Test
  @DisplayName("Should delegate shows sync to ShowSyncService")
  void shouldDelegateShowsSyncToShowSyncService() {
    // Given
    BaseSyncService.SyncResult mockResult = BaseSyncService.SyncResult.success("Shows", 5, 0);
    when(showSyncService.syncShows()).thenReturn(mockResult);

    // When
    BaseSyncService.SyncResult result = notionSyncService.syncShows();

    // Then
    assertThat(result).isEqualTo(mockResult);
    verify(showSyncService).syncShows();
  }

  @Test
  @DisplayName("Should delegate shows sync with operation ID to ShowSyncService")
  void shouldDelegateShowsSyncWithOperationIdToShowSyncService() {
    // Given
    String operationId = "test-shows-sync";
    BaseSyncService.SyncResult mockResult = BaseSyncService.SyncResult.success("Shows", 3, 1);
    when(showSyncService.syncShows(operationId)).thenReturn(mockResult);

    // When
    BaseSyncService.SyncResult result = notionSyncService.syncShows(operationId);

    // Then
    assertThat(result).isEqualTo(mockResult);
    verify(showSyncService).syncShows(operationId);
  }

  // ==================== WRESTLERS SYNC TESTS ====================

  @Test
  @DisplayName("Should delegate wrestlers sync to WrestlerSyncService")
  void shouldDelegateWrestlersSyncToWrestlerSyncService() {
    // Given
    String operationId = "test-wrestlers-sync";
    BaseSyncService.SyncResult mockResult = BaseSyncService.SyncResult.success("Wrestlers", 10, 2);
    when(wrestlerSyncService.syncWrestlers(operationId)).thenReturn(mockResult);

    // When
    BaseSyncService.SyncResult result = notionSyncService.syncWrestlers(operationId);

    // Then
    assertThat(result).isEqualTo(mockResult);
    verify(wrestlerSyncService).syncWrestlers(operationId);
  }

  // ==================== FACTIONS SYNC TESTS ====================

  @Test
  @DisplayName("Should delegate factions sync to FactionSyncService")
  void shouldDelegateFactionsSyncToFactionSyncService() {
    // Given
    String operationId = "test-factions-sync";
    BaseSyncService.SyncResult mockResult = BaseSyncService.SyncResult.success("Factions", 4, 0);
    when(factionSyncService.syncFactions(operationId)).thenReturn(mockResult);

    // When
    BaseSyncService.SyncResult result = notionSyncService.syncFactions(operationId);

    // Then
    assertThat(result).isEqualTo(mockResult);
    verify(factionSyncService).syncFactions(operationId);
  }

  // ==================== TEAMS SYNC TESTS ====================

  @Test
  @DisplayName("Should delegate teams sync to TeamSyncService")
  void shouldDelegateTeamsSyncToTeamSyncService() {
    // Given
    String operationId = "test-teams-sync";
    BaseSyncService.SyncResult mockResult = BaseSyncService.SyncResult.success("Teams", 6, 1);
    when(teamSyncService.syncTeams(operationId)).thenReturn(mockResult);

    // When
    BaseSyncService.SyncResult result = notionSyncService.syncTeams(operationId);

    // Then
    assertThat(result).isEqualTo(mockResult);
    verify(teamSyncService).syncTeams(operationId);
  }

  // ==================== MATCHES SYNC TESTS ====================

  @Test
  @DisplayName("Should delegate matches sync to MatchSyncService")
  void shouldDelegateMatchesSyncToMatchSyncService() {
    // Given
    String operationId = "test-matches-sync";
    BaseSyncService.SyncResult mockResult = BaseSyncService.SyncResult.success("Matches", 15, 3);
    when(matchSyncService.syncMatches(operationId)).thenReturn(mockResult);

    // When
    BaseSyncService.SyncResult result = notionSyncService.syncMatches(operationId);

    // Then
    assertThat(result).isEqualTo(mockResult);
    verify(matchSyncService).syncMatches(operationId);
  }

  // ==================== SEASONS SYNC TESTS ====================

  @Test
  @DisplayName("Should delegate seasons sync to SeasonSyncService")
  void shouldDelegateSeasonsSyncToSeasonSyncService() {
    // Given
    String operationId = "test-seasons-sync";
    BaseSyncService.SyncResult mockResult = BaseSyncService.SyncResult.success("Seasons", 2, 0);
    when(seasonSyncService.syncSeasons(operationId)).thenReturn(mockResult);

    // When
    BaseSyncService.SyncResult result = notionSyncService.syncSeasons(operationId);

    // Then
    assertThat(result).isEqualTo(mockResult);
    verify(seasonSyncService).syncSeasons(operationId);
  }

  // ==================== SHOW TEMPLATES SYNC TESTS ====================

  @Test
  @DisplayName("Should delegate show templates sync to ShowTemplateSyncService")
  void shouldDelegateShowTemplatesSyncToShowTemplateSyncService() {
    // Given
    String operationId = "test-show-templates-sync";
    BaseSyncService.SyncResult mockResult =
        BaseSyncService.SyncResult.success("ShowTemplates", 8, 1);
    when(showTemplateSyncService.syncShowTemplates(operationId)).thenReturn(mockResult);

    // When
    BaseSyncService.SyncResult result = notionSyncService.syncShowTemplates(operationId);

    // Then
    assertThat(result).isEqualTo(mockResult);
    verify(showTemplateSyncService).syncShowTemplates(operationId);
  }

  // ==================== SHOW TYPES SYNC TESTS ====================

  @Test
  @DisplayName("Should delegate show types sync to ShowTypeSyncService")
  void shouldDelegateShowTypesSyncToShowTypeSyncService() {
    // Given
    String operationId = "test-show-types-sync";
    BaseSyncService.SyncResult mockResult = BaseSyncService.SyncResult.success("ShowTypes", 3, 0);
    when(showTypeSyncService.syncShowTypes(operationId)).thenReturn(mockResult);

    // When
    BaseSyncService.SyncResult result = notionSyncService.syncShowTypes(operationId);

    // Then
    assertThat(result).isEqualTo(mockResult);
    verify(showTypeSyncService).syncShowTypes(operationId);
  }

  // ==================== INJURY TYPES SYNC TESTS ====================

  @Test
  @DisplayName("Should delegate injury types sync to InjurySyncService")
  void shouldDelegateInjuryTypesSyncToInjurySyncService() {
    // Given
    String operationId = "test-injury-types-sync";
    BaseSyncService.SyncResult mockResult = BaseSyncService.SyncResult.success("InjuryTypes", 7, 2);
    when(injurySyncService.syncInjuryTypes(operationId)).thenReturn(mockResult);

    // When
    BaseSyncService.SyncResult result = notionSyncService.syncInjuryTypes(operationId);

    // Then
    assertThat(result).isEqualTo(mockResult);
    verify(injurySyncService).syncInjuryTypes(operationId);
  }

  // ==================== ERROR HANDLING TESTS ====================

  @Test
  @DisplayName("Should handle sync service failures gracefully")
  void shouldHandleSyncServiceFailuresGracefully() {
    // Given
    String operationId = "test-error-handling";
    RuntimeException testException = new RuntimeException("Test sync failure");
    when(showSyncService.syncShows(operationId)).thenThrow(testException);

    // When & Then
    assertThrows(RuntimeException.class, () -> notionSyncService.syncShows(operationId));
    verify(showSyncService).syncShows(operationId);
  }

  /** Helper method to set private fields via reflection for testing. */
  private void setField(Object target, String fieldName, Object value) {
    try {
      Field field = findField(target.getClass(), fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set field " + fieldName, e);
    }
  }

  /** Recursively searches for a field in the class hierarchy. */
  private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
    try {
      return clazz.getDeclaredField(fieldName);
    } catch (NoSuchFieldException e) {
      Class<?> superclass = clazz.getSuperclass();
      if (superclass != null && superclass != Object.class) {
        return findField(superclass, fieldName);
      }
      throw e;
    }
  }
}
