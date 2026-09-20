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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateSegmentAssignment;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.segment.NPCSegmentResolutionService;
import com.github.javydreamercsw.management.service.segment.SegmentTeam;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for tournament-fed booking on PLE templates (ATW-oahn, ATW-z963). */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class TournamentTemplateBookingServiceTest {

  @Mock private TournamentService tournamentService;
  @Mock private NPCSegmentResolutionService segmentResolutionService;
  @Mock private TournamentPacingService pacingService;
  @Mock private SegmentTypeService segmentTypeService;
  @Mock private ShowService showService;
  @Mock private TournamentFormat format;

  private TournamentTemplateBookingService service;
  private Show show;
  private SegmentType rumbleType;
  private ShowTemplateSegmentAssignment assignment;
  private Tournament tournament;
  private Wrestler alice;
  private Wrestler bob;
  private SegmentRule rumbleRule;

  @BeforeEach
  void setUp() {
    service =
        new TournamentTemplateBookingService(
            tournamentService,
            segmentResolutionService,
            pacingService,
            segmentTypeService,
            showService);

    ShowType showType = new ShowType();
    showType.setName("PLE");
    show = new Show();
    show.setId(1L);
    show.setName("WrestlePalooza");
    show.setShowDate(LocalDate.of(2026, 6, 1));
    show.setType(showType);
    Universe universe = new Universe();
    universe.setId(1L);
    show.setUniverse(universe);

    rumbleType = new SegmentType();
    rumbleType.setId(10L);
    rumbleType.setName("Abu Dhabi Rumble");

    rumbleRule = new SegmentRule();
    rumbleRule.setId(20L);
    rumbleRule.setName("Rumble Rules");

    lenient()
        .when(pacingService.payoffKindOf(any()))
        .thenReturn(TournamentPacingService.PayoffKind.FINAL_AT_PLE);
    lenient().when(tournamentService.isTitleVacant(any())).thenReturn(true);
    lenient().when(tournamentService.currentChampionsOf(any())).thenReturn(java.util.List.of());
    lenient().when(format.getMaxEntrants()).thenReturn(8);
    lenient().when(format.getMinEntrants()).thenReturn(2);

    tournament = new Tournament();
    tournament.setId(5L);
    tournament.setName("Crown Cup");
    tournament.setFormatId("SINGLE_ELIMINATION");
    tournament.setStatus(TournamentStatus.SCHEDULED);

    com.github.javydreamercsw.management.domain.show.template.ShowTemplate template =
        new com.github.javydreamercsw.management.domain.show.template.ShowTemplate();
    template.setId(9L);
    template.setName("Weekly");
    assignment = new ShowTemplateSegmentAssignment();
    assignment.setTemplate(template);
    assignment.setTournament(tournament);
    assignment.setSegmentType(rumbleType);
    assignment.setMode(ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH);

    alice = wrestler(1L, "Alice");
    bob = wrestler(2L, "Bob");
  }

  @Test
  void scheduledTournament_autoSeedsAndStarts_thenBooksRound1() {
    // Tournament starts SCHEDULED and unseeded. seedAuto persists entries; the service then
    // re-fetches (findByIdWithDetails) and copies the seeded roster + generated bracket onto
    // its in-memory instance — the same instance is IN_PROGRESS with a round-1 bracket.
    TournamentEntry aliceEntry = entry(alice, 1, TournamentEntryStatus.ACTIVE);
    TournamentEntry bobEntry = entry(bob, 2, TournamentEntryStatus.ACTIVE);
    TournamentMatch match = match(1, aliceEntry, bobEntry);
    lenient()
        .when(tournamentService.findFormat("SINGLE_ELIMINATION"))
        .thenReturn(Optional.of(format));
    lenient().when(format.getMaxEntrants()).thenReturn(8);
    lenient().when(format.getMinEntrants()).thenReturn(2);
    lenient().when(tournamentService.hasEntries(5L)).thenReturn(false);
    // Eligibility pre-flight passes (10 available >= min 2) before seedAuto is attempted.
    lenient()
        .when(tournamentService.findEligibleWrestlersSortedByFans(any(), eq(1L)))
        .thenReturn(List.of(alice, bob, wrestler(3L, "Cara"), wrestler(4L, "Dave")));
    // seedAuto does not touch the in-memory collection — simulate by leaving the caller's
    // instance empty and having the refresh return the seeded, bracketed instance.
    lenient()
        .when(tournamentService.findByIdWithDetails(5L))
        .thenAnswer(
            invocation -> {
              tournament.setEntries(new ArrayList<>(List.of(aliceEntry, bobEntry)));
              tournament.setRounds(new ArrayList<>(List.of(round(1, match))));
              return Optional.of(tournament);
            });
    when(tournamentService.startTournament(tournament))
        .thenAnswer(
            invocation -> {
              tournament.setStatus(TournamentStatus.IN_PROGRESS);
              return tournament;
            });
    Segment booked = singles(alice, bob, alice);
    stubResolve(booked);

    Optional<TournamentTemplateBookingService.TournamentBooking> booking =
        service.bookTournamentFedSegment(assignment, rumbleType, show);

    assertTrue(booking.isPresent());
    assertEquals(booked, booking.get().segment());
    verify(tournamentService).seedAuto(tournament, 8, 1L);
    verify(tournamentService).startTournament(tournament);
    verify(tournamentService).recordMatchResult(eq(match), eq(aliceEntry));
    assertEquals(booked, match.getSegment());
  }

  @Test
  void scheduledTournament_belowFormatMinimum_fallsBackWithoutSeeding() {
    // Eligibility pre-flight: fewer eligible wrestlers than the format's minimum → never enter
    // the nested transactional calls, fall back cleanly (no rollback-only poisoning).
    lenient()
        .when(tournamentService.findFormat("SINGLE_ELIMINATION"))
        .thenReturn(Optional.of(format));
    lenient().when(format.getMinEntrants()).thenReturn(4);
    lenient().when(tournamentService.hasEntries(5L)).thenReturn(false);
    lenient()
        .when(tournamentService.findEligibleWrestlersSortedByFans(any(), eq(1L)))
        .thenReturn(List.of(alice, bob));

    assertTrue(
        service.bookTournamentFedSegment(assignment, rumbleType, show).isEmpty(),
        "SCHEDULED tournament below the format minimum must fall back to the AI path");
    verify(tournamentService, never()).seedAuto(any(), anyInt(), anyLong());
    verify(tournamentService, never()).startTournament(any());
  }

  @Test
  void scheduledTournament_alreadySeeded_skipsEligibilityPreflight() {
    // The tournament already has entries (seeded via the UI earlier): auto-start proceeds
    // without the eligibility check or seeding.
    TournamentEntry aliceEntry = entry(alice, 1, TournamentEntryStatus.ACTIVE);
    TournamentEntry bobEntry = entry(bob, 2, TournamentEntryStatus.ACTIVE);
    TournamentMatch match = match(1, aliceEntry, bobEntry);
    lenient()
        .when(tournamentService.findFormat("SINGLE_ELIMINATION"))
        .thenReturn(Optional.of(format));
    lenient().when(tournamentService.hasEntries(5L)).thenReturn(true);
    lenient().when(tournamentService.findByIdWithDetails(5L)).thenReturn(Optional.of(tournament));
    when(tournamentService.startTournament(tournament))
        .thenAnswer(
            invocation -> {
              tournament.setEntries(new ArrayList<>(List.of(aliceEntry, bobEntry)));
              tournament.setRounds(new ArrayList<>(List.of(round(1, match))));
              tournament.setStatus(TournamentStatus.IN_PROGRESS);
              return tournament;
            });
    Segment booked = singles(alice, bob, alice);
    stubResolve(booked);

    Optional<TournamentTemplateBookingService.TournamentBooking> booking =
        service.bookTournamentFedSegment(assignment, rumbleType, show);

    assertTrue(booking.isPresent());
    verify(tournamentService, never()).seedAuto(any(), anyInt(), anyLong());
    verify(tournamentService).startTournament(tournament);
    verify(tournamentService).recordMatchResult(eq(match), eq(aliceEntry));
  }

  @Test
  void scheduledTournament_unknownFormat_fallsBackWithoutSeeding() {
    when(tournamentService.findFormat("SINGLE_ELIMINATION")).thenReturn(Optional.empty());

    assertTrue(
        service.bookTournamentFedSegment(assignment, rumbleType, show).isEmpty(),
        "SCHEDULED tournament with an unresolvable format must fall back to the AI path");
    verify(tournamentService, never()).seedAuto(any(), anyInt(), anyLong());
  }

  @Test
  void inProgressTournament_booksFirstOpenMatch() {
    tournament.setStatus(TournamentStatus.IN_PROGRESS);
    TournamentEntry aliceEntry = entry(alice, 1, TournamentEntryStatus.ACTIVE);
    TournamentEntry bobEntry = entry(bob, 2, TournamentEntryStatus.ACTIVE);
    TournamentMatch match = match(1, aliceEntry, bobEntry);
    tournament.setRounds(new ArrayList<>(List.of(round(1, match))));

    Segment booked = singles(alice, bob, alice);
    stubResolve(booked);

    Optional<TournamentTemplateBookingService.TournamentBooking> booking =
        service.bookTournamentFedSegment(assignment, rumbleType, show);

    assertTrue(booking.isPresent());
    assertEquals(booked, booking.get().segment());
    verify(tournamentService, never()).startTournament(any());
    verify(tournamentService, never()).seedAuto(any(), anyInt(), anyLong());
    verify(tournamentService).markRoundInProgress(match.getRound());
    verify(tournamentService).recordMatchResult(eq(match), eq(aliceEntry));
    assertEquals(booked, match.getSegment());
  }

  @Test
  void decidedRounds_advanceOnceThenBookNextRound() {
    tournament.setStatus(TournamentStatus.IN_PROGRESS);
    TournamentEntry aliceEntry = entry(alice, 1, TournamentEntryStatus.ACTIVE);
    TournamentEntry bobEntry = entry(bob, 2, TournamentEntryStatus.ACTIVE);
    // Round 1 is COMPLETE (all matches decided); round 2 not yet generated.
    TournamentMatch decided = match(1, aliceEntry, bobEntry);
    decided.setWinner(aliceEntry);
    TournamentRound completeRound = round(1, decided);
    completeRound.setStatus(TournamentRoundStatus.COMPLETE);
    tournament.setRounds(new ArrayList<>(List.of(completeRound)));

    // advanceToNextRound generates round 2 (PENDING) onto the tournament instance.
    when(tournamentService.advanceToNextRound(tournament))
        .thenAnswer(
            invocation -> {
              TournamentMatch next = match(2, aliceEntry, bobEntry);
              tournament.getRounds().add(round(2, next));
              return List.of(next);
            });

    Segment booked = singles(alice, bob, bob);
    stubResolve(booked);

    Optional<TournamentTemplateBookingService.TournamentBooking> booking =
        service.bookTournamentFedSegment(assignment, rumbleType, show);

    assertTrue(booking.isPresent());
    assertEquals(booked, booking.get().segment());
    verify(tournamentService).advanceToNextRound(tournament);
    verify(tournamentService).recordMatchResult(any(), eq(bobEntry));
  }

  @Test
  void completeTournament_noTitle_fallsBackAndConsumes() {
    // Bracket finished before the PLE with no linked title: the final already played on a
    // weekly show — the pairing is consumed and nothing books at the PLE.
    tournament.setStatus(TournamentStatus.COMPLETE);
    TournamentEntry aliceEntry = entry(alice, 1, TournamentEntryStatus.WINNER);
    TournamentEntry bobEntry = entry(bob, 2, TournamentEntryStatus.ELIMINATED);
    tournament.setEntries(new ArrayList<>(List.of(aliceEntry, bobEntry)));

    assertTrue(
        service.bookTournamentFedSegment(assignment, rumbleType, show).isEmpty(),
        "COMPLETE tournament without a linked title must not book a showcase");
    assertNull(
        assignment.getTournament(),
        "Pairing must be consumed — no title means the final already played");
  }

  @Test
  void completeTournament_reigningChampion_booksShowcaseAndConsumes() {
    // Bracket finished before the PLE, champion reigns: PLE books champion vs tournament winner
    // as the title match, then consumes the pairing.
    tournament.setStatus(TournamentStatus.COMPLETE);
    Title title = new Title();
    title.setId(7L);
    title.setName("World Title");
    tournament.setLinkedTitle(title);
    TournamentEntry aliceEntry = entry(alice, 1, TournamentEntryStatus.WINNER);
    TournamentEntry bobEntry = entry(bob, 2, TournamentEntryStatus.ELIMINATED);
    tournament.setEntries(new ArrayList<>(List.of(aliceEntry, bobEntry)));
    lenient().when(tournamentService.isTitleVacant(title)).thenReturn(false);
    lenient().when(tournamentService.currentChampionsOf(title)).thenReturn(List.of(bob));

    Segment booked = singles(alice, bob, alice);
    stubResolve(booked);

    Optional<TournamentTemplateBookingService.TournamentBooking> booking =
        service.bookTournamentFedSegment(assignment, rumbleType, show);

    assertTrue(booking.isPresent());
    assertTrue(booking.get().titleMatch(), "Champion showcase must be flagged as a title match");
    assertEquals(title, booking.get().title());
    assertTrue(booked.getIsTitleSegment());
    assertTrue(booked.getTitles().contains(title));
    assertNull(assignment.getTournament(), "Showcase consumed the pairing");
    verify(tournamentService, never()).recordMatchResult(any(), any());
  }

  @Test
  void completeTournamentWithoutWinner_fallsBackEmpty() {
    tournament.setStatus(TournamentStatus.COMPLETE);
    tournament.setEntries(new ArrayList<>());

    assertTrue(
        service.bookTournamentFedSegment(assignment, rumbleType, show).isEmpty(),
        "COMPLETE tournament with no winner entry must fall back to the AI path");
  }

  @Test
  void noOpenMatchesAnywhere_fallsBackEmpty() {
    tournament.setStatus(TournamentStatus.IN_PROGRESS);
    TournamentEntry aliceEntry = entry(alice, 1, TournamentEntryStatus.ACTIVE);
    TournamentEntry bobEntry = entry(bob, 2, TournamentEntryStatus.ELIMINATED);
    TournamentMatch decided = match(1, aliceEntry, bobEntry);
    decided.setWinner(aliceEntry);
    TournamentRound completeRound = round(1, decided);
    completeRound.setStatus(TournamentRoundStatus.COMPLETE);
    tournament.setRounds(new ArrayList<>(List.of(completeRound)));
    // advanceToNextRound cannot help either (tournament over — markWinner path returns List.of()).
    when(tournamentService.advanceToNextRound(tournament)).thenReturn(List.of());

    assertTrue(
        service.bookTournamentFedSegment(assignment, rumbleType, show).isEmpty(),
        "IN_PROGRESS tournament with no bookable match must fall back to the AI path");
  }

  @Test
  void assignmentWithoutTournament_fallsBackEmpty() {
    ShowTemplateSegmentAssignment plain = new ShowTemplateSegmentAssignment();
    plain.setSegmentType(rumbleType);
    plain.setMode(ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH);

    assertTrue(
        service.bookTournamentFedSegment(plain, rumbleType, show).isEmpty(),
        "Assignment row without a tournament must fall back to the AI path");
  }

  @Test
  void pairedRule_isAppliedAsStipulation() {
    tournament.setStatus(TournamentStatus.IN_PROGRESS);
    TournamentEntry aliceEntry = entry(alice, 1, TournamentEntryStatus.ACTIVE);
    TournamentEntry bobEntry = entry(bob, 2, TournamentEntryStatus.ACTIVE);
    TournamentMatch match = match(1, aliceEntry, bobEntry);
    tournament.setRounds(new ArrayList<>(List.of(round(1, match))));
    assignment.setSegmentRule(rumbleRule);

    Segment booked = singles(alice, bob, alice);
    when(segmentResolutionService.resolveTeamSegment(
            any(), any(), eq(rumbleType), eq(show), eq("Rumble Rules")))
        .thenReturn(booked);

    assertTrue(service.bookTournamentFedSegment(assignment, rumbleType, show).isPresent());
  }

  @Test
  void bookedParticipants_comeFromTheBracket() {
    tournament.setStatus(TournamentStatus.IN_PROGRESS);
    TournamentEntry aliceEntry = entry(alice, 1, TournamentEntryStatus.ACTIVE);
    TournamentEntry bobEntry = entry(bob, 2, TournamentEntryStatus.ACTIVE);
    TournamentMatch match = match(1, aliceEntry, bobEntry);
    tournament.setRounds(new ArrayList<>(List.of(round(1, match))));

    Segment booked = singles(alice, bob, alice);
    when(segmentResolutionService.resolveTeamSegment(any(), any(), any(), any(), any()))
        .thenReturn(booked);

    Optional<TournamentTemplateBookingService.TournamentBooking> booking =
        service.bookTournamentFedSegment(assignment, rumbleType, show);

    assertTrue(booking.isPresent());
    // SegmentTeam has no equals — capture and assert on the wrestlers instead.
    ArgumentCaptor<SegmentTeam> team1Captor = ArgumentCaptor.forClass(SegmentTeam.class);
    ArgumentCaptor<SegmentTeam> team2Captor = ArgumentCaptor.forClass(SegmentTeam.class);
    Mockito.verify(segmentResolutionService)
        .resolveTeamSegment(
            team1Captor.capture(), team2Captor.capture(), eq(rumbleType), eq(show), eq(""));
    assertEquals("Alice", team1Captor.getValue().getMembers().get(0).getName());
    assertEquals("Bob", team2Captor.getValue().getMembers().get(0).getName());
  }

  @Test
  void finalAtPle_isTitleMatch_whenVacantTitleLinked_andConsumes() {
    // 4-entrant bracket: 3 matches total; round 1 has 2 matches, the final is the 3rd. Booking
    // the final at the PLE is the payoff — a title match (title vacant) that consumes the pairing.
    tournament.setStatus(TournamentStatus.IN_PROGRESS);
    tournament.setEntries(
        new ArrayList<>(
            List.of(
                entry(alice, 1, TournamentEntryStatus.ACTIVE),
                entry(bob, 2, TournamentEntryStatus.ACTIVE),
                entry(wrestler(3L, "Cara"), 3, TournamentEntryStatus.ACTIVE),
                entry(wrestler(4L, "Dave"), 4, TournamentEntryStatus.ACTIVE))));
    TournamentEntry aliceEntry = entry(alice, 1, TournamentEntryStatus.ACTIVE);
    TournamentEntry bobEntry = entry(bob, 2, TournamentEntryStatus.ACTIVE);
    // Round 1's two matches were booked+decided on weekly shows; the final is the only open one.
    TournamentMatch r1m1 =
        match(
            1,
            entry(alice, 1, TournamentEntryStatus.ELIMINATED),
            entry(wrestler(3L, "Cara"), 3, TournamentEntryStatus.ELIMINATED));
    r1m1.setSegment(new Segment());
    r1m1.setWinner(r1m1.getEntrant1());
    TournamentMatch r1m2 =
        match(
            1,
            entry(bob, 2, TournamentEntryStatus.ELIMINATED),
            entry(wrestler(4L, "Dave"), 4, TournamentEntryStatus.ELIMINATED));
    r1m2.setSegment(new Segment());
    r1m2.setWinner(r1m2.getEntrant1());
    TournamentRound round1 = round(1, r1m1, r1m2);
    round1.setStatus(TournamentRoundStatus.COMPLETE);
    TournamentMatch finalMatch = match(2, aliceEntry, bobEntry);
    TournamentRound finalRound =
        TournamentRound.builder()
            .roundNumber(2)
            .roundName("Final")
            .status(TournamentRoundStatus.PENDING)
            .build();
    finalRound.getMatches().add(finalMatch);
    finalMatch.setRound(finalRound);
    tournament.setRounds(new ArrayList<>(List.of(round1, finalRound)));

    Title title = new Title();
    title.setId(7L);
    title.setName("World Title");
    tournament.setLinkedTitle(title);
    lenient().when(tournamentService.isTitleVacant(title)).thenReturn(true);
    lenient()
        .when(tournamentService.findFormat("SINGLE_ELIMINATION"))
        .thenReturn(Optional.of(format));
    // estimateTotalMatches = entrants-1 = 3; booked = 0 → the open match (1 remaining before it)
    // makes this the final.
    lenient().when(format.estimateTotalMatches(tournament)).thenReturn(3);

    Segment booked = singles(alice, bob, alice);
    stubResolve(booked);

    Optional<TournamentTemplateBookingService.TournamentBooking> booking =
        service.bookTournamentFedSegment(assignment, rumbleType, show);

    assertTrue(booking.isPresent());
    assertTrue(booking.get().titleMatch(), "Vacant-title final at the PLE is the title match");
    assertEquals(title, booking.get().title());
    assertTrue(booked.getIsTitleSegment());
    assertTrue(booked.getTitles().contains(title));
    assertNull(assignment.getTournament(), "Payoff consumes the pairing");
    // The bracket result flips the tournament COMPLETE so the winner entry exists.
    verify(tournamentService).recordMatchResult(eq(finalMatch), eq(aliceEntry));
    verify(tournamentService).advanceToNextRound(tournament);
  }

  @Test
  void nonFinalPleMatch_isNotPayoff_doesNotConsume() {
    // IN_PROGRESS with 2 of 3 bracket matches left (round 1 open): the PLE books a regular
    // tournament match, not the payoff — pairing survives.
    tournament.setStatus(TournamentStatus.IN_PROGRESS);
    tournament.setEntries(
        new ArrayList<>(
            List.of(
                entry(alice, 1, TournamentEntryStatus.ACTIVE),
                entry(bob, 2, TournamentEntryStatus.ACTIVE))));
    TournamentEntry aliceEntry = entry(alice, 1, TournamentEntryStatus.ACTIVE);
    TournamentEntry bobEntry = entry(bob, 2, TournamentEntryStatus.ACTIVE);
    TournamentMatch match = match(1, aliceEntry, bobEntry);
    tournament.setRounds(new ArrayList<>(List.of(round(1, match))));
    lenient()
        .when(tournamentService.findFormat("SINGLE_ELIMINATION"))
        .thenReturn(Optional.of(format));
    lenient().when(format.estimateTotalMatches(tournament)).thenReturn(3);

    Segment booked = singles(alice, bob, alice);
    stubResolve(booked);

    Optional<TournamentTemplateBookingService.TournamentBooking> booking =
        service.bookTournamentFedSegment(assignment, rumbleType, show);

    assertTrue(booking.isPresent());
    assertFalse(booking.get().titleMatch());
    assertNotNull(assignment.getTournament(), "A non-final PLE match keeps the pairing alive");
  }

  @Test
  void weeklyRounds_paceToFinishBeforePle() {
    // 8-entrant bracket (7 matches); 2 remain after round 1 booked; PLE in 3 weeks with this
    // week + 2 more weekly slots → ceil(2/3) = 1 match this show.
    tournament.setStatus(TournamentStatus.IN_PROGRESS);
    tournament.setEntries(
        new ArrayList<>(
            List.of(
                entry(alice, 1, TournamentEntryStatus.ACTIVE),
                entry(bob, 2, TournamentEntryStatus.ACTIVE))));
    TournamentEntry aliceEntry = entry(alice, 1, TournamentEntryStatus.ACTIVE);
    TournamentEntry bobEntry = entry(bob, 2, TournamentEntryStatus.ACTIVE);
    TournamentMatch match = match(1, aliceEntry, bobEntry);
    tournament.setRounds(new ArrayList<>(List.of(round(1, match))));

    Show ple = new Show();
    ple.setId(2L);
    ple.setName("Big PLE");
    ple.setShowDate(LocalDate.of(2026, 6, 22));
    ple.setType(show.getType());
    Show weeklyShow = show;
    weeklyShow.setShowDate(LocalDate.of(2026, 6, 8));

    when(showService.getShowsByDateRange(any(), any()))
        .thenReturn(
            List.of(weeklyShow, ple, showWithName(3L, "Week 2", LocalDate.of(2026, 6, 15))));
    when(showService.getShowsByDateRange(weeklyShow.getShowDate(), ple.getShowDate()))
        .thenReturn(List.of(weeklyShow, showWithName(3L, "Week 2", LocalDate.of(2026, 6, 15))));
    TournamentPacingService.PacingPlan plan =
        new TournamentPacingService.PacingPlan(
            TournamentPacingService.PayoffKind.FINAL_AT_PLE, List.of(), 7, 5, 2);
    when(pacingService.planFor(tournament, ple)).thenReturn(plan);

    SegmentType singlesType = new SegmentType();
    singlesType.setId(11L);
    singlesType.setName("One on One");
    when(segmentTypeService.findByCode(WellKnownSegmentType.ONE_ON_ONE.getCode()))
        .thenReturn(Optional.of(singlesType));

    Segment booked = singles(alice, bob, alice);
    when(segmentResolutionService.resolveTeamSegment(
            any(), any(), eq(singlesType), eq(show), eq("")))
        .thenReturn(booked);

    List<TournamentTemplateBookingService.TournamentBooking> bookings =
        service.bookWeeklyRounds(assignment, weeklyShow);

    assertEquals(1, bookings.size(), "ceil(2 remaining / 2 slots) = 1 match this show");
    assertEquals(booked, bookings.get(0).segment());
    assertFalse(bookings.get(0).titleMatch());
  }

  @Test
  void weeklyRounds_payoffOnlyRemaining_booksNothing() {
    // Only the payoff remains (remainingNonFinal == 0): the final plays at the PLE — the weekly
    // show must book nothing tournament-fed.
    tournament.setStatus(TournamentStatus.IN_PROGRESS);
    Show ple = new Show();
    ple.setId(2L);
    ple.setName("Big PLE");
    ple.setShowDate(LocalDate.of(2026, 6, 22));
    Show weeklyShow = show;
    weeklyShow.setShowDate(LocalDate.of(2026, 6, 8));

    when(showService.getShowsByDateRange(any(), any())).thenReturn(List.of());
    TournamentPacingService.PacingPlan plan =
        new TournamentPacingService.PacingPlan(
            TournamentPacingService.PayoffKind.FINAL_AT_PLE, List.of(), 3, 2, 0);
    when(pacingService.planFor(tournament, ple)).thenReturn(plan);

    assertTrue(service.bookWeeklyRounds(assignment, weeklyShow).isEmpty());
    verify(segmentResolutionService, never()).resolveTeamSegment(any(), any(), any(), any(), any());
  }

  @Test
  void weeklyRounds_autoStartScheduledTournament() {
    // Pacing auto-starts a SCHEDULED tournament at the first weekly slot.
    Show ple = new Show();
    ple.setId(2L);
    ple.setName("Big PLE");
    ple.setShowDate(LocalDate.of(2026, 6, 22));
    Show weeklyShow = show;
    weeklyShow.setShowDate(LocalDate.of(2026, 6, 8));

    when(showService.getShowsByDateRange(any(), any())).thenReturn(List.of());
    TournamentPacingService.PacingPlan plan =
        new TournamentPacingService.PacingPlan(
            TournamentPacingService.PayoffKind.FINAL_AT_PLE, List.of(), 7, 0, 6);
    when(pacingService.planFor(tournament, ple)).thenReturn(plan);
    when(tournamentService.findFormat("SINGLE_ELIMINATION")).thenReturn(Optional.of(format));
    when(tournamentService.hasEntries(5L)).thenReturn(false);
    when(tournamentService.findEligibleWrestlersSortedByFans(any(), eq(1L)))
        .thenReturn(List.of(alice, bob, wrestler(3L, "Cara"), wrestler(4L, "Dave")));
    when(tournamentService.findByIdWithDetails(5L)).thenReturn(Optional.of(tournament));
    when(tournamentService.startTournament(tournament))
        .thenAnswer(
            invocation -> {
              tournament.setStatus(TournamentStatus.IN_PROGRESS);
              return tournament;
            });
    SegmentType singlesType = new SegmentType();
    singlesType.setId(11L);
    singlesType.setName("One on One");
    when(segmentTypeService.findByCode(WellKnownSegmentType.ONE_ON_ONE.getCode()))
        .thenReturn(Optional.of(singlesType));
    TournamentEntry aliceEntry = entry(alice, 1, TournamentEntryStatus.ACTIVE);
    TournamentEntry bobEntry = entry(bob, 2, TournamentEntryStatus.ACTIVE);
    TournamentMatch match = match(1, aliceEntry, bobEntry);
    tournament.setRounds(new ArrayList<>(List.of(round(1, match))));
    Segment booked = singles(alice, bob, alice);
    when(segmentResolutionService.resolveTeamSegment(any(), any(), eq(singlesType), any(), any()))
        .thenReturn(booked);

    List<TournamentTemplateBookingService.TournamentBooking> bookings =
        service.bookWeeklyRounds(assignment, weeklyShow);

    assertEquals(1, bookings.size());
    verify(tournamentService).seedAuto(tournament, 8, 1L);
    verify(tournamentService).startTournament(tournament);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private void stubResolve(Segment booked) {
    when(segmentResolutionService.resolveTeamSegment(any(), any(), any(), any(), any()))
        .thenReturn(booked);
  }

  private static Wrestler wrestler(Long id, String name) {
    Wrestler w = new Wrestler();
    w.setId(id);
    w.setName(name);
    return w;
  }

  private static Show showWithName(Long id, String name, LocalDate date) {
    Show s = new Show();
    s.setId(id);
    s.setName(name);
    s.setShowDate(date);
    s.setType(showTypeNamed("Weekly"));
    return s;
  }

  private static ShowType showTypeNamed(String name) {
    ShowType t = new ShowType();
    t.setName(name);
    return t;
  }

  private static TournamentEntry entry(Wrestler wrestler, int seed, TournamentEntryStatus status) {
    return TournamentEntry.builder().wrestler(wrestler).seed(seed).status(status).build();
  }

  private static TournamentMatch match(int roundNumber, TournamentEntry e1, TournamentEntry e2) {
    return TournamentMatch.builder().entrant1(e1).entrant2(e2).build();
  }

  private static TournamentRound round(int roundNumber, TournamentMatch... matches) {
    TournamentRound r =
        TournamentRound.builder()
            .roundNumber(roundNumber)
            .roundName(roundNumber == 1 ? "Round 1" : "Semi-Final")
            .status(TournamentRoundStatus.IN_PROGRESS)
            .build();
    for (TournamentMatch m : matches) {
      m.setRound(r);
      r.getMatches().add(m);
    }
    return r;
  }

  private static Segment singles(Wrestler a, Wrestler b, Wrestler winner) {
    Segment segment = new Segment();
    segment.setSegmentType(new SegmentType());
    segment.addParticipant(a, 1);
    segment.addParticipant(b, 1);
    segment.setWinners(List.of(winner));
    return segment;
  }
}
