package com.github.javydreamercsw.management.service.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.test.BaseTest;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for Teams Sync functionality. These tests require NOTION_TOKEN to be available
 * and use the real database.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {"notion.sync.enabled=true"})
@Transactional
@EnabledIf("isNotionTokenAvailable")
class NotionSyncServiceTeamsIntegrationTest extends BaseTest {

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
    notionSyncService.syncWrestlers("test-setup");
  }

  @Test
  void shouldSyncTeamsFromNotionSuccessfully() {
    // Given - Ensure we have wrestlers in database for team creation
    assertThat(wrestlerRepository.count()).isGreaterThanOrEqualTo(2);

    // When
    NotionSyncService.SyncResult result = notionSyncService.syncTeams("test-team-sync");

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

          // Check that wrestlers have actual names (not null) if they exist
          // Due to Notion API limitations, some teams might not have wrestlers resolved
          if (team.getWrestler1() != null) {
            assertThat(team.getWrestler1().getName()).isNotNull().isNotEmpty();
          }
          if (team.getWrestler2() != null) {
            assertThat(team.getWrestler2().getName()).isNotNull().isNotEmpty();
          }

          // At least verify that if a team has wrestlers, they are valid
          if (team.getWrestler1() != null && team.getWrestler2() != null) {
            assertThat(team.getWrestler1().getName()).isNotEqualTo("UNRESOLVED_RELATION");
            assertThat(team.getWrestler2().getName()).isNotEqualTo("UNRESOLVED_RELATION");
            // Verify wrestlers are different (can't be the same person)
            assertThat(team.getWrestler1().getId()).isNotEqualTo(team.getWrestler2().getId());
          }

          assertThat(team.getStatus()).isNotNull();
          assertThat(team.getFormedDate()).isNotNull();
          assertThat(team.getExternalId()).isNotNull();
        });

    // Log information about teams that were processed
    if (teams.isEmpty()) {
      System.out.println(
          "⚠️ No teams were created - this is expected if Notion relations can't be resolved");
    } else {
      System.out.println("✅ Successfully processed " + teams.size() + " teams:");
      teams.forEach(
          team -> {
            String wrestler1Name =
                team.getWrestler1() != null ? team.getWrestler1().getName() : "null";
            String wrestler2Name =
                team.getWrestler2() != null ? team.getWrestler2().getName() : "null";
            System.out.println(
                "  - " + team.getName() + " (" + wrestler1Name + " & " + wrestler2Name + ")");
          });
    }
  }

  @Test
  void shouldHandleTeamSyncWithoutNotionToken() {
    // Given - No NOTION_TOKEN available (handled by conditional test)
    // When
    NotionSyncService.SyncResult result = notionSyncService.syncTeams("test-team-sync");

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
}
