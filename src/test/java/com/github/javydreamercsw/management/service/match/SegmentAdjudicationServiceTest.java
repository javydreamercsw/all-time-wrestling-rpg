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
package com.github.javydreamercsw.management.service.match;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.BumpAddition;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.feud.FeudResolutionService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SegmentAdjudicationServiceTest {

  @Mock private RivalryService rivalryService;
  @Mock private WrestlerService wrestlerService;
  @Mock private FeudResolutionService feudResolutionService;
  @Mock private MultiWrestlerFeudService feudService;
  @Mock private Random random;
  @Mock private Segment segment;
  @Mock private Wrestler winner;
  @Mock private Wrestler loser;
  @Mock private SegmentType segmentType;
  @Mock private Show show;
  @Mock private TitleService titleService;
  @Mock private MatchRewardService matchRewardService;
  @Mock private MatchFulfillmentRepository matchFulfillmentRepository;

  @Mock
  private com.github.javydreamercsw.management.domain.league.LeagueRosterRepository
      leagueRosterRepository;

  @Mock private LegacyService legacyService;
  @Mock private FactionService factionService;

  private SegmentAdjudicationService segmentAdjudicationService;

  @BeforeEach
  void setUp() {
    segmentAdjudicationService =
        new SegmentAdjudicationService(
            rivalryService,
            wrestlerService,
            feudResolutionService,
            feudService,
            titleService,
            matchRewardService,
            matchFulfillmentRepository,
            leagueRosterRepository,
            legacyService,
            factionService,
            random);
    when(segment.getWinners()).thenReturn(List.of(winner));
    when(segment.getLosers()).thenReturn(List.of(loser));
    when(segment.getWrestlers()).thenReturn(List.of(winner, loser));
    when(winner.getId()).thenReturn(1L);
    when(loser.getId()).thenReturn(2L);
    when(segment.getSegmentType()).thenReturn(segmentType);
    when(segmentType.getName()).thenReturn("Test Match");
    when(segment.getShow()).thenReturn(show);
    when(show.isPremiumLiveEvent()).thenReturn(false);
    when(matchFulfillmentRepository.findBySegment(segment)).thenReturn(Optional.empty());
  }

  @Test
  void testProcessRewardsCalled() {
    SegmentRule rule = new SegmentRule();
    rule.setBumpAddition(BumpAddition.WINNERS);
    when(segment.getSegmentRules()).thenReturn(java.util.Set.of(rule));

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(matchRewardService, times(1)).processRewards(segment, 1.0);
  }

  @Test
  void testAffinityGainOnVictory() {
    com.github.javydreamercsw.management.domain.faction.Faction faction =
        mock(com.github.javydreamercsw.management.domain.faction.Faction.class);
    when(faction.getId()).thenReturn(100L);

    Wrestler w1 = mock(Wrestler.class);
    Wrestler w2 = mock(Wrestler.class);
    when(w1.getFaction()).thenReturn(faction);
    when(w2.getFaction()).thenReturn(faction);
    when(w1.getId()).thenReturn(10L);
    when(w2.getId()).thenReturn(11L);

    when(segment.getWrestlers()).thenReturn(List.of(w1, w2, loser));
    when(segment.getWinners()).thenReturn(List.of(w1, w2));
    when(segment.isMainEvent()).thenReturn(false);

    segmentAdjudicationService.adjudicateMatch(segment);

    // Calculation: (2-1) [participation] + 2 [victory bonus] = 3
    verify(factionService).addAffinity(100L, 3);
  }
}
