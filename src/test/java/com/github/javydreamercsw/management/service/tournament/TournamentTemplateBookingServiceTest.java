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
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateSegmentAssignment;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for tournament-fed booking on PLE templates (ATW-oahn). */
@ExtendWith(MockitoExtension.class)
class TournamentTemplateBookingServiceTest {

  @Mock private TournamentService tournamentService;
  @Mock private NPCSegmentResolutionService segmentResolutionService;
  @Mock private TournamentFormat format;

  private TournamentTemplateBookingService service;
  private Show show;
  private SegmentType rumbleType;
  private ShowTemplateSegmentAssignment assignment;
  private Tournament tournament;
  private Wrestler alice;
  private Wrestler bob;

  @BeforeEach
  void setUp() {
    service = new TournamentTemplateBookingService(tournamentService, segmentResolutionService);

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

    tournament = new Tournament();
    tournament.setId(5L);
    tournament.setName("Crown Cup");
    tournament.setFormatId("SINGLE_ELIMINATION");
    tournament.setStatus(TournamentStatus.SCHEDULED);

    assignment = new ShowTemplateSegmentAssignment();
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
    // seedAuto does not touch the in-memory collection — simulate by leaving the caller's
    // instance empty and having the refresh return the seeded, bracketed instance.
    lenient()
        .when(tournamentService.findByIdWithDetails(5L))
        .thenAnswer(
            invocation -> {
              tournament.setEntries(new java.util.ArrayList<>(List.of(aliceEntry, bobEntry)));
              tournament.setRounds(new java.util.ArrayList<>(List.of(round(1, match))));
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
  void inProgressTournament_booksFirstOpenMatch() {
    tournament.setStatus(TournamentStatus.IN_PROGRESS);
    TournamentEntry aliceEntry = entry(alice, 1, TournamentEntryStatus.ACTIVE);
    TournamentEntry bobEntry = entry(bob, 2, TournamentEntryStatus.ACTIVE);
    TournamentMatch match = match(1, aliceEntry, bobEntry);
    tournament.setRounds(new java.util.ArrayList<>(List.of(round(1, match))));

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
    tournament.setRounds(new java.util.ArrayList<>(List.of(completeRound)));

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
  void completeTournament_booksWinnerShowcase() {
    tournament.setStatus(TournamentStatus.COMPLETE);
    TournamentEntry aliceEntry = entry(alice, 1, TournamentEntryStatus.WINNER);
    TournamentEntry bobEntry = entry(bob, 2, TournamentEntryStatus.ELIMINATED);
    tournament.setEntries(new java.util.ArrayList<>(List.of(aliceEntry, bobEntry)));

    Segment booked = singles(alice, bob, alice);
    stubResolve(booked);

    Optional<TournamentTemplateBookingService.TournamentBooking> booking =
        service.bookTournamentFedSegment(assignment, rumbleType, show);

    assertTrue(booking.isPresent());
    assertEquals(booked, booking.get().segment());
    assertTrue(booking.get().detail().contains("Winner showcase"));
    verify(tournamentService, never()).startTournament(any());
    verify(tournamentService, never()).recordMatchResult(any(), any());
  }

  @Test
  void completeTournamentWithoutWinner_fallsBackEmpty() {
    tournament.setStatus(TournamentStatus.COMPLETE);
    tournament.setEntries(new java.util.ArrayList<>());

    assertTrue(
        service.bookTournamentFedSegment(assignment, rumbleType, show).isEmpty(),
        "COMPLETE tournament with no winner entry must fall back to the AI path");
  }

  @Test
  void autoStartFailure_fallsBackEmpty() {
    // Unseeded tournament whose format is unresolvable → seedAuto throws.
    when(tournamentService.findFormat("SINGLE_ELIMINATION")).thenReturn(Optional.empty());

    assertTrue(
        service.bookTournamentFedSegment(assignment, rumbleType, show).isEmpty(),
        "SCHEDULED tournament that cannot start must fall back to the AI path");
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
    tournament.setRounds(new java.util.ArrayList<>(List.of(completeRound)));
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
    tournament.setRounds(new java.util.ArrayList<>(List.of(round(1, match))));

    com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule rule =
        new com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule();
    rule.setId(20L);
    rule.setName("Rumble Rules");
    assignment.setSegmentRule(rule);

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
    tournament.setRounds(new java.util.ArrayList<>(List.of(round(1, match))));

    Segment booked = singles(alice, bob, alice);
    when(segmentResolutionService.resolveTeamSegment(any(), any(), any(), any(), any()))
        .thenReturn(booked);

    Optional<TournamentTemplateBookingService.TournamentBooking> booking =
        service.bookTournamentFedSegment(assignment, rumbleType, show);

    assertTrue(booking.isPresent());
    // SegmentTeam has no equals — capture and assert on the wrestlers instead.
    org.mockito.ArgumentCaptor<com.github.javydreamercsw.management.service.segment.SegmentTeam>
        team1Captor =
            org.mockito.ArgumentCaptor.forClass(
                com.github.javydreamercsw.management.service.segment.SegmentTeam.class);
    org.mockito.ArgumentCaptor<com.github.javydreamercsw.management.service.segment.SegmentTeam>
        team2Captor =
            org.mockito.ArgumentCaptor.forClass(
                com.github.javydreamercsw.management.service.segment.SegmentTeam.class);
    org.mockito.Mockito.verify(segmentResolutionService)
        .resolveTeamSegment(
            team1Captor.capture(), team2Captor.capture(), eq(rumbleType), eq(show), eq(""));
    assertEquals("Alice", team1Captor.getValue().getMembers().get(0).getName());
    assertEquals("Bob", team2Captor.getValue().getMembers().get(0).getName());
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
