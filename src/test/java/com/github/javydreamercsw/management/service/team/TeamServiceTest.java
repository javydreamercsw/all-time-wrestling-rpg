package com.github.javydreamercsw.management.service.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Unit tests for TeamService. Tests the service layer methods for team management functionality.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TeamService Unit Tests")
class TeamServiceTest {

  @Mock private TeamRepository teamRepository;
  @Mock private WrestlerRepository wrestlerRepository;

  @InjectMocks private TeamService teamService;
  private Wrestler wrestler1;
  private Wrestler wrestler2;
  private Team team;

  @BeforeEach
  void setUp() {

    // Create test wrestlers
    wrestler1 = new Wrestler();
    wrestler1.setId(1L);
    wrestler1.setName("John Cena");
    wrestler1.setTier(WrestlerTier.MAIN_EVENTER);

    wrestler2 = new Wrestler();
    wrestler2.setId(2L);
    wrestler2.setName("The Rock");
    wrestler2.setTier(WrestlerTier.MAIN_EVENTER);

    // Create test team
    team = new Team();
    team.setId(1L);
    team.setName("The Cenation");
    team.setDescription("Test team description");
    team.setWrestler1(wrestler1);
    team.setWrestler2(wrestler2);
    team.setStatus(TeamStatus.ACTIVE);
    team.setFormedDate(Instant.now());
  }

