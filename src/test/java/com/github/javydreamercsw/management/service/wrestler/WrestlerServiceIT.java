package com.github.javydreamercsw.management.service.wrestler;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.TestcontainersConfiguration;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.wrestler.TitleTier;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for WrestlerService ATW RPG functionality. Tests the complete service layer
 * with real database interactions.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("WrestlerService Integration Tests")
class WrestlerServiceIT {

  @Autowired WrestlerService wrestlerService;
  @Autowired WrestlerRepository wrestlerRepository;
  @Autowired DeckRepository deckRepository;

  @AfterEach
  void cleanUp() {
    // Delete in correct order to avoid foreign key constraint violations
    deckRepository.deleteAll();
    wrestlerRepository.deleteAll();
  }

  @Test
  @DisplayName("Should create wrestler with ATW RPG defaults")
  void shouldCreateWrestlerWithAtwRpgDefaults() {
    // When
    Wrestler wrestler =
        wrestlerService.createAtwWrestler("Test Wrestler", true, "Test description", "High-Flying");

    // Then
    assertThat(wrestler.getId()).isNotNull();
    assertThat(wrestler.getName()).isEqualTo("Test Wrestler");
    assertThat(wrestler.getFans()).isEqualTo(0L);
    assertThat(wrestler.getTier()).isEqualTo(WrestlerTier.ROOKIE);
    assertThat(wrestler.getBumps()).isEqualTo(0);
    assertThat(wrestler.getIsPlayer()).isTrue();
    assertThat(wrestler.getDescription()).isEqualTo("Test description");
    assertThat(wrestler.getWrestlingStyle()).isEqualTo("High-Flying");
    assertThat(wrestler.getDeckSize()).isEqualTo(15);
    assertThat(wrestler.getStartingHealth()).isEqualTo(15);
  }

  @Test
  @DisplayName("Should award fans and persist tier changes")
  void shouldAwardFansAndPersistTierChanges() {
    // Given
    Wrestler wrestler = wrestlerService.createAtwWrestler("Test Wrestler", true, null, null);
    assertThat(wrestler.getTier()).isEqualTo(WrestlerTier.ROOKIE);

    // When - Award enough fans to reach Contender tier
    Optional<Wrestler> updated = wrestlerService.awardFans(wrestler.getId(), 45000L);

    // Then
    assertThat(updated).isPresent();
    assertThat(updated.get().getFans()).isEqualTo(45000L);
    assertThat(updated.get().getTier()).isEqualTo(WrestlerTier.CONTENDER);

    // Verify persistence
    Optional<Wrestler> fromDb = wrestlerRepository.findById(wrestler.getId());
    assertThat(fromDb).isPresent();
    assertThat(fromDb.get().getFans()).isEqualTo(45000L);
    assertThat(fromDb.get().getTier()).isEqualTo(WrestlerTier.CONTENDER);
  }

  @Test
  @DisplayName("Should handle bump system with persistence")
  void shouldHandleBumpSystemWithPersistence() {
    // Given
    Wrestler wrestler = wrestlerService.createAtwWrestler("Test Wrestler", true, null, null);

    // When - Add bumps
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
    Wrestler wrestler = wrestlerService.createAtwWrestler("Test Wrestler", true, null, null);
    wrestlerService.awardFans(wrestler.getId(), 50000L); // Contender tier

    // When
    boolean success = wrestlerService.spendFans(wrestler.getId(), 15000L);

    // Then
    assertThat(success).isTrue();

    // Verify persistence and tier update
    Optional<Wrestler> fromDb = wrestlerRepository.findById(wrestler.getId());
    assertThat(fromDb).isPresent();
    assertThat(fromDb.get().getFans()).isEqualTo(35000L);
    assertThat(fromDb.get().getTier()).isEqualTo(WrestlerTier.RISER);
  }

  @Test
  @DisplayName("Should filter wrestlers by eligibility")
  void shouldFilterWrestlersByEligibility() {
    // Given - Create wrestlers with different fan levels
    Wrestler rookie = wrestlerService.createAtwWrestler("Rookie", true, null, null);
    // rookie has 0 fans (Rookie tier)

    Wrestler riser = wrestlerService.createAtwWrestler("Riser", true, null, null);
    wrestlerService.awardFans(riser.getId(), 30000L); // Riser tier

    Wrestler contender = wrestlerService.createAtwWrestler("Contender", true, null, null);
    wrestlerService.awardFans(contender.getId(), 45000L); // Contender tier

    Wrestler mainEventer = wrestlerService.createAtwWrestler("Main Eventer", true, null, null);
    wrestlerService.awardFans(mainEventer.getId(), 120000L); // Main Eventer tier

    // When
    List<Wrestler> extremeEligible = wrestlerService.getEligibleWrestlers(TitleTier.EXTREME);
    List<Wrestler> worldEligible = wrestlerService.getEligibleWrestlers(TitleTier.WORLD);

    // Then
    assertThat(extremeEligible).hasSize(3); // Riser, Contender, Main Eventer
    assertThat(extremeEligible)
        .extracting(Wrestler::getName)
        .containsExactlyInAnyOrder("Riser", "Contender", "Main Eventer");

    assertThat(worldEligible).hasSize(1); // Only Main Eventer
    assertThat(worldEligible).extracting(Wrestler::getName).containsExactly("Main Eventer");
  }

