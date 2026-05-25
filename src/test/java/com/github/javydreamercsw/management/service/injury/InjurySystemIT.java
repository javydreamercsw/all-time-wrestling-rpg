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
package com.github.javydreamercsw.management.service.injury;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.security.WithCustomMockUser;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjuryRepository;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests for the complete injury system including bump conversion, segment integration,
 * and health calculations.
 */
@DisplayName("Injury System Integration Tests")
@TestPropertySource(properties = "data.initializer.enabled=false")
@WithCustomMockUser(
    username = "admin",
    roles = {"ADMIN", "BOOKER"})
class InjurySystemIT extends ManagementIntegrationTest {
  @Autowired private InjuryService injuryService;
  @Autowired private InjuryRepository injuryRepository;
  @Autowired private WrestlerStateRepository wrestlerStateRepository;
  @Autowired private UniverseRepository universeRepository;

  private Wrestler wrestler1;
  private Long universeId;

  @BeforeEach
  public void setUp() {
    wrestler1 = createTestWrestler("Test Wrestler 1");
    universeId = universeRepository.findAll().stream().findFirst().orElseThrow().getId();
    wrestlerService.getOrCreateState(wrestler1.getId(), universeId).setFans(10_000L);
    wrestler1 = wrestlerRepository.save(wrestler1);
  }

  @Test
  @DisplayName("Should convert 3 bumps to injury and reset bumps")
  void shouldConvert3BumpsToInjuryAndResetBumps() {
    // Given - Add 2 bumps first
    assert wrestler1.getId() != null;
    Optional<com.github.javydreamercsw.management.domain.wrestler.WrestlerState> afterFirstBump =
        wrestlerService.addBump(wrestler1.getId(), universeId);
    assertThat(afterFirstBump).isPresent();
    assertThat(afterFirstBump.get().getBumps()).isEqualTo(1);

    Optional<com.github.javydreamercsw.management.domain.wrestler.WrestlerState> afterSecondBump =
        wrestlerService.addBump(wrestler1.getId(), universeId);
    assertThat(afterSecondBump).isPresent();
    assertThat(afterSecondBump.get().getBumps()).isEqualTo(2);

    // When - Add third bump (should trigger injury)
    Optional<com.github.javydreamercsw.management.domain.wrestler.WrestlerState> afterThirdBump =
        wrestlerService.addBump(wrestler1.getId(), universeId);

    // Then - Bumps should be reset and injury created
    assertThat(afterThirdBump).isPresent();
    assertThat(afterThirdBump.get().getBumps()).isEqualTo(0);

    List<Injury> injuries = injuryRepository.findActiveInjuriesForWrestler(wrestler1);
    assertThat(injuries).hasSize(1);
    assertThat(injuries.get(0).getInjuryNotes()).contains("Generated from bump accumulation");
  }

  @Test
  @DisplayName("Should apply injury penalties to health calculations")
  void shouldApplyInjuryPenaltiesToHealthCalculations() {
    // Given - Create injury for wrestler
    Optional<Injury> createdInjury =
        injuryService.createInjury(
            wrestler1.getId(),
            universeId,
            "Test Injury",
            "Test injury for health calculation",
            InjurySeverity.MODERATE,
            "Test injury");

    assertThat(createdInjury).isPresent();

    // When - Check health calculations using injury service
    List<Injury> activeInjuries = injuryRepository.findActiveInjuriesForWrestler(wrestler1);
    assertThat(activeInjuries).hasSize(1);

    Integer totalPenalty = activeInjuries.stream().mapToInt(Injury::getHealthPenalty).sum();

    // Then - Injury penalty should be applied
    assertThat(totalPenalty).isGreaterThan(0);
    assertThat(createdInjury.get().getHealthPenalty()).isEqualTo(totalPenalty);
  }

  @Test
  @DisplayName("Should calculate injury statistics correctly")
  void shouldCalculateInjuryStatisticsCorrectly() {
    // Given - Create multiple injuries for wrestler
    injuryService.createInjury(
        wrestler1.getId(),
        universeId,
        "Active Injury 1",
        "First active injury",
        InjurySeverity.MINOR,
        "Test");

    injuryService.createInjury(
        wrestler1.getId(),
        universeId,
        "Active Injury 2",
        "Second active injury",
        InjurySeverity.MODERATE,
        "Test");

    // Create and heal one injury
    Injury healedInjury =
        injuryService
            .createInjury(
                wrestler1.getId(),
                universeId,
                "Healed Injury",
                "This injury will be healed",
                InjurySeverity.SEVERE,
                "Test")
            .orElseThrow();

    healedInjury.heal();
    injuryRepository.save(healedInjury);

    // When - Get injury statistics
    InjuryService.InjuryStats stats =
        injuryService.getInjuryStatsForWrestler(wrestler1.getId(), universeId);

    // Then - Statistics should be accurate
    assertThat(stats).isNotNull();
    assertThat(stats.wrestlerName()).isEqualTo("Test Wrestler 1");
    assertThat(stats.activeInjuries()).isEqualTo(2);
    assertThat(stats.healedInjuries()).isEqualTo(1);
    assertThat(stats.totalHealthPenalty()).isGreaterThan(0);
    assertThat(stats.effectiveHealth()).isLessThanOrEqualTo(wrestler1.getStartingHealth());
    assertThat(stats.totalHealingCost()).isGreaterThan(0);
  }

