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
package com.github.javydreamercsw.management.service.sync.entity.notion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.base.ai.notion.TeamPage;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.sync.AbstractSyncTest;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

class TeamSyncServiceTest extends AbstractSyncTest {

  @Mock private WrestlerService wrestlerService;
  @Mock private TeamService teamService;

  private TeamSyncService teamSyncService;

  @BeforeEach
  @Override
  public void setUp() {
    super.setUp(); // Call parent setup first
    lenient()
        .when(progressTracker.startOperation(anyString(), anyString(), anyInt()))
        .thenReturn(createMockProgress());

    teamSyncService =
        new TeamSyncService(
            objectMapper, syncServiceDependencies, teamService, wrestlerService, notionApiExecutor);
  }

  @Test
  void shouldSyncTeamsSuccessfully() {
    // Given
    List<TeamPage> teamPages = createMockTeamPages();
    lenient().when(notionHandler.loadAllTeams()).thenReturn(teamPages);
    lenient()
        .doReturn("Test Team")
        .when(notionPageDataExtractor)
        .extractNameFromNotionPage(any(TeamPage.class));

    // Mock wrestlers
    Wrestler wrestler1 = createMockWrestler("John Doe", "wrestler-1");
    Wrestler wrestler2 = createMockWrestler("Jane Smith", "wrestler-2");
    lenient()
        .when(wrestlerService.findByExternalId("wrestler-1"))
        .thenReturn(Optional.of(wrestler1));
    lenient()
        .when(wrestlerService.findByExternalId("wrestler-2"))
        .thenReturn(Optional.of(wrestler2));

    // Mock Manager
    Npc manager = new Npc();
    manager.setId(10L);
    manager.setName("Paul Heyman");
    manager.setExternalId("manager-id");
    lenient().when(npcRepository.findByExternalId("manager-id")).thenReturn(Optional.of(manager));

    // Mock team service
    lenient().when(teamService.getTeamByExternalId(anyString())).thenReturn(Optional.empty());
    lenient().when(teamService.getTeamByName(anyString())).thenReturn(Optional.empty());

    Team mockTeam = createMockTeam();
    lenient()
        .when(
            teamService.createTeam(
                anyString(), nullable(String.class), anyLong(), anyLong(), any(), any()))
        .thenReturn(Optional.of(mockTeam));

    // When
    BaseSyncService.SyncResult result = teamSyncService.syncTeams("test-team-sync");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntityType()).isEqualTo("Teams");
    assertThat(result.getSyncedCount()).isEqualTo(1);

    ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
    verify(teamRepository).saveAndFlush(teamCaptor.capture());
    Team savedTeam = teamCaptor.getValue();

    assertThat(savedTeam.getName()).isEqualTo("Test Team");
    assertThat(savedTeam.getThemeSong()).isEqualTo("Test Theme");
    assertThat(savedTeam.getArtist()).isEqualTo("Test Artist");
    assertThat(savedTeam.getTeamFinisher()).isEqualTo("Test Finisher");
    assertThat(savedTeam.getManager()).isEqualTo(manager);
  }

  @Test
  void shouldHandleEmptyTeamsList() {
    // Given
    when(notionHandler.loadAllTeams()).thenReturn(new ArrayList<>());

    // When
    BaseSyncService.SyncResult result = teamSyncService.syncTeams("test-team-sync");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntityType()).isEqualTo("Teams");
    assertThat(result.getSyncedCount()).isEqualTo(0);
  }

  @Test
  void shouldHandleSyncFailure() {
    // Given
    when(notionHandler.loadAllTeams()).thenThrow(new RuntimeException("Connection failed"));

    // When
    BaseSyncService.SyncResult result = teamSyncService.syncTeams("test-team-sync");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getEntityType()).isEqualTo("Teams");
    assertThat(result.getErrorMessage()).contains("Connection failed");

    verify(healthMonitor).recordFailure(eq("Teams"), anyString());
  }

  private List<TeamPage> createMockTeamPages() {
    TeamPage teamPage = new TeamPage();
    teamPage.setId("team-123");

    Map<String, Object> rawProperties = new HashMap<>();
    rawProperties.put("Name", "Test Team");
    rawProperties.put("Member 1", List.of(Map.of("id", "wrestler-1")));
    rawProperties.put("Member 2", List.of(Map.of("id", "wrestler-2")));
    rawProperties.put("Status", true);
    rawProperties.put("Theme Song", "Test Theme");
    rawProperties.put("Artist", "Test Artist");
    rawProperties.put("Team Finisher", "Test Finisher");
    rawProperties.put("Manager", List.of(Map.of("id", "manager-id")));
    rawProperties.put("Description", "This is a test team description.");

    teamPage.setRawProperties(rawProperties);

    List<TeamPage> pages = new ArrayList<>();
    pages.add(teamPage);
    return pages;
  }

  private SyncProgressTracker.SyncProgress createMockProgress() {
    return mock(SyncProgressTracker.SyncProgress.class);
  }

  private Wrestler createMockWrestler(String name, String externalId) {
    Wrestler wrestler = Wrestler.builder().build();
    wrestler.setId(name.equals("John Doe") ? 1L : 2L);
    wrestler.setName(name);
    wrestler.setExternalId(externalId);
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
