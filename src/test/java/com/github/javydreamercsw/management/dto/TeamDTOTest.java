package com.github.javydreamercsw.management.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for TeamDTO. Tests the data transfer object functionality and entity conversion. */
@DisplayName("TeamDTO Tests")
class TeamDTOTest {

  private Team team;
  private Instant formedDate;

  @BeforeEach
  void setUp() {
    formedDate = Instant.now();

    // Create test wrestlers
    Wrestler wrestler1 = Wrestler.builder().build();
    wrestler1.setId(1L);
    wrestler1.setName("John Cena");
    wrestler1.setTier(WrestlerTier.MAIN_EVENTER);

    Wrestler wrestler2 = Wrestler.builder().build();
    wrestler2.setId(2L);
    wrestler2.setName("The Rock");
    wrestler2.setTier(WrestlerTier.MAIN_EVENTER);

    // Create test faction
    Faction faction = Faction.builder().build();
    faction.setId(1L);
    faction.setName("Test Faction");
    faction.setLeader(wrestler1);

    // Create test team
    team = new Team();
    team.setId(1L);
    team.setName("The Cenation");
    team.setDescription("Test team description");
    team.setWrestler1(wrestler1);
    team.setWrestler2(wrestler2);
    team.setFaction(faction);
    team.setStatus(TeamStatus.ACTIVE);
    team.setFormedDate(formedDate);
    team.setExternalId("notion-123");
  }

  @Test
  @DisplayName("Should create DTO from Team entity")
  void shouldCreateDTOFromTeamEntity() {
    // When
    TeamDTO dto = TeamDTO.fromEntity(team);

    // Then
    assertThat(dto.getId()).isEqualTo(1L);
    assertThat(dto.getName()).isEqualTo("The Cenation");
    assertThat(dto.getDescription()).isEqualTo("Test team description");
    assertThat(dto.getWrestler1Id()).isEqualTo(1L);
    assertThat(dto.getWrestler1Name()).isEqualTo("John Cena");
    assertThat(dto.getWrestler2Id()).isEqualTo(2L);
    assertThat(dto.getWrestler2Name()).isEqualTo("The Rock");
    assertThat(dto.getFactionId()).isEqualTo(1L);
    assertThat(dto.getFactionName()).isEqualTo("Test Faction");
    assertThat(dto.getStatus()).isEqualTo(TeamStatus.ACTIVE);
    assertThat(dto.getFormedDate()).isEqualTo(formedDate);
    assertThat(dto.getDisbandedDate()).isNull();
    assertThat(dto.getExternalId()).isEqualTo("notion-123");
    assertThat(dto.getMemberNames()).isEqualTo("John Cena & The Rock");
    assertThat(dto.getDisplayName()).isEqualTo("The Cenation");
    assertThat(dto.isActive()).isTrue();
  }

  @Test
  @DisplayName("Should create DTO from Team entity without faction")
  void shouldCreateDTOFromTeamEntityWithoutFaction() {
    // Given
    team.setFaction(null);

    // When
    TeamDTO dto = TeamDTO.fromEntity(team);

    // Then
    assertThat(dto.getFactionId()).isNull();
    assertThat(dto.getFactionName()).isNull();
    // Other fields should still be populated
    assertThat(dto.getName()).isEqualTo("The Cenation");
    assertThat(dto.getWrestler1Name()).isEqualTo("John Cena");
    assertThat(dto.getWrestler2Name()).isEqualTo("The Rock");
  }

  @Test
  @DisplayName("Should create DTO from disbanded team")
  void shouldCreateDTOFromDisbandedTeam() {
    // Given
    team.disband();

    // When
    TeamDTO dto = TeamDTO.fromEntity(team);

    // Then
    assertThat(dto.getStatus()).isEqualTo(TeamStatus.DISBANDED);
    assertThat(dto.getDisbandedDate()).isNotNull();
    assertThat(dto.isActive()).isFalse();
    assertThat(dto.isDisbanded()).isTrue();
    assertThat(dto.getDisplayName()).isEqualTo("The Cenation (Disbanded)");
  }

  @Test
  @DisplayName("Should create DTO from inactive team")
  void shouldCreateDTOFromInactiveTeam() {
    // Given
    team.setStatus(TeamStatus.INACTIVE);

    // When
    TeamDTO dto = TeamDTO.fromEntity(team);

    // Then
    assertThat(dto.getStatus()).isEqualTo(TeamStatus.INACTIVE);
    assertThat(dto.isActive()).isFalse();
    assertThat(dto.isDisbanded()).isFalse();
    assertThat(dto.isInactive()).isTrue();
  }