  @Test
  @DisplayName("Should get all teams with pagination")
  void shouldGetAllTeamsWithPagination() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);
    Page<Team> expectedPage = new PageImpl<>(Arrays.asList(team));
    when(teamRepository.findAll(pageable)).thenReturn(expectedPage);

    // When
    Page<Team> result = teamService.getAllTeams(pageable);

    // Then
    assertThat(result).isEqualTo(expectedPage);
    verify(teamRepository).findAll(pageable);
  }

  @Test
  @DisplayName("Should get team by ID")
  void shouldGetTeamById() {
    // Given
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

    // When
    Optional<Team> result = teamService.getTeamById(1L);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(team);
    verify(teamRepository).findById(1L);
  }

  @Test
  @DisplayName("Should get team by name")
  void shouldGetTeamByName() {
    // Given
    when(teamRepository.findByName("The Cenation")).thenReturn(Optional.of(team));

    // When
    Optional<Team> result = teamService.getTeamByName("The Cenation");

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(team);
    verify(teamRepository).findByName("The Cenation");
  }

  @Test
  @DisplayName("Should create team successfully")
  void shouldCreateTeamSuccessfully() {
    // Given
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler1));
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(wrestler2));
    when(teamRepository.existsByName("New Team")).thenReturn(false);
    when(teamRepository.findActiveTeamByBothWrestlers(wrestler1, wrestler2))
        .thenReturn(Optional.empty());
    when(teamRepository.saveAndFlush(any(Team.class))).thenReturn(team);

    // When
    Optional<Team> result = teamService.createTeam("New Team", "Description", 1L, 2L, null);

    // Then
    assertThat(result).isPresent();
    verify(teamRepository).saveAndFlush(any(Team.class));
  }

  @Test
  @DisplayName("Should not create team when wrestler not found")
  void shouldNotCreateTeamWhenWrestlerNotFound() {
    // Given
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.empty());
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(wrestler2));

    // When
    Optional<Team> result = teamService.createTeam("New Team", "Description", 1L, 2L, null);

    // Then
    assertThat(result).isEmpty();
    verify(teamRepository, never()).saveAndFlush(any(Team.class));
  }

  @Test
  @DisplayName("Should not create team when wrestlers are the same")
  void shouldNotCreateTeamWhenWrestlersAreSame() {
    // Given
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler1));

    // When
    Optional<Team> result = teamService.createTeam("New Team", "Description", 1L, 1L, null);

    // Then
    assertThat(result).isEmpty();
    verify(teamRepository, never()).saveAndFlush(any(Team.class));
  }

  @Test
  @DisplayName("Should not create team when name already exists")
  void shouldNotCreateTeamWhenNameAlreadyExists() {
    // Given
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler1));
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(wrestler2));
    when(teamRepository.existsByName("Existing Team")).thenReturn(true);

    // When
    Optional<Team> result = teamService.createTeam("Existing Team", "Description", 1L, 2L, null);

    // Then
    assertThat(result).isEmpty();
    verify(teamRepository, never()).saveAndFlush(any(Team.class));
  }

  @Test
  @DisplayName("Should not create team when wrestlers already have active team")
  void shouldNotCreateTeamWhenWrestlersAlreadyHaveActiveTeam() {
    // Given
    when(wrestlerRepository.findById(1L)).thenReturn(Optional.of(wrestler1));
    when(wrestlerRepository.findById(2L)).thenReturn(Optional.of(wrestler2));
    when(teamRepository.existsByName("New Team")).thenReturn(false);
    when(teamRepository.findActiveTeamByBothWrestlers(wrestler1, wrestler2))
        .thenReturn(Optional.of(team));

    // When
    Optional<Team> result = teamService.createTeam("New Team", "Description", 1L, 2L, null);

    // Then
    assertThat(result).isEmpty();
    verify(teamRepository, never()).saveAndFlush(any(Team.class));
  }

  @Test
  @DisplayName("Should update team successfully")
  void shouldUpdateTeamSuccessfully() {
    // Given
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(teamRepository.existsByName("Updated Name")).thenReturn(false);
    when(teamRepository.saveAndFlush(any(Team.class))).thenReturn(team);

    // When
    Optional<Team> result =
        teamService.updateTeam(1L, "Updated Name", "Updated Description", TeamStatus.ACTIVE, null);

    // Then
    assertThat(result).isPresent();
    verify(teamRepository).saveAndFlush(any(Team.class));
  }

  @Test
  @DisplayName("Should not update team when not found")
  void shouldNotUpdateTeamWhenNotFound() {
    // Given
    when(teamRepository.findById(1L)).thenReturn(Optional.empty());

    // When
    Optional<Team> result =
        teamService.updateTeam(1L, "Updated Name", "Updated Description", TeamStatus.ACTIVE, null);

    // Then
    assertThat(result).isEmpty();
    verify(teamRepository, never()).saveAndFlush(any(Team.class));
  }

  @Test
  @DisplayName("Should not update team when new name conflicts")
  void shouldNotUpdateTeamWhenNewNameConflicts() {
    // Given
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(teamRepository.existsByName("Conflicting Name")).thenReturn(true);

    // When
    Optional<Team> result =
        teamService.updateTeam(
            1L, "Conflicting Name", "Updated Description", TeamStatus.ACTIVE, null);

    // Then
    assertThat(result).isEmpty();
    verify(teamRepository, never()).saveAndFlush(any(Team.class));
  }

  @Test
  @DisplayName("Should delete team successfully")
  void shouldDeleteTeamSuccessfully() {
    // Given
    when(teamRepository.existsById(1L)).thenReturn(true);

    // When
    boolean result = teamService.deleteTeam(1L);

    // Then
    assertThat(result).isTrue();
    verify(teamRepository).deleteById(1L);
  }

  @Test
  @DisplayName("Should not delete team when not found")
  void shouldNotDeleteTeamWhenNotFound() {
    // Given
    when(teamRepository.existsById(1L)).thenReturn(false);

    // When
    boolean result = teamService.deleteTeam(1L);

    // Then
    assertThat(result).isFalse();
    verify(teamRepository, never()).deleteById(anyLong());
  }

  @Test
  @DisplayName("Should get active teams")
  void shouldGetActiveTeams() {
    // Given
    List<Team> activeTeams = Arrays.asList(team);
    when(teamRepository.findByStatus(TeamStatus.ACTIVE)).thenReturn(activeTeams);

    // When
    List<Team> result = teamService.getActiveTeams();

    // Then
    assertThat(result).isEqualTo(activeTeams);
    verify(teamRepository).findByStatus(TeamStatus.ACTIVE);
  }

  @Test
  @DisplayName("Should get teams by wrestler")
  void shouldGetTeamsByWrestler() {
    // Given
    List<Team> wrestlerTeams = Arrays.asList(team);
    when(teamRepository.findByWrestler(wrestler1)).thenReturn(wrestlerTeams);

    // When
    List<Team> result = teamService.getTeamsByWrestler(wrestler1);

    // Then
    assertThat(result).isEqualTo(wrestlerTeams);
    verify(teamRepository).findByWrestler(wrestler1);
  }

  @Test
  @DisplayName("Should find active team by wrestlers")
  void shouldFindActiveTeamByWrestlers() {
    // Given
    when(teamRepository.findActiveTeamByBothWrestlers(wrestler1, wrestler2))
        .thenReturn(Optional.of(team));

    // When
    Optional<Team> result = teamService.findActiveTeamByWrestlers(wrestler1, wrestler2);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(team);
    verify(teamRepository).findActiveTeamByBothWrestlers(wrestler1, wrestler2);
  }

  @Test
  @DisplayName("Should count active teams")
  void shouldCountActiveTeams() {
    // Given
    when(teamRepository.countByStatus(TeamStatus.ACTIVE)).thenReturn(5L);

    // When
    long result = teamService.countActiveTeams();

    // Then
    assertThat(result).isEqualTo(5L);
    verify(teamRepository).countByStatus(TeamStatus.ACTIVE);
  }

  @Test
  @DisplayName("Should disband team")
  void shouldDisbandTeam() {
    // Given
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(teamRepository.saveAndFlush(any(Team.class))).thenReturn(team);

    // When
    Optional<Team> result = teamService.disbandTeam(1L);

    // Then
    assertThat(result).isPresent();
    verify(teamRepository).saveAndFlush(any(Team.class));
  }

  @Test
  @DisplayName("Should reactivate team")
  void shouldReactivateTeam() {
    // Given
    team.setStatus(TeamStatus.DISBANDED);
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(teamRepository.saveAndFlush(any(Team.class))).thenReturn(team);

    // When
    Optional<Team> result = teamService.reactivateTeam(1L);

    // Then
    assertThat(result).isPresent();
    verify(teamRepository).saveAndFlush(any(Team.class));
  }
}
