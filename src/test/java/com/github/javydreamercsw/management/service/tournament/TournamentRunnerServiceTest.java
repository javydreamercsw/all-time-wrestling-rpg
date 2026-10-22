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
package com.github.javydreamercsw.management.service.tournament;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.segment.NPCSegmentResolutionService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.title.TitleOpportunityService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for the standalone tournament runner (ATW-p0sz).
 *
 * <p>Uses a real {@link BracketMatchResolver} over mocked collaborators so the loop state (open
 * matches, winners) advances coherently — a mock on the resolver's scan would freeze the run loop
 * on the same match forever.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TournamentRunnerServiceTest {

  @Mock private TournamentService tournamentService;
  @Mock private NPCSegmentResolutionService segmentResolutionService;
  @Mock private SegmentService segmentService;
  @Mock private SegmentTypeService segmentTypeService;
  @Mock private ShowService showService;
  @Mock private ShowTypeService showTypeService;
  @Mock private UniverseContextService universeContextService;
  @Mock private TitleOpportunityService titleOpportunityService;

  private TournamentRunnerService service;
  private BracketMatchResolver resolver;

  private Show existingShow;
  private ShowType showType;
  private SegmentType oneOnOne;
  private Tournament tournament;
  private Wrestler alice;
  private Wrestler bob;

  @BeforeEach
  void setUp() {
    resolver =
        new BracketMatchResolver(tournamentService, segmentResolutionService, segmentTypeService);
    service =
        new TournamentRunnerService(
            tournamentService,
            resolver,
            segmentService,
            showService,
            showTypeService,
            universeContextService,
            titleOpportunityService);

    showType = new ShowType();
    showType.setId(2L);
    showType.setName("Weekly");
    lenient().when(showTypeService.findAll()).thenReturn(List.of(showType));
    lenient().when(universeContextService.getCurrentUniverseId()).thenReturn(1L);

    existingShow = new Show();
    existingShow.setId(1L);
    existingShow.setName("Existing Show");
    existingShow.setShowDate(LocalDate.of(2026, 10, 21));
    existingShow.setType(showType);

    oneOnOne = new SegmentType();
    oneOnOne.setId(10L);
    oneOnOne.setName("One on One");

    tournament = new Tournament();
    tournament.setId(5L);
    tournament.setName("Crown Cup");
    tournament.setFormatId("SINGLE_ELIMINATION");
    tournament.setStatus(TournamentStatus.IN_PROGRESS);

    alice = wrestler(1L, "Alice");
    bob = wrestler(2L, "Bob");

    // The real resolver resolves round types through the format + catalog: default to a
    // single-elimination format whose one_on_one rounds exist in the catalog. Tests override
    // findFormat when they need different bracket math.
    stubFormat(new SingleElimFormatStub(1, () -> false));
    // The runner re-fetches the tournament inside its transaction (the view passes a detached
    // instance) — the stub hands back the fixture, so tests assert on the same object graph.
    lenient().when(tournamentService.findByIdWithDetails(5L)).thenReturn(Optional.of(tournament));
    lenient().when(segmentTypeService.findByCode("one_on_one")).thenReturn(Optional.of(oneOnOne));
    lenient().when(showService.getShowById(1L)).thenReturn(Optional.of(existingShow));
    // Auto-create path default: the created show is the existing fixture (tests that assert on
    // the createShow args re-stub this with their own capture).
    lenient()
        .when(
            showService.createShow(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(existingShow);

    // The stipulation hierarchy's fixedRule tier → fallback (mirrors TournamentService).
    lenient()
        .when(tournamentService.resolveRoundStipulation(any(), any(), any(), any()))
        .thenAnswer(
            inv -> {
              TournamentRound r = inv.getArgument(1);
              return r.getFixedRule() != null ? r.getFixedRule().getName() : inv.getArgument(3);
            });
    // recordMatchResult really mutates the bracket (eliminates losers, stamps the winner) so the
    // run loop's open-match scan observes progress.
    lenient()
        .doAnswer(
            inv -> {
              TournamentMatch m = inv.getArgument(0);
              TournamentEntry winner = inv.getArgument(1);
              for (TournamentEntry entrant : m.entrants()) {
                if (!entrant.equals(winner)) {
                  entrant.setStatus(TournamentEntryStatus.ELIMINATED);
                }
              }
              m.setWinner(winner);
              return null;
            })
        .when(tournamentService)
        .recordMatchResult(any(), any());
  }

  @Test
  void runRound_resolvesEveryOpenMatchOfCurrentRound() {
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    TournamentMatch m1 = match(1, aliceEntry, bobEntry);
    TournamentMatch m2 = match(1, entry(wrestler(3L, "Cara"), 3), entry(wrestler(4L, "Dave"), 4));
    TournamentRound round1 = round(1, m1, m2);
    attach(tournament, List.of(aliceEntry, bobEntry), List.of(round1));
    Segment booked = singles(alice, bob, alice);
    stubBookedSegment(booked);

    TournamentRunnerService.RoundRunReport report = service.runRound(tournament, 1L);

    assertEquals(2, report.matchesPlayed());
    assertEquals(existingShow, report.hostShow());
    verify(segmentResolutionService, times(2))
        .resolveTeamSegment(any(), any(), eq(oneOnOne), eq(existingShow), anyString());
    // First resolved match activates the round; every match records a winner.
    verify(tournamentService, times(2)).markRoundInProgress(round1);
    verify(tournamentService, times(2)).recordMatchResult(any(), any());
    assertEquals(TournamentEntryStatus.ELIMINATED, bobEntry.getStatus());
    assertEquals(booked, m1.getSegment());
  }

  @Test
  void runRound_withNoShows_autoCreatesTournamentNightShow() {
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    attach(
        tournament,
        List.of(aliceEntry, bobEntry),
        List.of(round(1, match(1, aliceEntry, bobEntry))));
    stubBookedSegment(singles(alice, bob, alice));
    Show created = new Show();
    created.setId(9L);
    created.setName("Crown Cup Night");
    created.setType(showType);
    when(showService.createShow(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(created);

    TournamentRunnerService.RoundRunReport report = service.runRound(tournament, null);

    assertEquals(created, report.hostShow());
    verify(showService)
        .createShow(
            eq("Crown Cup Night"),
            anyString(),
            eq(2L),
            any(LocalDate.class),
            any(),
            any(),
            eq(1L),
            any(),
            any(),
            any());
  }

  @Test
  void runRound_blankName_fallsBackToGenericTournamentNight() {
    tournament.setName("");
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    attach(
        tournament,
        List.of(aliceEntry, bobEntry),
        List.of(round(1, match(1, aliceEntry, bobEntry))));
    stubBookedSegment(singles(alice, bob, alice));
    when(showService.createShow(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(existingShow);

    service.runRound(tournament, null);

    verify(showService)
        .createShow(
            eq("Tournament Night"),
            anyString(),
            eq(2L),
            any(LocalDate.class),
            any(),
            any(),
            eq(1L),
            any(),
            any(),
            any());
  }

  @Test
  void runRound_withRequestedShowId_usesThatShow() {
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    attach(
        tournament,
        List.of(aliceEntry, bobEntry),
        List.of(round(1, match(1, aliceEntry, bobEntry))));
    when(showService.getShowById(1L)).thenReturn(Optional.of(existingShow));
    stubBookedSegment(singles(alice, bob, alice));

    TournamentRunnerService.RoundRunReport report = service.runRound(tournament, 1L);

    assertEquals(existingShow, report.hostShow());
    verify(showService, never())
        .createShow(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void runRound_unknownShowId_throwsCleanly() {
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    attach(
        tournament,
        List.of(aliceEntry, bobEntry),
        List.of(round(1, match(1, aliceEntry, bobEntry))));
    when(showService.getShowById(404L)).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> service.runRound(tournament, 404L));
    verify(segmentResolutionService, never()).resolveTeamSegment(any(), any(), any(), any(), any());
  }

  @Test
  void runRound_noShowTypesConfigured_throwsWithClearMessage() {
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    attach(
        tournament,
        List.of(aliceEntry, bobEntry),
        List.of(round(1, match(1, aliceEntry, bobEntry))));
    when(showTypeService.findAll()).thenReturn(List.of());

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> service.runRound(tournament, null));
    assertTrue(ex.getMessage().contains("No show types"));
  }

  @Test
  void runRound_honorsRoundFixedRule() {
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    TournamentMatch m1 = match(1, aliceEntry, bobEntry);
    TournamentRound round1 = round(1, m1);
    SegmentRule fixed = new SegmentRule();
    fixed.setId(30L);
    fixed.setName("No DQ");
    round1.setFixedRule(fixed);
    attach(tournament, List.of(aliceEntry, bobEntry), List.of(round1));
    stubBookedSegment(singles(alice, bob, alice));

    service.runRound(tournament, null);

    verify(tournamentService).resolveRoundStipulation(eq(tournament), eq(round1), any(), any());
    verify(segmentResolutionService)
        .resolveTeamSegment(any(), any(), eq(oneOnOne), any(), eq("No DQ"));
    verify(tournamentService).recordMatchResult(eq(m1), any());
  }

  @Test
  void runRound_bracketFinalWithLinkedTitle_flagsTitleSegment() {
    Title title = new Title();
    title.setId(7L);
    title.setName("World Championship");
    tournament.setLinkedTitle(title);
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    TournamentMatch m1 = match(1, aliceEntry, bobEntry);
    attach(tournament, List.of(aliceEntry, bobEntry), List.of(round(1, m1)));
    // 2 entrants → 1 total match; it is unbooked → this match is the bracket final.
    stubFormat(new SingleElimFormatStub(1, () -> false));
    Segment booked = singles(alice, bob, alice);
    when(segmentResolutionService.resolveTeamSegment(any(), any(), any(), any(), any()))
        .thenReturn(booked);

    service.runRound(tournament, null);

    assertTrue(booked.getIsTitleSegment());
    assertTrue(booked.getTitles().contains(title));
  }

  @Test
  void runRound_completesRoundAndAdvances() {
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    attach(
        tournament,
        List.of(aliceEntry, bobEntry),
        List.of(round(1, match(1, aliceEntry, bobEntry))));
    stubBookedSegment(singles(alice, bob, alice));

    TournamentRunnerService.RoundRunReport report = service.runRound(tournament, null);

    // Round decided → advance ran; the format is unresolvable → not-complete reported cleanly.
    assertFalse(report.tournamentComplete());
    verify(tournamentService).advanceToNextRound(tournament);
  }

  @Test
  void runTournament_completesSingleEliminationBracket() {
    // 4-entrant single elimination: 2 round-1 matches + 1 final. advanceToNextRound is stubbed
    // to generate the final the way the real format would, and the format reports complete once
    // the final has a winner.
    Wrestler cara = wrestler(3L, "Cara");
    Wrestler dave = wrestler(4L, "Dave");
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    TournamentEntry caraEntry = entry(cara, 3);
    TournamentEntry daveEntry = entry(dave, 4);
    TournamentMatch r1m1 = match(1, aliceEntry, bobEntry);
    TournamentMatch r1m2 = match(1, caraEntry, daveEntry);
    TournamentRound round1 = round(1, r1m1, r1m2);
    attach(tournament, List.of(aliceEntry, bobEntry, caraEntry, daveEntry), List.of(round1));

    TournamentMatch finalMatch = match(2, aliceEntry, caraEntry);
    TournamentRound finalRound = round(2, finalMatch);
    when(tournamentService.advanceToNextRound(tournament))
        .thenAnswer(
            invocation -> {
              if (!tournament.getRounds().contains(finalRound)) {
                finalMatch.setRound(finalRound);
                tournament.getRounds().add(finalRound);
                aliceEntry.setStatus(TournamentEntryStatus.WINNER); // markWinner's stamp
              }
              return List.of(finalMatch);
            });
    stubFormat(new SingleElimFormatStub(3, () -> finalMatch.getWinner() != null));
    stubBookedSegment(singles(alice, bob, alice));

    TournamentRunnerService.TournamentRunReport report = service.runTournament(tournament, 1L);

    assertTrue(report.tournamentComplete());
    assertEquals(3, report.matchesPlayed());
    assertEquals(aliceEntry, report.winner());
    verify(tournamentService, times(3)).recordMatchResult(any(), any());
    assertEquals(TournamentEntryStatus.ELIMINATED, bobEntry.getStatus());
    assertEquals(TournamentEntryStatus.WINNER, aliceEntry.getStatus());
  }

  @Test
  void runTournament_scheduledTournament_rejected() {
    tournament.setStatus(TournamentStatus.SCHEDULED);
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> service.runTournament(tournament, null));
    assertTrue(ex.getMessage().contains("start it before running it"));
  }

  @Test
  void runRound_scheduledTournament_rejected() {
    tournament.setStatus(TournamentStatus.SCHEDULED);
    assertThrows(IllegalStateException.class, () -> service.runRound(tournament, null));
  }

  @Test
  void runRound_completedTournament_rejected() {
    tournament.setStatus(TournamentStatus.COMPLETE);
    assertThrows(IllegalStateException.class, () -> service.runRound(tournament, null));
  }

  @Test
  void runTournament_neverCompletingFormat_terminatesWithoutHanging() {
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    attach(
        tournament,
        List.of(aliceEntry, bobEntry),
        List.of(round(1, match(1, aliceEntry, bobEntry))));
    stubBookedSegment(singles(alice, bob, alice));
    stubFormat(new NeverCompletingFormat(3));

    TournamentRunnerService.TournamentRunReport report = service.runTournament(tournament, null);

    assertFalse(report.tournamentComplete());
  }

  @Test
  void runRound_noOpenMatches_throwsCleanly() {
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    TournamentMatch m1 = match(1, aliceEntry, bobEntry);
    m1.setWinner(aliceEntry);
    attach(tournament, List.of(aliceEntry, bobEntry), List.of(round(1, m1)));

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> service.runRound(tournament, null));
    assertTrue(ex.getMessage().contains("no open matches"));
  }

  @Test
  void hasRunnableWork_and_remainingMatches_reflectOpenMatches() {
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    TournamentEntry caraEntry = entry(wrestler(3L, "Cara"), 3);
    TournamentEntry daveEntry = entry(wrestler(4L, "Dave"), 4);
    TournamentMatch open = match(1, aliceEntry, bobEntry);
    TournamentMatch decided = match(1, caraEntry, daveEntry);
    decided.setWinner(caraEntry);
    attach(tournament, List.of(aliceEntry, bobEntry), List.of(round(1, open, decided)));

    assertTrue(service.hasRunnableWork(tournament));
    assertEquals(1, service.remainingMatches(tournament));

    open.setWinner(aliceEntry);
    assertFalse(service.hasRunnableWork(tournament));
    assertEquals(0, service.remainingMatches(tournament));
  }

  @Test
  void autoCreateHostShow_delegatesToSameAutoCreation() {
    Show created = new Show();
    created.setId(9L);
    created.setName("Crown Cup Night");
    created.setType(showType);
    when(showService.createShow(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(created);

    Show show = service.autoCreateHostShow(tournament);

    assertEquals(created, show);
    verify(showService)
        .createShow(
            eq("Crown Cup Night"),
            anyString(),
            eq(2L),
            any(LocalDate.class),
            any(),
            any(),
            eq(1L),
            any(),
            any(),
            any());
  }

  // ── Per-match interactive path (ATW-p0sz.1) ─────────────────────────────────

  @Test
  void recordManualResult_stampsBookerWinnerOnSegment() {
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    aliceEntry.setId(11L);
    bobEntry.setId(12L);
    TournamentMatch m1 = match(1, aliceEntry, bobEntry);
    m1.setId(21L);
    attach(tournament, List.of(aliceEntry, bobEntry), List.of(round(1, m1)));
    // Engine pick is Bob — the booker overrides with Alice.
    Segment booked = singles(bob, alice, bob);
    when(segmentResolutionService.resolveTeamSegment(any(), any(), any(), any(), any()))
        .thenReturn(booked);
    when(segmentService.updateSegment(any())).thenAnswer(inv -> inv.getArgument(0));

    TournamentRunnerService.ManualResultReport report =
        service.recordManualResult(tournament, 21L, 11L, 1L);

    assertEquals(aliceEntry, report.winner());
    // The segment's winners were overridden with Alice's side (solo → [alice]).
    assertTrue(booked.getWinners().contains(alice));
    assertFalse(booked.getWinners().contains(bob));
    verify(segmentService).updateSegment(booked);
    verify(tournamentService).recordMatchResult(m1, aliceEntry);
  }

  @Test
  void recordManualResult_bookedMatch_reusesExistingSegment() {
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    aliceEntry.setId(11L);
    bobEntry.setId(12L);
    TournamentMatch m1 = match(1, aliceEntry, bobEntry);
    m1.setId(21L);
    Segment existing = singles(bob, alice, bob);
    m1.setSegment(existing);
    attach(tournament, List.of(aliceEntry, bobEntry), List.of(round(1, m1)));
    when(segmentService.updateSegment(any())).thenAnswer(inv -> inv.getArgument(0));

    service.recordManualResult(tournament, 21L, 11L, null);

    // The booked segment is reused — no new resolution, winners stamped on it.
    verify(segmentResolutionService, never()).resolveTeamSegment(any(), any(), any(), any(), any());
    assertTrue(existing.getWinners().contains(alice));
    verify(segmentService).updateSegment(existing);
  }

  @Test
  void recordManualResult_lastMatchOfRound_advancesBracket() {
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    aliceEntry.setId(11L);
    bobEntry.setId(12L);
    TournamentMatch m1 = match(1, aliceEntry, bobEntry);
    m1.setId(21L);
    attach(tournament, List.of(aliceEntry, bobEntry), List.of(round(1, m1)));
    when(segmentResolutionService.resolveTeamSegment(any(), any(), any(), any(), any()))
        .thenReturn(singles(alice, bob, alice));
    when(segmentService.updateSegment(any())).thenAnswer(inv -> inv.getArgument(0));

    TournamentRunnerService.ManualResultReport report =
        service.recordManualResult(tournament, 21L, 11L, null);

    // Round fully decided → auto-advance ran (the v1 Generate-Next-Round click is gone).
    verify(tournamentService).advanceToNextRound(tournament);
    assertFalse(report.tournamentComplete()); // unresolvable format stub reports not-complete
  }

  @Test
  void recordManualResult_completesTournament_grantsBriefcase() {
    tournament = briefcaseTournament();
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    aliceEntry.setId(11L);
    bobEntry.setId(12L);
    TournamentMatch m1 = match(1, aliceEntry, bobEntry);
    m1.setId(21L);
    attach(tournament, List.of(aliceEntry, bobEntry), List.of(round(1, m1)));
    when(tournamentService.findByIdWithDetails(5L)).thenReturn(Optional.of(tournament));
    when(segmentResolutionService.resolveTeamSegment(any(), any(), any(), any(), any()))
        .thenReturn(singles(alice, bob, alice));
    when(segmentService.updateSegment(any())).thenAnswer(inv -> inv.getArgument(0));
    // markWinner (inside the real advanceToNextRound) stamps the status and winner entry —
    // stub that whole flow so the grant path sees COMPLETE with a WINNER entry.
    doAnswer(
            inv -> {
              TournamentMatch mm = inv.getArgument(0);
              TournamentEntry w = inv.getArgument(1);
              mm.setWinner(w);
              w.setStatus(TournamentEntryStatus.WINNER);
              return null;
            })
        .when(tournamentService)
        .recordMatchResult(any(), any());
    doAnswer(
            inv -> {
              Tournament t = inv.getArgument(0);
              t.setStatus(TournamentStatus.COMPLETE);
              return List.of();
            })
        .when(tournamentService)
        .advanceToNextRound(tournament);
    when(tournamentService.findFormat("SINGLE_ELIMINATION"))
        .thenReturn(Optional.of(new SingleElimFormatStub(1, () -> true)));

    service.recordManualResult(tournament, 21L, 11L, null);

    verify(titleOpportunityService).grantFromTournament(tournament, alice);
  }

  @Test
  void recordManualResult_unknownMatchId_throws() {
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    attach(
        tournament,
        List.of(aliceEntry, bobEntry),
        List.of(round(1, match(1, aliceEntry, bobEntry))));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> service.recordManualResult(tournament, 999L, 11L, null));
    assertTrue(ex.getMessage().contains("Match not found"));
  }

  @Test
  void recordManualResult_alreadyDecided_throws() {
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    aliceEntry.setId(11L);
    TournamentMatch m1 = match(1, aliceEntry, bobEntry);
    m1.setId(21L);
    m1.setWinner(aliceEntry);
    attach(tournament, List.of(aliceEntry, bobEntry), List.of(round(1, m1)));

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> service.recordManualResult(tournament, 21L, 11L, null));
    assertTrue(ex.getMessage().contains("already has a winner"));
  }

  @Test
  void recordManualResult_winnerNotInMatch_throws() {
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    aliceEntry.setId(11L);
    bobEntry.setId(12L);
    TournamentMatch m1 = match(1, aliceEntry, bobEntry);
    m1.setId(21L);
    TournamentEntry outsider = entry(wrestler(9L, "Outsider"), 3);
    outsider.setId(99L);
    attach(tournament, List.of(aliceEntry, bobEntry), List.of(round(1, m1)));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> service.recordManualResult(tournament, 21L, 99L, null));
    assertTrue(ex.getMessage().contains("not an entrant"));
  }

  @Test
  void recordManualResult_postResolutionFailure_compensatesOrphanSegment() {
    // NPCSegmentResolutionService commits its segment insert in a REQUIRES_NEW transaction;
    // if a later step (here recordMatchResult) fails, the outer transaction rolls back but the
    // committed segment would be orphaned on the host show. The runner must compensate by
    // deleting it in a fresh transaction before rethrowing.
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    aliceEntry.setId(11L);
    bobEntry.setId(12L);
    TournamentMatch m1 = match(1, aliceEntry, bobEntry);
    m1.setId(21L);
    attach(tournament, List.of(aliceEntry, bobEntry), List.of(round(1, m1)));
    Segment committed = singles(alice, bob, alice);
    stubBookedSegment(committed);
    doThrow(new IllegalStateException("bracket advance blew up"))
        .when(tournamentService)
        .recordMatchResult(any(), any());

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> service.recordManualResult(tournament, 21L, 11L, null));
    assertEquals("bracket advance blew up", ex.getMessage());
    // The compensation ran in a fresh transaction: the orphaned segment is deleted by id.
    verify(segmentService).deleteSegmentInNewTransaction(100L);
  }

  @Test
  void recordManualResult_preResolutionFailure_noCompensationNeeded() {
    // Failures before any segment is created (here: the resolve call itself) must not attempt
    // a compensation delete — there is nothing to delete.
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    aliceEntry.setId(11L);
    bobEntry.setId(12L);
    TournamentMatch m1 = match(1, aliceEntry, bobEntry);
    m1.setId(21L);
    attach(tournament, List.of(aliceEntry, bobEntry), List.of(round(1, m1)));
    when(segmentResolutionService.resolveTeamSegment(any(), any(), any(), any(), any()))
        .thenThrow(new IllegalStateException("resolution blew up"));

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> service.recordManualResult(tournament, 21L, 11L, null));
    assertEquals("resolution blew up", ex.getMessage());
    verify(segmentService, never()).deleteSegmentInNewTransaction(any());
  }

  @Test
  void runEngineMatch_postResolutionFailure_compensatesOrphanSegment() {
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    aliceEntry.setId(11L);
    bobEntry.setId(12L);
    TournamentMatch m1 = match(1, aliceEntry, bobEntry);
    m1.setId(21L);
    attach(tournament, List.of(aliceEntry, bobEntry), List.of(round(1, m1)));
    Segment committed = singles(alice, bob, alice);
    stubBookedSegment(committed);
    doThrow(new IllegalStateException("advance blew up"))
        .when(tournamentService)
        .recordMatchResult(any(), any());

    assertThrows(IllegalStateException.class, () -> service.runEngineMatch(tournament, 21L, null));
    verify(segmentService).deleteSegmentInNewTransaction(100L);
  }

  @Test
  void runEngineMatch_compensationFailure_stillThrowsOriginal() {
    // The compensation is best-effort: if the delete itself fails, the original error still
    // surfaces (the compensation failure is logged, not swallowed into the throw path).
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    aliceEntry.setId(11L);
    bobEntry.setId(12L);
    TournamentMatch m1 = match(1, aliceEntry, bobEntry);
    m1.setId(21L);
    attach(tournament, List.of(aliceEntry, bobEntry), List.of(round(1, m1)));
    Segment committed = singles(alice, bob, alice);
    stubBookedSegment(committed);
    doThrow(new IllegalStateException("advance blew up"))
        .when(tournamentService)
        .recordMatchResult(any(), any());
    doThrow(new IllegalStateException("delete blew up"))
        .when(segmentService)
        .deleteSegmentInNewTransaction(any());

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class, () -> service.runEngineMatch(tournament, 21L, null));
    assertEquals("advance blew up", ex.getMessage());
  }

  @Test
  void recordManualResult_scheduledTournament_rejected() {
    tournament.setStatus(TournamentStatus.SCHEDULED);
    assertThrows(
        IllegalStateException.class, () -> service.recordManualResult(tournament, 21L, 11L, null));
  }

  @Test
  void runEngineMatch_resolvesOnlyTheRequestedMatch() {
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    TournamentEntry caraEntry = entry(wrestler(3L, "Cara"), 3);
    TournamentEntry daveEntry = entry(wrestler(4L, "Dave"), 4);
    aliceEntry.setId(11L);
    bobEntry.setId(12L);
    caraEntry.setId(13L);
    daveEntry.setId(14L);
    TournamentMatch m1 = match(1, aliceEntry, bobEntry);
    m1.setId(21L);
    TournamentMatch m2 = match(1, caraEntry, daveEntry);
    m2.setId(22L);
    attach(tournament, List.of(aliceEntry, bobEntry), List.of(round(1, m1, m2)));
    Segment booked = singles(alice, bob, alice);
    when(segmentResolutionService.resolveTeamSegment(any(), any(), any(), any(), any()))
        .thenReturn(booked);

    TournamentRunnerService.MatchRunReport report = service.runEngineMatch(tournament, 21L, null);

    assertEquals(booked, report.segment());
    verify(segmentResolutionService, times(1))
        .resolveTeamSegment(any(), any(), any(), any(), any());
    verify(tournamentService, times(1)).recordMatchResult(m1, aliceEntry);
    // The other open match is untouched.
    assertNull(m2.getWinner());
    assertEquals(booked, m1.getSegment());
  }

  @Test
  void runEngineMatch_completesRoundAndAdvances() {
    TournamentEntry aliceEntry = entry(alice, 1);
    TournamentEntry bobEntry = entry(bob, 2);
    aliceEntry.setId(11L);
    bobEntry.setId(12L);
    TournamentMatch m1 = match(1, aliceEntry, bobEntry);
    m1.setId(21L);
    attach(tournament, List.of(aliceEntry, bobEntry), List.of(round(1, m1)));
    when(segmentResolutionService.resolveTeamSegment(any(), any(), any(), any(), any()))
        .thenReturn(singles(alice, bob, alice));

    TournamentRunnerService.MatchRunReport report = service.runEngineMatch(tournament, 21L, null);

    assertTrue(report.roundDecided());
    verify(tournamentService).advanceToNextRound(tournament);
  }

  /** A briefcase-deciding variant of the fixture tournament. */
  private Tournament briefcaseTournament() {
    Tournament t = new Tournament();
    t.setId(5L);
    t.setName("Briefcase Cup");
    t.setFormatId("SINGLE_ELIMINATION");
    t.setStatus(TournamentStatus.IN_PROGRESS);
    t.setBriefcaseDeciding(true);
    return t;
  }

  // ── Fixture helpers (mirroring TournamentTemplateBookingServiceTest) ────────

  private void stubBookedSegment(Segment booked) {
    lenient()
        .when(segmentResolutionService.resolveTeamSegment(any(), any(), any(), any(), any()))
        .thenReturn(booked);
  }

  private void stubFormat(TournamentFormat format) {
    lenient()
        .when(tournamentService.findFormat("SINGLE_ELIMINATION"))
        .thenReturn(Optional.of(format));
  }

  private static Wrestler wrestler(Long id, String name) {
    Wrestler w = new Wrestler();
    w.setId(id);
    w.setName(name);
    return w;
  }

  private static TournamentEntry entry(Wrestler wrestler, int seed) {
    return TournamentEntry.builder()
        .wrestler(wrestler)
        .seed(seed)
        .status(TournamentEntryStatus.ACTIVE)
        .build();
  }

  private static TournamentMatch match(int roundNumber, TournamentEntry e1, TournamentEntry e2) {
    return TournamentMatch.builder().entrant1(e1).entrant2(e2).build();
  }

  private static TournamentRound round(int roundNumber, TournamentMatch... matches) {
    TournamentRound r =
        TournamentRound.builder()
            .roundNumber(roundNumber)
            .roundName(roundNumber == 1 ? "Round 1" : "Final")
            .status(TournamentRoundStatus.IN_PROGRESS)
            .build();
    for (TournamentMatch m : matches) {
      m.setRound(r);
      r.getMatches().add(m);
    }
    return r;
  }

  private void attach(
      Tournament tournament, List<TournamentEntry> entries, List<TournamentRound> rounds) {
    tournament.setEntries(new ArrayList<>(entries));
    tournament.setRounds(new ArrayList<>(rounds));
  }

  /** A resolved segment whose winner is {@code winner}. */
  private static Segment singles(Wrestler a, Wrestler b, Wrestler winner) {
    Segment s = new Segment();
    s.setId(100L);
    s.addParticipant(a);
    s.addParticipant(b);
    if (winner != null) {
      s.getWinners().add(winner);
    }
    return s;
  }

  /** Minimal format stub: configurable estimate and completion supplier. */
  private static final class SingleElimFormatStub implements TournamentFormat {
    private final int estimate;
    private final BooleanSupplier complete;

    SingleElimFormatStub(int estimate, BooleanSupplier complete) {
      this.estimate = estimate;
      this.complete = complete;
    }

    @Override
    public String getFormatId() {
      return "SINGLE_ELIMINATION";
    }

    @Override
    public String getDisplayName() {
      return "Single Elimination";
    }

    @Override
    public int getMinEntrants() {
      return 2;
    }

    @Override
    public int getMaxEntrants() {
      return 64;
    }

    @Override
    public int estimateTotalMatches(Tournament tournament) {
      return estimate;
    }

    @Override
    public boolean isComplete(Tournament tournament) {
      return complete.getAsBoolean();
    }

    @Override
    public List<TournamentMatch> advanceRound(Tournament tournament, TournamentFormatContext ctx) {
      return List.of();
    }

    @Override
    public List<TournamentRound> generateBracket(
        Tournament tournament, TournamentFormatContext ctx) {
      return List.of();
    }
  }

  /** A format that never reports complete — for the termination test. */
  private static final class NeverCompletingFormat implements TournamentFormat {
    private final int estimate;

    NeverCompletingFormat(int estimate) {
      this.estimate = estimate;
    }

    @Override
    public String getFormatId() {
      return "SINGLE_ELIMINATION";
    }

    @Override
    public String getDisplayName() {
      return "Broken";
    }

    @Override
    public int getMinEntrants() {
      return 2;
    }

    @Override
    public int getMaxEntrants() {
      return 64;
    }

    @Override
    public int estimateTotalMatches(Tournament tournament) {
      return estimate;
    }

    @Override
    public boolean isComplete(Tournament tournament) {
      return false;
    }

    @Override
    public List<TournamentMatch> advanceRound(Tournament tournament, TournamentFormatContext ctx) {
      return List.of();
    }

    @Override
    public List<TournamentRound> generateBracket(
        Tournament tournament, TournamentFormatContext ctx) {
      return List.of();
    }
  }
}
