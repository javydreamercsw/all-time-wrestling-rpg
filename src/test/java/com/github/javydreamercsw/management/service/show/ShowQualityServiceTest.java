/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.show;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentParticipant;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShowQualityServiceTest {

  @Mock private SegmentRepository segmentRepository;
  @Mock private WrestlerStateRepository wrestlerStateRepository;
  @Mock private WrestlerService wrestlerService;
  @Mock private RivalryService rivalryService;
  @Mock private PromoBookingService promoBookingService;

  private ShowQualityService service;

  private Show show;
  private Universe universe;

  @BeforeEach
  void setUp() {
    service =
        new ShowQualityService(
            segmentRepository,
            wrestlerStateRepository,
            wrestlerService,
            rivalryService,
            promoBookingService);

    universe = new Universe();
    universe.setId(1L);

    show = new Show();
    show.setId(10L);
    show.setName("Test Show");
    show.setUniverse(universe);

    when(segmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  // ---------- computeAndPersist ----------

  @Test
  void emptySegmentList_returnsMinStarsAndSetsField() {
    when(promoBookingService.isPromoSegment(any())).thenReturn(false);

    double result = service.computeAndPersist(show, Collections.emptyList());

    assertThat(result).isEqualTo(1.0);
    assertThat(show.getQualityScore()).isEqualTo(1.0);
  }

  @Test
  void allPromoSegments_returnsMinStars() {
    Segment promo = makeSegment("Promo", false);
    when(promoBookingService.isPromoSegment(promo)).thenReturn(true);

    double result = service.computeAndPersist(show, List.of(promo));

    assertThat(result).isEqualTo(1.0);
  }

  @Test
  void matchSegmentWithTitle_segmentRatingReflectsBonus() {
    // Title = +10 pts; with just that it scores 10/20 = 0.5 → clamped to 1.0★.
    // The segment rating on the entity should be 10, proving the bonus is counted.
    Segment titleMatch = makeSegment("One on One", false);
    titleMatch.setIsTitleSegment(true);
    when(promoBookingService.isPromoSegment(titleMatch)).thenReturn(false);
    when(wrestlerStateRepository.findByWrestlerIdAndUniverseId(anyLong(), anyLong()))
        .thenReturn(Optional.empty());

    service.computeAndPersist(show, List.of(titleMatch));

    assertThat(titleMatch.getSegmentRating()).isGreaterThanOrEqualTo(10);
  }

  @Test
  void mainEventFlag_addsToScore() {
    Segment plainMatch = makeSegment("One on One", false);
    plainMatch.setMainEvent(false);
    Segment mainEvent = makeSegment("One on One", false);
    mainEvent.setMainEvent(true);

    when(promoBookingService.isPromoSegment(any())).thenReturn(false);
    when(wrestlerStateRepository.findByWrestlerIdAndUniverseId(anyLong(), anyLong()))
        .thenReturn(Optional.empty());

    double plain = service.computeAndPersist(show, List.of(plainMatch));
    show.setQualityScore(null);
    double main = service.computeAndPersist(show, List.of(mainEvent));

    assertThat(main).isGreaterThanOrEqualTo(plain);
  }

  @Test
  void qualityIsClampedTo5Stars() {
    // Construct a maximally scored segment
    Segment segment = makeSegment("One on One", true);
    segment.setIsTitleSegment(true);
    segment.setMainEvent(true);
    when(promoBookingService.isPromoSegment(segment)).thenReturn(false);
    when(wrestlerStateRepository.findByWrestlerIdAndUniverseId(anyLong(), anyLong()))
        .thenReturn(Optional.empty());

    // Force all participants worn-out by setting starting=100, final=0
    for (SegmentParticipant p : segment.getParticipants()) {
      Wrestler w = p.getWrestler();
      w.setStartingStamina(100);
      w.setStartingHealth(100);
      p.setFinalStamina(0);
      p.setFinalHealth(0);
      p.setFinalMomentum(100);
      p.setIsWinner(true);
    }

    double result = service.computeAndPersist(show, List.of(segment));

    assertThat(result).isLessThanOrEqualTo(5.0);
  }

  @Test
  void qualityIsRoundedToHalfStar() {
    Segment segment = makeSegment("One on One", false);
    when(promoBookingService.isPromoSegment(segment)).thenReturn(false);
    when(wrestlerStateRepository.findByWrestlerIdAndUniverseId(anyLong(), anyLong()))
        .thenReturn(Optional.empty());

    double result = service.computeAndPersist(show, List.of(segment));

    // Result must be a multiple of 0.5
    assertThat(result * 2 % 1).isEqualTo(0.0);
  }

  // ---------- qualifiesForFanBonus ----------

  @Test
  void qualifiesForFanBonus_falseBelow4() {
    assertThat(service.qualifiesForFanBonus(3.5)).isFalse();
    assertThat(service.qualifiesForFanBonus(3.9)).isFalse();
  }

  @Test
  void qualifiesForFanBonus_trueAt4AndAbove() {
    assertThat(service.qualifiesForFanBonus(4.0)).isTrue();
    assertThat(service.qualifiesForFanBonus(5.0)).isTrue();
  }

  // ---------- computeFanBonus ----------

  @Test
  void computeFanBonus_fivePercentOfFans() {
    assertThat(service.computeFanBonus(1000L)).isEqualTo(50L);
    assertThat(service.computeFanBonus(200L)).isEqualTo(10L);
  }

  @Test
  void computeFanBonus_minimumOne() {
    assertThat(service.computeFanBonus(0L)).isEqualTo(1L);
    assertThat(service.computeFanBonus(1L)).isEqualTo(1L);
  }

  // ---------- awardFanBonusesIfEligible ----------

  @Test
  void awardFanBonuses_skipsWhenQualityBelow4() {
    service.awardFanBonusesIfEligible(show, List.of(), 3.5);
    verify(wrestlerService, never()).awardFans(anyLong(), anyLong(), anyLong());
  }

  @Test
  void awardFanBonuses_skipsWhenNoUniverse() {
    show.setUniverse(null);
    service.awardFanBonusesIfEligible(show, List.of(), 4.5);
    verify(wrestlerService, never()).awardFans(anyLong(), anyLong(), anyLong());
  }

  @Test
  void awardFanBonuses_awardsUniqueWrestlers() {
    Segment seg = makeSegment("One on One", false);
    when(promoBookingService.isPromoSegment(seg)).thenReturn(false);

    WrestlerState state = new WrestlerState();
    state.setFans(2000L);
    when(wrestlerStateRepository.findByWrestlerIdAndUniverseId(anyLong(), anyLong()))
        .thenReturn(Optional.of(state));

    service.awardFanBonusesIfEligible(show, List.of(seg), 4.5);

    // Two distinct wrestlers in the segment each get a bonus
    verify(wrestlerService, times(2)).awardFans(anyLong(), anyLong(), anyLong());
  }

  @Test
  void awardFanBonuses_deduplicatesWrestlersAcrossSegments() {
    Segment seg1 = makeSegment("One on One", false);
    Segment seg2 = makeSegment("One on One", false);

    // Make seg2 reuse the same wrestlers as seg1
    Set<SegmentParticipant> sharedParticipants = seg1.getParticipants();
    seg2.setParticipants(sharedParticipants);

    when(promoBookingService.isPromoSegment(any())).thenReturn(false);
    WrestlerState state = new WrestlerState();
    state.setFans(1000L);
    when(wrestlerStateRepository.findByWrestlerIdAndUniverseId(anyLong(), anyLong()))
        .thenReturn(Optional.of(state));

    service.awardFanBonusesIfEligible(show, List.of(seg1, seg2), 4.5);

    // Only 2 unique wrestlers, not 4
    verify(wrestlerService, times(2)).awardFans(anyLong(), anyLong(), anyLong());
  }

  // ---------- individual scoring factors ----------

  @Test
  void heatContributesToScore() {
    Segment noHeat = makeSegment("One on One", false);
    Segment withHeat = makeSegment("One on One", false);
    withHeat.setRivalryId(99L);

    Rivalry rivalry = new Rivalry();
    rivalry.setHeat(100); // max heat → +20 pts

    when(rivalryService.getRivalryById(99L)).thenReturn(Optional.of(rivalry));
    when(promoBookingService.isPromoSegment(any())).thenReturn(false);
    when(wrestlerStateRepository.findByWrestlerIdAndUniverseId(anyLong(), anyLong()))
        .thenReturn(Optional.empty());

    service.computeAndPersist(show, List.of(noHeat));
    int noHeatRating = noHeat.getSegmentRating();

    show.setQualityScore(null);
    service.computeAndPersist(show, List.of(withHeat));
    int highHeatRating = withHeat.getSegmentRating();

    assertThat(highHeatRating).isGreaterThan(noHeatRating);
  }

  @Test
  void tierSpreadContributesToScore() {
    Segment sameTier = makeSegment("One on One", false);
    Segment bigSpread = makeSegment("One on One", false);

    WrestlerState rookie = new WrestlerState();
    rookie.setTier(WrestlerTier.ROOKIE); // ordinal 0

    WrestlerState icon = new WrestlerState();
    icon.setTier(WrestlerTier.ICON); // ordinal 5

    when(promoBookingService.isPromoSegment(any())).thenReturn(false);

    // Same tier: both wrestlers return ROOKIE state
    when(wrestlerStateRepository.findByWrestlerIdAndUniverseId(anyLong(), anyLong()))
        .thenReturn(Optional.of(rookie));
    service.computeAndPersist(show, List.of(sameTier));
    int sameTierRating = sameTier.getSegmentRating();

    // Big spread: first wrestler ROOKIE, second ICON
    List<SegmentParticipant> parts = List.copyOf(bigSpread.getParticipants());
    when(wrestlerStateRepository.findByWrestlerIdAndUniverseId(
            parts.get(0).getWrestler().getId(), universe.getId()))
        .thenReturn(Optional.of(rookie));
    when(wrestlerStateRepository.findByWrestlerIdAndUniverseId(
            parts.get(1).getWrestler().getId(), universe.getId()))
        .thenReturn(Optional.of(icon));

    show.setQualityScore(null);
    service.computeAndPersist(show, List.of(bigSpread));
    int bigSpreadRating = bigSpread.getSegmentRating();

    assertThat(bigSpreadRating).isGreaterThan(sameTierRating);
  }

  @Test
  void stipulationContributesToScore() {
    Segment plain = makeSegment("One on One", false);
    Segment stip = makeSegment("One on One", false);

    SegmentRule ladderMatch = new SegmentRule();
    ladderMatch.setName("Ladder Match");
    stip.setSegmentRules(new HashSet<>(Set.of(ladderMatch)));

    when(promoBookingService.isPromoSegment(any())).thenReturn(false);
    when(wrestlerStateRepository.findByWrestlerIdAndUniverseId(anyLong(), anyLong()))
        .thenReturn(Optional.empty());

    service.computeAndPersist(show, List.of(plain));
    int plainRating = plain.getSegmentRating();

    show.setQualityScore(null);
    service.computeAndPersist(show, List.of(stip));
    int stipRating = stip.getSegmentRating();

    // One stipulation = +5 pts
    assertThat(stipRating).isEqualTo(plainRating + 5);
  }

  @Test
  void npcOnlyMatchWithNoFactorsScoresZero() {
    Segment segment = makeSegment("One on One", false);
    // No rivalry, no title, not main event, no stipulation, no universe tier data
    segment.setIsTitleSegment(false);
    segment.setMainEvent(false);
    segment.setSegmentRules(new HashSet<>());

    when(promoBookingService.isPromoSegment(segment)).thenReturn(false);
    when(wrestlerStateRepository.findByWrestlerIdAndUniverseId(anyLong(), anyLong()))
        .thenReturn(Optional.empty());

    service.computeAndPersist(show, List.of(segment));

    assertThat(segment.getSegmentRating()).isEqualTo(0);
  }

  @Test
  void playerMatchScoresHigherThanEquivalentNpcMatch() {
    Segment npcMatch = makeSegment("One on One", false);
    Segment playerMatch = makeSegment("One on One", true);

    // Wear-and-tear is set in makeParticipant: starting 100, final 80 = 20% burn
    when(promoBookingService.isPromoSegment(any())).thenReturn(false);
    when(wrestlerStateRepository.findByWrestlerIdAndUniverseId(anyLong(), anyLong()))
        .thenReturn(Optional.empty());

    service.computeAndPersist(show, List.of(npcMatch));
    int npcRating = npcMatch.getSegmentRating();

    show.setQualityScore(null);
    service.computeAndPersist(show, List.of(playerMatch));
    int playerRating = playerMatch.getSegmentRating();

    assertThat(playerRating).isGreaterThan(npcRating);
  }

  // ---------- helpers ----------

  private Segment makeSegment(final String typeName, final boolean playerInvolved) {
    SegmentType type = new SegmentType();
    type.setName(typeName);

    Segment segment = new Segment();
    segment.setId((long) (Math.random() * 10000));
    segment.setSegmentType(type);
    segment.setIsTitleSegment(false);
    segment.setMainEvent(false);

    Wrestler w1 = makeWrestler(100L, playerInvolved);
    Wrestler w2 = makeWrestler(101L, false);

    SegmentParticipant p1 = makeParticipant(w1);
    SegmentParticipant p2 = makeParticipant(w2);

    Set<SegmentParticipant> participants = new HashSet<>(Set.of(p1, p2));
    segment.setParticipants(participants);

    return segment;
  }

  private Wrestler makeWrestler(final long id, final boolean isPlayer) {
    Wrestler w = new Wrestler();
    w.setId(id);
    w.setName("Wrestler " + id);
    w.setIsPlayer(isPlayer);
    w.setStartingStamina(100);
    w.setStartingHealth(100);
    w.setDecks(new HashSet<>());
    return w;
  }

  private SegmentParticipant makeParticipant(final Wrestler wrestler) {
    SegmentParticipant p = new SegmentParticipant();
    p.setWrestler(wrestler);
    p.setFinalStamina(80);
    p.setFinalHealth(80);
    p.setFinalMomentum(50);
    p.setIsWinner(false);
    p.setCardsPlayed(new HashMap<>());
    return p;
  }
}
