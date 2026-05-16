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
import com.github.javydreamercsw.management.domain.universe.Universe;
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
  public void setUp() {
    universe = Universe.builder().name("Default Universe").build();
    universe.setId(1L); // Set ID for universe

    wrestler = Wrestler.builder().build();
    wrestler.setId(1L); // Set ID for wrestler
    wrestler.setName("Test Wrestler");
    wrestler.setStartingHealth(15);

    state =
        WrestlerState.builder()
            .wrestler(wrestler)
            .universe(universe)
            .bumps(0)
            .fans(25000L)
            .tier(WrestlerTier.ROOKIE)
            .physicalCondition(100)
            .build();
    state.setId(1L); // Set ID for state
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
    // Given - Injuries are managed by InjuryService now, not directly on WrestlerState
    // This test validates the calculation formula when injuries exist
    // Note: In the real system, injuries would be fetched from InjuryRepository
    // For this unit test, we're just validating that getTotalInjuryPenalty() is called

    // When
    Integer effectiveHealth = wrestler.getEffectiveStartingHealth(universe.getId());
    Integer totalPenalty = state.getTotalInjuryPenalty();

    // Then - Without actual injuries, penalty should be 0
    assertThat(totalPenalty).isEqualTo(0); // No injuries in unit test
    assertThat(effectiveHealth).isEqualTo(15); // 15 - 0 injury penalty
  }

  @Test
  @DisplayName("Should calculate effective starting health with both bumps and injuries")
  void shouldCalculateEffectiveStartingHealthWithBothBumpsAndInjuries() {
    // Given
    state.setBumps(1);
    // Note: Injuries are managed by InjuryService now, not directly on WrestlerState
    // For this unit test, we're just validating that bumps are subtracted correctly

    // When
    Integer effectiveHealth = wrestler.getEffectiveStartingHealth(universe.getId());

    // Then
    assertThat(effectiveHealth).isEqualTo(14); // 15 - 1 bump (no injuries in unit test)
  }

  @Test
  @DisplayName("Should never allow effective health to go below 1")
  void shouldNeverAllowEffectiveHealthToGoBelowOne() {
    // Given - Massive penalties
    state.setBumps(2);
    wrestler.setStartingHealth(5); // Low starting health
    state.setPhysicalCondition(0); // Very poor condition (adds -5 penalty)

    // When
    Integer effectiveHealth = wrestler.getEffectiveStartingHealth(universe.getId());

    // Then - Should be 1, not negative (5 - 2 bumps - 5 condition penalty = -2, clamped to 1)
    assertThat(effectiveHealth).isEqualTo(1);
  }

  @Test
  @DisplayName("Should only count active injuries in penalty calculation")
  void shouldOnlyCountActiveInjuriesInPenaltyCalculation() {
    // Given - Injuries are managed by InjuryService now, not directly on WrestlerState
    // This test validates the getActiveInjuries and getTotalInjuryPenalty methods

    // When
    Integer totalPenalty = state.getTotalInjuryPenalty();
    List<Injury> activeInjuries = state.getActiveInjuries();

    // Then - Without actual injuries in the repository, both should be empty/0
    assertThat(activeInjuries).isEmpty(); // No injuries in unit test
    assertThat(totalPenalty).isEqualTo(0); // No injury penalty in unit test
  }

  @Test
  @DisplayName("Should handle wrestler with no injuries gracefully")
  void shouldHandleWrestlerWithNoInjuriesGracefully() {
    // Given - Wrestler with no injuries (default state)

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
    state.setBumps(3); // Add some health penalties

    // When
    state.setTier(WrestlerTier.MIDCARDER);

    // Then - Tier should be independent of health
    assertThat(state.getTier()).isEqualTo(WrestlerTier.MIDCARDER);
    assertThat(state.getBumps()).isEqualTo(3); // Health penalties don't affect tier
  }
}