  @Test
  @DisplayName("Should get status display name")
  void shouldGetStatusDisplayName() {
    // Given
    TeamDTO dto = TeamDTO.fromEntity(team);

    // When/Then
    assertThat(dto.getStatusDisplayName()).isEqualTo("Active");

    // Given - disbanded team
    team.setStatus(TeamStatus.DISBANDED);
    dto = TeamDTO.fromEntity(team);

    // When/Then
    assertThat(dto.getStatusDisplayName()).isEqualTo("Disbanded");

    // Given - inactive team
    team.setStatus(TeamStatus.INACTIVE);
    dto = TeamDTO.fromEntity(team);

    // When/Then
    assertThat(dto.getStatusDisplayName()).isEqualTo("Inactive");
  }

  @Test
  @DisplayName("Should handle null status gracefully")
  void shouldHandleNullStatusGracefully() {
    // Given
    TeamDTO dto = new TeamDTO();
    dto.setStatus(null);

    // When/Then
    assertThat(dto.getStatusDisplayName()).isEqualTo("Unknown");
    assertThat(dto.isActive()).isFalse();
    assertThat(dto.isDisbanded()).isFalse();
    assertThat(dto.isInactive()).isFalse();
  }

  @Test
  @DisplayName("Should handle team with null wrestlers gracefully")
  void shouldHandleTeamWithNullWrestlersGracefully() {
    // Given
    team.setWrestler1(null);
    team.setWrestler2(null);

    // When
    TeamDTO dto = TeamDTO.fromEntity(team);

    // Then
    assertThat(dto.getWrestler1Id()).isNull();
    assertThat(dto.getWrestler1Name()).isNull();
    assertThat(dto.getWrestler2Id()).isNull();
    assertThat(dto.getWrestler2Name()).isNull();
    // Should not throw exception
    assertThat(dto.getMemberNames()).isEqualTo("null & null");
  }

  @Test
  @DisplayName("Should create DTO with all status checks")
  void shouldCreateDTOWithAllStatusChecks() {
    // Test ACTIVE status
    team.setStatus(TeamStatus.ACTIVE);
    TeamDTO activeDto = TeamDTO.fromEntity(team);
    assertThat(activeDto.isActive()).isTrue();
    assertThat(activeDto.isDisbanded()).isFalse();
    assertThat(activeDto.isInactive()).isFalse();

    // Test DISBANDED status
    team.setStatus(TeamStatus.DISBANDED);
    TeamDTO disbandedDto = TeamDTO.fromEntity(team);
    assertThat(disbandedDto.isActive()).isFalse();
    assertThat(disbandedDto.isDisbanded()).isTrue();
    assertThat(disbandedDto.isInactive()).isFalse();

    // Test INACTIVE status
    team.setStatus(TeamStatus.INACTIVE);
    TeamDTO inactiveDto = TeamDTO.fromEntity(team);
    assertThat(inactiveDto.isActive()).isFalse();
    assertThat(inactiveDto.isDisbanded()).isFalse();
    assertThat(inactiveDto.isInactive()).isTrue();
  }

  @Test
  @DisplayName("Should preserve all entity data in DTO conversion")
  void shouldPreserveAllEntityDataInDTOConversion() {
    // Given - team with all possible data
    Instant disbandedDate = Instant.now().plusSeconds(3600);
    team.setDisbandedDate(disbandedDate);

    // When
    TeamDTO dto = TeamDTO.fromEntity(team);

    // Then - verify all fields are preserved
    assertThat(dto.getId()).isEqualTo(team.getId());
    assertThat(dto.getName()).isEqualTo(team.getName());
    assertThat(dto.getDescription()).isEqualTo(team.getDescription());
    assertThat(dto.getWrestler1Id()).isEqualTo(team.getWrestler1().getId());
    assertThat(dto.getWrestler1Name()).isEqualTo(team.getWrestler1().getName());
    assertThat(dto.getWrestler2Id()).isEqualTo(team.getWrestler2().getId());
    assertThat(dto.getWrestler2Name()).isEqualTo(team.getWrestler2().getName());
    assertThat(dto.getFactionId()).isEqualTo(team.getFaction().getId());
    assertThat(dto.getFactionName()).isEqualTo(team.getFaction().getName());
    assertThat(dto.getStatus()).isEqualTo(team.getStatus());
    assertThat(dto.getFormedDate()).isEqualTo(team.getFormedDate());
    assertThat(dto.getDisbandedDate()).isEqualTo(team.getDisbandedDate());
    assertThat(dto.getExternalId()).isEqualTo(team.getExternalId());
  }
}
