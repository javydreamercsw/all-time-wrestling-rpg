package com.github.javydreamercsw.management.service.wrestler;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.transaction.annotation.Transactional;

@EnabledIf("isNotionTokenAvailable")
@Transactional
class WrestlerServiceIT extends ManagementIntegrationTest {

  @Test
  @DisplayName("Should create wrestler with ATW RPG defaults")
  void shouldCreateWrestlerWithAtwRpgDefaults() {
    // When
    Wrestler wrestler = wrestlerService.createWrestler("Test Wrestler", true, "Test description");

    // Then
    assertThat(wrestler.getId()).isNotNull();
    assertThat(wrestler.getName()).isEqualTo("Test Wrestler");
    assertThat(wrestler.getFans()).isEqualTo(0L);
    assertThat(wrestler.getTier()).isEqualTo(WrestlerTier.ROOKIE);
    assertThat(wrestler.getBumps()).isEqualTo(0);
    assertThat(wrestler.getIsPlayer()).isTrue();
    assertThat(wrestler.getDescription()).isEqualTo("Test description");

    assertThat(wrestler.getDeckSize()).isEqualTo(15);
    assertThat(wrestler.getStartingHealth()).isEqualTo(15);
  }

  @Test
  @DisplayName("Should award fans and persist tier changes")
  void shouldAwardFansAndPersistTierChanges() {
    // Given
    Wrestler wrestler = wrestlerService.createWrestler("Test Wrestler", true, null);
    assertThat(wrestler.getTier()).isEqualTo(WrestlerTier.ROOKIE);

    // When - Award enough fans to reach Contender tier
    Assertions.assertNotNull(wrestler.getId());
    Optional<Wrestler> updated = wrestlerService.awardFans(wrestler.getId(), 45_000L);

    // Then
    assertThat(updated).isPresent();
    assertThat(updated.get().getFans()).isEqualTo(45_000L);
    assertThat(updated.get().getTier()).isEqualTo(WrestlerTier.CONTENDER);

    // Verify persistence
    Optional<Wrestler> fromDb = wrestlerRepository.findById(wrestler.getId());
    assertThat(fromDb).isPresent();
    assertThat(fromDb.get().getFans()).isEqualTo(45_000L);
    assertThat(fromDb.get().getTier()).isEqualTo(WrestlerTier.CONTENDER);
  }

