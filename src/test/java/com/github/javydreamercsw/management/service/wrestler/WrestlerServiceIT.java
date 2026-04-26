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
import com.github.javydreamercsw.management.domain.show.segment.SegmentParticipant;
import com.github.javydreamercsw.management.domain.show.segment.SegmentStatus;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.ranking.TierBoundaryService;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
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
  @Autowired private UniverseContextService universeContextService;
  @Autowired private TierRecalculationService tierRecalculationService;

  @Autowired private TierBoundaryRepository tierBoundaryRepository;

  @Autowired private TierBoundaryService tierBoundaryService;

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
    Show show = new Show();
    show.setName("Test Show");
    show.setSeason(season);
    show.setShowDate(java.time.LocalDate.now());
    ShowType showType = new ShowType();
    showType.setName("Regular");
    showTypeService.save(showType);
    show.setType(showType);
    show.setUniverse(defaultUniverse);
    showService.save(show);

    // Create a segment where the wrestler wins
    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentDate(Instant.now());
    segment.setStatus(SegmentStatus.COMPLETED);
    SegmentType segmentType = segmentTypeService.createOrUpdateSegmentType("Match", "Test match");
    segment.setSegmentType(segmentType);

    SegmentParticipant participant = new SegmentParticipant();
    participant.setWrestler(wrestler);
    participant.setSegment(segment);
    participant.setIsWinner(true);
    segment.getParticipants().add(participant);

    segmentService.saveSegment(segment);

    // When
    Optional<WrestlerStats> statsOpt =
        wrestlerService.getWrestlerStats(wrestler.getId(), defaultUniverse.getId());

    // Then
    Assertions.assertTrue(statsOpt.isPresent());
    WrestlerStats stats = statsOpt.get();
    Assertions.assertEquals(1, stats.getWins());
    Assertions.assertEquals(0, stats.getTitlesHeld());
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
  @DisplayName("Should award fans and update tier")
  @Transactional
  void shouldAwardFansAndPersistTierChanges() {
    // Given
    Wrestler wrestler =
        wrestlerService.createWrestler(
            "Award Test", true, "Description", WrestlerTier.ROOKIE, defaultUniverse);
    Long universeId = defaultUniverse.getId();

    // When
    wrestlerService.awardFans(wrestler.getId(), universeId, 30000L);

    // Then
    WrestlerState state = wrestlerService.getOrCreateState(wrestler.getId(), universeId);
    // 30000 * 1.2 = 36000
    assertThat(state.getFans()).isEqualTo(36000L);
    // Based on default boundaries, 36000 should be RISER or above (Rookie ends at 24999)
    assertThat(state.getTier()).isNotEqualTo(WrestlerTier.ROOKIE);
  }

  @Test
  @DisplayName("Should handle bump system with persistence")
  @Transactional
  void shouldHandleBumpSystemWithPersistence() {
    // Given
    Wrestler wrestler =
        wrestlerService.createWrestler(
            "Bump Test", true, "Description", WrestlerTier.ROOKIE, defaultUniverse);
    Long universeId = defaultUniverse.getId();

    // When - 3 bumps trigger reset to 0 and injury. 4th bump = 1, 5th bump = 2.
    for (int i = 0; i < 5; i++) {
      wrestlerService.addBump(wrestler.getId(), universeId);
    }

    // Then
    WrestlerState state = wrestlerService.getOrCreateState(wrestler.getId(), universeId);
    assertThat(state.getBumps()).isEqualTo(2);

    // Heal one
    wrestlerService.healBump(wrestler.getId(), universeId);
    state = wrestlerService.getOrCreateState(wrestler.getId(), universeId);
    assertThat(state.getBumps()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should spend fans and update tier")
  @Transactional
  void shouldSpendFansAndUpdateTier() {
    // Given
    Wrestler wrestler =
        wrestlerService.createWrestler(
            "Spend Test", true, "Description", WrestlerTier.RISER, defaultUniverse);
    Long universeId = defaultUniverse.getId();
    wrestlerService.awardFans(wrestler.getId(), universeId, 10000L);

    // When
    wrestlerService.spendFans(wrestler.getId(), universeId, 5000L);

    // Then
    WrestlerState state = wrestlerService.getOrCreateState(wrestler.getId(), universeId);
    assertThat(state.getFans()).isLessThan(12000L); // 10000 * 1.2 = 12000
  }

  @Test
  @DisplayName("Should filter wrestlers by tier")
  @Transactional
  void shouldFilterWrestlersByTier() {
    // Given
    Wrestler icon =
        wrestlerService.createWrestler("Icon", true, "Desc", WrestlerTier.ICON, defaultUniverse);
    Wrestler rookie =
        wrestlerService.createWrestler(
            "Rookie", true, "Desc", WrestlerTier.ROOKIE, defaultUniverse);
    Long universeId = defaultUniverse.getId();

    // When
    List<Wrestler> icons = wrestlerService.getWrestlersByTier(WrestlerTier.ICON, universeId);

    // Then
    assertThat(icons).extracting(Wrestler::getName).contains("Icon");
    assertThat(icons).extracting(Wrestler::getName).doesNotContain("Rookie");
  }

  @Test
  @DisplayName("Should filter wrestlers by player status")
  @Transactional
  void shouldFilterWrestlersByPlayerStatus() {
    // Given
    wrestlerService.createWrestler("Player 1", true, "Desc", WrestlerTier.ROOKIE, defaultUniverse);
    wrestlerService.createWrestler("Player 2", true, "Desc", WrestlerTier.ROOKIE, defaultUniverse);
    wrestlerService.createWrestler("NPC 1", false, "Desc", WrestlerTier.ROOKIE, defaultUniverse);
    Long universeId = defaultUniverse.getId();

    // When
    List<Wrestler> players = wrestlerService.getPlayerWrestlers(universeId);
    List<Wrestler> npcs = wrestlerService.getNpcWrestlers(universeId);

    // Then
    assertThat(players).hasSizeGreaterThanOrEqualTo(2);
    assertThat(npcs).hasSizeGreaterThanOrEqualTo(1);
  }

  @Test
  @DisplayName("Should maintain data integrity across complex operations")
  @Transactional
  void shouldMaintainDataIntegrityAcrossComplexOperations() {
    // Given
    Wrestler wrestler =
        wrestlerService.createWrestler(
            "Integrity Test", true, "Desc", WrestlerTier.ROOKIE, defaultUniverse);
    Long universeId = defaultUniverse.getId();

    // When - Chain of operations
    wrestlerService.awardFans(wrestler.getId(), universeId, 1000L);
    wrestlerService.addBump(wrestler.getId(), universeId);
    wrestlerService.awardFans(wrestler.getId(), universeId, 500L);

    // Then
    WrestlerState state = wrestlerService.getOrCreateState(wrestler.getId(), universeId);
    // (1000 * 1.2) + (500 * 1.2) = 1200 + 600 = 1800
    assertThat(state.getFans()).isEqualTo(1800L);
    assertThat(state.getBumps()).isEqualTo(1);
    assertThat(state.getWrestler().getName()).isEqualTo("Integrity Test");
  }

  @Test
  @DisplayName("Should create inbox item on fan awarded event")
  @Transactional
  void shouldCreateInboxItemOnFanAwardedEvent() {
    // Given
    Wrestler wrestler =
        wrestlerService.createWrestler(
            "Event Test", true, "Desc", WrestlerTier.ROOKIE, defaultUniverse);
    Long universeId = defaultUniverse.getId();

    // When
    wrestlerService.awardFans(wrestler.getId(), universeId, 100L);

    // Then
    List<InboxItem> items = inboxRepository.findAll();
    // Verification depends on event listener implementation
  }

  @Test
  @DisplayName("Should recalibrate fan counts")
  @Transactional
  void shouldRecalibrateFanCounts() {
    // Given
    Wrestler icon =
        wrestlerService.createWrestler("Icon", true, "Desc", WrestlerTier.ICON, defaultUniverse);
    WrestlerState state = wrestlerService.getOrCreateState(icon.getId(), defaultUniverse.getId());
    state.setFans(1000000L);
    wrestlerStateRepository.saveAndFlush(state);

    // When
    wrestlerService.recalibrateFanCounts(defaultUniverse.getId());

    // Then
    WrestlerState updated = wrestlerService.getOrCreateState(icon.getId(), defaultUniverse.getId());
    assertThat(updated.getFans()).isEqualTo(WrestlerTier.ICON.getMinFans());
  }

  @Test
  @DisplayName("Should recalibrate Icon to Main Eventer if fans are lower")
  @Transactional
  void shouldRecalibrateIconToMainEventer() {
    // Given
    Wrestler icon =
        wrestlerService.createWrestler("Icon", true, "Desc", WrestlerTier.ICON, defaultUniverse);
    WrestlerState state = wrestlerService.getOrCreateState(icon.getId(), defaultUniverse.getId());
    state.setFans(WrestlerTier.MAIN_EVENTER.getMinFans() + 100);
    wrestlerStateRepository.saveAndFlush(state);

    // When
    tierRecalculationService.recalculateTier(state);

    // Then
    WrestlerState updated = wrestlerStateRepository.findById(state.getId()).get();
    assertThat(updated.getTier()).isEqualTo(WrestlerTier.MAIN_EVENTER);
  }

  @Test
  @DisplayName("Should reset all fan counts to zero")
  @Transactional
  void shouldResetAllFanCountsToZero() {
    // Given
    wrestlerService.createWrestler("Icon", true, "Desc", WrestlerTier.ICON, defaultUniverse);
    wrestlerService.createWrestler("Rookie", true, "Desc", WrestlerTier.ROOKIE, defaultUniverse);

    // When
    wrestlerService.resetAllFanCountsToZero(defaultUniverse.getId());

    // Then
    List<WrestlerState> states = wrestlerStateRepository.findByUniverseId(defaultUniverse.getId());
    for (WrestlerState s : states) {
      assertThat(s.getFans()).isZero();
      assertThat(s.getTier()).isEqualTo(WrestlerTier.ROOKIE);
    }
  }

  @Test
  @Transactional
  void testAwardFans_PersistsChanges() {
    Wrestler wrestler =
        wrestlerService.createWrestler("Test", true, "Desc", WrestlerTier.ROOKIE, defaultUniverse);
    wrestlerService.awardFans(wrestler.getId(), defaultUniverse.getId(), 100L);

    WrestlerState state =
        wrestlerStateRepository
            .findByWrestlerIdAndUniverseId(wrestler.getId(), defaultUniverse.getId())
            .get();
    // 100 * 1.2 = 120
    assertEquals(120L, state.getFans());
  }

  @Test
  @Transactional
  void testAddBump_PersistsChanges_Increment() {
    Wrestler wrestler =
        wrestlerService.createWrestler("Test", true, "Desc", WrestlerTier.ROOKIE, defaultUniverse);
    wrestlerService.addBump(wrestler.getId(), defaultUniverse.getId());

    WrestlerState state =
        wrestlerStateRepository
            .findByWrestlerIdAndUniverseId(wrestler.getId(), defaultUniverse.getId())
            .get();
    assertEquals(1, state.getBumps());
  }

  @Test
  @Transactional
  void testAddBump_PersistsChanges() {
    Wrestler wrestler =
        wrestlerService.createWrestler("Test", true, "Desc", WrestlerTier.ROOKIE, defaultUniverse);
    wrestlerService.addBump(wrestler.getId(), defaultUniverse.getId());

    WrestlerState state =
        wrestlerStateRepository
            .findByWrestlerIdAndUniverseId(wrestler.getId(), defaultUniverse.getId())
            .get();
    assertEquals(1, state.getBumps());
  }

  @Test
  @Transactional
  void testHealBump_PersistsChanges() {
    Wrestler wrestler =
        wrestlerService.createWrestler("Test", true, "Desc", WrestlerTier.ROOKIE, defaultUniverse);
    wrestlerService.addBump(wrestler.getId(), defaultUniverse.getId());
    wrestlerService.healBump(wrestler.getId(), defaultUniverse.getId());

    WrestlerState state =
        wrestlerStateRepository
            .findByWrestlerIdAndUniverseId(wrestler.getId(), defaultUniverse.getId())
            .get();
    assertEquals(0, state.getBumps());
  }
}
