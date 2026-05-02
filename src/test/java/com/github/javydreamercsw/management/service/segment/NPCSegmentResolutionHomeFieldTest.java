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
package com.github.javydreamercsw.management.service.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.ringside.RingsideActionDataService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.lang.reflect.Field;
import java.util.Collections;
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
class NPCSegmentResolutionHomeFieldTest {

  @Mock private Show show;
  @Mock private Arena arena;
  @Mock private Location location;
  @Mock private Wrestler homeWrestler;
  @Mock private Wrestler awayWrestler;
  @Mock private RingsideActionDataService ringsideActionDataService;
  @Mock private WrestlerService wrestlerService;
  @Mock private InjuryService injuryService;

  private NPCSegmentResolutionService.TeamStatsCalculator calculator;
  private NPCSegmentResolutionServiceTestHelper helper;

  @BeforeEach
  void setUp() throws Exception {
    helper = new NPCSegmentResolutionServiceTestHelper();
    // Inject mocks so calculateTeamWeight() doesn't NPE
    Field field = NPCSegmentResolutionService.class.getDeclaredField("ringsideActionDataService");
    field.setAccessible(true);
    field.set(helper, ringsideActionDataService);

    Field wsField = NPCSegmentResolutionService.class.getDeclaredField("wrestlerService");
    wsField.setAccessible(true);
    wsField.set(helper, wrestlerService);

    Field isField = NPCSegmentResolutionService.class.getDeclaredField("injuryService");
    isField.setAccessible(true);
    isField.set(helper, injuryService);

    when(ringsideActionDataService.findAllActions()).thenReturn(Collections.emptyList());
    when(injuryService.getTotalHealthPenaltyForWrestler(anyLong(), anyLong())).thenReturn(0);

    // Mock WrestlerStates
    WrestlerState homeState =
        WrestlerState.builder().fans(10_000L).tier(WrestlerTier.MIDCARDER).build();
    WrestlerState awayState =
        WrestlerState.builder().fans(10_000L).tier(WrestlerTier.MIDCARDER).build();

    when(wrestlerService.getOrCreateState(eq(1L), anyLong())).thenReturn(homeState);
    when(wrestlerService.getOrCreateState(eq(2L), anyLong())).thenReturn(awayState);

    // Both wrestlers: equal fans and tier, no injuries, no bumps
    when(homeWrestler.getId()).thenReturn(1L);
    when(homeWrestler.getFans()).thenReturn(10_000L);
    when(homeWrestler.getFanWeight()).thenReturn(2000);
    when(homeWrestler.getTier()).thenReturn(WrestlerTier.MIDCARDER);
    when(homeWrestler.getBumps()).thenReturn(0);
    when(homeWrestler.getInjuries()).thenReturn(Collections.emptyList());
    when(homeWrestler.getFaction()).thenReturn(null);
    when(homeWrestler.getManager()).thenReturn(null);
    when(homeWrestler.getHeritageTag()).thenReturn("Japan");

    when(awayWrestler.getId()).thenReturn(2L);
    when(awayWrestler.getFans()).thenReturn(10_000L);
    when(awayWrestler.getFanWeight()).thenReturn(2000);
    when(awayWrestler.getTier()).thenReturn(WrestlerTier.MIDCARDER);
    when(awayWrestler.getBumps()).thenReturn(0);
    when(awayWrestler.getInjuries()).thenReturn(Collections.emptyList());
    when(awayWrestler.getFaction()).thenReturn(null);
    when(awayWrestler.getManager()).thenReturn(null);
    when(awayWrestler.getHeritageTag()).thenReturn("USA");

    when(location.getName()).thenReturn("Tokyo Dome");
    when(location.getCulturalTags())
        .thenReturn(Set.of("japan", "massive-scale", "respectful-silence"));
    when(arena.getLocation()).thenReturn(location);
    when(show.getArena()).thenReturn(arena);
  }

  @Test
  void testHomeFieldBoostIncreasesWinWeight() {
    calculator = helper.createCalculator(show);

    SegmentTeam homeTeam = new SegmentTeam(homeWrestler);
    SegmentTeam awayTeam = new SegmentTeam(awayWrestler);
    homeTeam.calculateTeamStats(calculator);
    awayTeam.calculateTeamStats(calculator);

    // Home wrestler should have 10% more weight than away wrestler
    assertThat(homeTeam.getTotalWeight()).isGreaterThan(awayTeam.getTotalWeight());
    double ratio = (double) homeTeam.getTotalWeight() / awayTeam.getTotalWeight();
    assertThat(ratio).isCloseTo(1.10, org.assertj.core.data.Offset.offset(0.01));
  }

  @Test
  void testNoHomeFieldWhenArenaIsNull() {
    when(show.getArena()).thenReturn(null);
    calculator = helper.createCalculator(show);

    SegmentTeam homeTeam = new SegmentTeam(homeWrestler);
    SegmentTeam awayTeam = new SegmentTeam(awayWrestler);
    homeTeam.calculateTeamStats(calculator);
    awayTeam.calculateTeamStats(calculator);

    assertThat(homeTeam.getTotalWeight()).isEqualTo(awayTeam.getTotalWeight());
  }

  @Test
  void testNoHomeFieldWhenHeritageMismatch() {
    when(homeWrestler.getHeritageTag()).thenReturn("Brazil");
    calculator = helper.createCalculator(show);

    SegmentTeam homeTeam = new SegmentTeam(homeWrestler);
    SegmentTeam awayTeam = new SegmentTeam(awayWrestler);
    homeTeam.calculateTeamStats(calculator);
    awayTeam.calculateTeamStats(calculator);

    assertThat(homeTeam.getTotalWeight()).isEqualTo(awayTeam.getTotalWeight());
  }

  /** Helper to expose TeamStatsCalculator for testing (inner class requires outer instance). */
  static class NPCSegmentResolutionServiceTestHelper extends NPCSegmentResolutionService {
    public NPCSegmentResolutionService.TeamStatsCalculator createCalculator(Show show) {
      return new TeamStatsCalculator(show);
    }
  }
}
