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
package com.github.javydreamercsw.management.service.team;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.domain.faction.Faction;
import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * Integration tests for TeamService. Tests the complete service layer with real database
 * interactions.
 */
@DisplayName("TeamService Integration Tests")
class TeamServiceTest extends ManagementIntegrationTest {

  private Wrestler wrestler1;
  private Wrestler wrestler2;
  private Wrestler wrestler3;
  private Faction faction;

  @BeforeEach
  public void setUp() {
    clearAllRepositories();
    wrestler1 = wrestlerRepository.save(createTestWrestler("Wrestler 1"));
    wrestler2 = wrestlerRepository.save(createTestWrestler("Wrestler 2"));
    wrestler3 = wrestlerRepository.save(createTestWrestler("Wrestler 3"));

    faction = Faction.builder().build();
    faction.setName("Test Faction");
    faction = factionRepository.save(faction);
  }

  @Test
  @DisplayName("Should create team successfully with real database")
  void shouldCreateTeamSuccessfully() {
    // When
    Optional<Team> result =
        teamService.createTeam(
            "The Cenation",
            "Test team description",
            wrestler1.getId(),
            wrestler2.getId(),
            faction.getId());

    // Then
    assertThat(result).isPresent();
    Team savedTeam = result.get();
    assertThat(savedTeam.getName()).isEqualTo("The Cenation");
    assertThat(savedTeam.getDescription()).isEqualTo("Test team description");
    assertThat(savedTeam.getWrestler1()).isEqualTo(wrestler1);
    assertThat(savedTeam.getWrestler2()).isEqualTo(wrestler2);
    assertThat(savedTeam.getStatus()).isEqualTo(TeamStatus.ACTIVE);
    assertThat(savedTeam.getFormedDate()).isNotNull();

    // Verify it's persisted
    Assertions.assertNotNull(savedTeam.getId());
    Optional<Team> fromDb = teamRepository.findById(savedTeam.getId());
    assertThat(fromDb).isPresent();
    assertThat(fromDb.get().getName()).isEqualTo("The Cenation");
  }

  @Test
  @DisplayName("Should not create duplicate team names")
  void shouldNotCreateDuplicateTeamNames() {
    // Given - create first team
    teamService.createTeam(
        "Duplicate Name", "Description 1", wrestler1.getId(), wrestler2.getId(), null);

    // When - try to create team with same name
    Optional<Team> result =
        teamService.createTeam(
            "Duplicate Name", "Description 2", wrestler1.getId(), wrestler3.getId(), null);

    // Then
    assertThat(result).isEmpty();

    // Verify only one team exists
    List<Team> teams = teamRepository.findAll();
    assertThat(teams).hasSize(1);
    assertThat(teams.get(0).getDescription()).isEqualTo("Description 1");
  }

  @Test
  @DisplayName("Should not create team with same wrestlers twice")
  void shouldNotCreateTeamWithSameWrestlersTwice() {
    // Given - create first team
    teamService.createTeam("Team 1", "Description 1", wrestler1.getId(), wrestler2.getId(), null);

    // When - try to create team with same wrestlers (different order)
    Optional<Team> result =
        teamService.createTeam(
            "Team 2", "Description 2", wrestler2.getId(), wrestler1.getId(), null);

    // Then
    assertThat(result).isEmpty();

    // Verify only one team exists
    List<Team> teams = teamRepository.findAll();
    assertThat(teams).hasSize(1);
    assertThat(teams.get(0).getName()).isEqualTo("Team 1");
  }

  @Test
  @DisplayName("Should update team successfully")
  void shouldUpdateTeamSuccessfully() {
    // Given - create team
    Optional<Team> created =
        teamService.createTeam(
            "Original Name", "Original Description", wrestler1.getId(), wrestler2.getId(), null);
    assertThat(created).isPresent();
    Long teamId = created.get().getId();

    // When - update team
    Optional<Team> result =
        teamService.updateTeam(
            teamId, "Updated Name", "Updated Description", TeamStatus.ACTIVE, null);

    // Then
    assertThat(result).isPresent();
    Team updatedTeam = result.get();
    assertThat(updatedTeam.getName()).isEqualTo("Updated Name");
    assertThat(updatedTeam.getDescription()).isEqualTo("Updated Description");

    // Verify it's persisted
    Assertions.assertNotNull(teamId);
    Optional<Team> fromDb = teamRepository.findById(teamId);
    assertThat(fromDb).isPresent();
    assertThat(fromDb.get().getName()).isEqualTo("Updated Name");
    assertThat(fromDb.get().getDescription()).isEqualTo("Updated Description");
  }

  @Test
  @DisplayName("Should disband and reactivate team")
  void shouldDisbandAndReactivateTeam() {
    // Given - create team
    Optional<Team> created =
        teamService.createTeam(
            "Test Team", "Description", wrestler1.getId(), wrestler2.getId(), null);
    assertThat(created).isPresent();
    Long teamId = created.get().getId();

    // When - disband team
    Optional<Team> disbanded = teamService.disbandTeam(teamId);

    // Then - verify disbanded
    assertThat(disbanded).isPresent();
    assertThat(disbanded.get().getStatus()).isEqualTo(TeamStatus.DISBANDED);
    assertThat(disbanded.get().getDisbandedDate()).isNotNull();

    // When - reactivate team
    Optional<Team> reactivated = teamService.reactivateTeam(teamId);

    // Then - verify reactivated
    assertThat(reactivated).isPresent();
    assertThat(reactivated.get().getStatus()).isEqualTo(TeamStatus.ACTIVE);
    assertThat(reactivated.get().getDisbandedDate()).isNull();
  }

