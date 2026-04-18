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
package com.github.javydreamercsw.management.service.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.BumpAddition;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.test.AbstractMockUserIntegrationTest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class SegmentAdjudicationServiceIT extends AbstractMockUserIntegrationTest {
  @Autowired private SegmentAdjudicationService segmentAdjudicationService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private WrestlerStateRepository wrestlerStateRepository;
  @Autowired private UniverseRepository universeRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentRuleRepository segmentRuleRepository;
  @Autowired private TitleRepository titleRepository;
  @Autowired private TitleService titleService;
  @Autowired private WrestlerService wrestlerService;

  private Universe defaultUniverse;

  @BeforeEach
  void setUpUniverse() {
    defaultUniverse =
        universeRepository
            .findById(1L)
            .orElseGet(
                () ->
                    universeRepository.save(
                        Universe.builder().id(1L).name("Default Universe").build()));
  }

  @Test
  void testAdjudicateMatchWithTitleChangeLinksToSegment() {
    // Given
    Wrestler winner = Wrestler.builder().build();
    winner.setName("New Champion");
    winner = wrestlerRepository.save(winner);

    WrestlerState winnerState = wrestlerService.getOrCreateState(winner.getId(), 1L);
    winnerState.setFans(100_000L);
    winnerState.setTier(WrestlerTier.MAIN_EVENTER);
    wrestlerStateRepository.save(winnerState);

    Wrestler loser = Wrestler.builder().build();
    loser.setName("Former Champion");
    loser = wrestlerRepository.save(loser);

    WrestlerState loserState = wrestlerService.getOrCreateState(loser.getId(), 1L);
    loserState.setFans(100_000L);
    loserState.setTier(WrestlerTier.MAIN_EVENTER);
    wrestlerStateRepository.save(loserState);

    Title title =
        titleService.createTitle(
            "World Title", "The top title", WrestlerTier.MAIN_EVENTER, ChampionshipType.SINGLE, 1L);
    title.awardTitleTo(List.of(loser), java.time.Instant.now());
    titleRepository.save(title);

    ShowType showType = new ShowType();
    showType.setName("Title Test Show Type");
    showType.setDescription("Title Test Description");
    showTypeRepository.save(showType);

    Show show = new Show();
    show.setName("Title Test Show");
    show.setDescription("Title Test Show Description");
    show.setShowDate(LocalDate.now());
    show.setType(showType);
    show.setUniverse(defaultUniverse);
    showRepository.save(show);

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Title Match");
    segmentTypeRepository.save(segmentType);

    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(segmentType);
    segment.setIsTitleSegment(true);
    segment.getTitles().add(title);
    segment.addParticipant(winner);
    segment.addParticipant(loser);
    segment.setWinners(List.of(winner));
    segmentRepository.save(segment);

    // When
    segmentAdjudicationService.adjudicateMatch(segment);

    // Then
    Title updatedTitle = titleRepository.findById(title.getId()).get();
    Assertions.assertTrue(updatedTitle.getCurrentChampions().contains(winner));
    Assertions.assertTrue(updatedTitle.getCurrentReign().isPresent());
    Assertions.assertEquals(
        segment.getId(), updatedTitle.getCurrentReign().get().getWonAtSegment().getId());
  }

  @Test
  void testAdjudicateMatch() {
    // Given
    long initialFans = 10_000L;
    Wrestler winner = Wrestler.builder().build();
    winner.setName("Winner");
    winner.setStartingHealth(13);
    winner.setStartingStamina(14);
    winner.setLowHealth(2);
    winner.setLowStamina(2);
    winner.setDeckSize(16);
    winner.setIsPlayer(false);
    winner = wrestlerRepository.save(winner);

    WrestlerState winnerState = wrestlerService.getOrCreateState(winner.getId(), 1L);
    winnerState.setFans(initialFans);
    winnerState.setBumps(0);
    winnerState.setTier(WrestlerTier.MIDCARDER);
    wrestlerStateRepository.save(winnerState);

    Wrestler loser = Wrestler.builder().build();
    loser.setName("Loser");
    loser.setStartingHealth(14);
    loser.setStartingStamina(16);
    loser.setLowHealth(4);
    loser.setLowStamina(2);
    loser.setDeckSize(15);
    loser.setIsPlayer(false);
    loser = wrestlerRepository.save(loser);

    WrestlerState loserState = wrestlerService.getOrCreateState(loser.getId(), 1L);
    loserState.setFans(initialFans);
    loserState.setBumps(0);
    loserState.setTier(WrestlerTier.MIDCARDER);
    wrestlerStateRepository.save(loserState);

    ShowType showType = new ShowType();
    showType.setName("Test Show Type");
    showType.setDescription("Test Description");
    showTypeRepository.save(showType);

    Show show = new Show();
    show.setName("Test Show");
    show.setDescription("Test Description");
    show.setShowDate(LocalDate.now());
    show.setType(showType);
    show.setUniverse(defaultUniverse);
    showRepository.save(show);

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Test Match");
    segmentTypeRepository.save(segmentType);
    if (segmentRuleRepository.findByName("Submission").isEmpty()) {
      SegmentRule rule = new SegmentRule();
      rule.setName("Submission");
      rule.setDescription("Submission Match");
      rule.setBumpAddition(BumpAddition.LOSERS);
      rule.setRequiresHighHeat(false);
      segmentRuleRepository.save(rule);
    }

    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(segmentType);
    segment.addParticipant(winner);
    segment.addParticipant(loser);
    segment.setWinners(List.of(winner));
    segment.getSegmentRules().add(segmentRuleRepository.findByName("Submission").get());
    segmentRepository.save(segment);

    // When
    segmentAdjudicationService.adjudicateMatch(segment);

    // Then
    Assertions.assertNotNull(winner.getId());
    winnerState = wrestlerStateRepository.findByWrestlerIdAndUniverseId(winner.getId(), 1L).get();

    Assertions.assertNotNull(loser.getId());
    loserState = wrestlerStateRepository.findByWrestlerIdAndUniverseId(loser.getId(), 1L).get();

    // Winner fan gain: (2d6 + 3) * 1000 + matchQualityBonus
    // Min: (2+3)*1000 + 0 = 5000. Max: (12+3)*1000 + 10000 = 25000
    org.assertj.core.api.Assertions.assertThat(winnerState.getFans())
        .isBetween(initialFans + 5_000, initialFans + 25_000);

    // Loser fan gain: (1d6 - 4) * 1000 + matchQualityBonus
    // Min: (1-4)*1000 + 0 = -3000. Max: (6-4)*1000 + 10000 = 12000
    org.assertj.core.api.Assertions.assertThat(loserState.getFans())
        .isBetween(initialFans - 3_000, initialFans + 12_000);

    assertEquals(0, winnerState.getBumps());
    assertEquals(1, loserState.getBumps());
  }

  @Test
  void testAdjudicatePromo() {
    // Given
    long initialFans = 10_000L;
    Wrestler participant1 = Wrestler.builder().build();
    participant1.setName("Participant 1");
    participant1.setStartingHealth(13);
    participant1.setStartingStamina(14);
    participant1.setLowHealth(2);
    participant1.setLowStamina(2);
    participant1.setDeckSize(16);
    participant1.setIsPlayer(false);
    participant1 = wrestlerRepository.save(participant1);

    WrestlerState state1 = wrestlerService.getOrCreateState(participant1.getId(), 1L);
    state1.setFans(initialFans);
    state1.setBumps(0);
    state1.setTier(WrestlerTier.MIDCARDER);
    wrestlerStateRepository.save(state1);

    Wrestler participant2 = Wrestler.builder().build();
    participant2.setName("Participant 2");
    participant2.setStartingHealth(14);
    participant2.setStartingStamina(16);
    participant2.setLowHealth(4);
    participant2.setLowStamina(2);
    participant2.setDeckSize(15);
    participant2.setIsPlayer(false);
    participant2 = wrestlerRepository.save(participant2);

    WrestlerState state2 = wrestlerService.getOrCreateState(participant2.getId(), 1L);
    state2.setFans(initialFans);
    state2.setBumps(0);
    state2.setTier(WrestlerTier.MIDCARDER);
    wrestlerStateRepository.save(state2);

    ShowType showType =
        showTypeRepository
            .findByName("Promo Test Show Type")
            .orElseGet(
                () -> {
                  ShowType st = new ShowType();
                  st.setName("Promo Test Show Type");
                  st.setDescription("Test Description");
                  return showTypeRepository.save(st);
                });

    Show show = new Show();
    show.setName("Promo Test Show");
    show.setDescription("Test Description");
    show.setShowDate(LocalDate.now());
    show.setType(showType);
    show.setUniverse(defaultUniverse);
    showRepository.save(show);

    SegmentType segmentType =
        segmentTypeRepository
            .findByName("Promo")
            .orElseGet(
                () -> {
                  SegmentType st = new SegmentType();
                  st.setName("Promo");
                  return segmentTypeRepository.save(st);
                });

    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(segmentType);
    segment.addParticipant(participant1);
    segment.addParticipant(participant2);
    segmentRepository.save(segment);

    // When
    segmentAdjudicationService.adjudicateMatch(segment);

    // Then
    Assertions.assertNotNull(participant1.getId());
    state1 = wrestlerStateRepository.findByWrestlerIdAndUniverseId(participant1.getId(), 1L).get();

    Assertions.assertNotNull(participant2.getId());
    state2 = wrestlerStateRepository.findByWrestlerIdAndUniverseId(participant2.getId(), 1L).get();

    // Min bonus: 0. Max bonus: 18 (3d6).
    long minFanGain = 0L;
    long maxFanGain = 18 * 1_000L;

    org.assertj.core.api.Assertions.assertThat(state1.getFans())
        .isBetween(initialFans + minFanGain, initialFans + maxFanGain);

    org.assertj.core.api.Assertions.assertThat(state2.getFans())
        .isBetween(initialFans + minFanGain, initialFans + maxFanGain);

    // Bumps should not be assigned for promos
    assertEquals(0, state1.getBumps());
    assertEquals(0, state2.getBumps());
  }

  @Test
  void testAdjudicateMatchWithAllBumps() {
    // Given
    long initialFans = 10_000L;
    Wrestler winner = Wrestler.builder().build();
    winner.setName("Winner All Bumps");
    winner.setStartingHealth(13);
    winner.setStartingStamina(14);
    winner.setLowHealth(2);
    winner.setLowStamina(2);
    winner.setDeckSize(16);
    winner.setIsPlayer(false);
    winner = wrestlerRepository.save(winner);

    WrestlerState winnerState = wrestlerService.getOrCreateState(winner.getId(), 1L);
    winnerState.setFans(initialFans);
    winnerState.setBumps(0);
    winnerState.setTier(WrestlerTier.MIDCARDER);
    wrestlerStateRepository.save(winnerState);

    Wrestler loser = Wrestler.builder().build();
    loser.setName("Loser All Bumps");
    loser.setStartingHealth(14);
    loser.setStartingStamina(16);
    loser.setLowHealth(4);
    loser.setLowStamina(2);
    loser.setDeckSize(15);
    loser.setIsPlayer(false);
    loser = wrestlerRepository.save(loser);

    WrestlerState loserState = wrestlerService.getOrCreateState(loser.getId(), 1L);
    loserState.setFans(initialFans);
    loserState.setBumps(0);
    loserState.setTier(WrestlerTier.MIDCARDER);
    wrestlerStateRepository.save(loserState);

    ShowType showType = new ShowType();
    showType.setName("Test Show Type All Bumps");
    showType.setDescription("Test Description");
    showTypeRepository.save(showType);

    Show show = new Show();
    show.setName("Test Show All Bumps");
    show.setDescription("Test Description");
    show.setShowDate(LocalDate.now());
    show.setType(showType);
    show.setUniverse(defaultUniverse);
    showRepository.save(show);

    SegmentType segmentType = new SegmentType();
    segmentType.setName("Test Match All Bumps");
    segmentTypeRepository.save(segmentType);

    if (segmentRuleRepository.findByName("No DQ").isEmpty()) {
      SegmentRule rule = new SegmentRule();
      rule.setName("No DQ");
      rule.setDescription("No Disqualification Match");
      rule.setBumpAddition(BumpAddition.ALL);
      rule.setRequiresHighHeat(false);
      segmentRuleRepository.save(rule);
    }

    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(segmentType);
    segment.addParticipant(winner);
    segment.addParticipant(loser);
    segment.setWinners(List.of(winner));
    segment.getSegmentRules().add(segmentRuleRepository.findByName("No DQ").get());
    segmentRepository.save(segment);

    // When
    segmentAdjudicationService.adjudicateMatch(segment);

    // Then
    Assertions.assertNotNull(winner.getId());
    winnerState = wrestlerStateRepository.findByWrestlerIdAndUniverseId(winner.getId(), 1L).get();

    Assertions.assertNotNull(loser.getId());
    loserState = wrestlerStateRepository.findByWrestlerIdAndUniverseId(loser.getId(), 1L).get();

    assertEquals(1, winnerState.getBumps());
    assertEquals(1, loserState.getBumps());
  }
}
