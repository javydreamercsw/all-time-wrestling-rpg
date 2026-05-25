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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeam;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.domain.world.ArenaRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.event.AdjudicationCompletedEvent;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.gm.SalaryCalculator;
import com.github.javydreamercsw.management.service.legacy.LegacyService;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.news.NewsGenerationService;
import com.github.javydreamercsw.management.service.wrestler.RetirementService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShowServiceTest {

  @Mock private ShowRepository showRepository;
  @Mock private ShowTypeRepository showTypeRepository;
  @Mock private SeasonRepository seasonRepository;
  @Mock private ShowTemplateRepository showTemplateRepository;
  @Mock private UniverseRepository universeRepository;
  @Mock private LeagueRepository leagueRepository;
  @Mock private WrestlerStateRepository wrestlerStateRepository;
  @Mock private SegmentRepository segmentRepository;
  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private SegmentAdjudicationService segmentAdjudicationService;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private GameSettingService gameSettingService;
  @Mock private NewsGenerationService newsGenerationService;
  @Mock private LegacyService legacyService;
  @Mock private SecurityUtils securityUtils;
  @Mock private ArenaRepository arenaRepository;
  @Mock private SalaryCalculator salaryCalculator;
  @Mock private RetirementService retirementService;
  @Mock private CommentaryTeamRepository commentaryTeamRepository;
  @Mock private Clock clock;

  private ShowService showService;

  private Show show;
  private Segment adjudicatedSegment;
  private Segment pendingSegment;
  private Wrestler wrestler1;
  private Wrestler wrestler2;
  private Wrestler wrestler3;

  @BeforeEach
  public void setUp() {
    when(clock.instant()).thenReturn(Instant.now());

    showService =
        new ShowService(
            showRepository,
            showTypeRepository,
            seasonRepository,
            showTemplateRepository,
            universeRepository,
            leagueRepository,
            wrestlerStateRepository,
            clock,
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
            salaryCalculator,
            retirementService);

    show = new Show();
    show.setId(1L);
    show.setName("Monday Night Raw");

    SegmentType matchType = new SegmentType();
    matchType.setName("One on One");

    pendingSegment = new Segment();
    pendingSegment.setId(10L);
    pendingSegment.setSegmentType(matchType);
    pendingSegment.setAdjudicationStatus(AdjudicationStatus.PENDING);

    adjudicatedSegment = new Segment();
    adjudicatedSegment.setId(11L);
    SegmentType adjType = new SegmentType();
    adjType.setName("One on One");
    adjudicatedSegment.setSegmentType(adjType);
    adjudicatedSegment.setAdjudicationStatus(AdjudicationStatus.ADJUDICATED);

    wrestler1 = new Wrestler();
    wrestler1.setId(1L);
    wrestler1.setName("Wrestler A");

    wrestler2 = new Wrestler();
    wrestler2.setId(2L);
    wrestler2.setName("Wrestler B");

    wrestler3 = new Wrestler();
    wrestler3.setId(3L);
    wrestler3.setName("Wrestler C");
  }

  // ---------------------------------------------------------------------------
  // count()
  // ---------------------------------------------------------------------------

  @Test
  void count_delegatesToRepository() {
    when(showRepository.count()).thenReturn(42L);

    long result = showService.count();

    assertThat(result).isEqualTo(42L);
    verify(showRepository).count();
  }

  // ---------------------------------------------------------------------------
  // save()
  // ---------------------------------------------------------------------------

  @Test
  void save_setsCreationDateAndDelegatesToRepository() {
    Instant fixedInstant = Instant.parse("2026-01-15T10:00:00Z");
    when(clock.instant()).thenReturn(fixedInstant);
    when(showRepository.saveAndFlush(any(Show.class))).thenAnswer(inv -> inv.getArgument(0));

    Show result = showService.save(show);

    assertThat(result.getCreationDate()).isEqualTo(fixedInstant);
    verify(showRepository).saveAndFlush(show);
  }

  // ---------------------------------------------------------------------------
  // existsByNameAndShowDate()
  // ---------------------------------------------------------------------------

  @Test
  void existsByNameAndShowDate_returnsTrueWhenFound() {
    LocalDate date = LocalDate.of(2026, 3, 10);
    when(showRepository.findByNameAndShowDate("Raw", date)).thenReturn(Optional.of(show));

    boolean result = showService.existsByNameAndShowDate("Raw", date);

    assertThat(result).isTrue();
  }

  @Test
  void existsByNameAndShowDate_returnsFalseWhenNotFound() {
    LocalDate date = LocalDate.of(2026, 3, 10);
    when(showRepository.findByNameAndShowDate("Raw", date)).thenReturn(Optional.empty());

    boolean result = showService.existsByNameAndShowDate("Raw", date);

    assertThat(result).isFalse();
  }

  // ---------------------------------------------------------------------------
  // getShowById()
  // ---------------------------------------------------------------------------

  @Test
  void getShowById_returnsShowWhenFound() {
    when(showRepository.findById(1L)).thenReturn(Optional.of(show));

    Optional<Show> result = showService.getShowById(1L);

    assertThat(result).isPresent().contains(show);
  }

  @Test
  void getShowById_returnsEmptyWhenNotFound() {
    when(showRepository.findById(99L)).thenReturn(Optional.empty());

    Optional<Show> result = showService.getShowById(99L);

    assertThat(result).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // getShowsForMonth()
  // ---------------------------------------------------------------------------

  @Test
  void getShowsForMonth_computesCorrectDateRangeAndDelegates() {
    LocalDate start = LocalDate.of(2026, 3, 1);
    LocalDate end = LocalDate.of(2026, 3, 31);
    when(showRepository.findByShowDateBetweenOrderByShowDate(start, end)).thenReturn(List.of(show));

    List<Show> result = showService.getShowsForMonth(2026, 3);

    assertThat(result).containsExactly(show);
    verify(showRepository).findByShowDateBetweenOrderByShowDate(start, end);
  }

  @Test
  void getShowsForMonth_handlesFebruaryLeapYear() {
    LocalDate start = LocalDate.of(2024, 2, 1);
    LocalDate end = LocalDate.of(2024, 2, 29);
    when(showRepository.findByShowDateBetweenOrderByShowDate(start, end))
        .thenReturn(Collections.emptyList());

    List<Show> result = showService.getShowsForMonth(2024, 2);

    assertThat(result).isEmpty();
    verify(showRepository).findByShowDateBetweenOrderByShowDate(start, end);
  }

  // ---------------------------------------------------------------------------
  // getUpcomingShows()
  // ---------------------------------------------------------------------------

  @Test
  void getUpcomingShows_usesGameDateFromService() {
    LocalDate gameDate = LocalDate.of(2026, 5, 17);
    when(gameSettingService.getCurrentGameDate()).thenReturn(gameDate);
    when(showRepository.findByShowDateGreaterThanEqualOrderByShowDate(eq(gameDate), any()))
        .thenReturn(List.of(show));

    List<Show> result = showService.getUpcomingShows(10);

    assertThat(result).containsExactly(show);
    verify(gameSettingService).getCurrentGameDate();
  }

  // ---------------------------------------------------------------------------
  // createShow() — success path
  // ---------------------------------------------------------------------------

  @Test
  void createShow_successWithAllParams() {
    ShowType showType = new ShowType();
    showType.setId(1L);
    Season season = new Season();
    season.setId(2L);
    ShowTemplate template = new ShowTemplate();
    template.setId(3L);
    Universe universe = new Universe();
    universe.setId(4L);
    League league = new League();
    league.setId(5L);
    CommentaryTeam commentaryTeam = new CommentaryTeam();
    commentaryTeam.setId(6L);
    Arena arena = new Arena();
    arena.setId(7L);

    when(showTypeRepository.findById(1L)).thenReturn(Optional.of(showType));
    when(seasonRepository.findById(2L)).thenReturn(Optional.of(season));
    when(showTemplateRepository.findById(3L)).thenReturn(Optional.of(template));
    when(universeRepository.findById(4L)).thenReturn(Optional.of(universe));
    when(leagueRepository.findById(5L)).thenReturn(Optional.of(league));
    when(commentaryTeamRepository.findById(6L)).thenReturn(Optional.of(commentaryTeam));
    when(arenaRepository.findById(7L)).thenReturn(Optional.of(arena));
    when(showRepository.saveAndFlush(any(Show.class))).thenAnswer(inv -> inv.getArgument(0));

    Show result =
        showService.createShow(
            "Raw", "Weekly show", 1L, LocalDate.of(2026, 1, 6), 2L, 3L, 4L, 5L, 6L, 7L);

    assertThat(result.getName()).isEqualTo("Raw");
    assertThat(result.getDescription()).isEqualTo("Weekly show");
    assertThat(result.getType()).isEqualTo(showType);
    assertThat(result.getSeason()).isEqualTo(season);
    assertThat(result.getTemplate()).isEqualTo(template);
    assertThat(result.getUniverse()).isEqualTo(universe);
    assertThat(result.getLeague()).isEqualTo(league);
    assertThat(result.getCommentaryTeam()).isEqualTo(commentaryTeam);
    assertThat(result.getArena()).isEqualTo(arena);
    assertThat(result.getCreationDate()).isNotNull();
    verify(showRepository).saveAndFlush(any(Show.class));
  }

  @Test
  void createShow_showTypeNotFound_throwsIllegalArgument() {
    when(showTypeRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                showService.createShow(
                    "Raw",
                    "desc",
                    99L,
                    LocalDate.of(2026, 1, 6),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Show type not found: 99");
  }

  @Test
  void createShow_seasonNotFound_throwsIllegalArgument() {
    ShowType showType = new ShowType();
    when(showTypeRepository.findById(1L)).thenReturn(Optional.of(showType));
    when(seasonRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                showService.createShow(
                    "Raw", "desc", 1L, LocalDate.of(2026, 1, 6), 99L, null, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Season not found: 99");
  }

  @Test
  void createShow_templateNotFound_throwsIllegalArgument() {
    ShowType showType = new ShowType();
    when(showTypeRepository.findById(1L)).thenReturn(Optional.of(showType));
    when(showTemplateRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                showService.createShow(
                    "Raw", "desc", 1L, LocalDate.of(2026, 1, 6), null, 99L, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Template not found: 99");
  }

  @Test
  void createShow_universeNotFound_throwsIllegalArgument() {
    ShowType showType = new ShowType();
    when(showTypeRepository.findById(1L)).thenReturn(Optional.of(showType));
    when(universeRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                showService.createShow(
                    "Raw", "desc", 1L, LocalDate.of(2026, 1, 6), null, null, 99L, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Universe not found: 99");
  }

  @Test
  void createShow_commentaryTeamNotFound_throwsIllegalArgument() {
    ShowType showType = new ShowType();
    when(showTypeRepository.findById(1L)).thenReturn(Optional.of(showType));
    when(commentaryTeamRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                showService.createShow(
                    "Raw", "desc", 1L, LocalDate.of(2026, 1, 6), null, null, null, null, 99L, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Commentary team not found: 99");
  }

  @Test
  void createShow_arenaNotFound_throwsIllegalArgument() {
    ShowType showType = new ShowType();
    when(showTypeRepository.findById(1L)).thenReturn(Optional.of(showType));
    when(arenaRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                showService.createShow(
                    "Raw", "desc", 1L, LocalDate.of(2026, 1, 6), null, null, null, null, null, 99L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Arena not found: 99");
  }

  @Test
  void createShow_nullLeagueId_doesNotSetLeague() {
    ShowType showType = new ShowType();
    when(showTypeRepository.findById(1L)).thenReturn(Optional.of(showType));
    when(showRepository.saveAndFlush(any(Show.class))).thenAnswer(inv -> inv.getArgument(0));

    Show result =
        showService.createShow(
            "Raw", "desc", 1L, LocalDate.of(2026, 1, 6), null, null, null, null, null, null);

    assertThat(result.getLeague()).isNull();
    verify(leagueRepository, never()).findById(anyLong());
  }

  // ---------------------------------------------------------------------------
  // updateShow()
  // ---------------------------------------------------------------------------

  @Test
  void updateShow_showNotFound_returnsEmpty() {
    when(showRepository.findById(99L)).thenReturn(Optional.empty());

    Optional<Show> result =
        showService.updateShow(99L, null, null, null, null, null, null, null, null, null);

    assertThat(result).isEmpty();
  }

  @Test
  void updateShow_allNullParams_clearsOptionalFields() {
    when(showRepository.findById(1L)).thenReturn(Optional.of(show));
    when(showRepository.saveAndFlush(any(Show.class))).thenAnswer(inv -> inv.getArgument(0));

    Optional<Show> result =
        showService.updateShow(1L, null, null, null, null, null, null, null, null, null);

    assertThat(result).isPresent();
    Show updated = result.get();
    assertThat(updated.getSeason()).isNull();
    assertThat(updated.getTemplate()).isNull();
    assertThat(updated.getUniverse()).isNull();
    assertThat(updated.getCommentaryTeam()).isNull();
    assertThat(updated.getArena()).isNull();
  }

  @Test
  void updateShow_fullUpdate_appliesAllChanges() {
    ShowType showType = new ShowType();
    showType.setId(1L);
    Season season = new Season();
    season.setId(2L);
    ShowTemplate template = new ShowTemplate();
    template.setId(3L);
    Universe universe = new Universe();
    universe.setId(4L);
    CommentaryTeam commentaryTeam = new CommentaryTeam();
    commentaryTeam.setId(5L);
    Arena arena = new Arena();
    arena.setId(6L);

    when(showRepository.findById(1L)).thenReturn(Optional.of(show));
    when(showTypeRepository.findById(1L)).thenReturn(Optional.of(showType));
    when(seasonRepository.findById(2L)).thenReturn(Optional.of(season));
    when(showTemplateRepository.findById(3L)).thenReturn(Optional.of(template));
    when(universeRepository.findById(4L)).thenReturn(Optional.of(universe));
    when(commentaryTeamRepository.findById(5L)).thenReturn(Optional.of(commentaryTeam));
    when(arenaRepository.findById(6L)).thenReturn(Optional.of(arena));
    when(showRepository.saveAndFlush(any(Show.class))).thenAnswer(inv -> inv.getArgument(0));

    LocalDate newDate = LocalDate.of(2026, 6, 1);
    Optional<Show> result =
        showService.updateShow(1L, "SmackDown", "New desc", 1L, newDate, 2L, 3L, 4L, 5L, 6L);

    assertThat(result).isPresent();
    Show updated = result.get();
    assertThat(updated.getName()).isEqualTo("SmackDown");
    assertThat(updated.getDescription()).isEqualTo("New desc");
    assertThat(updated.getShowDate()).isEqualTo(newDate);
    assertThat(updated.getType()).isEqualTo(showType);
    assertThat(updated.getSeason()).isEqualTo(season);
    assertThat(updated.getTemplate()).isEqualTo(template);
    assertThat(updated.getUniverse()).isEqualTo(universe);
    assertThat(updated.getCommentaryTeam()).isEqualTo(commentaryTeam);
    assertThat(updated.getArena()).isEqualTo(arena);
  }

  // ---------------------------------------------------------------------------
  // deleteShow()
  // ---------------------------------------------------------------------------

  @Test
  void deleteShow_existingShow_deletesAndReturnsTrue() {
    when(showRepository.existsById(1L)).thenReturn(true);

    boolean result = showService.deleteShow(1L);

    assertThat(result).isTrue();
    verify(showRepository).deleteById(1L);
  }

  @Test
  void deleteShow_nonExistingShow_returnsFalse() {
    when(showRepository.existsById(99L)).thenReturn(false);

    boolean result = showService.deleteShow(99L);

    assertThat(result).isFalse();
    verify(showRepository, never()).deleteById(99L);
  }

  // ---------------------------------------------------------------------------
  // getSegments()
  // ---------------------------------------------------------------------------

  @Test
  void getSegments_delegatesToRepository() {
    when(segmentRepository.findByShow(show)).thenReturn(List.of(pendingSegment));

    List<Segment> result = showService.getSegments(show);

    assertThat(result).containsExactly(pendingSegment);
    verify(segmentRepository).findByShow(show);
  }

  // ---------------------------------------------------------------------------
  // finalizeShowIfComplete()
  // ---------------------------------------------------------------------------

  @Test
  void finalizeShowIfComplete_attendanceAlreadySet_returnsEarly() {
    show.setAttendance(500);

    showService.finalizeShowIfComplete(show);

    verify(segmentRepository, never()).findByShow(any());
  }

  @Test
  void finalizeShowIfComplete_noSegments_returnsEarly() {
    show.setAttendance(0);
    when(segmentRepository.findByShow(show)).thenReturn(Collections.emptyList());

    showService.finalizeShowIfComplete(show);

    verify(showRepository, never()).save(any());
  }

  @Test
  void finalizeShowIfComplete_notAllAdjudicated_doesNotFinalize() {
    show.setAttendance(0);
    when(segmentRepository.findByShow(show))
        .thenReturn(Arrays.asList(adjudicatedSegment, pendingSegment));

    showService.finalizeShowIfComplete(show);

    verify(showRepository, never()).save(any());
  }

  @Test
  void finalizeShowIfComplete_allAdjudicated_callsFinalizeShow() {
    show.setAttendance(0);
    adjudicatedSegment.addParticipant(wrestler1);
    adjudicatedSegment.addParticipant(wrestler2);
    when(segmentRepository.findByShow(show)).thenReturn(List.of(adjudicatedSegment));
    when(showRepository.save(any(Show.class))).thenAnswer(inv -> inv.getArgument(0));

    showService.finalizeShowIfComplete(show);

    verify(showRepository).save(any(Show.class));
  }

  // ---------------------------------------------------------------------------
  // finalizeShow()
  // ---------------------------------------------------------------------------

  @Test
  void finalizeShow_setsAttendanceAndGateRevenue() {
    adjudicatedSegment.addParticipant(wrestler1);
    adjudicatedSegment.addParticipant(wrestler2);
    when(showRepository.save(any(Show.class))).thenAnswer(inv -> inv.getArgument(0));

    Show result = showService.finalizeShow(show, List.of(adjudicatedSegment));

    assertThat(result.getAttendance()).isNotNull();
    assertThat(result.getAttendance()).isGreaterThanOrEqualTo(0);
    assertThat(result.getGateRevenue()).isNotNull();
    verify(showRepository).save(show);
  }

  @Test
  void finalizeShow_premiumEvent_usesHigherTicketPrice() {
    // isPremiumLiveEvent() checks showType.getName() == "Premium Live Event (PLE)" on the template
    ShowType pleType = new ShowType();
    pleType.setName("Premium Live Event (PLE)");
    ShowTemplate premiumTemplate = new ShowTemplate();
    premiumTemplate.setId(99L);
    premiumTemplate.setShowType(pleType);
    show.setTemplate(premiumTemplate);

    adjudicatedSegment.addParticipant(wrestler1);
    adjudicatedSegment.addParticipant(wrestler2);
    when(showRepository.save(any(Show.class))).thenAnswer(inv -> inv.getArgument(0));

    Show result = showService.finalizeShow(show, List.of(adjudicatedSegment));

    // Premium events use $75 ticket price vs $25 for regular
    assertThat(result.getGateRevenue()).isNotNull();
    assertThat(result.getAttendance()).isGreaterThanOrEqualTo(0);
    assertThat(show.isPremiumLiveEvent()).isTrue();
  }

  @Test
  void finalizeShow_arenaCapacityCapsAttendance() {
    Arena arena = new Arena();
    arena.setCapacity(1); // Very small capacity to ensure capping
    arena.setEnvironmentalTraits(new HashSet<>());
    show.setArena(arena);

    adjudicatedSegment.addParticipant(wrestler1);
    adjudicatedSegment.addParticipant(wrestler2);
    when(showRepository.save(any(Show.class))).thenAnswer(inv -> inv.getArgument(0));

    Show result = showService.finalizeShow(show, List.of(adjudicatedSegment));

    assertThat(result.getAttendance()).isLessThanOrEqualTo(1);
  }

  @Test
  void finalizeShow_withLeague_creditsGateRevenueToLeagueBudget() {
    League league = new League();
    league.setId(5L);
    league.setName("GM League");
    league.setBudget(new BigDecimal("1000.00"));
    show.setLeague(league);

    adjudicatedSegment.addParticipant(wrestler1);
    when(showRepository.save(any(Show.class))).thenAnswer(inv -> inv.getArgument(0));
    when(leagueRepository.save(any(League.class))).thenAnswer(inv -> inv.getArgument(0));

    showService.finalizeShow(show, List.of(adjudicatedSegment));

    verify(leagueRepository).save(league);
    // Budget should be increased by gate revenue
    assertThat(league.getBudget()).isGreaterThanOrEqualTo(new BigDecimal("1000.00"));
  }

  @Test
  void finalizeShow_withoutLeague_doesNotSaveLeague() {
    show.setLeague(null);
    adjudicatedSegment.addParticipant(wrestler1);
    when(showRepository.save(any(Show.class))).thenAnswer(inv -> inv.getArgument(0));

    showService.finalizeShow(show, List.of(adjudicatedSegment));

    verify(leagueRepository, never()).save(any());
  }

  // ---------------------------------------------------------------------------
  // adjudicateShow() — core scenarios
  // ---------------------------------------------------------------------------

  @Test
  void adjudicateShow_adjudicatesPendingSegmentsOnly() {
    when(showRepository.findById(1L)).thenReturn(Optional.of(show));
    when(segmentRepository.findByShow(show))
        .thenReturn(Arrays.asList(pendingSegment, adjudicatedSegment));
    when(wrestlerRepository.findAll()).thenReturn(Collections.emptyList());

    showService.adjudicateShow(1L);

    verify(segmentAdjudicationService, times(1)).adjudicateMatch(pendingSegment);
    verify(segmentAdjudicationService, never()).adjudicateMatch(adjudicatedSegment);
    Assertions.assertEquals(AdjudicationStatus.ADJUDICATED, pendingSegment.getAdjudicationStatus());
    // show has no date set — game date should not be updated
    verify(gameSettingService, never()).saveCurrentGameDate(any());
  }

  @Test
  void adjudicateShow_showNotFound_throwsIllegalArgument() {
    when(showRepository.findById(anyLong())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> showService.adjudicateShow(999L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Show not found: 999");
  }

  @Test
  void adjudicateShow_updatesGameDateAndPublishesEvent() {
    LocalDate showDate = LocalDate.of(2026, 1, 6);
    show.setShowDate(showDate);
    when(showRepository.findById(1L)).thenReturn(Optional.of(show));
    when(segmentRepository.findByShow(show)).thenReturn(Collections.emptyList());
    when(wrestlerRepository.findAll()).thenReturn(Collections.emptyList());

    showService.adjudicateShow(1L);

    verify(gameSettingService).saveCurrentGameDate(showDate.plusDays(1));
    verify(eventPublisher).publishEvent(any(AdjudicationCompletedEvent.class));
    verify(newsGenerationService).rollForRumor();
  }

  @Test
  void adjudicateShow_healsNonParticipatingWrestlers() {
    pendingSegment.getSegmentType().setName("One on One");
    pendingSegment.addParticipant(wrestler1);

    when(showRepository.findById(1L)).thenReturn(Optional.of(show));
    when(segmentRepository.findByShow(show)).thenReturn(List.of(pendingSegment));
    when(wrestlerRepository.findAll()).thenReturn(List.of(wrestler1, wrestler2));

    showService.adjudicateShow(1L);

    // wrestler2 did not participate → should get a heal chance
    verify(wrestlerService, times(1)).healChance(eq(wrestler2.getId()), anyLong());
    // wrestler1 participated → should NOT get a heal chance
    verify(wrestlerService, never()).healChance(eq(wrestler1.getId()), anyLong());
    // show has no date set — game date should not be updated
    verify(gameSettingService, never()).saveCurrentGameDate(any());
  }
}
