package com.github.javydreamercsw.management.service.sync.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.TeamPage;
import com.github.javydreamercsw.base.test.BaseTest;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamSyncServiceTest extends BaseTest {

  @Mock private ObjectMapper objectMapper;
  @Mock private NotionHandler notionHandler;
  @Mock private NotionSyncProperties syncProperties;
  @Mock private SyncProgressTracker progressTracker;
  @Mock private SyncHealthMonitor healthMonitor;
  @Mock private WrestlerService wrestlerService;
  @Mock private TeamService teamService;
  @Mock private TeamRepository teamRepository;

  private TeamSyncService teamSyncService;

  @BeforeEach
  void setUp() {
    teamSyncService = new TeamSyncService(objectMapper, syncProperties);

    // Manually inject the mocked dependencies using reflection
    setField(teamSyncService, "notionHandler", notionHandler);
    setField(teamSyncService, "progressTracker", progressTracker);
    setField(teamSyncService, "healthMonitor", healthMonitor);
    setField(teamSyncService, "wrestlerService", wrestlerService);
    setField(teamSyncService, "teamService", teamService);
    setField(teamSyncService, "teamRepository", teamRepository);
  }

  @Test
  void shouldSyncTeamsSuccessfully() {
    // Given
    List<TeamPage> teamPages = createMockTeamPages();
    SyncProgressTracker.SyncProgress mockProgress = createMockProgress();

    when(progressTracker.startOperation(anyString(), anyString(), anyInt()))
        .thenReturn(mockProgress);
    when(notionHandler.loadAllTeams()).thenReturn(teamPages);

    // Mock wrestlers
    Wrestler wrestler1 = createMockWrestler("John Doe");
    Wrestler wrestler2 = createMockWrestler("Jane Smith");
    when(wrestlerService.findByName("John Doe")).thenReturn(Optional.of(wrestler1));
    when(wrestlerService.findByName("Jane Smith")).thenReturn(Optional.of(wrestler2));

    // Mock team service
    when(teamService.getTeamByExternalId(anyString())).thenReturn(Optional.empty());
    when(teamService.getTeamByName(anyString())).thenReturn(Optional.empty());
    when(teamService.createTeam(anyString(), anyString(), anyLong(), anyLong(), any()))
        .thenReturn(Optional.of(createMockTeam()));

    // When
    BaseSyncService.SyncResult result = teamSyncService.syncTeams("test-team-sync");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntityType()).isEqualTo("Teams");
    assertThat(result.getSyncedCount()).isEqualTo(1);
    assertThat(result.getErrorCount()).isEqualTo(0);

    // Verify interactions
    verify(notionHandler).loadAllTeams();
    verify(progressTracker).startOperation("Teams Sync", "Synchronizing teams from Notion", 0);
    verify(progressTracker).completeOperation(anyString(), eq(true), anyString(), eq(1));
    verify(healthMonitor).recordSuccess(eq("Teams"), anyLong(), eq(1));
    verify(teamService).createTeam(anyString(), anyString(), anyLong(), anyLong(), any());
  }

  @Test
  void shouldHandleEmptyTeamsList() {
    // Given
    SyncProgressTracker.SyncProgress mockProgress = createMockProgress();
    when(progressTracker.startOperation(anyString(), anyString(), anyInt()))
        .thenReturn(mockProgress);
    when(notionHandler.loadAllTeams()).thenReturn(new ArrayList<>());

    // When
    BaseSyncService.SyncResult result = teamSyncService.syncTeams("test-team-sync");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntityType()).isEqualTo("Teams");
    assertThat(result.getSyncedCount()).isEqualTo(0);
    assertThat(result.getErrorCount()).isEqualTo(0);

    verify(progressTracker).completeOperation(anyString(), eq(true), eq("No teams to sync"), eq(0));
    verify(healthMonitor).recordSuccess(eq("Teams"), anyLong(), eq(0));
  }

  @Test
  void shouldHandleSyncFailure() {
    // Given
    SyncProgressTracker.SyncProgress mockProgress = createMockProgress();
    when(progressTracker.startOperation(anyString(), anyString(), anyInt()))
        .thenReturn(mockProgress);
    when(notionHandler.loadAllTeams()).thenThrow(new RuntimeException("Connection failed"));

    // When
    BaseSyncService.SyncResult result = teamSyncService.syncTeams("test-team-sync");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getEntityType()).isEqualTo("Teams");
    assertThat(result.getErrorMessage()).contains("Connection failed");

    verify(progressTracker).failOperation(anyString(), anyString());
    verify(healthMonitor).recordFailure(eq("Teams"), anyString());
  }

  @Test
  void shouldSkipTeamWithMissingWrestlers() {
    // Given
    List<TeamPage> teamPages = createMockTeamPages();
    SyncProgressTracker.SyncProgress mockProgress = createMockProgress();

    when(progressTracker.startOperation(anyString(), anyString(), anyInt()))
        .thenReturn(mockProgress);
    when(notionHandler.loadAllTeams()).thenReturn(teamPages);

    // Mock missing wrestlers (lenient to avoid unnecessary stubbing warnings)
    lenient().when(wrestlerService.findByName("John Doe")).thenReturn(Optional.empty());
    lenient().when(wrestlerService.findByName("Jane Smith")).thenReturn(Optional.empty());

    // When
    BaseSyncService.SyncResult result = teamSyncService.syncTeams("test-team-sync");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(0); // No teams saved due to missing wrestlers

    verify(teamService, never()).createTeam(anyString(), anyString(), anyLong(), anyLong(), any());
    verify(progressTracker).completeOperation(anyString(), eq(true), anyString(), eq(0));
  }

  @Test
  void shouldUpdateExistingTeam() {
    // Given
    List<TeamPage> teamPages = createMockTeamPages();
    SyncProgressTracker.SyncProgress mockProgress = createMockProgress();
    Team existingTeam = createMockTeam();

    when(progressTracker.startOperation(anyString(), anyString(), anyInt()))
        .thenReturn(mockProgress);
    when(notionHandler.loadAllTeams()).thenReturn(teamPages);
    when(teamService.getTeamByExternalId(anyString())).thenReturn(Optional.of(existingTeam));

    // Mock wrestlers
    Wrestler wrestler1 = createMockWrestler("John Doe");
    Wrestler wrestler2 = createMockWrestler("Jane Smith");
    when(wrestlerService.findByName("John Doe")).thenReturn(Optional.of(wrestler1));
    when(wrestlerService.findByName("Jane Smith")).thenReturn(Optional.of(wrestler2));

    // When
    BaseSyncService.SyncResult result = teamSyncService.syncTeams("test-team-sync");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getSyncedCount()).isEqualTo(1);

    verify(teamRepository).saveAndFlush(existingTeam);
    verify(teamService, never()).createTeam(anyString(), anyString(), anyLong(), anyLong(), any());
  }

  private List<TeamPage> createMockTeamPages() {
    TeamPage teamPage = new TeamPage();
    teamPage.setId("team-123");

    // Create mock raw properties following the pattern from other tests
    Map<String, Object> rawProperties = new HashMap<>();
    rawProperties.put("Name", "Test Team");
    rawProperties.put("Member 1", "John Doe");
    rawProperties.put("Member 2", "Jane Smith");
    rawProperties.put("Status", "Active");
    rawProperties.put("FormedDate", "2024-01-01T00:00:00Z");

    teamPage.setRawProperties(rawProperties);

    List<TeamPage> pages = new ArrayList<>();
    pages.add(teamPage);
    return pages;
  }

  private SyncProgressTracker.SyncProgress createMockProgress() {
    SyncProgressTracker.SyncProgress progress = mock(SyncProgressTracker.SyncProgress.class);
    when(progress.getOperationId()).thenReturn("test-operation-id");
    return progress;
  }

  private Wrestler createMockWrestler(String name) {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(name.equals("John Doe") ? 1L : 2L); // Different IDs for different wrestlers
    wrestler.setName(name);
    return wrestler;
  }

  private Team createMockTeam() {
    Team team = new Team();
    team.setId(1L);
    team.setName("Test Team");
    team.setStatus(TeamStatus.ACTIVE);
    team.setFormedDate(Instant.now());
    return team;
  }
}
