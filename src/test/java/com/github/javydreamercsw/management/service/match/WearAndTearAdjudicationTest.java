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
package com.github.javydreamercsw.management.service.match;

import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.feud.FeudResolutionService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import com.github.javydreamercsw.management.service.ringside.RingsideActionService;
import com.github.javydreamercsw.management.service.ringside.RingsideAiService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.RetirementService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WearAndTearAdjudicationTest {

  @Mock private RivalryService rivalryService;
  @Mock private WrestlerService wrestlerService;
  @Mock private FeudResolutionService feudResolutionService;
  @Mock private MultiWrestlerFeudService feudService;
  @Mock private Random random;
  @Mock private Segment segment;
  @Mock private Wrestler wrestler;
  @Mock private SegmentType segmentType;
  @Mock private Show show;
  @Mock private TitleService titleService;
  @Mock private MatchFulfillmentRepository matchFulfillmentRepository;
  @Mock private RetirementService retirementService;

  @Mock
  private com.github.javydreamercsw.management.domain.league.LeagueRosterRepository
      leagueRosterRepository;

  @Mock private LegacyService legacyService;
  @Mock private FactionService factionService;
  @Mock private RingsideActionService ringsideActionService;
  @Mock private RingsideAiService ringsideAiService;
  @Mock private com.github.javydreamercsw.management.service.GameSettingService gameSettingService;
  @Mock private com.github.javydreamercsw.management.service.world.LocationService locationService;
  @Mock private com.github.javydreamercsw.management.service.world.ArenaService arenaService;

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
            matchFulfillmentRepository,
            leagueRosterRepository,
            legacyService,
            factionService,
            ringsideActionService,
            ringsideAiService,
            retirementService,
            gameSettingService,
            random);

    when(gameSettingService.isWearAndTearEnabled()).thenReturn(true);
    when(wrestler.getId()).thenReturn(1L);
    when(wrestler.getPhysicalCondition()).thenReturn(100);
    when(segment.getWrestlers()).thenReturn(List.of(wrestler));
    when(segment.getWinners()).thenReturn(List.of(wrestler));
    when(segment.getSegmentType()).thenReturn(segmentType);
    when(segmentType.getName()).thenReturn("One on One");
    when(segment.getShow()).thenReturn(show);
    when(show.isPremiumLiveEvent()).thenReturn(false);
    when(matchFulfillmentRepository.findBySegment(segment)).thenReturn(Optional.empty());
    when(segment.getSegmentRules()).thenReturn(Set.of());
  }

  @Test
  void testApplyWearAndTear_StandardMatch() {
    when(random.nextInt(3)).thenReturn(1); // baseLoss = 1 + 1 = 2
    when(segment.isMainEvent()).thenReturn(false);

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(wrestler).setPhysicalCondition(98);
    verify(wrestlerService).save(wrestler);
    verify(retirementService).checkRetirement(wrestler);
  }

  @Test
  void testApplyWearAndTear_ExtremeMainEvent() {
    when(random.nextInt(3)).thenReturn(2); // baseLoss = 1 + 2 = 3
    when(segment.isMainEvent()).thenReturn(true); // +1

    SegmentRule extremeRule = mock(SegmentRule.class);
    when(extremeRule.getName()).thenReturn("Extreme");
    when(extremeRule.getBumpAddition())
        .thenReturn(
            com.github.javydreamercsw.management.domain.show.segment.rule.BumpAddition.NONE);
    when(segment.getSegmentRules()).thenReturn(Set.of(extremeRule)); // x2

    // (3 * 2) + 1 = 7
    segmentAdjudicationService.adjudicateMatch(segment);

    verify(wrestler).setPhysicalCondition(93);
    verify(wrestlerService).save(wrestler);
  }

  @Test
  void testApplyWearAndTear_PromoSkips() {
    when(segmentType.getName()).thenReturn("Promo");

    segmentAdjudicationService.adjudicateMatch(segment);

    verify(wrestler, never()).setPhysicalCondition(anyInt());
  }
}