  @Test
  @DisplayName("Should filter wrestlers by tier")
  void shouldFilterWrestlersByTier() {
    // Given
    Wrestler rookie1 = wrestlerService.createAtwWrestler("Rookie 1", true, null, null);
    Wrestler rookie2 = wrestlerService.createAtwWrestler("Rookie 2", true, null, null);

    Wrestler riser = wrestlerService.createAtwWrestler("Riser", true, null, null);
    wrestlerService.awardFans(riser.getId(), 30000L);

    Wrestler contender = wrestlerService.createAtwWrestler("Contender", true, null, null);
    wrestlerService.awardFans(contender.getId(), 45000L);

    // When
    List<Wrestler> rookies = wrestlerService.getWrestlersByTier(WrestlerTier.ROOKIE);
    List<Wrestler> risers = wrestlerService.getWrestlersByTier(WrestlerTier.RISER);
    List<Wrestler> contenders = wrestlerService.getWrestlersByTier(WrestlerTier.CONTENDER);

    // Then
    assertThat(rookies).hasSize(2);
    assertThat(rookies)
        .extracting(Wrestler::getName)
        .containsExactlyInAnyOrder("Rookie 1", "Rookie 2");

    assertThat(risers).hasSize(1);
    assertThat(risers).extracting(Wrestler::getName).containsExactly("Riser");

    assertThat(contenders).hasSize(1);
    assertThat(contenders).extracting(Wrestler::getName).containsExactly("Contender");
  }

  @Test
  @DisplayName("Should filter wrestlers by player status")
  void shouldFilterWrestlersByPlayerStatus() {
    // Given
    Wrestler player1 = wrestlerService.createAtwWrestler("Player 1", true, null, null);
    Wrestler player2 = wrestlerService.createAtwWrestler("Player 2", true, null, null);
    Wrestler npc1 = wrestlerService.createAtwWrestler("NPC 1", false, null, null);
    Wrestler npc2 = wrestlerService.createAtwWrestler("NPC 2", false, null, null);

    // When
    List<Wrestler> players = wrestlerService.getPlayerWrestlers();
    List<Wrestler> npcs = wrestlerService.getNpcWrestlers();

    // Then
    assertThat(players).hasSize(2);
    assertThat(players)
        .extracting(Wrestler::getName)
        .containsExactlyInAnyOrder("Player 1", "Player 2");

    assertThat(npcs).hasSize(2);
    assertThat(npcs).extracting(Wrestler::getName).containsExactlyInAnyOrder("NPC 1", "NPC 2");
  }

  @Test
  @DisplayName("Should maintain data integrity across complex operations")
  void shouldMaintainDataIntegrityAcrossComplexOperations() {
    // Given
    Wrestler wrestler =
        wrestlerService.createAtwWrestler(
            "Complex Test", true, "Test wrestler for complex operations", "Technical");

    // When - Perform multiple operations
    wrestlerService.awardFans(wrestler.getId(), 60000L); // Intertemporal tier
    wrestlerService.addBump(wrestler.getId());
    wrestlerService.spendFans(wrestler.getId(), 10000L); // Still Intertemporal
    wrestlerService.addBump(wrestler.getId());

    // Then - Verify final state
    Optional<Wrestler> finalState = wrestlerRepository.findById(wrestler.getId());
    assertThat(finalState).isPresent();

    Wrestler finalWrestler = finalState.get();
    assertThat(finalWrestler.getFans()).isEqualTo(50000L);
    assertThat(finalWrestler.getTier())
        .isEqualTo(WrestlerTier.CONTENDER); // Dropped from Intertemporal
    assertThat(finalWrestler.getBumps()).isEqualTo(2);
    assertThat(finalWrestler.getEffectiveStartingHealth()).isEqualTo(13); // 15 - 2 bumps
    assertThat(finalWrestler.isEligibleForTitle(TitleTier.TAG_TEAM)).isTrue();
    assertThat(finalWrestler.isEligibleForTitle(TitleTier.INTERTEMPORAL)).isFalse();
    assertThat(finalWrestler.getDescription()).isEqualTo("Test wrestler for complex operations");
    assertThat(finalWrestler.getWrestlingStyle()).isEqualTo("Technical");
  }
}
