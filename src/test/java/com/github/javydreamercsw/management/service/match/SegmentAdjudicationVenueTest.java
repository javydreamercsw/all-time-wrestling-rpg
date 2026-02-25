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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.campaign.AlignmentType;
import com.github.javydreamercsw.management.domain.campaign.WrestlerAlignment;
import com.github.javydreamercsw.management.domain.league.LeagueRosterRepository;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.GameSettingService;
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
class SegmentAdjudicationVenueTest {

  @Mock private RivalryService rivalryService;
  @Mock private WrestlerService wrestlerService;
  @Mock private FeudResolutionService feudResolutionService;
  @Mock private MultiWrestlerFeudService feudService;
  @Mock private Random random;
  @Mock private Segment segment;
  @Mock private Wrestler wrestler;
  @Mock private SegmentType segmentType;
  @Mock private Show show;
  @Mock private Arena arena;
  @Mock private Location location;
  @Mock private WrestlerAlignment alignment;
  @Mock private TitleService titleService;
  @Mock private MatchFulfillmentRepository matchFulfillmentRepository;
  @Mock private LeagueRosterRepository leagueRosterRepository;
  @Mock private LegacyService legacyService;
  @Mock private FactionService factionService;
  @Mock private RingsideActionService ringsideActionService;
  @Mock private RingsideAiService ringsideAiService;
  @Mock private RetirementService retirementService;
  @Mock private GameSettingService gameSettingService;

  private SegmentAdjudicationService adjudicationService;

  @BeforeEach
  void setUp() {
    adjudicationService =
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

    when(segment.getShow()).thenReturn(show);
    when(show.getArena()).thenReturn(arena);
    when(arena.getLocation()).thenReturn(location);
    when(segment.getWrestlers()).thenReturn(List.of(wrestler));
    when(segment.getWinners()).thenReturn(List.of(wrestler));
    when(segment.getSegmentType()).thenReturn(segmentType);
    when(segmentType.getName()).thenReturn("Match");
    when(wrestler.getId()).thenReturn(1L);
    when(wrestler.getName()).thenReturn("Test Wrestler");
    when(wrestler.getAlignment()).thenReturn(alignment);
    when(alignment.getAlignmentType()).thenReturn(AlignmentType.HEEL);

    // Fixed rolls for deterministic testing
    // Match roll: 1 (no quality bonus)
    // Winner roll: 2d6 + 3 = 2+3+3 = 8
    // Total base: 8,000
    when(random.nextInt(anyInt())).thenReturn(0); // For various DiceBag rolls
    when(random.nextInt(20)).thenReturn(0); // Match roll 1
    when(random.nextInt(6)).thenReturn(2); // 2d6 rolls (returns 2, which is 3 on 1-based d6)
  }

  @Test
  void testAlignmentBiasBonus_HeelInHeelArena() {
    when(arena.getAlignmentBias()).thenReturn(Arena.AlignmentBias.HEEL_FAVORABLE);
    when(wrestler.getHeritageTag()).thenReturn("Unknown");
    when(location.getName()).thenReturn("Nowhere");
    when(location.getCulturalTags()).thenReturn(Set.of());

    adjudicationService.adjudicateMatch(segment);

    // Base 9,000 (3+3+3) * 1.25 = 11,250
    verify(wrestlerService).awardFans(eq(1L), eq(11250L));
  }

  @Test
  void testHomeTerritoryBonus() {
    when(arena.getAlignmentBias()).thenReturn(Arena.AlignmentBias.NEUTRAL);
    when(wrestler.getHeritageTag()).thenReturn("Pittsburgh");
    when(location.getName()).thenReturn("Pittsburgh Citadel");
    when(location.getCulturalTags()).thenReturn(Set.of("USA", "Industrial"));

    adjudicationService.adjudicateMatch(segment);

    // Base 9,000 * 1.10 = 9,900
    verify(wrestlerService).awardFans(eq(1L), eq(9900L));
  }

  @Test
  void testCombinedVenueBonuses() {
    when(arena.getAlignmentBias()).thenReturn(Arena.AlignmentBias.HEEL_FAVORABLE);
    when(wrestler.getHeritageTag()).thenReturn("Japan");
    when(location.getName()).thenReturn("Osaka Cyber-District");
    when(location.getCulturalTags()).thenReturn(Set.of("Japan", "Future"));

    adjudicationService.adjudicateMatch(segment);

    // Base 9,000 * (1.0 + 0.25 + 0.10) = 9,000 * 1.35 = 12,150
    verify(wrestlerService).awardFans(eq(1L), eq(12150L));
  }
}
