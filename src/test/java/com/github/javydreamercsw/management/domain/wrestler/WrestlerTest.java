package com.github.javydreamercsw.management.domain.wrestler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for Wrestler entity ATW RPG functionality. Tests the fan system, tier management,
 * injury system, and title eligibility.
 */
@DisplayName("Wrestler ATW RPG Tests")
class WrestlerTest {

  private Wrestler wrestler;

  @BeforeEach
  void setUp() {
    wrestler = new Wrestler();
    wrestler.setName("Test Wrestler");
    wrestler.setStartingHealth(15);
    wrestler.setFans(0L);
    wrestler.setBumps(0);
    wrestler.setIsPlayer(true);
    wrestler.updateTier(); // Explicitly update tier for unit tests
  }

  @Test
  @DisplayName("Should initialize with default ATW RPG values")
  void shouldInitializeWithDefaultAtwRpgValues() {
    assertThat(wrestler.getFans()).isEqualTo(0L);
    assertThat(wrestler.getTier()).isEqualTo(WrestlerTier.ROOKIE);
    assertThat(wrestler.getBumps()).isEqualTo(0);
    assertThat(wrestler.getIsPlayer()).isTrue();
  }

  @Test
  @DisplayName("Should calculate fan weight correctly")
  void shouldCalculateFanWeightCorrectly() {
    wrestler.setFans(25L); // 25,000 fans
    assertThat(wrestler.getFanWeight()).isEqualTo(5);

    wrestler.setFans(47L); // 47,000 fans
    assertThat(wrestler.getFanWeight()).isEqualTo(9);

    wrestler.setFans(3L); // 3,000 fans
    assertThat(wrestler.getFanWeight()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should calculate effective starting health with bumps")
  void shouldCalculateEffectiveStartingHealthWithBumps() {
    wrestler.setStartingHealth(15);
    wrestler.setBumps(0);
    assertThat(wrestler.getEffectiveStartingHealth()).isEqualTo(15);

    wrestler.setBumps(2);
    assertThat(wrestler.getEffectiveStartingHealth()).isEqualTo(13);

    // Should never go below 1
    wrestler.setBumps(20);
    assertThat(wrestler.getEffectiveStartingHealth()).isEqualTo(1);
  }

  @ParameterizedTest
  @DisplayName("Should check title eligibility correctly")
  @CsvSource({
    "20000, true, false, false, false",
    "25000, true, false, false, false",
    "40000, true, true, false, false",
    "60000, true, true, true, false",
    "100000, true, true, true, true",
    "150000, true, true, true, true"
  })
  void shouldCheckTitleEligibilityCorrectly(
      Long fans, boolean rookie, boolean contender, boolean midcarder, boolean maineventer) {
    wrestler.setFans(fans);

    assertThat(wrestler.isEligibleForTitle(WrestlerTier.ROOKIE)).isEqualTo(rookie);
    assertThat(wrestler.isEligibleForTitle(WrestlerTier.CONTENDER)).isEqualTo(contender);
    assertThat(wrestler.isEligibleForTitle(WrestlerTier.MIDCARDER)).isEqualTo(midcarder);
    assertThat(wrestler.isEligibleForTitle(WrestlerTier.MAIN_EVENTER)).isEqualTo(maineventer);
  }

  @Test
  @DisplayName("Should update tier automatically when fans change")
  void shouldUpdateTierAutomaticallyWhenFansChange() {
    wrestler.setFans(20000L);
    wrestler.updateTier();
    assertThat(wrestler.getTier()).isEqualTo(WrestlerTier.ROOKIE);

    wrestler.setFans(30000L);
    wrestler.updateTier();
    assertThat(wrestler.getTier()).isEqualTo(WrestlerTier.RISER);

    wrestler.setFans(120000L);
    wrestler.updateTier();
    assertThat(wrestler.getTier()).isEqualTo(WrestlerTier.MAIN_EVENTER);
  }

  @Test
  @DisplayName("Should add fans and update tier automatically")
  void shouldAddFansAndUpdateTierAutomatically() {
    wrestler.setFans(20000L); // Rookie
    wrestler.updateTier();
    assertThat(wrestler.getTier()).isEqualTo(WrestlerTier.ROOKIE);

    wrestler.addFans(15000L); // Now 35k fans -> Riser
    assertThat(wrestler.getFans()).isEqualTo(35000L);
    assertThat(wrestler.getTier()).isEqualTo(WrestlerTier.RISER);

    wrestler.addFans(30000L); // Now 65k fans -> Intertemporal
    assertThat(wrestler.getFans()).isEqualTo(65000L);
    assertThat(wrestler.getTier()).isEqualTo(WrestlerTier.MIDCARDER);
  }

  @Test
  @DisplayName("Should handle negative fan gains")
  void shouldHandleNegativeFanGains() {
    wrestler.setFans(50000L);
    wrestler.addFans(-20000L);

    assertThat(wrestler.getFans()).isEqualTo(30000L);
    assertThat(wrestler.getTier()).isEqualTo(WrestlerTier.RISER);
  }

  @Test
  @DisplayName("Should not allow fans to go below zero")
  void shouldNotAllowFansToBelowZero() {
    wrestler.setFans(10000L);
    wrestler.addFans(-20000L);

    assertThat(wrestler.getFans()).isEqualTo(0L);
    assertThat(wrestler.getTier()).isEqualTo(WrestlerTier.ROOKIE);
  }

  @Test
  @DisplayName("Should handle bump system correctly")
  void shouldHandleBumpSystemCorrectly() {
    // First bump
    boolean injuryOccurred = wrestler.addBump();
    assertThat(injuryOccurred).isFalse();
    assertThat(wrestler.getBumps()).isEqualTo(1);

    // Second bump
    injuryOccurred = wrestler.addBump();
    assertThat(injuryOccurred).isFalse();
    assertThat(wrestler.getBumps()).isEqualTo(2);

    // Third bump should trigger injury
    injuryOccurred = wrestler.addBump();
    assertThat(injuryOccurred).isTrue();
    assertThat(wrestler.getBumps()).isEqualTo(0); // Reset after injury
  }

  @Test
  @DisplayName("Should create display name with tier emoji")
  void shouldCreateDisplayNameWithTierEmoji() {
    wrestler.setName("John Cena");
    wrestler.setFans(120000L); // Main Eventer
    wrestler.updateTier();

    assertThat(wrestler.getDisplayNameWithTier()).isEqualTo("👑 John Cena");
  }

  @Test
  @DisplayName("Should check if wrestler can afford costs")
  void shouldCheckIfWrestlerCanAffordCosts() {
    wrestler.setFans(50000L);

    assertThat(wrestler.canAfford(30000L)).isTrue();
    assertThat(wrestler.canAfford(50000L)).isTrue();
    assertThat(wrestler.canAfford(51000L)).isFalse();
  }

  @Test
  @DisplayName("Should spend fans successfully when affordable")
  void shouldSpendFansSuccessfullyWhenAffordable() {
    wrestler.setFans(50000L); // Contender tier
    wrestler.updateTier();
    assertThat(wrestler.getTier()).isEqualTo(WrestlerTier.CONTENDER);

    boolean success = wrestler.spendFans(15000L);

    assertThat(success).isTrue();
    assertThat(wrestler.getFans()).isEqualTo(35000L);
    assertThat(wrestler.getTier()).isEqualTo(WrestlerTier.RISER); // Tier updated
  }

  @Test
  @DisplayName("Should fail to spend fans when unaffordable")
  void shouldFailToSpendFansWhenUnaffordable() {
    wrestler.setFans(10000L);

    boolean success = wrestler.spendFans(15000L);

    assertThat(success).isFalse();
    assertThat(wrestler.getFans()).isEqualTo(10000L); // Unchanged
  }

  @Test
  @DisplayName("Should handle edge case fan amounts")
  void shouldHandleEdgeCaseFanAmounts() {
    // Test exact tier boundaries
    wrestler.setFans(25000L); // Exactly at Riser threshold
    wrestler.updateTier();
    assertThat(wrestler.getTier()).isEqualTo(WrestlerTier.RISER);

    wrestler.setFans(24999L); // Just below Riser threshold
    wrestler.updateTier();
    assertThat(wrestler.getTier()).isEqualTo(WrestlerTier.ROOKIE);

    wrestler.setFans(150000L); // Exactly at Icon threshold
    wrestler.updateTier();
    assertThat(wrestler.getTier()).isEqualTo(WrestlerTier.ICON);
  }

  @Test
  @DisplayName("Should maintain data integrity across operations")
  void shouldMaintainDataIntegrityAcrossOperations() {
    // Start as rookie
    wrestler.setFans(0L);
    wrestler.setBumps(0);
    wrestler.updateTier();

    // Add fans and bumps
    wrestler.addFans(45000L); // Contender tier
    wrestler.addBump();
    wrestler.addBump();

    // Verify state
    assertThat(wrestler.getFans()).isEqualTo(45000L);
    assertThat(wrestler.getTier()).isEqualTo(WrestlerTier.CONTENDER);
    assertThat(wrestler.getBumps()).isEqualTo(2);
    assertThat(wrestler.getEffectiveStartingHealth()).isEqualTo(13); // 15 - 2 bumps
    assertThat(wrestler.isEligibleForTitle(WrestlerTier.RISER)).isTrue();
    assertThat(wrestler.isEligibleForTitle(WrestlerTier.MIDCARDER)).isFalse();
  }
}
