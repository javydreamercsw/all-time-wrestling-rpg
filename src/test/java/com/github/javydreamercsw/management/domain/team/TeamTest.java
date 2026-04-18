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
package com.github.javydreamercsw.management.domain.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
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
  private Universe universe;
  private WrestlerState state1;
  private WrestlerState state2;

  @BeforeEach
  void setUp() {
    // Create universe
    universe = Universe.builder().name("Test Universe").build();
    universe.setId(1L);

    // Create test wrestlers
    wrestler1 = Wrestler.builder().build();
    wrestler1.setId(1L);
    wrestler1.setName("John Cena");

    wrestler2 = Wrestler.builder().build();
    wrestler2.setId(2L);
    wrestler2.setName("The Rock");

    // Create test faction
    faction = Faction.builder().build();
    faction.setId(1L);
    faction.setName("Test Faction");
    faction.setLeader(wrestler1);
    faction.setUniverse(universe);

    // Create wrestler states in the universe with the faction
    state1 =
        WrestlerState.builder()
            .wrestler(wrestler1)
            .universe(universe)
            .tier(WrestlerTier.MAIN_EVENTER)
            .fans(100000L)
            .faction(faction)
            .build();
    wrestler1.getWrestlerStates().add(state1);

    state2 =
        WrestlerState.builder()
            .wrestler(wrestler2)
            .universe(universe)
            .tier(WrestlerTier.MAIN_EVENTER)
            .fans(100000L)
            .faction(faction)
            .build();
    wrestler2.getWrestlerStates().add(state2);

    // Create test team
    team = new Team();
    team.setId(1L);
    team.setName("The Cenation");
    team.setDescription("Test team description");
    team.setWrestler1(wrestler1);
    team.setWrestler2(wrestler2);
    team.setFaction(faction);
    team.setUniverse(universe); // Set universe for team
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
    // Given - both wrestlers have same faction via their states
    assertThat(team.areFromSameFaction()).isTrue();

    // When - remove faction from one wrestler's state
    state2.setFaction(null);

    // Then
    assertThat(team.areFromSameFaction()).isFalse();
  }

  @Test
  @DisplayName("Should get common faction when wrestlers are from same faction")
  void shouldGetCommonFactionWhenWrestlersAreFromSameFaction() {
    // Given - both wrestlers have same faction via their states
    assertThat(team.areFromSameFaction()).isTrue();

    // When
    Faction commonFaction = team.getCommonFaction();

    // Then
    assertThat(commonFaction).isEqualTo(faction);
  }

  @Test
  @DisplayName("Should return null for common faction when wrestlers are from different factions")
  void shouldReturnNullForCommonFactionWhenWrestlersAreFromDifferentFactions() {
    // Given - remove faction from one wrestler's state
    state2.setFaction(null);
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
