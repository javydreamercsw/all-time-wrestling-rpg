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
package com.github.javydreamercsw.management.domain.wrestler;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.universe.Universe;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for wrestler health calculation methods. Tests the ATW RPG health system with injuries
 * and bumps.
 */
@DisplayName("Wrestler Health Calculation Tests")
class WrestlerHealthCalculationTest {

  private Wrestler wrestler;
  private WrestlerState state;
  private Universe universe;

  @BeforeEach
  void setUp() {
    universe = Universe.builder().id(1L).name("Default Universe").build();

    wrestler = Wrestler.builder().build();
    wrestler.setName("Test Wrestler");
    wrestler.setStartingHealth(15);
    wrestler.setCurrentHealth(15);

    state =
        WrestlerState.builder()
            .wrestler(wrestler)
            .universe(universe)
            .bumps(0)
            .fans(25000L)
            .tier(WrestlerTier.ROOKIE)
            .physicalCondition(100)
            .build();
    wrestler.getWrestlerStates().add(state);
  }

  @Test
  @DisplayName("Should calculate effective starting health with no injuries or bumps")
  void shouldCalculateEffectiveStartingHealthWithNoInjuriesOrBumps() {
    // Given - Clean wrestler
    assertThat(state.getBumps()).isEqualTo(0);
    assertThat(state.getActiveInjuries()).isEmpty();

    // When
    Integer effectiveHealth = wrestler.getEffectiveStartingHealth(universe.getId());

    // Then
    assertThat(effectiveHealth).isEqualTo(15);
  }

  @Test
  @DisplayName("Should calculate effective starting health with bumps only")
  void shouldCalculateEffectiveStartingHealthWithBumpsOnly() {
    // Given
    state.setBumps(2);

    // When
    Integer effectiveHealth = wrestler.getEffectiveStartingHealth(universe.getId());

    // Then
    assertThat(effectiveHealth).isEqualTo(13); // 15 - 2 bumps
  }

  @Test
  @DisplayName("Should calculate effective starting health with injuries only")
  void shouldCalculateEffectiveStartingHealthWithInjuriesOnly() {
    // Given
    Injury minorInjury = createInjury(InjurySeverity.MINOR, 2);
    Injury moderateInjury = createInjury(InjurySeverity.MODERATE, 3);
    state.getInjuries().add(minorInjury);
    state.getInjuries().add(moderateInjury);

    // When
    Integer effectiveHealth = wrestler.getEffectiveStartingHealth(universe.getId());
    Integer totalPenalty = state.getTotalInjuryPenalty();

    // Then
    assertThat(totalPenalty).isEqualTo(5); // 2 + 3
    assertThat(effectiveHealth).isEqualTo(10); // 15 - 5 injury penalty
  }

  @Test
  @DisplayName("Should calculate effective starting health with both bumps and injuries")
  void shouldCalculateEffectiveStartingHealthWithBothBumpsAndInjuries() {
    // Given
    state.setBumps(1);
    Injury injury = createInjury(InjurySeverity.SEVERE, 4);
    state.getInjuries().add(injury);

    // When
    Integer effectiveHealth = wrestler.getEffectiveStartingHealth(universe.getId());

    // Then
    assertThat(effectiveHealth).isEqualTo(10); // 15 - 1 bump - 4 injury penalty
  }

  @Test
  @DisplayName("Should never allow effective health to go below 1")
  void shouldNeverAllowEffectiveHealthToGoBelowOne() {
    // Given - Massive penalties
    state.setBumps(2);
    wrestler.setStartingHealth(5); // Low starting health

    // Add severe injuries
    Injury severeInjury1 = createInjury(InjurySeverity.SEVERE, 6);
    Injury severeInjury2 = createInjury(InjurySeverity.SEVERE, 7);
    state.getInjuries().add(severeInjury1);
    state.getInjuries().add(severeInjury2);

    // When
    Integer effectiveHealth = wrestler.getEffectiveStartingHealth(universe.getId());

    // Then - Should be 1, not negative
    assertThat(effectiveHealth).isEqualTo(1);
  }

  @Test
  @DisplayName("Should only count active injuries in penalty calculation")
  void shouldOnlyCountActiveInjuriesInPenaltyCalculation() {
    // Given
    Injury activeInjury = createInjury(InjurySeverity.MODERATE, 3);
    Injury healedInjury = createInjury(InjurySeverity.SEVERE, 5);
    healedInjury.setIsActive(false);

    state.getInjuries().add(activeInjury);
    state.getInjuries().add(healedInjury);

    // When
    Integer totalPenalty = state.getTotalInjuryPenalty();
    List<Injury> activeInjuries = state.getActiveInjuries();

    // Then
    assertThat(activeInjuries).hasSize(1);
    assertThat(activeInjuries.get(0)).isEqualTo(activeInjury);
    assertThat(totalPenalty).isEqualTo(3); // Only active injury counts
  }

  @Test
  @DisplayName("Should handle wrestler with no injuries gracefully")
  void shouldHandleWrestlerWithNoInjuriesGracefully() {
    // Given - Wrestler with empty injury list
    state.getInjuries().clear();

    // When
    Integer totalPenalty = state.getTotalInjuryPenalty();
    List<Injury> activeInjuries = state.getActiveInjuries();

    // Then
    assertThat(activeInjuries).isEmpty();
    assertThat(totalPenalty).isEqualTo(0);
  }

  @Test
  @DisplayName("Should update tier correctly regardless of health status")
  void shouldUpdateTierCorrectlyRegardlessOfHealthStatus() {
    // Given
    state.setFans(75000L);

    // Add injuries
    Injury injury = createInjury(InjurySeverity.SEVERE, 6);
    state.getInjuries().add(injury);

    // When
    state.setTier(WrestlerTier.MIDCARDER);

    // Then - Tier should be based on fans, not health
    assertThat(state.getTier()).isEqualTo(WrestlerTier.MIDCARDER);
  }

  private Injury createInjury(InjurySeverity severity, int healthPenalty) {
    Injury injury = new Injury();
    injury.setWrestler(wrestler);
    injury.setUniverse(universe);
    injury.setName("Test " + severity.name() + " Injury");
    injury.setDescription("Test injury for health calculation");
    injury.setSeverity(severity);
    injury.setHealthPenalty(healthPenalty);
    injury.setHealingCost(severity.getBaseHealingCost());
    injury.setIsActive(true);
    injury.setInjuryDate(Instant.now());
    return injury;
  }
}