  @Test
  @DisplayName("Should delete team successfully")
  void shouldDeleteTeamSuccessfully() {
    // Given - create team
    Optional<Team> created =
        teamService.createTeam(
            "Team to Delete", "Description", wrestler1.getId(), wrestler2.getId(), null);
    assertThat(created).isPresent();
    Long teamId = created.get().getId();

    // When - delete team
    boolean result = teamService.deleteTeam(teamId);

    // Then
    assertThat(result).isTrue();

    // Verify it's deleted
    Assertions.assertNotNull(teamId);
    Optional<Team> fromDb = teamRepository.findById(teamId);
    assertThat(fromDb).isEmpty();
  }

  @Test
  @DisplayName("Should get teams with pagination")
  void shouldGetTeamsWithPagination() {
    // Given - create multiple teams
    teamService.createTeam("Team 1", "Description 1", wrestler1.getId(), wrestler2.getId(), null);
    teamService.createTeam("Team 2", "Description 2", wrestler1.getId(), wrestler3.getId(), null);
    teamService.createTeam("Team 3", "Description 3", wrestler2.getId(), wrestler3.getId(), null);

    // When - get first page
    Page<Team> page = teamService.getAllTeams(PageRequest.of(0, 2));

    // Then
    assertThat(page.getContent()).hasSize(2);
    assertThat(page.getTotalElements()).isEqualTo(3);
    assertThat(page.getTotalPages()).isEqualTo(2);
  }

  @Test
  @DisplayName("Should find teams by wrestler")
  void shouldFindTeamsByWrestler() {
    // Given - create teams
    teamService.createTeam("Team 1", "Description 1", wrestler1.getId(), wrestler2.getId(), null);
    teamService.createTeam("Team 2", "Description 2", wrestler1.getId(), wrestler3.getId(), null);
    teamService.createTeam("Team 3", "Description 3", wrestler2.getId(), wrestler3.getId(), null);

    // When - find teams for wrestler1
    List<Team> wrestler1Teams = teamService.getTeamsByWrestler(wrestler1);

    // Then
    assertThat(wrestler1Teams).hasSize(2);
    assertThat(wrestler1Teams)
        .allMatch(
            team -> {
              Assertions.assertNotNull(team.getWrestler1().getId());
              if (team.getWrestler1().getId().equals(wrestler1.getId())) return true;
              Assertions.assertNotNull(team.getWrestler2().getId());
              return team.getWrestler2().getId().equals(wrestler1.getId());
            });
  }

  @Test
  @DisplayName("Should find active teams only")
  void shouldFindActiveTeamsOnly() {
    // Given - create teams with different statuses
    Optional<Team> team1 =
        teamService.createTeam(
            "Active Team", "Description", wrestler1.getId(), wrestler2.getId(), null);
    Optional<Team> team2 =
        teamService.createTeam(
            "Team to Disband", "Description", wrestler1.getId(), wrestler3.getId(), null);

    assertThat(team1).isPresent();
    assertThat(team2).isPresent();

    // Disband one team
    teamService.disbandTeam(team2.get().getId());

    // When - get active teams
    List<Team> activeTeams = teamService.getActiveTeams();

    // Then
    assertThat(activeTeams).hasSize(1);
    assertThat(activeTeams.get(0).getName()).isEqualTo("Active Team");
    assertThat(activeTeams.get(0).getStatus()).isEqualTo(TeamStatus.ACTIVE);
  }

  @Test
  @DisplayName("Should count active teams correctly")
  void shouldCountActiveTeamsCorrectly() {
    // Given - create teams
    teamService.createTeam("Team 1", "Description", wrestler1.getId(), wrestler2.getId(), null);
    Optional<Team> team2 =
        teamService.createTeam("Team 2", "Description", wrestler1.getId(), wrestler3.getId(), null);
    teamService.createTeam("Team 3", "Description", wrestler2.getId(), wrestler3.getId(), null);

    // Disband one team
    assertThat(team2).isPresent();
    teamService.disbandTeam(team2.get().getId());

    // When - count active teams
    long activeCount = teamService.countActiveTeams();

    // Then
    assertThat(activeCount).isEqualTo(2);
  }

  @Test
  @DisplayName("Should find team by both wrestlers regardless of order")
  void shouldFindTeamByBothWrestlersRegardlessOfOrder() {
    // Given - create team
    Optional<Team> created =
        teamService.createTeam(
            "Test Team", "Description", wrestler1.getId(), wrestler2.getId(), null);
    assertThat(created).isPresent();

    // When - find by wrestlers in different order
    Optional<Team> found1 = teamService.findTeamByWrestlers(wrestler1, wrestler2);
    Optional<Team> found2 = teamService.findTeamByWrestlers(wrestler2, wrestler1);

    // Then
    assertThat(found1).isPresent();
    assertThat(found2).isPresent();
    assertThat(found1.get()).isEqualTo(found2.get());
    assertThat(found1.get().getName()).isEqualTo("Test Team");
  }
}
