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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.service.feud.FeudResolutionService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SegmentAdjudicationServiceUnitTest {

  @Mock private RivalryService rivalryService;
  @Mock private WrestlerService wrestlerService;
  @Mock private FeudResolutionService feudResolutionService;
  @Mock private MultiWrestlerFeudService feudService;

  @Mock private Random random;
  @Mock private TitleService titleService;

  @InjectMocks private SegmentAdjudicationService adjudicationService;

  private Segment promoSegment;
  private Segment matchSegment;
  private Wrestler wrestler1;
  private Wrestler wrestler2;

  @BeforeEach
  void setUp() {
    adjudicationService =
        new SegmentAdjudicationService(
            rivalryService,
            wrestlerService,
            feudResolutionService,
            feudService,
            titleService,
            random);

    wrestler1 = Wrestler.builder().build();
    wrestler1.setId(1L);
    wrestler1.setName("Wrestler 1");

    wrestler2 = Wrestler.builder().build();
    wrestler2.setId(2L);
    wrestler2.setName("Wrestler 2");

    SegmentType promoType = new SegmentType();
    promoType.setName("Promo");

    promoSegment = new Segment();
    promoSegment.setSegmentType(promoType);
    promoSegment.addParticipant(wrestler1);
    promoSegment.addParticipant(wrestler2);

    SegmentType matchType = new SegmentType();
    matchType.setName("Match");
    matchSegment = new Segment();
    matchSegment.setSegmentType(matchType);
    matchSegment.addParticipant(wrestler1);
    matchSegment.addParticipant(wrestler2);

    Show show = new Show();
    ShowType showType = new ShowType();
    showType.setName("Weekly Show");
    show.setType(showType);
    promoSegment.setShow(show);
    matchSegment.setShow(show);
  }

  @Test
  void testAdjudicatePromo_Roll1() {
    // Roll 1 on d20
    when(random.nextInt(20)).thenReturn(0);

    adjudicationService.adjudicateMatch(promoSegment);

    // promoQualityBonus should be 0
    verify(wrestlerService, times(1)).awardFans(eq(1L), eq(0L));
    verify(wrestlerService, times(1)).awardFans(eq(2L), eq(0L));
  }

  @Test
  void testAdjudicatePromo_Roll2() {
    // Roll 2 on d20
    when(random.nextInt(20)).thenReturn(1);
    // Roll 2 on d3 for bonus (1+1)
    when(random.nextInt(3)).thenReturn(1);

    adjudicationService.adjudicateMatch(promoSegment);

    // promoQualityBonus should be 2
    long expectedFans = 2 * 1_000L;
    verify(wrestlerService, times(1)).awardFans(eq(1L), eq(expectedFans));
    verify(wrestlerService, times(1)).awardFans(eq(2L), eq(expectedFans));
  }

  @Test
  void testAdjudicatePromo_Roll3() {
    // Roll 3 on d20
    when(random.nextInt(20)).thenReturn(2);
    // Roll 3 on d3 for bonus (2+1)
    when(random.nextInt(3)).thenReturn(2);

    adjudicationService.adjudicateMatch(promoSegment);

    // promoQualityBonus should be 3
    long expectedFans = 3 * 1_000L;
    verify(wrestlerService, times(1)).awardFans(eq(1L), eq(expectedFans));
    verify(wrestlerService, times(1)).awardFans(eq(2L), eq(expectedFans));
  }

  @Test
  void testAdjudicatePromo_Roll4() {
    // Roll 4 on d20
    when(random.nextInt(20)).thenReturn(3);
    // Roll 4 on d6 for bonus (3+1)
    when(random.nextInt(6)).thenReturn(3);

    adjudicationService.adjudicateMatch(promoSegment);

    // promoQualityBonus should be 4
    long expectedFans = 4 * 1_000L;
    verify(wrestlerService, times(1)).awardFans(eq(1L), eq(expectedFans));
    verify(wrestlerService, times(1)).awardFans(eq(2L), eq(expectedFans));
  }

  @Test
  void testAdjudicatePromo_Roll16() {
    // Roll 16 on d20
    when(random.nextInt(20)).thenReturn(15);
    // Roll 5 on d6 for bonus (4+1)
    when(random.nextInt(6)).thenReturn(4);

    adjudicationService.adjudicateMatch(promoSegment);

    // promoQualityBonus should be 5
    long expectedFans = 5 * 1_000L;
    verify(wrestlerService, times(1)).awardFans(eq(1L), eq(expectedFans));
    verify(wrestlerService, times(1)).awardFans(eq(2L), eq(expectedFans));
  }

  @Test
  void testAdjudicatePromo_Roll17() {
    // Roll 17 on d20
    when(random.nextInt(20)).thenReturn(16);
    // Roll 3 on first d6 (2+1), 4 on second d6 (3+1) for bonus
    when(random.nextInt(6)).thenReturn(2).thenReturn(3);

    adjudicationService.adjudicateMatch(promoSegment);

    // promoQualityBonus should be 3 + 4 = 7
    long expectedFans = 7 * 1_000L;
    verify(wrestlerService, times(1)).awardFans(eq(1L), eq(expectedFans));
    verify(wrestlerService, times(1)).awardFans(eq(2L), eq(expectedFans));
  }

  @Test
  void testAdjudicatePromo_Roll19() {
    // Roll 19 on d20
    when(random.nextInt(20)).thenReturn(18);
    // Roll 5 on first d6 (4+1), 6 on second d6 (5+1) for bonus
    when(random.nextInt(6)).thenReturn(4).thenReturn(5);

    adjudicationService.adjudicateMatch(promoSegment);

    // promoQualityBonus should be 5 + 6 = 11
    long expectedFans = 11 * 1_000L;
    verify(wrestlerService, times(1)).awardFans(eq(1L), eq(expectedFans));
    verify(wrestlerService, times(1)).awardFans(eq(2L), eq(expectedFans));
  }

  @Test
  void testAdjudicatePromo_Roll20() {
    // Roll 20 on d20
    when(random.nextInt(20)).thenReturn(19);
    // Roll 1, 2, 3 on three d6's for bonus
    when(random.nextInt(6)).thenReturn(0).thenReturn(1).thenReturn(2);

    adjudicationService.adjudicateMatch(promoSegment);

    // promoQualityBonus should be 1 + 2 + 3 = 6
    long expectedFans = 6 * 1_000L;
    verify(wrestlerService, times(1)).awardFans(eq(1L), eq(expectedFans));
    verify(wrestlerService, times(1)).awardFans(eq(2L), eq(expectedFans));
  }

  @Test
  void testAdjudicateMatch_HeatUpdate() {
    // Given a match segment
    when(random.nextInt(20)).thenReturn(10); // Roll 11

    // When
    adjudicationService.adjudicateMatch(matchSegment);

    // Then
    verify(rivalryService, times(1))
        .addHeatBetweenWrestlers(
            Objects.requireNonNull(eq(wrestler1.getId())),
            Objects.requireNonNull(eq(wrestler2.getId())),
            eq(1),
            eq("From segment: Match"));
  }

  @Test
  void testAdjudicateMatch_LoserLosesFans() {
    // Given a match segment with a winner and a loser
    matchSegment.setWinners(List.of(wrestler1));

    // Roll 10 on d20 for no match quality bonus
    when(random.nextInt(20)).thenReturn(9);
    // Roll 1 for loser fan calculation (1-4) * 1000 = -3000
    // The winner rolls first, so the loser's roll is the second one.
    when(random.nextInt(6)).thenReturn(5).thenReturn(0);

    // When
    adjudicationService.adjudicateMatch(matchSegment);

    // Then
    // Winner's fan gain should be calculated, but we are interested in the loser.
    // Loser should lose fans.
    verify(wrestlerService, times(1)).awardFans(eq(2L), eq(-3000L));
  }
}
