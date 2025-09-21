package com.github.javydreamercsw.management.domain.wrestler;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import java.time.Instant;
import java.util.ArrayList;
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

  @BeforeEach
  void setUp() {
    wrestler = new Wrestler();
    wrestler.setName("Test Wrestler");
    wrestler.setStartingHealth(15);
    wrestler.setCurrentHealth(15);
    wrestler.setBumps(0);
    wrestler.setFans(25000L);
    wrestler.updateTier();
    wrestler.setInjuries(new ArrayList<>());
  }

  @Test
  @DisplayName("Should calculate effective starting health with no injuries or bumps")
  void shouldCalculateEffectiveStartingHealthWithNoInjuriesOrBumps() {
    // Given - Clean wrestler
    assertThat(wrestler.getBumps()).isEqualTo(0);
    assertThat(wrestler.getActiveInjuries()).isEmpty();

    // When
    Integer effectiveHealth = wrestler.getEffectiveStartingHealth();

    // Then
    assertThat(effectiveHealth).isEqualTo(15);
  }

  @Test
  @DisplayName("Should calculate effective starting health with bumps only")
  void shouldCalculateEffectiveStartingHealthWithBumpsOnly() {
    // Given
    wrestler.setBumps(2);

    // When
    Integer effectiveHealth = wrestler.getEffectiveStartingHealth();

    // Then
    assertThat(effectiveHealth).isEqualTo(13); // 15 - 2 bumps
  }

  @Test
  @DisplayName("Should calculate effective starting health with injuries only")
  void shouldCalculateEffectiveStartingHealthWithInjuriesOnly() {
    // Given
    Injury minorInjury = createInjury(InjurySeverity.MINOR, 2);
    Injury moderateInjury = createInjury(InjurySeverity.MODERATE, 3);
    wrestler.getInjuries().add(minorInjury);
    wrestler.getInjuries().add(moderateInjury);

    // When
    Integer effectiveHealth = wrestler.getEffectiveStartingHealth();
    Integer totalPenalty = wrestler.getTotalInjuryPenalty();

    // Then
    assertThat(totalPenalty).isEqualTo(5); // 2 + 3
    assertThat(effectiveHealth).isEqualTo(10); // 15 - 5 injury penalty
  }

  @Test
  @DisplayName("Should calculate effective starting health with both bumps and injuries")
  void shouldCalculateEffectiveStartingHealthWithBothBumpsAndInjuries() {
    // Given
    wrestler.setBumps(1);
    Injury injury = createInjury(InjurySeverity.SEVERE, 4);
    wrestler.getInjuries().add(injury);

    // When
    Integer effectiveHealth = wrestler.getEffectiveStartingHealth();

    // Then
    assertThat(effectiveHealth).isEqualTo(10); // 15 - 1 bump - 4 injury penalty
  }

  @Test
  @DisplayName("Should never allow effective health to go below 1")
  void shouldNeverAllowEffectiveHealthToGoBelowOne() {
    // Given - Massive penalties
    wrestler.setBumps(2);
    wrestler.setStartingHealth(5); // Low starting health

    // Add severe injuries
    Injury criticalInjury1 = createInjury(InjurySeverity.CRITICAL, 6);
    Injury criticalInjury2 = createInjury(InjurySeverity.CRITICAL, 7);
    wrestler.getInjuries().add(criticalInjury1);
    wrestler.getInjuries().add(criticalInjury2);

    // When
    Integer effectiveHealth = wrestler.getEffectiveStartingHealth();

    // Then - Should be 1, not negative
    assertThat(effectiveHealth).isEqualTo(1);
  }

  @Test
  @DisplayName("Should calculate current health with penalties correctly")
  void shouldCalculateCurrentHealthWithPenaltiesCorrectly() {
    // Given
    wrestler.setCurrentHealth(12); // Damaged in segment
    wrestler.setBumps(1);
    Injury injury = createInjury(InjurySeverity.MODERATE, 3);
    wrestler.getInjuries().add(injury);

    // When
    Integer currentHealthWithPenalties = wrestler.getCurrentHealthWithPenalties();

    // Then
    assertThat(currentHealthWithPenalties).isEqualTo(8); // 12 - 1 bump - 3 injury
  }

  @Test
  @DisplayName("Should use effective starting health when current health is null")
  void shouldUseEffectiveStartingHealthWhenCurrentHealthIsNull() {
    // Given
    wrestler.setCurrentHealth(null);
    wrestler.setBumps(1);
    Injury injury = createInjury(InjurySeverity.MINOR, 2);
    wrestler.getInjuries().add(injury);

    // When
    Integer currentHealthWithPenalties = wrestler.getCurrentHealthWithPenalties();
    Integer effectiveStartingHealth = wrestler.getEffectiveStartingHealth();

    // Then
    assertThat(currentHealthWithPenalties).isEqualTo(effectiveStartingHealth);
    assertThat(currentHealthWithPenalties).isEqualTo(12); // 15 - 1 - 2
  }

  @Test
  @DisplayName("Should refresh current health to segment effective starting health")
  void shouldRefreshCurrentHealthToMatchEffectiveStartingHealth() {
    // Given
    wrestler.setCurrentHealth(10); // Some arbitrary value
    wrestler.setBumps(2);
    Injury injury = createInjury(InjurySeverity.MODERATE, 3);
    wrestler.getInjuries().add(injury);

    // When
    wrestler.refreshCurrentHealth();

    // Then
    Integer expectedHealth = wrestler.getEffectiveStartingHealth();
    assertThat(wrestler.getCurrentHealth()).isEqualTo(expectedHealth);
    assertThat(wrestler.getCurrentHealth()).isEqualTo(10); // 15 - 2 - 3
  }

  @Test
  @DisplayName("Should only count active injuries in penalty calculation")
  void shouldOnlyCountActiveInjuriesInPenaltyCalculation() {
    // Given
    Injury activeInjury = createInjury(InjurySeverity.MODERATE, 3);
    Injury healedInjury = createInjury(InjurySeverity.SEVERE, 5);
    healedInjury.heal(); // Heal this injury

    wrestler.getInjuries().add(activeInjury);
    wrestler.getInjuries().add(healedInjury);

    // When
    Integer totalPenalty = wrestler.getTotalInjuryPenalty();
    List<Injury> activeInjuries = wrestler.getActiveInjuries();

    // Then
    assertThat(activeInjuries).hasSize(1);
    assertThat(activeInjuries.get(0)).isEqualTo(activeInjury);
    assertThat(totalPenalty).isEqualTo(3); // Only active injury counts
  }

  @Test
  @DisplayName("Should handle wrestler with no injuries gracefully")
  void shouldHandleWrestlerWithNoInjuriesGracefully() {
    // Given - Wrestler with empty injury list
    wrestler.setInjuries(new ArrayList<>());

    // When
    Integer totalPenalty = wrestler.getTotalInjuryPenalty();
    List<Injury> activeInjuries = wrestler.getActiveInjuries();

    // Then
    assertThat(activeInjuries).isEmpty();
    assertThat(totalPenalty).isEqualTo(0);
  }

  @Test
  @DisplayName("Should calculate fan weight correctly regardless of health")
  void shouldCalculateFanWeightCorrectlyRegardlessOfHealth() {
    // Given
    wrestler.setFans(25000L); // Should give weight of 5000 (25000 / 5)

    // Add injuries and bumps
    wrestler.setBumps(2);
    Injury injury = createInjury(InjurySeverity.SEVERE, 4);
    wrestler.getInjuries().add(injury);

    // When
    Integer fanWeight = wrestler.getFanWeight();

    // Then - Fan weight should not be affected by health
    assertThat(fanWeight).isEqualTo(5000);
  }

  @Test
  @DisplayName("Should update tier correctly regardless of health status")
  void shouldUpdateTierCorrectlyRegardlessOfHealthStatus() {
    // Given
    wrestler.setFans(75000L); // Should be INTERTEMPORAL_TIER (60,000 - 99,999)

    // Add injuries
    Injury injury = createInjury(InjurySeverity.CRITICAL, 6);
    wrestler.getInjuries().add(injury);

    // When
    wrestler.updateTier();

    // Then - Tier should be based on fans, not health
    assertThat(wrestler.getTier()).isEqualTo(WrestlerTier.MIDCARDER);
  }

  private Injury createInjury(InjurySeverity severity, int healthPenalty) {
    Injury injury = new Injury();
    injury.setWrestler(wrestler);
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
