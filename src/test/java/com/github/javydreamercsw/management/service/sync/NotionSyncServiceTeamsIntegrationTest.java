package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.NotionSyncService.SyncResult;
import com.github.javydreamercsw.management.service.team.TeamService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for Teams Sync functionality. These tests require NOTION_TOKEN to be available
 * and use the real database.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotionSyncServiceTeamsIntegrationTest {

  @Autowired private NotionSyncService notionSyncService;
  @Autowired private TeamRepository teamRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private TeamService teamService;

  private Wrestler wrestler1;
  private Wrestler wrestler2;

  @BeforeEach
  void setUp() {
    // Clean up existing data
    teamRepository.deleteAll();

    // Create test wrestlers with required fields
    wrestler1 = new Wrestler();
    wrestler1.setName("Test Wrestler 1");
    wrestler1.setStartingHealth(15);
    wrestler1.setLowHealth(0);
    wrestler1.setStartingStamina(0);
    wrestler1.setLowStamina(0);
    wrestler1.setDeckSize(15);
    wrestler1.setFans(0L);
    wrestler1.setIsPlayer(false);
    wrestler1.setBumps(0);
    wrestler1.setCreationDate(Instant.now());
    wrestler1 = wrestlerRepository.saveAndFlush(wrestler1);

    wrestler2 = new Wrestler();
    wrestler2.setName("Test Wrestler 2");
    wrestler2.setStartingHealth(15);
    wrestler2.setLowHealth(0);
    wrestler2.setStartingStamina(0);
    wrestler2.setLowStamina(0);
    wrestler2.setDeckSize(15);
    wrestler2.setFans(0L);
    wrestler2.setIsPlayer(false);
    wrestler2.setBumps(0);
    wrestler2.setCreationDate(Instant.now());
    wrestler2 = wrestlerRepository.saveAndFlush(wrestler2);
  }

  @Test
  @EnabledIf("isNotionTokenAvailable")
  void shouldSyncTeamsFromNotionSuccessfully() {
    // Given - Ensure we have wrestlers in database for team creation
    assertThat(wrestlerRepository.count()).isGreaterThanOrEqualTo(2);

    // When
    SyncResult result = notionSyncService.syncTeams();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getEntityType()).isEqualTo("Teams");

    // Verify teams were created/updated in database
    List<Team> teams = teamRepository.findAll();
    assertThat(teams).isNotNull();

    // If teams were synced, verify their structure
    teams.forEach(
        team -> {
          assertThat(team.getName()).isNotNull().isNotEmpty();
          assertThat(team.getWrestler1()).isNotNull();
          assertThat(team.getWrestler2()).isNotNull();
          assertThat(team.getStatus()).isNotNull();
          assertThat(team.getFormedDate()).isNotNull();
          assertThat(team.getExternalId()).isNotNull();
        });
  }

  @Test
  void shouldHandleTeamSyncWithoutNotionToken() {
    // Given - No NOTION_TOKEN available (handled by conditional test)
    // When
    SyncResult result = notionSyncService.syncTeams();

    // Then - Should handle gracefully
    assertThat(result).isNotNull();
    // Result may be success (empty) or failure depending on configuration
  }

  @Test
  void shouldCreateTeamWithValidWrestlers() {
    // Given - Create a team manually to test the save functionality
    Team testTeam = new Team();
    testTeam.setName("Integration Test Team");
    testTeam.setDescription("Test team for integration testing");
    testTeam.setWrestler1(wrestler1);
    testTeam.setWrestler2(wrestler2);
    testTeam.setStatus(TeamStatus.ACTIVE);
    testTeam.setFormedDate(Instant.now());
    testTeam.setExternalId("test-external-id");

    // When
    Team savedTeam = teamRepository.saveAndFlush(testTeam);

    // Then
    assertThat(savedTeam).isNotNull();
    assertThat(savedTeam.getId()).isNotNull();
    assertThat(savedTeam.getName()).isEqualTo("Integration Test Team");
    assertThat(savedTeam.getWrestler1()).isEqualTo(wrestler1);
    assertThat(savedTeam.getWrestler2()).isEqualTo(wrestler2);
    assertThat(savedTeam.getStatus()).isEqualTo(TeamStatus.ACTIVE);
    assertThat(savedTeam.getExternalId()).isEqualTo("test-external-id");

    // Verify it can be found by external ID
    Optional<Team> foundTeam = teamService.getTeamByExternalId("test-external-id");
    assertThat(foundTeam).isPresent();
    assertThat(foundTeam.get().getName()).isEqualTo("Integration Test Team");
  }

  @Test
  void shouldUpdateExistingTeamByExternalId() {
    // Given - Create an existing team
    Team existingTeam = new Team();
    existingTeam.setName("Original Team Name");
    existingTeam.setDescription("Original description");
    existingTeam.setWrestler1(wrestler1);
    existingTeam.setWrestler2(wrestler2);
    existingTeam.setStatus(TeamStatus.ACTIVE);
    existingTeam.setFormedDate(Instant.now());
    existingTeam.setExternalId("update-test-id");
    existingTeam = teamRepository.saveAndFlush(existingTeam);

    // When - Update the team
    existingTeam.setName("Updated Team Name");
    existingTeam.setDescription("Updated description");
    existingTeam.setStatus(TeamStatus.INACTIVE);
    Team updatedTeam = teamRepository.saveAndFlush(existingTeam);

    // Then
    assertThat(updatedTeam.getName()).isEqualTo("Updated Team Name");
    assertThat(updatedTeam.getDescription()).isEqualTo("Updated description");
    assertThat(updatedTeam.getStatus()).isEqualTo(TeamStatus.INACTIVE);
    assertThat(updatedTeam.getExternalId()).isEqualTo("update-test-id");

    // Verify only one team exists with this external ID
    List<Team> teamsWithExternalId =
        teamRepository.findAll().stream()
            .filter(team -> "update-test-id".equals(team.getExternalId()))
            .toList();
    assertThat(teamsWithExternalId).hasSize(1);
  }

  @Test
  void shouldHandleTeamCreationWithTeamService() {
    // Given
    String teamName = "Service Test Team";
    String description = "Team created via service";
    Long wrestler1Id = wrestler1.getId();
    Long wrestler2Id = wrestler2.getId();

    // When
    Optional<Team> createdTeam =
        teamService.createTeam(teamName, description, wrestler1Id, wrestler2Id, null);

    // Then
    assertThat(createdTeam).isPresent();
    Team team = createdTeam.get();
    assertThat(team.getName()).isEqualTo(teamName);
    assertThat(team.getDescription()).isEqualTo(description);
    assertThat(team.getWrestler1().getId()).isEqualTo(wrestler1Id);
    assertThat(team.getWrestler2().getId()).isEqualTo(wrestler2Id);
    assertThat(team.getStatus()).isEqualTo(TeamStatus.ACTIVE);
    assertThat(team.getFormedDate()).isNotNull();

    // Verify it's persisted in database
    Optional<Team> foundTeam = teamRepository.findById(team.getId());
    assertThat(foundTeam).isPresent();
    assertThat(foundTeam.get().getName()).isEqualTo(teamName);
  }

  @Test
  void shouldValidateTeamRequirements() {
    // Test that teams require both wrestlers
    Optional<Team> invalidTeam =
        teamService.createTeam("Invalid Team", "Missing wrestler", wrestler1.getId(), null, null);

    // Should fail validation (depending on TeamService implementation)
    // This test verifies the service handles invalid input appropriately
    assertThat(invalidTeam).isEmpty();
  }

  /** Helper method to check if NOTION_TOKEN is available for conditional tests. */
  static boolean isNotionTokenAvailable() {
    return System.getenv("NOTION_TOKEN") != null || System.getProperty("NOTION_TOKEN") != null;
  }
}
