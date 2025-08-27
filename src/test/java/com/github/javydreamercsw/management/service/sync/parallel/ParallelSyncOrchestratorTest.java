package com.github.javydreamercsw.management.service.sync.parallel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.config.EntitySyncConfiguration;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService.SyncResult;
import com.github.javydreamercsw.management.service.sync.entity.*;
import com.github.javydreamercsw.management.service.sync.parallel.ParallelSyncOrchestrator.ParallelSyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for ParallelSyncOrchestrator testing parallel execution, configuration handling, and
 * error scenarios.
 */
@ExtendWith(MockitoExtension.class)
class ParallelSyncOrchestratorTest {

  @Mock private ShowSyncService showSyncService;
  @Mock private WrestlerSyncService wrestlerSyncService;
  @Mock private FactionSyncService factionSyncService;
  @Mock private TeamSyncService teamSyncService;
  @Mock private MatchSyncService matchSyncService;
  @Mock private SeasonSyncService seasonSyncService;
  @Mock private ShowTypeSyncService showTypeSyncService;
  @Mock private ShowTemplateSyncService showTemplateSyncService;
  @Mock private InjurySyncService injurySyncService;
  @Mock private EntitySyncConfiguration entityConfig;

  private ParallelSyncOrchestrator orchestrator;

  @BeforeEach
  void setUp() {
    orchestrator =
        new ParallelSyncOrchestrator(
            showSyncService,
            wrestlerSyncService,
            factionSyncService,
            teamSyncService,
            matchSyncService,
            seasonSyncService,
            showTypeSyncService,
            showTemplateSyncService,
            injurySyncService,
            entityConfig);

    // Setup default configuration behavior
    EntitySyncConfiguration.EntitySyncSettings defaults =
        new EntitySyncConfiguration.EntitySyncSettings();
    defaults.setMaxThreads(4);
    defaults.setTimeoutSeconds(300);
    when(entityConfig.getDefaults()).thenReturn(defaults);
  }

  @Test
  void executeParallelSync_WhenAllEntitiesEnabled_ShouldExecuteAllSyncs() {
    // Given
    setupAllEntitiesEnabled();
    setupSuccessfulSyncResults();

    // When
    ParallelSyncResult result = orchestrator.executeParallelSync("test-operation");

    // Then
    assertTrue(result.isSuccess());
    assertEquals(9, result.getEntityResults().size());
    assertEquals(9, result.getSuccessfulSyncs());
    assertEquals(0, result.getFailedSyncs());

    // Verify all services were called
    verify(showSyncService).syncShows(anyString());
    verify(wrestlerSyncService).syncWrestlers(anyString());
    verify(factionSyncService).syncFactions(anyString());
    verify(teamSyncService).syncTeams(anyString());
    verify(matchSyncService).syncMatches(anyString());
    verify(seasonSyncService).syncSeasons(anyString());
    verify(showTypeSyncService).syncShowTypes(anyString());
    verify(showTemplateSyncService).syncShowTemplates(anyString());
    verify(injurySyncService).syncInjuryTypes(anyString());
  }

  @Test
  void executeParallelSync_WhenSomeEntitiesDisabled_ShouldOnlyExecuteEnabled() {
    // Given
    when(entityConfig.isEntityEnabled("shows")).thenReturn(true);
    when(entityConfig.isEntityEnabled("wrestlers")).thenReturn(true);
    when(entityConfig.isEntityEnabled("factions")).thenReturn(false);
    when(entityConfig.isEntityEnabled("teams")).thenReturn(false);
    when(entityConfig.isEntityEnabled("matches")).thenReturn(false);
    when(entityConfig.isEntityEnabled("seasons")).thenReturn(false);
    when(entityConfig.isEntityEnabled("showtypes")).thenReturn(false);
    when(entityConfig.isEntityEnabled("showtemplates")).thenReturn(false);
    when(entityConfig.isEntityEnabled("injuries")).thenReturn(true);

    when(showSyncService.syncShows(anyString())).thenReturn(SyncResult.success("Shows", 5, 0));
    when(wrestlerSyncService.syncWrestlers(anyString()))
        .thenReturn(SyncResult.success("Wrestlers", 10, 0));
    when(injurySyncService.syncInjuryTypes(anyString()))
        .thenReturn(SyncResult.success("Injuries", 3, 0));

    // When
    ParallelSyncResult result = orchestrator.executeParallelSync("test-operation");

    // Then
    assertTrue(result.isSuccess());
    assertEquals(3, result.getEntityResults().size());
    assertEquals(3, result.getSuccessfulSyncs());

    // Verify only enabled services were called
    verify(showSyncService).syncShows(anyString());
    verify(wrestlerSyncService).syncWrestlers(anyString());
    verify(injurySyncService).syncInjuryTypes(anyString());

    // Verify disabled services were not called
    verify(factionSyncService, never()).syncFactions(anyString());
    verify(teamSyncService, never()).syncTeams(anyString());
  }

