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
package com.github.javydreamercsw.management.service.wrestler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javydreamercsw.base.domain.wrestler.TierBoundaryRepository;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerStats;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.DataInitializer;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxRepository;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.ranking.TierBoundaryService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.transaction.annotation.Transactional;

@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class WrestlerServiceIT extends ManagementIntegrationTest {

  @Autowired private SegmentService segmentService;
  @Autowired private TitleService titleService;
  @Autowired private ShowService showService;
  @Autowired private SeasonService seasonService;
  @Autowired private SegmentTypeService segmentTypeService;
  @Autowired private ShowTypeService showTypeService;
  @Autowired private InboxRepository inboxRepository;
  @Autowired private DataInitializer dataInitializer;
  @Autowired private InjuryService injuryService;
  @Autowired private WrestlerStateRepository wrestlerStateRepository;
  @Autowired private UniverseRepository universeRepository;

  @Autowired private TierBoundaryRepository tierBoundaryRepository;

  @Autowired private TierBoundaryService tierBoundaryService;

  private Universe defaultUniverse;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
    tierBoundaryRepository.deleteAllInBatch();
    tierBoundaryService.resetTierBoundaries();
    dataInitializer.init();

    defaultUniverse =
        universeRepository
            .findById(1L)
            .orElseGet(
                () ->
                    universeRepository.save(
                        Universe.builder().id(1L).name("Default Universe").build()));
  }

  private static final Long DEFAULT_UNIVERSE_ID = 1L;

  @Test
  @DisplayName("Should get wrestler stats")
  @Transactional
  void shouldGetWrestlerStats() {
    // Given
    Wrestler wrestler =
        wrestlerService.createWrestler(
            "Stat Test", true, null, WrestlerTier.ROOKIE, defaultUniverse);
    Assertions.assertNotNull(wrestler.getId());
    // Retrieve the wrestler again to ensure it's a managed entity
    wrestler = wrestlerService.findById(wrestler.getId()).orElseThrow();

    // Create a season and show for context
    Season season = seasonService.createSeason("Test Season", "Test Season", 5);
    ShowType showType = showTypeService.createOrUpdateShowType("Weekly", "Weekly Show", 5, 2);
    Show show =
        showService.createShow(
            "Test Show",
            "Test Show",
            showType.getId(),
            null,
            season.getId(),
            null,
            DEFAULT_UNIVERSE_ID,
            null,
            null); // Added null for arenaId

    // Create some segments
    SegmentType matchType = segmentTypeService.findByName("One on One").get();
    Segment winSegment = segmentService.createSegment(show, matchType, Instant.now());
    winSegment.addParticipant(wrestler);
    winSegment.setWinners(List.of(wrestler));
    segmentService.updateSegment(winSegment);

    Segment lossSegment = segmentService.createSegment(show, matchType, Instant.now());
    lossSegment.addParticipant(wrestler);
    Wrestler opponent =
        wrestlerService.createWrestler(
            "Opponent", false, null, WrestlerTier.ROOKIE, defaultUniverse);
    lossSegment.addParticipant(opponent);
    lossSegment.setWinners(List.of(opponent));
    segmentService.updateSegment(lossSegment);

    // Create a title and have the wrestler win it
    Title title =
        titleService.createTitle(
            "Test Title",
            "Test Title",
            WrestlerTier.ROOKIE,
            ChampionshipType.SINGLE,
            DEFAULT_UNIVERSE_ID);
    titleService.awardTitleTo(title, List.of(wrestler));
    wrestlerRepository.flush();

    // When
    Optional<WrestlerStats> stats =
        wrestlerService.getWrestlerStats(wrestler.getId(), DEFAULT_UNIVERSE_ID);

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
    Wrestler wrestler =
        wrestlerService.createWrestler(
            "Test Wrestler", true, "Test description", WrestlerTier.ROOKIE, defaultUniverse);
    var state = wrestlerService.getOrCreateState(wrestler.getId(), DEFAULT_UNIVERSE_ID);

    // Then
    assertThat(wrestler.getId()).isNotNull();
    assertThat(wrestler.getName()).isEqualTo("Test Wrestler");
    assertThat(state.getFans()).isEqualTo(0L);
    assertThat(state.getTier()).isEqualTo(WrestlerTier.ROOKIE);
    assertThat(state.getBumps()).isEqualTo(0);
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
    Wrestler wrestler =
        wrestlerService.createWrestler(
            "Test Wrestler", true, null, WrestlerTier.ROOKIE, defaultUniverse);
    var initialState = wrestlerService.getOrCreateState(wrestler.getId(), DEFAULT_UNIVERSE_ID);
    assertThat(initialState.getTier()).isEqualTo(WrestlerTier.ROOKIE);

    // When - Award enough fans to reach Contender tier
    Assertions.assertNotNull(wrestler.getId());
    var updated = wrestlerService.awardFans(wrestler.getId(), DEFAULT_UNIVERSE_ID, 45_000L);

    // Then
    assertThat(updated).isPresent();
    assertThat(updated.get().getFans()).isEqualTo(45_000L);
    assertThat(updated.get().getTier()).isEqualTo(WrestlerTier.CONTENDER);

    // Verify persistence
    var fromDb = wrestlerService.getOrCreateState(wrestler.getId(), DEFAULT_UNIVERSE_ID);
    assertThat(fromDb.getFans()).isEqualTo(45_000L);
    assertThat(fromDb.getTier()).isEqualTo(WrestlerTier.CONTENDER);
  }

  @Test
  @DisplayName("Should handle bump system with persistence")
  @Transactional
  void shouldHandleBumpSystemWithPersistence() {
    // Given
    Wrestler wrestler =
        wrestlerService.createWrestler(
            "Test Wrestler", true, null, WrestlerTier.ROOKIE, defaultUniverse);

    // When - Add bumps
    Assertions.assertNotNull(wrestler.getId());
    wrestlerService.addBump(wrestler.getId(), DEFAULT_UNIVERSE_ID);
    wrestlerService.addBump(wrestler.getId(), DEFAULT_UNIVERSE_ID);
    var afterTwoBumps = wrestlerService.addBump(wrestler.getId(), DEFAULT_UNIVERSE_ID);

    // Then - Third bump should trigger injury
    assertThat(afterTwoBumps).isPresent();
    assertThat(afterTwoBumps.get().getBumps()).isEqualTo(0); // Reset after injury

    // Verify persistence
    var fromDb = wrestlerService.getOrCreateState(wrestler.getId(), DEFAULT_UNIVERSE_ID);
    assertThat(fromDb.getBumps()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should spend fans and update tier")
  @Transactional
  void shouldSpendFansAndUpdateTier() {
    // Given
    Wrestler wrestler =
        wrestlerService.createWrestler(
            "Test Wrestler", true, null, WrestlerTier.ROOKIE, defaultUniverse);
    Assertions.assertNotNull(wrestler.getId());
    wrestlerService.awardFans(wrestler.getId(), DEFAULT_UNIVERSE_ID, 50_000L); // Contender tier

    // When
    boolean success = wrestlerService.spendFans(wrestler.getId(), DEFAULT_UNIVERSE_ID, 15_000L);

    // Then
    assertThat(success).isTrue();

    // Verify persistence and tier update
    var fromDb = wrestlerService.getOrCreateState(wrestler.getId(), DEFAULT_UNIVERSE_ID);
    assertThat(fromDb.getFans()).isEqualTo(35_000L);
    assertThat(fromDb.getTier()).isEqualTo(WrestlerTier.RISER);
  }

  @Test
  @DisplayName("Should filter wrestlers by tier")
  @Transactional
  void shouldFilterWrestlersByTier() {
    // Given
    wrestlerService.createWrestler("Rookie 1", true, null, WrestlerTier.ROOKIE, defaultUniverse);
    wrestlerService.createWrestler("Rookie 2", true, null, WrestlerTier.ROOKIE, defaultUniverse);

    Wrestler riser =
        wrestlerService.createWrestler("Riser", true, null, WrestlerTier.ROOKIE, defaultUniverse);
    Assertions.assertNotNull(riser.getId());
    wrestlerService.awardFans(riser.getId(), DEFAULT_UNIVERSE_ID, 30_000L);

    Wrestler contender =
        wrestlerService.createWrestler(
            "Contender", true, null, WrestlerTier.ROOKIE, defaultUniverse);
    Assertions.assertNotNull(contender.getId());
    wrestlerService.awardFans(contender.getId(), DEFAULT_UNIVERSE_ID, 45_000L);

    // When
    List<Wrestler> rookies =
        wrestlerService.getWrestlersByTier(WrestlerTier.ROOKIE, DEFAULT_UNIVERSE_ID);
    List<Wrestler> risers =
        wrestlerService.getWrestlersByTier(WrestlerTier.RISER, DEFAULT_UNIVERSE_ID);
    List<Wrestler> contenders =
        wrestlerService.getWrestlersByTier(WrestlerTier.CONTENDER, DEFAULT_UNIVERSE_ID);

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
    wrestlerService.createWrestler("Player 1", true, null, WrestlerTier.ROOKIE, defaultUniverse);
    wrestlerService.createWrestler("Player 2", true, null, WrestlerTier.ROOKIE, defaultUniverse);
    wrestlerService.createWrestler("NPC 1", false, null, WrestlerTier.ROOKIE, defaultUniverse);
    wrestlerService.createWrestler("NPC 2", false, null, WrestlerTier.ROOKIE, defaultUniverse);

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
            "Complex Test",
            true,
            "Test wrestler for complex operations",
            WrestlerTier.ROOKIE,
            defaultUniverse);

    // When - Perform multiple operations
    Assertions.assertNotNull(wrestler.getId());
    wrestlerService.awardFans(wrestler.getId(), DEFAULT_UNIVERSE_ID, 60_000L); // Intertemporal tier
    wrestlerService.addBump(wrestler.getId(), DEFAULT_UNIVERSE_ID);
    wrestlerService.spendFans(
        wrestler.getId(), DEFAULT_UNIVERSE_ID, 10_000L); // Still Intertemporal
    wrestlerService.addBump(wrestler.getId(), DEFAULT_UNIVERSE_ID);

    // Then - Verify final state
    Assertions.assertNotNull(wrestler.getId());
    var finalState = wrestlerService.getOrCreateState(wrestler.getId(), DEFAULT_UNIVERSE_ID);

    assertThat(finalState.getFans()).isEqualTo(50_000L);
    assertThat(finalState.getTier())
        .isEqualTo(WrestlerTier.CONTENDER); // Dropped from Intertemporal
    assertThat(finalState.getBumps()).isEqualTo(2);

    assertThat(finalState.getTier()).isEqualTo(WrestlerTier.CONTENDER);
    assertThat(wrestler.getDescription()).isEqualTo("Test wrestler for complex operations");
  }

  @Test
  @DisplayName("Should create InboxItem on FanAwardedEvent")
  @Transactional
  void shouldCreateInboxItemOnFanAwardedEvent() {
    // Given
    Wrestler wrestler =
        wrestlerService.createWrestler(
            "Inbox Test Wrestler", true, null, WrestlerTier.ROOKIE, defaultUniverse);
    Assertions.assertNotNull(wrestler.getId());
    long initialInboxItemCount = inboxRepository.count();

    // When
    wrestlerService.awardFans(wrestler.getId(), DEFAULT_UNIVERSE_ID, 1_000L);

    // Then
    assertThat(inboxRepository.count()).isEqualTo(initialInboxItemCount + 1);
    InboxItem inboxItem = inboxRepository.findAll().get(0);
    assertThat(inboxItem.getDescription()).contains("Inbox Test Wrestler gained 1000 fans");
    assertThat(inboxItem.getTargets()).hasSize(1);
    assertThat(inboxItem.getTargets().get(0).getTargetId()).isEqualTo(wrestler.getId().toString());
  }

  @Test
  void shouldRecalibrateFanCounts() {
    // Create some wrestlers
    Wrestler icon =
        wrestlerService.createWrestler(
            "The Icon", false, "Iconic wrestler", WrestlerTier.ICON, defaultUniverse);
    var iconState = wrestlerService.getOrCreateState(icon.getId(), DEFAULT_UNIVERSE_ID);
    iconState.setFans(1_000_000L);
    wrestlerService.save(icon);

    Wrestler mainEventer =
        wrestlerService.createWrestler(
            "The Main Eventer", false, "Top star", WrestlerTier.MAIN_EVENTER, defaultUniverse);
    var meState = wrestlerService.getOrCreateState(mainEventer.getId(), DEFAULT_UNIVERSE_ID);
    meState.setFans(800_000L);
    wrestlerService.save(mainEventer);

    Wrestler rookie =
        wrestlerService.createWrestler(
            "The Rookie", false, "Newcomer", WrestlerTier.ROOKIE, defaultUniverse);
    var rookieState = wrestlerService.getOrCreateState(rookie.getId(), DEFAULT_UNIVERSE_ID);
    rookieState.setFans(5_000L);
    wrestlerService.save(rookie);

    // Reset fan counts
    wrestlerService.recalibrateFanCounts(DEFAULT_UNIVERSE_ID);

    // Verify Icon is now Main Eventer with min fans
    var updatedIconState = wrestlerService.getOrCreateState(icon.getId(), DEFAULT_UNIVERSE_ID);
    assertEquals(WrestlerTier.MAIN_EVENTER, updatedIconState.getTier());
    assertThat(updatedIconState.getFans()).isGreaterThanOrEqualTo(0L);

    // Verify Main Eventer is reset to min fans
    var updatedMeState = wrestlerService.getOrCreateState(mainEventer.getId(), DEFAULT_UNIVERSE_ID);
    assertEquals(WrestlerTier.MAIN_EVENTER.getMinFans(), updatedMeState.getFans());

    // Verify Rookie is reset to min fans
    var updatedRookieState = wrestlerService.getOrCreateState(rookie.getId(), DEFAULT_UNIVERSE_ID);
    assertEquals(0L, updatedRookieState.getFans());
  }

  @Test
  void shouldRecalibrateIconToMainEventer() {
    // Create an Icon wrestler
    Wrestler icon =
        wrestlerService.createWrestler(
            "The Icon", false, "Iconic wrestler", WrestlerTier.ICON, defaultUniverse);
    var iconState = wrestlerService.getOrCreateState(icon.getId(), DEFAULT_UNIVERSE_ID);
    iconState.setFans(1_000_000L);
    wrestlerService.save(icon);

    // Reset fan counts
    wrestlerService.recalibrateFanCounts(DEFAULT_UNIVERSE_ID);

    // Verify the wrestler is now a Main Eventer with min fans
    var updatedState = wrestlerService.getOrCreateState(icon.getId(), DEFAULT_UNIVERSE_ID);
    assertEquals(WrestlerTier.MAIN_EVENTER, updatedState.getTier());
    assertEquals(WrestlerTier.MAIN_EVENTER.getMinFans(), updatedState.getFans());
  }

  @Test
  void shouldResetAllFanCountsToZero() {
    // Create some wrestlers
    Wrestler icon =
        wrestlerService.createWrestler(
            "The Icon", false, "Iconic wrestler", WrestlerTier.ICON, defaultUniverse);
    var iconState = wrestlerService.getOrCreateState(icon.getId(), DEFAULT_UNIVERSE_ID);
    iconState.setFans(1_000_000L);

    Wrestler mainEventer =
        wrestlerService.createWrestler(
            "The Main Eventer", false, "Top star", WrestlerTier.MAIN_EVENTER, defaultUniverse);
    var meState = wrestlerService.getOrCreateState(mainEventer.getId(), DEFAULT_UNIVERSE_ID);
    meState.setFans(800_000L);

    Wrestler rookie =
        wrestlerService.createWrestler(
            "The Rookie", false, "Newcomer", WrestlerTier.ROOKIE, defaultUniverse);
    var rookieState = wrestlerService.getOrCreateState(rookie.getId(), DEFAULT_UNIVERSE_ID);
    rookieState.setFans(5_000L);

    // Reset fan counts
    wrestlerService.resetAllFanCountsToZero(DEFAULT_UNIVERSE_ID);

    // Verify all wrestlers are now rookies with 0 fans
    var updatedIconState = wrestlerService.getOrCreateState(icon.getId(), DEFAULT_UNIVERSE_ID);
    assertEquals(WrestlerTier.ROOKIE, updatedIconState.getTier());
    assertEquals(0L, updatedIconState.getFans());

    var updatedMeState = wrestlerService.getOrCreateState(mainEventer.getId(), DEFAULT_UNIVERSE_ID);
    assertEquals(WrestlerTier.ROOKIE, updatedMeState.getTier());
    assertEquals(0L, updatedMeState.getFans());

    var updatedRookieState = wrestlerService.getOrCreateState(rookie.getId(), DEFAULT_UNIVERSE_ID);
    assertEquals(WrestlerTier.ROOKIE, updatedRookieState.getTier());
    assertEquals(0L, updatedRookieState.getFans());
  }

  @Test
  @DisplayName("Should award fans and persist changes")
  void testAwardFans_PersistsChanges() {
    // Given
    Wrestler wrestler =
        wrestlerService.createWrestler(
            "Test Wrestler", true, null, WrestlerTier.ROOKIE, defaultUniverse);
    Assertions.assertNotNull(wrestler.getId());
    var state = wrestlerService.getOrCreateState(wrestler.getId(), DEFAULT_UNIVERSE_ID);
    state.setFans(100L);
    state.setTier(WrestlerTier.ROOKIE);
    wrestlerService.save(wrestler); // Save initial state

    long fansToAdd = 1234;
    long expectedFans = 100L + 1000L; // It rounds to nearest 1000. 1234 -> 1000

    // When
    wrestlerService.awardFans(wrestler.getId(), DEFAULT_UNIVERSE_ID, fansToAdd);

    // Then
    var updatedState = wrestlerService.getOrCreateState(wrestler.getId(), DEFAULT_UNIVERSE_ID);
    assertEquals(expectedFans, updatedState.getFans());
  }

  @Test
  @DisplayName("Should add bump and persist changes")
  void testAddBump_PersistsChanges_Increment() {
    // Given
    Wrestler wrestler =
        wrestlerService.createWrestler(
            "Test Wrestler", true, null, WrestlerTier.ROOKIE, defaultUniverse);
    Assertions.assertNotNull(wrestler.getId());
    var state = wrestlerService.getOrCreateState(wrestler.getId(), DEFAULT_UNIVERSE_ID);
    state.setBumps(1);
    wrestlerService.save(wrestler); // Save initial state

    // When
    wrestlerService.addBump(wrestler.getId(), DEFAULT_UNIVERSE_ID);

    // Then
    var updatedState = wrestlerService.getOrCreateState(wrestler.getId(), DEFAULT_UNIVERSE_ID);
    assertEquals(2, updatedState.getBumps());
  }

  @Test
  @DisplayName("Should add bump and reset to 0 and create an injury after 3 bumps")
  void testAddBump_PersistsChanges() {
    // Given
    Wrestler wrestler =
        wrestlerService.createWrestler(
            "Test Wrestler", true, null, WrestlerTier.ROOKIE, defaultUniverse);
    Assertions.assertNotNull(wrestler.getId());
    var state = wrestlerService.getOrCreateState(wrestler.getId(), DEFAULT_UNIVERSE_ID);
    state.setBumps(2);
    wrestlerService.save(wrestler); // Save initial state
    long initialInjuryCount =
        injuryService.getAllInjuriesForWrestler(wrestler.getId(), DEFAULT_UNIVERSE_ID).size();

    // When
    wrestlerService.addBump(wrestler.getId(), DEFAULT_UNIVERSE_ID);

    // Then
    var updatedState = wrestlerService.getOrCreateState(wrestler.getId(), DEFAULT_UNIVERSE_ID);
    assertEquals(0, updatedState.getBumps());

    // Verify an injury was created
    long newInjuryCount =
        injuryService.getAllInjuriesForWrestler(wrestler.getId(), DEFAULT_UNIVERSE_ID).size();
    assertEquals(initialInjuryCount + 1, newInjuryCount);
  }

  @Test
  @DisplayName("Should heal bump and persist changes")
  void testHealBump_PersistsChanges() {
    // Given
    Wrestler wrestler =
        wrestlerService.createWrestler(
            "Test Wrestler", true, null, WrestlerTier.ROOKIE, defaultUniverse);
    Assertions.assertNotNull(wrestler.getId());
    var state = wrestlerService.getOrCreateState(wrestler.getId(), DEFAULT_UNIVERSE_ID);
    state.setBumps(2);
    wrestlerService.save(wrestler); // Save initial state

    // When
    wrestlerService.healBump(wrestler.getId(), DEFAULT_UNIVERSE_ID);

    // Then
    var updatedState = wrestlerService.getOrCreateState(wrestler.getId(), DEFAULT_UNIVERSE_ID);
    assertEquals(1, updatedState.getBumps());
  }
}