  @Test
  @DisplayName("Should handle bump system with persistence")
  void shouldHandleBumpSystemWithPersistence() {
    // Given
    Wrestler wrestler = wrestlerService.createWrestler("Test Wrestler", true, null);

    // When - Add bumps
    Assertions.assertNotNull(wrestler.getId());
    wrestlerService.addBump(wrestler.getId());
    wrestlerService.addBump(wrestler.getId());
    Optional<Wrestler> afterTwoBumps = wrestlerService.addBump(wrestler.getId());

    // Then - Third bump should trigger injury
    assertThat(afterTwoBumps).isPresent();
    assertThat(afterTwoBumps.get().getBumps()).isEqualTo(0); // Reset after injury

    // Verify persistence
    Optional<Wrestler> fromDb = wrestlerRepository.findById(wrestler.getId());
    assertThat(fromDb).isPresent();
    assertThat(fromDb.get().getBumps()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should spend fans and update tier")
  void shouldSpendFansAndUpdateTier() {
    // Given
    Wrestler wrestler = wrestlerService.createWrestler("Test Wrestler", true, null);
    Assertions.assertNotNull(wrestler.getId());
    wrestlerService.awardFans(wrestler.getId(), 50_000L); // Contender tier

    // When
    boolean success = wrestlerService.spendFans(wrestler.getId(), 15_000L);

    // Then
    assertThat(success).isTrue();

    // Verify persistence and tier update
    Optional<Wrestler> fromDb = wrestlerRepository.findById(wrestler.getId());
    assertThat(fromDb).isPresent();
    assertThat(fromDb.get().getFans()).isEqualTo(35_000L);
    assertThat(fromDb.get().getTier()).isEqualTo(WrestlerTier.RISER);
  }

  @Test
  @DisplayName("Should filter wrestlers by eligibility")
  void shouldFilterWrestlersByEligibility() {
    int initialEligibleRookieWrestlers =
        wrestlerService.getEligibleWrestlers(WrestlerTier.ROOKIE).size();
    int initialEligibleMainEventerWrestlers =
        wrestlerService.getEligibleWrestlers(WrestlerTier.MAIN_EVENTER).size();
    // Given - Create wrestlers with different fan levels
    wrestlerService.createWrestler("Rookie", true, null);
    // rookie has 0 fans (Rookie tier)

    Wrestler riser = wrestlerService.createWrestler("Riser", true, null);
    Assertions.assertNotNull(riser.getId());
    wrestlerService.awardFans(riser.getId(), 30_000L); // Riser tier

    Wrestler contender = wrestlerService.createWrestler("Contender", true, null);
    Assertions.assertNotNull(contender.getId());
    wrestlerService.awardFans(contender.getId(), 45_000L); // Contender tier

    Wrestler mainEventer = wrestlerService.createWrestler("Main Eventer", true, null);
    Assertions.assertNotNull(mainEventer.getId());
    wrestlerService.awardFans(mainEventer.getId(), 120_000L); // Main Eventer tier

    // When
    List<Wrestler> extremeEligible = wrestlerService.getEligibleWrestlers(WrestlerTier.ROOKIE);
    List<Wrestler> worldEligible = wrestlerService.getEligibleWrestlers(WrestlerTier.MAIN_EVENTER);

    // Then
    assertThat(extremeEligible)
        .hasSize(initialEligibleRookieWrestlers + 4); // Rookie, Riser, Contender, Main Eventer
    assertThat(extremeEligible)
        .extracting(Wrestler::getName)
        .contains("Rookie", "Riser", "Contender", "Main Eventer");

    assertThat(worldEligible).hasSize(initialEligibleMainEventerWrestlers + 1); // Only Main Eventer
    assertThat(worldEligible).extracting(Wrestler::getName).contains("Main Eventer");
  }

  @Test
  @DisplayName("Should filter wrestlers by tier")
  void shouldFilterWrestlersByTier() {
    // Given
    wrestlerService.createWrestler("Rookie 1", true, null);
    wrestlerService.createWrestler("Rookie 2", true, null);

    Wrestler riser = wrestlerService.createWrestler("Riser", true, null);
    Assertions.assertNotNull(riser.getId());
    wrestlerService.awardFans(riser.getId(), 30_000L);

    Wrestler contender = wrestlerService.createWrestler("Contender", true, null);
    Assertions.assertNotNull(contender.getId());
    wrestlerService.awardFans(contender.getId(), 45_000L);

    // When
    List<Wrestler> rookies = wrestlerService.getWrestlersByTier(WrestlerTier.ROOKIE);
    List<Wrestler> risers = wrestlerService.getWrestlersByTier(WrestlerTier.RISER);
    List<Wrestler> contenders = wrestlerService.getWrestlersByTier(WrestlerTier.CONTENDER);

    // Then
    assertThat(rookies).extracting(Wrestler::getName).contains("Rookie 1", "Rookie 2");

    assertThat(risers).hasSize(1);
    assertThat(risers).extracting(Wrestler::getName).containsExactly("Riser");

    assertThat(contenders).hasSize(1);
    assertThat(contenders).extracting(Wrestler::getName).containsExactly("Contender");
  }

  @Test
  @DisplayName("Should filter wrestlers by player status")
  void shouldFilterWrestlersByPlayerStatus() {
    int initialSize = wrestlerService.getPlayerWrestlers().size();
    // Given
    wrestlerService.createWrestler("Player 1", true, null);
    wrestlerService.createWrestler("Player 2", true, null);
    wrestlerService.createWrestler("NPC 1", false, null);
    wrestlerService.createWrestler("NPC 2", false, null);

    // When
    List<Wrestler> players = wrestlerService.getPlayerWrestlers();
    List<Wrestler> npcs = wrestlerService.getNpcWrestlers();

    // Then
    assertThat(players).hasSize(initialSize + 2);
    assertThat(players).extracting(Wrestler::getName).contains("Player 1", "Player 2");

    assertThat(npcs).extracting(Wrestler::getName).contains("NPC 1", "NPC 2");
  }

  @Test
  @DisplayName("Should maintain data integrity across complex operations")
  void shouldMaintainDataIntegrityAcrossComplexOperations() {
    // Given
    Wrestler wrestler =
        wrestlerService.createWrestler(
            "Complex Test", true, "Test wrestler for complex operations");

    // When - Perform multiple operations
    Assertions.assertNotNull(wrestler.getId());
    wrestlerService.awardFans(wrestler.getId(), 60_000L); // Intertemporal tier
    wrestlerService.addBump(wrestler.getId());
    wrestlerService.spendFans(wrestler.getId(), 10_000L); // Still Intertemporal
    wrestlerService.addBump(wrestler.getId());

    // Then - Verify final state
    Assertions.assertNotNull(wrestler.getId());
    Optional<Wrestler> finalState = wrestlerRepository.findById(wrestler.getId());
    assertThat(finalState).isPresent();

    Wrestler finalWrestler = finalState.get();
    assertThat(finalWrestler.getFans()).isEqualTo(50_000L);
    assertThat(finalWrestler.getTier())
        .isEqualTo(WrestlerTier.CONTENDER); // Dropped from Intertemporal
    assertThat(finalWrestler.getBumps()).isEqualTo(2);
    // Calculate expected health manually to avoid lazy loading issues
    int expectedHealth = finalWrestler.getStartingHealth() - finalWrestler.getBumps();
    assertThat(expectedHealth).isEqualTo(13); // 15 - 2 bumps
    assertThat(finalWrestler.isEligibleForTitle(WrestlerTier.RISER)).isTrue();
    assertThat(finalWrestler.isEligibleForTitle(WrestlerTier.MIDCARDER)).isFalse();
    assertThat(finalWrestler.getDescription()).isEqualTo("Test wrestler for complex operations");
  }
}