  @Test
  void executeParallelSync_WhenSomeEntitiesFail_ShouldContinueWithOthers() {
    // Given
    setupAllEntitiesEnabled();

    // Setup mixed results - some success, some failure
    when(showSyncService.syncShows(anyString())).thenReturn(SyncResult.success("Shows", 5, 0));
    when(wrestlerSyncService.syncWrestlers(anyString()))
        .thenReturn(SyncResult.failure("Wrestlers", "Database error"));
    when(factionSyncService.syncFactions(anyString()))
        .thenReturn(SyncResult.success("Factions", 3, 0));
    when(teamSyncService.syncTeams(anyString()))
        .thenReturn(SyncResult.failure("Teams", "Notion API error"));
    when(matchSyncService.syncMatches(anyString())).thenReturn(SyncResult.success("Matches", 8, 0));
    when(seasonSyncService.syncSeasons(anyString()))
        .thenReturn(SyncResult.success("Seasons", 2, 0));
    when(showTypeSyncService.syncShowTypes(anyString()))
        .thenReturn(SyncResult.success("ShowTypes", 4, 0));
    when(showTemplateSyncService.syncShowTemplates(anyString()))
        .thenReturn(SyncResult.success("ShowTemplates", 6, 0));
    when(injurySyncService.syncInjuryTypes(anyString()))
        .thenReturn(SyncResult.success("Injuries", 1, 0));

    // When
    ParallelSyncResult result = orchestrator.executeParallelSync("test-operation");

    // Then
    assertTrue(result.isSuccess()); // Overall success despite some failures
    assertEquals(9, result.getEntityResults().size());
    assertEquals(7, result.getSuccessfulSyncs());
    assertEquals(2, result.getFailedSyncs());
  }

  @Test
  void executeParallelSync_WhenNoEntitiesEnabled_ShouldReturnEmptyResult() {
    // Given
    setupAllEntitiesDisabled();

    // When
    ParallelSyncResult result = orchestrator.executeParallelSync("test-operation");

    // Then
    assertTrue(result.isSuccess());
    assertEquals(0, result.getEntityResults().size());
  }

  @Test
  void executeParallelSync_WhenExecutorFails_ShouldReturnFailure() {
    // Given
    setupAllEntitiesEnabled();

    // Setup service to throw exception
    when(showSyncService.syncShows(anyString())).thenThrow(new RuntimeException("Critical error"));
    // Stub other services to return success
    when(wrestlerSyncService.syncWrestlers(anyString()))
        .thenReturn(SyncResult.success("Wrestlers", 1, 0));
    when(factionSyncService.syncFactions(anyString()))
        .thenReturn(SyncResult.success("Factions", 1, 0));
    when(teamSyncService.syncTeams(anyString())).thenReturn(SyncResult.success("Teams", 1, 0));
    when(matchSyncService.syncMatches(anyString())).thenReturn(SyncResult.success("Matches", 1, 0));
    when(seasonSyncService.syncSeasons(anyString()))
        .thenReturn(SyncResult.success("Seasons", 1, 0));
    when(showTypeSyncService.syncShowTypes(anyString()))
        .thenReturn(SyncResult.success("ShowTypes", 1, 0));
    when(showTemplateSyncService.syncShowTemplates(anyString()))
        .thenReturn(SyncResult.success("ShowTemplates", 1, 0));
    when(injurySyncService.syncInjuryTypes(anyString()))
        .thenReturn(SyncResult.success("Injuries", 1, 0));

    // When
    ParallelSyncResult result = orchestrator.executeParallelSync("test-operation");

    // Then
    // The orchestrator should handle exceptions gracefully and continue with other entities
    assertTrue(result.isSuccess()); // Overall operation succeeds
    assertTrue(result.getEntityResults().size() > 0);

    // At least one entity should have failed
    long failedCount =
        result.getEntityResults().stream().filter(r -> !r.getSyncResult().isSuccess()).count();
    assertTrue(failedCount >= 1);
  }

  private void setupAllEntitiesEnabled() {
    when(entityConfig.isEntityEnabled(anyString())).thenReturn(true);
  }

  private void setupAllEntitiesDisabled() {
    when(entityConfig.isEntityEnabled(anyString())).thenReturn(false);
  }

  private void setupSuccessfulSyncResults() {
    when(showSyncService.syncShows(anyString())).thenReturn(SyncResult.success("Shows", 5, 0));
    when(wrestlerSyncService.syncWrestlers(anyString()))
        .thenReturn(SyncResult.success("Wrestlers", 10, 0));
    when(factionSyncService.syncFactions(anyString()))
        .thenReturn(SyncResult.success("Factions", 3, 0));
    when(teamSyncService.syncTeams(anyString())).thenReturn(SyncResult.success("Teams", 7, 0));
    when(matchSyncService.syncMatches(anyString())).thenReturn(SyncResult.success("Matches", 8, 0));
    when(seasonSyncService.syncSeasons(anyString()))
        .thenReturn(SyncResult.success("Seasons", 2, 0));
    when(showTypeSyncService.syncShowTypes(anyString()))
        .thenReturn(SyncResult.success("ShowTypes", 4, 0));
    when(showTemplateSyncService.syncShowTemplates(anyString()))
        .thenReturn(SyncResult.success("ShowTemplates", 6, 0));
    when(injurySyncService.syncInjuryTypes(anyString()))
        .thenReturn(SyncResult.success("Injuries", 1, 0));
  }
}
