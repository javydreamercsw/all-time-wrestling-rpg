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
package com.github.javydreamercsw.management.domain.injury;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for Injury entity. Tests the ATW RPG injury system functionality. */
@DisplayName("Injury Tests")
class InjuryTest {

  private Injury injury;
  private Wrestler wrestler;
  private WrestlerState state;
  private Universe universe;

  @BeforeEach
  void setUp() {
    universe = Universe.builder().id(1L).name("Default").build();

    wrestler = Wrestler.builder().build();
    wrestler.setName("Test Wrestler");
    wrestler.setStartingHealth(15);

    state = WrestlerState.builder().wrestler(wrestler).universe(universe).fans(50000L).build();

    injury = new Injury();
    injury.setWrestler(wrestler);
    injury.setUniverse(universe);
    injury.setName("Knee Injury");
    injury.setDescription("Torn ACL from high-impact move");
    injury.setSeverity(InjurySeverity.MODERATE);
    injury.setHealthPenalty(3);
    injury.setHealingCost(10000L);
    injury.setInjuryDate(Instant.now()); // Initialize injury date
  }

  @Test
  @DisplayName("Should initialize with default values")
  void shouldInitializeWithDefaultValues() {
    Injury newInjury = new Injury();

    assertThat(newInjury.getIsActive()).isTrue();
    assertThat(newInjury.getHealedDate()).isNull();
    assertThat(newInjury.getHealingCost()).isEqualTo(10000L);
  }

  @Test
  @DisplayName("Should check if currently active")
  void shouldCheckIfCurrentlyActive() {
    // Active injury with no healed date
    assertThat(injury.isCurrentlyActive()).isTrue();

    // Inactive injury
    injury.setIsActive(false);
    assertThat(injury.isCurrentlyActive()).isFalse();

    // Active but healed injury
    injury.setIsActive(true);
    injury.setHealedDate(Instant.now());
    assertThat(injury.isCurrentlyActive()).isFalse();
  }

  @Test
  @DisplayName("Should heal injury properly")
  void shouldHealInjuryProperly() {
    assertThat(injury.isCurrentlyActive()).isTrue();

    Instant beforeHeal = Instant.now();
    injury.heal();
    Instant afterHeal = Instant.now();

    assertThat(injury.getIsActive()).isFalse();
    assertThat(injury.getHealedDate()).isBetween(beforeHeal, afterHeal);
    assertThat(injury.isCurrentlyActive()).isFalse();
  }

  @Test
  @DisplayName("Should calculate days active")
  void shouldCalculateDaysActive() {
    // Same day injury
    assertThat(injury.getDaysActive()).isEqualTo(0);

    // Injury from 5 days ago
    injury.setInjuryDate(Instant.now().minusSeconds(5 * 24 * 60 * 60));
    assertThat(injury.getDaysActive()).isEqualTo(5);

    // Healed injury (should use healed date)
    Instant healedDate = injury.getInjuryDate().plusSeconds(3 * 24 * 60 * 60);
    injury.setHealedDate(healedDate);
    assertThat(injury.getDaysActive()).isEqualTo(3);
  }

  @Test
  @DisplayName("Should maintain relationship with wrestler")
  void shouldMaintainRelationshipWithWrestler() {
    assertThat(injury.getWrestler()).isEqualTo(wrestler);
    assertThat(injury.getWrestler().getName()).isEqualTo("Test Wrestler");
  }

  @Test
  @DisplayName("Should handle different severity levels")
  void shouldHandleDifferentSeverityLevels() {
    injury.setSeverity(InjurySeverity.MINOR);
    assertThat(injury.getStatusEmoji()).isEqualTo("🟡");

    injury.setSeverity(InjurySeverity.MODERATE);
    assertThat(injury.getStatusEmoji()).isEqualTo("🟠");

    injury.setSeverity(InjurySeverity.SEVERE);
    assertThat(injury.getStatusEmoji()).isEqualTo("🔴");

    injury.setSeverity(InjurySeverity.CRITICAL);
    assertThat(injury.getStatusEmoji()).isEqualTo("💀");
  }
}
