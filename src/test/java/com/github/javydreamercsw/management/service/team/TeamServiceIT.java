package com.github.javydreamercsw.management.service.team;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.TestcontainersConfiguration;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for TeamService. Tests the complete service layer with real database
 * interactions.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("TeamService Integration Tests")
class TeamServiceIT {

  @Autowired private TeamService teamService;
  @Autowired private TeamRepository teamRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private FactionRepository factionRepository;

  private Wrestler wrestler1;
  private Wrestler wrestler2;
  private Wrestler wrestler3;
  private Faction faction;

  @BeforeEach
  void setUp() {
    // Create test wrestlers with all required fields
    wrestler1 = new Wrestler();
    wrestler1.setName("John Cena");
    wrestler1.setTier(WrestlerTier.MAIN_EVENTER);
    wrestler1.setStartingStamina(100);
    wrestler1.setLowStamina(25);
    wrestler1.setStartingHealth(100);
    wrestler1.setLowHealth(25);
    wrestler1.setDeckSize(40);
    wrestler1.setCreationDate(Instant.now());
    wrestler1 = wrestlerRepository.saveAndFlush(wrestler1);

    wrestler2 = new Wrestler();
    wrestler2.setName("The Rock");
    wrestler2.setTier(WrestlerTier.MAIN_EVENTER);
    wrestler2.setStartingStamina(100);
    wrestler2.setLowStamina(25);
    wrestler2.setStartingHealth(100);
    wrestler2.setLowHealth(25);
    wrestler2.setDeckSize(40);
    wrestler2.setCreationDate(Instant.now());
    wrestler2 = wrestlerRepository.saveAndFlush(wrestler2);

    wrestler3 = new Wrestler();
    wrestler3.setName("Stone Cold");
    wrestler3.setTier(WrestlerTier.MAIN_EVENTER);
    wrestler3.setStartingStamina(100);
    wrestler3.setLowStamina(25);
    wrestler3.setStartingHealth(100);
    wrestler3.setLowHealth(25);
    wrestler3.setDeckSize(40);
    wrestler3.setCreationDate(Instant.now());
    wrestler3 = wrestlerRepository.saveAndFlush(wrestler3);

    // Create test faction
    faction = new Faction();
    faction.setName("Test Faction");
    faction.setDescription("Test faction description");
    faction.setLeader(wrestler1);
    faction = factionRepository.saveAndFlush(faction);
  }

  @AfterEach
  void tearDown() {
    teamRepository.deleteAll();
    factionRepository.deleteAll();
    wrestlerRepository.deleteAll();
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
            team ->
                team.getWrestler1().getId().equals(wrestler1.getId())
                    || team.getWrestler2().getId().equals(wrestler1.getId()));
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
