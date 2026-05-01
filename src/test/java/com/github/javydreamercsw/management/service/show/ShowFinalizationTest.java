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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRosterRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.domain.world.ArenaRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerContractRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.gm.SalaryCalculator;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.news.NewsGenerationService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShowFinalizationTest {

  @Mock private ShowRepository showRepository;
  @Mock private SegmentRepository segmentRepository;
  @Mock private LeagueRepository leagueRepository;
  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private SegmentAdjudicationService segmentAdjudicationService;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private GameSettingService gameSettingService;
  @Mock private NewsGenerationService newsGenerationService;
  @Mock private LegacyService legacyService;
  @Mock private SecurityUtils securityUtils;
  @Mock private ArenaRepository arenaRepository;
  @Mock private LeagueRosterRepository leagueRosterRepository;
  @Mock private WrestlerContractRepository contractRepository;
  @Mock private SalaryCalculator salaryCalculator;
  @Mock private CommentaryTeamRepository commentaryTeamRepository;

  private ShowService showService;

  @BeforeEach
  void setUp() {
    showService =
        new ShowService(
            showRepository,
            null,
            null,
            null,
            leagueRepository,
            java.time.Clock.systemUTC(),
            segmentAdjudicationService,
            segmentRepository,
            eventPublisher,
            wrestlerService,
            wrestlerRepository,
            gameSettingService,
            commentaryTeamRepository,
            newsGenerationService,
            legacyService,
            securityUtils,
            arenaRepository,
            leagueRosterRepository,
            contractRepository,
            salaryCalculator);

    when(showRepository.save(any(Show.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  private Wrestler makeWrestler(long id, long fans) {
    Wrestler w = new Wrestler();
    w.setId(id);
    w.setFans(fans);
    return w;
  }

  private Segment adjudicatedSegment(Show show, List<Wrestler> wrestlers) {
    Segment s = new Segment();
    s.setShow(show);
    s.setAdjudicationStatus(AdjudicationStatus.ADJUDICATED);
    SegmentType st = new SegmentType();
    st.setName("One on One");
    s.setSegmentType(st);
    wrestlers.forEach(s::addParticipant);
    return s;
  }

  private ShowTemplate weeklyTemplate() {
    ShowType st = new ShowType();
    st.setName("Weekly");
    ShowTemplate t = new ShowTemplate();
    t.setName("Weekly");
    t.setShowType(st);
    return t;
  }

  private ShowTemplate pleTemplate() {
    ShowType st = new ShowType();
    st.setName("Premium Live Event (PLE)");
    ShowTemplate t = new ShowTemplate();
    t.setName("PLE");
    t.setShowType(st);
    return t;
  }

  @Test
  void testAttendanceCappedByArenaCapacity() {
    Show show = new Show();
    show.setTemplate(weeklyTemplate());

    Arena arena = new Arena();
    arena.setCapacity(100); // tiny venue
    arena.setEnvironmentalTraits(Collections.emptySet());
    show.setArena(arena);

    // Wrestlers with huge fan counts that would exceed capacity
    Wrestler w1 = makeWrestler(1L, 500_000L);
    Wrestler w2 = makeWrestler(2L, 500_000L);
    List<Segment> segments = List.of(adjudicatedSegment(show, List.of(w1, w2)));

    Show result = showService.finalizeShow(show, segments);

    assertThat(result.getAttendance()).isLessThanOrEqualTo(100);
  }

  @Test
  void testAttendanceScalesWithFanWeight() {
    Show lowFanShow = new Show();
    lowFanShow.setTemplate(weeklyTemplate());
    lowFanShow.setArena(null);

    Show highFanShow = new Show();
    highFanShow.setTemplate(weeklyTemplate());
    highFanShow.setArena(null);

    Wrestler low1 = makeWrestler(1L, 1_000L);
    Wrestler low2 = makeWrestler(2L, 1_000L);
    Wrestler high1 = makeWrestler(3L, 100_000L);
    Wrestler high2 = makeWrestler(4L, 100_000L);

    List<Segment> lowSegments = List.of(adjudicatedSegment(lowFanShow, List.of(low1, low2)));
    List<Segment> highSegments = List.of(adjudicatedSegment(highFanShow, List.of(high1, high2)));

    Show lowResult = showService.finalizeShow(lowFanShow, lowSegments);
    Show highResult = showService.finalizeShow(highFanShow, highSegments);

    assertThat(highResult.getAttendance()).isGreaterThan(lowResult.getAttendance());
  }

  @Test
  void testPLEMultiplierApplied() {
    Show weekly = new Show();
    weekly.setTemplate(weeklyTemplate());
    weekly.setArena(null);

    Show ple = new Show();
    ple.setTemplate(pleTemplate());
    ple.setArena(null);

    Wrestler w1 = makeWrestler(1L, 50_000L);
    Wrestler w2 = makeWrestler(2L, 50_000L);

    List<Segment> weeklySegments = List.of(adjudicatedSegment(weekly, List.of(w1, w2)));
    List<Segment> pleSegments = List.of(adjudicatedSegment(ple, List.of(w1, w2)));

    Show weeklyResult = showService.finalizeShow(weekly, weeklySegments);
    Show pleResult = showService.finalizeShow(ple, pleSegments);

    assertThat(pleResult.getAttendance()).isGreaterThan(weeklyResult.getAttendance());
  }

  @Test
  void testTraitBoostApplied() {
    Arena arenaWithTrait = new Arena();
    arenaWithTrait.setCapacity(100_000);
    arenaWithTrait.setEnvironmentalTraits(Set.of("hardcore-vibe"));

    Show showWithTrait = new Show();
    showWithTrait.setTemplate(weeklyTemplate());
    showWithTrait.setArena(arenaWithTrait);

    Show showNoTrait = new Show();
    showNoTrait.setTemplate(weeklyTemplate());
    Arena plainArena = new Arena();
    plainArena.setCapacity(100_000);
    plainArena.setEnvironmentalTraits(Collections.emptySet());
    showNoTrait.setArena(plainArena);

    Wrestler w1 = makeWrestler(1L, 50_000L);
    Wrestler w2 = makeWrestler(2L, 50_000L);

    // Build a No-DQ segment for the trait show
    SegmentRule noDqRule = new SegmentRule();
    noDqRule.setName("No DQ");
    noDqRule.setNoDq(true);
    Segment noDqSegment = adjudicatedSegment(showWithTrait, List.of(w1, w2));
    noDqSegment.addSegmentRule(noDqRule);

    List<Segment> traitSegments = List.of(noDqSegment);
    List<Segment> plainSegments = List.of(adjudicatedSegment(showNoTrait, List.of(w1, w2)));

    Show traitResult = showService.finalizeShow(showWithTrait, traitSegments);
    Show plainResult = showService.finalizeShow(showNoTrait, plainSegments);

    assertThat(traitResult.getAttendance()).isGreaterThan(plainResult.getAttendance());
  }

  @Test
  void testGateRevenueAddedToLeagueBudget() {
    League league = new League();
    league.setId(1L);
    league.setBudget(BigDecimal.ZERO);

    Show show = new Show();
    show.setTemplate(weeklyTemplate());
    show.setLeague(league);
    show.setArena(null);

    Wrestler w1 = makeWrestler(1L, 10_000L);
    Wrestler w2 = makeWrestler(2L, 10_000L);
    List<Segment> segments = List.of(adjudicatedSegment(show, List.of(w1, w2)));

    when(leagueRepository.save(any(League.class))).thenAnswer(inv -> inv.getArgument(0));

    Show result = showService.finalizeShow(show, segments);

    assertThat(result.getGateRevenue()).isGreaterThan(BigDecimal.ZERO);
    ArgumentCaptor<League> leagueCaptor = ArgumentCaptor.forClass(League.class);
    verify(leagueRepository).save(leagueCaptor.capture());
    assertThat(leagueCaptor.getValue().getBudget()).isEqualTo(result.getGateRevenue());
  }

  @Test
  void testGateRevenueNotAddedWhenNoLeague() {
    Show show = new Show();
    show.setTemplate(weeklyTemplate());
    show.setLeague(null);
    show.setArena(null);

    Wrestler w1 = makeWrestler(1L, 10_000L);
    List<Segment> segments = List.of(adjudicatedSegment(show, List.of(w1)));

    Show result = showService.finalizeShow(show, segments);

    assertThat(result.getGateRevenue()).isGreaterThan(BigDecimal.ZERO);
    assertThat(result.getAttendance()).isGreaterThan(0);
    // No league save should happen
    verify(leagueRepository, org.mockito.Mockito.never()).save(any(League.class));
  }

  @Test
  void testFinalizeShowIdempotent() {
    Show show = new Show();
    show.setTemplate(weeklyTemplate());
    show.setAttendance(999);
    show.setArena(null);
    show.setLeague(null);

    Segment seg = new Segment();
    seg.setAdjudicationStatus(AdjudicationStatus.ADJUDICATED);
    SegmentType st = new SegmentType();
    st.setName("One on One");
    seg.setSegmentType(st);

    when(segmentRepository.findByShow(show)).thenReturn(List.of(seg));

    showService.finalizeShowIfComplete(show);

    // attendance was already set — show should NOT be modified
    assertThat(show.getAttendance()).isEqualTo(999);
    verify(showRepository, org.mockito.Mockito.never()).save(any(Show.class));
  }
}
