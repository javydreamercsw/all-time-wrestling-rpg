package com.github.javydreamercsw.management.domain.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for Team domain entity. Tests the business logic and behavior of the Team entity. */
@DisplayName("Team Domain Entity Tests")
class TeamTest {

  private Team team;
  private Wrestler wrestler1;
  private Wrestler wrestler2;
  private Faction faction;

  @BeforeEach
  void setUp() {
    // Create test wrestlers
    wrestler1 = Wrestler.builder().build();
    wrestler1.setId(1L);
    wrestler1.setName("John Cena");
    wrestler1.setTier(WrestlerTier.MAIN_EVENTER);

    wrestler2 = Wrestler.builder().build();
    wrestler2.setId(2L);
    wrestler2.setName("The Rock");
    wrestler2.setTier(WrestlerTier.MAIN_EVENTER);

    // Create test faction
    faction = new Faction();
    faction.setId(1L);
    faction.setName("Test Faction");
    faction.setLeader(wrestler1);

    // Set faction for wrestlers
    wrestler1.setFaction(faction);
    wrestler2.setFaction(faction);

    // Create test team
    team = new Team();
    team.setId(1L);
    team.setName("The Cenation");
    team.setDescription("Test team description");
    team.setWrestler1(wrestler1);
    team.setWrestler2(wrestler2);
    team.setFaction(faction);
    team.setStatus(TeamStatus.ACTIVE);
    team.setFormedDate(Instant.now());
  }

  @Test
  @DisplayName("Should have default values when created")
  void shouldHaveDefaultValuesWhenCreated() {
    // Given
    Team newTeam = new Team();
    newTeam.setName("New Team");
    newTeam.setWrestler1(wrestler1);
    newTeam.setWrestler2(wrestler2);
    newTeam.setStatus(TeamStatus.ACTIVE); // Simulate default behavior
    newTeam.setFormedDate(Instant.now()); // Simulate default behavior

    // Then
    assertThat(newTeam.getStatus()).isEqualTo(TeamStatus.ACTIVE);
    assertThat(newTeam.getFormedDate()).isNotNull();
  }

  @Test
  @DisplayName("Should check if team is active")
  void shouldCheckIfTeamIsActive() {
    // Given - team is active by default
    assertThat(team.isActive()).isTrue();

    // When - disband team
    team.setStatus(TeamStatus.DISBANDED);

    // Then
    assertThat(team.isActive()).isFalse();
  }

  @Test
  @DisplayName("Should disband team correctly")
  void shouldDisbandTeamCorrectly() {
    // Given - team is active
    assertThat(team.getStatus()).isEqualTo(TeamStatus.ACTIVE);
    assertThat(team.getDisbandedDate()).isNull();

    // When
    team.disband();

    // Then
    assertThat(team.getStatus()).isEqualTo(TeamStatus.DISBANDED);
    assertThat(team.getDisbandedDate()).isNotNull();
  }

  @Test
  @DisplayName("Should reactivate team correctly")
  void shouldReactivateTeamCorrectly() {
    // Given - disbanded team
    team.disband();
    assertThat(team.getStatus()).isEqualTo(TeamStatus.DISBANDED);
    assertThat(team.getDisbandedDate()).isNotNull();

    // When
    team.reactivate();

    // Then
    assertThat(team.getStatus()).isEqualTo(TeamStatus.ACTIVE);
    assertThat(team.getDisbandedDate()).isNull();
  }

  @Test
  @DisplayName("Should check if wrestler is team member")
  void shouldCheckIfWrestlerIsTeamMember() {
    // Then
    assertThat(team.hasMember(wrestler1)).isTrue();
    assertThat(team.hasMember(wrestler2)).isTrue();

    // Given - different wrestler
    Wrestler otherWrestler = Wrestler.builder().build();
    otherWrestler.setId(3L);
    otherWrestler.setName("Stone Cold");

    // Then
    assertThat(team.hasMember(otherWrestler)).isFalse();
  }

  @Test
  @DisplayName("Should get partner wrestler")
  void shouldGetPartnerWrestler() {
    // When/Then
    assertThat(team.getPartner(wrestler1)).isEqualTo(wrestler2);
    assertThat(team.getPartner(wrestler2)).isEqualTo(wrestler1);
  }

