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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.feud.FeudResolutionService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import com.github.javydreamercsw.management.service.match.MatchRewardService;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.ringside.RingsideActionService;
import com.github.javydreamercsw.management.service.ringside.RingsideAiService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.Objects;
import java.util.Optional;
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
  @Mock private MatchRewardService matchRewardService;
  @Mock private MatchFulfillmentRepository matchFulfillmentRepository;

  @Mock
  private com.github.javydreamercsw.management.domain.league.LeagueRosterRepository
      leagueRosterRepository;

  @Mock private LegacyService legacyService;
  @Mock private FactionService factionService;
  @Mock private RingsideActionService ringsideActionService;
  @Mock private RingsideAiService ringsideAiService;

  @Mock
  private com.github.javydreamercsw.management.service.wrestler.RetirementService retirementService;

  @Mock private com.github.javydreamercsw.management.service.GameSettingService gameSettingService;

  @InjectMocks private SegmentAdjudicationService adjudicationService;

  private Segment promoSegment;
  private Segment matchSegment;
  private Wrestler wrestler1;
  private Wrestler wrestler2;

  @BeforeEach
  void setUp() {
    lenient().when(gameSettingService.isWearAndTearEnabled()).thenReturn(true);
    adjudicationService =
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
            ringsideActionService,
            ringsideAiService,
            retirementService,
            gameSettingService,
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

    // Default mock behavior
    org.mockito.Mockito.lenient()
        .when(matchFulfillmentRepository.findBySegment(any(Segment.class)))
        .thenReturn(Optional.empty());
  }

  @Test
  void testAdjudicateMatch_HeatUpdate() {
    // Given a match segment
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
}
