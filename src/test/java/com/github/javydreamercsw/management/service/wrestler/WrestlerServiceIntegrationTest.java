package com.github.javydreamercsw.management.service.wrestler;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStats;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class WrestlerServiceIntegrationTest extends ManagementIntegrationTest {

  @Autowired private SegmentService segmentService;
  @Autowired private TitleService titleService;
  @Autowired private ShowService showService;
  @Autowired private SeasonService seasonService;
  @Autowired private SegmentTypeService segmentTypeService;
  @Autowired private ShowTypeService showTypeService;

  @Test
  @DisplayName("Should get wrestler stats")
  @Transactional
  void shouldGetWrestlerStats() {
    // Given
    Wrestler wrestler = wrestlerService.createWrestler("Stat Test", true, null);
    Assertions.assertNotNull(wrestler.getId());
    // Retrieve the wrestler again to ensure it's a managed entity
    wrestler = wrestlerService.findById(wrestler.getId()).orElseThrow();

    // Create a season and show for context
    Season season = seasonService.createSeason("Test Season", "Test Season", 5);
    ShowType showType = showTypeService.createOrUpdateShowType("Weekly", "Weekly Show");
    Show show =
        showService.createShow(
            "Test Show", "Test Show", showType.getId(), null, season.getId(), null);

    // Create some segments
    SegmentType matchType = segmentTypeService.findByName("One on One").get();
    Segment winSegment = segmentService.createSegment(show, matchType, Instant.now());
    winSegment.addParticipant(wrestler);
    winSegment.setWinners(List.of(wrestler));
    segmentService.updateSegment(winSegment);

    Segment lossSegment = segmentService.createSegment(show, matchType, Instant.now());
    lossSegment.addParticipant(wrestler);
    Wrestler opponent = wrestlerService.createWrestler("Opponent", false, null);
    lossSegment.addParticipant(opponent);
    lossSegment.setWinners(List.of(opponent));
    segmentService.updateSegment(lossSegment);

    // Create a title and have the wrestler win it
    Title title = titleService.createTitle("Test Title", "Test Title", WrestlerTier.ROOKIE);
    titleService.awardTitleTo(title, List.of(wrestler));
    wrestlerRepository.flush();

    // When
    Optional<WrestlerStats> stats = wrestlerService.getWrestlerStats(wrestler.getId());

    // Then
    assertThat(stats).isPresent();
    assertThat(stats.get().getWins()).isEqualTo(1);
    assertThat(stats.get().getLosses()).isEqualTo(1);
    assertThat(stats.get().getTitlesHeld()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should create wrestler with ATW RPG defaults")
  @Transactional
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
  @Transactional
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
  @Transactional
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
  @Transactional
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
  @Transactional
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
  @Transactional
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
  @Transactional
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
  @Transactional
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