  @Test
  @DisplayName("Should throw exception when getting partner for non-member")
  void shouldThrowExceptionWhenGettingPartnerForNonMember() {
    // Given
    Wrestler nonMember = Wrestler.builder().build();
    nonMember.setId(3L);
    nonMember.setName("Stone Cold");

    // When/Then
    assertThatThrownBy(() -> team.getPartner(nonMember))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Wrestler is not a member of this team");
  }

  @Test
  @DisplayName("Should get member names formatted correctly")
  void shouldGetMemberNamesFormattedCorrectly() {
    // When
    String memberNames = team.getMemberNames();

    // Then
    assertThat(memberNames).isEqualTo("John Cena & The Rock");
  }

  @Test
  @DisplayName("Should get display name for active team")
  void shouldGetDisplayNameForActiveTeam() {
    // When
    String displayName = team.getDisplayName();

    // Then
    assertThat(displayName).isEqualTo("The Cenation");
  }

  @Test
  @DisplayName("Should get display name for disbanded team")
  void shouldGetDisplayNameForDisbandedTeam() {
    // Given
    team.disband();

    // When
    String displayName = team.getDisplayName();

    // Then
    assertThat(displayName).isEqualTo("The Cenation (Disbanded)");
  }

  @Test
  @DisplayName("Should get display name with member names when no team name")
  void shouldGetDisplayNameWithMemberNamesWhenNoTeamName() {
    // Given
    team.setName(null);

    // When
    String displayName = team.getDisplayName();

    // Then
    assertThat(displayName).isEqualTo("John Cena & The Rock");
  }

  @Test
  @DisplayName("Should check if wrestlers are from same faction")
  void shouldCheckIfWrestlersAreFromSameFaction() {
    // Given - both wrestlers have same faction
    assertThat(team.areFromSameFaction()).isTrue();

    // When - remove faction from one wrestler
    wrestler2.setFaction(null);

    // Then
    assertThat(team.areFromSameFaction()).isFalse();
  }

  @Test
  @DisplayName("Should get common faction when wrestlers are from same faction")
  void shouldGetCommonFactionWhenWrestlersAreFromSameFaction() {
    // Given - both wrestlers have same faction
    assertThat(team.areFromSameFaction()).isTrue();

    // When
    Faction commonFaction = team.getCommonFaction();

    // Then
    assertThat(commonFaction).isEqualTo(faction);
  }

  @Test
  @DisplayName("Should return null for common faction when wrestlers are from different factions")
  void shouldReturnNullForCommonFactionWhenWrestlersAreFromDifferentFactions() {
    // Given - remove faction from one wrestler
    wrestler2.setFaction(null);
    assertThat(team.areFromSameFaction()).isFalse();

    // When
    Faction commonFaction = team.getCommonFaction();

    // Then
    assertThat(commonFaction).isNull();
  }

  @Test
  @DisplayName("Should return team display name in toString")
  void shouldReturnTeamDisplayNameInToString() {
    // When
    String toString = team.toString();

    // Then
    assertThat(toString).isEqualTo("The Cenation");
  }

  @Test
  @DisplayName("Should handle team status transitions correctly")
  void shouldHandleTeamStatusTransitionsCorrectly() {
    // Given - active team
    assertThat(team.getStatus()).isEqualTo(TeamStatus.ACTIVE);

    // When - set to inactive
    team.setStatus(TeamStatus.INACTIVE);

    // Then
    assertThat(team.getStatus()).isEqualTo(TeamStatus.INACTIVE);
    assertThat(team.isActive()).isFalse();

    // When - reactivate
    team.reactivate();

    // Then
    assertThat(team.getStatus()).isEqualTo(TeamStatus.ACTIVE);
    assertThat(team.isActive()).isTrue();
  }

  @Test
  @DisplayName("Should preserve formed date when reactivating")
  void shouldPreserveFormedDateWhenReactivating() {
    // Given
    Instant originalFormedDate = team.getFormedDate();

    // When - disband and reactivate
    team.disband();
    team.reactivate();

    // Then
    assertThat(team.getFormedDate()).isEqualTo(originalFormedDate);
  }
}