  @Test
  @DisplayName("Should handle injury healing with fan cost")
  void shouldHandleInjuryHealingWithFanCost() {
    // Given - Create injury and ensure wrestler has enough fans
    com.github.javydreamercsw.management.domain.wrestler.WrestlerState state =
        wrestlerService.getOrCreateState(wrestler1.getId(), universeId);
    state.setFans(50000L); // Plenty of fans
    wrestlerStateRepository.save(state);

    Injury injury =
        injuryService
            .createInjury(
                wrestler1.getId(),
                universeId,
                "Healable Injury",
                "This injury can be healed",
                InjurySeverity.MINOR,
                "Test")
            .orElseThrow();

    Long originalFans = state.getFans();
    Long healingCost = injury.getHealingCost();

    // When - Attempt healing with good dice roll
    assert injury.getId() != null;
    InjuryService.HealingResult result = injuryService.attemptHealing(injury.getId(), 6);

    // Then - Healing should succeed and fans should be spent
    assertThat(result.success()).isTrue();
    assertThat(result.fansSpent()).isTrue();

    state = wrestlerService.getOrCreateState(wrestler1.getId(), universeId);
    assertThat(state.getFans()).isEqualTo(originalFans - healingCost);

    injury = injuryRepository.findById(injury.getId()).orElseThrow();
    assertThat(injury.isCurrentlyActive()).isFalse();
  }

  @Test
  @DisplayName("getTotalHealthPenaltyForWrestler sums active injury penalties only")
  void shouldReturnTotalHealthPenaltyFromActiveInjuries() {
    // Given - two active injuries and one healed injury
    Injury active1 =
        injuryService
            .createInjury(
                wrestler1.getId(),
                universeId,
                "Shoulder Injury",
                "Dislocated Shoulder",
                InjurySeverity.SEVERE,
                "Test")
            .orElseThrow();

    Injury active2 =
        injuryService
            .createInjury(
                wrestler1.getId(),
                universeId,
                "Knee Injury",
                "Sprained Knee",
                InjurySeverity.MINOR,
                "Test")
            .orElseThrow();

    Injury healedInjury =
        injuryService
            .createInjury(
                wrestler1.getId(),
                universeId,
                "Old Injury",
                "Already healed",
                InjurySeverity.MODERATE,
                "Test")
            .orElseThrow();
    healedInjury.heal();
    injuryRepository.save(healedInjury);

    // When
    int totalPenalty =
        injuryService.getTotalHealthPenaltyForWrestler(wrestler1.getId(), universeId);

    // Then - only active injuries contribute; healed injury is excluded
    int expectedPenalty = active1.getHealthPenalty() + active2.getHealthPenalty();
    assertThat(totalPenalty).isEqualTo(expectedPenalty);
    assertThat(totalPenalty).isGreaterThan(0);
  }

  @Test
  @DisplayName("Should prevent healing when wrestler cannot afford cost")
  void shouldPreventHealingWhenWrestlerCannotAffordCost() {
    // Given - Create injury and ensure wrestler has insufficient fans
    com.github.javydreamercsw.management.domain.wrestler.WrestlerState state =
        wrestlerService.getOrCreateState(wrestler1.getId(), universeId);
    state.setFans(1000L); // Not enough fans
    wrestlerStateRepository.save(state);

    Injury injury =
        injuryService
            .createInjury(
                wrestler1.getId(),
                universeId,
                "Expensive Injury",
                "This injury is too expensive to heal",
                InjurySeverity.SEVERE, // Expensive to heal
                "Test")
            .orElseThrow();

    Long originalFans = state.getFans();

    // When - Attempt healing
    assert injury.getId() != null;
    InjuryService.HealingResult result = injuryService.attemptHealing(injury.getId(), 6);

    // Then - Healing should fail and no fans should be spent
    assertThat(result.success()).isFalse();
    assertThat(result.fansSpent()).isFalse();
    assertThat(result.message()).contains("cannot afford");

    state = wrestlerService.getOrCreateState(wrestler1.getId(), universeId);
    assertThat(state.getFans()).isEqualTo(originalFans);

    injury = injuryRepository.findById(injury.getId()).orElseThrow();
    assertThat(injury.isCurrentlyActive()).isTrue();
  }
}
