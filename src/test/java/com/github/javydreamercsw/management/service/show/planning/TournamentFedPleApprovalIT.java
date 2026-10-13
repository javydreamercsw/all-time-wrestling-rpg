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
package com.github.javydreamercsw.management.service.show.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.tournament.*;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end approval IT for tournament-fed PLE segments (ATW-oahn), on the real service graph with
 * a live database: unseeded SCHEDULED tournament paired to a PLE template's event-only segment type
 * → {@code approveSegments} → the bracket fills the match, auto-seeds and auto-starts, and mirrors
 * the winner.
 */
class TournamentFedPleApprovalIT extends AbstractTournamentFedPleIT {

  @Autowired private TournamentMatchRepository matchRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private RivalryService rivalryService;

  @BeforeEach
  void setUpFixture() {
    buildTournamentFedPleFixture();
  }

  @Test
  @DisplayName("Approving a card books the tournament's first match into the paired segment")
  void approval_booksTournamentMatch() {
    // Card: a normal singles match plus the event-only paired type (AI-proposed participants
    // for the paired slot will be ignored).
    ProposedSegment opener =
        singles(roster.get(0).getName(), roster.get(1).getName(), roster.get(0).getName());
    ProposedSegment rumble =
        singles(roster.get(2).getName(), roster.get(3).getName(), roster.get(2).getName());
    rumble.setType(eventType.getName());

    approveCard(show, List.of(opener, rumble));

    // Tournament: auto-seeded from the roster (fixture wrestlers top the fan ranking),
    // auto-started, and its round 1 bracket feeds the paired segment. The fixture's `tournament`
    // reference is detached — re-read for post-approval state.
    Tournament after = tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow();
    assertThat(after.getStatus()).isEqualTo(TournamentStatus.IN_PROGRESS);

    List<Segment> saved = segmentRepository.findByShowOrderBySegmentOrderAsc(show);
    assertThat(saved).hasSize(2);

    Segment fedSegment = saved.get(1);
    assertThat(fedSegment.getSegmentType().getId()).isEqualTo(eventType.getId());
    // Participants must be the bracket's round-1 entrants, NOT the AI-proposed
    // roster.get(2)/get(3). The fixture wrestlers top the fan ranking, so the lowest-id
    // round-1 match (first open match = top seed vs bottom seed) contains the top fixture
    // wrestler; the bracket guarantees seeds 1..N pairing i vs N+1-i.
    List<TournamentMatch> round1Matches = matchRepository.findByRoundId(firstRoundId());
    // Round 1 pairs every seeded entrant: auto-seed requested the format max and clamped to
    // the active roster (fixture wrestlers + the sync-seeded roster).
    assertThat(round1Matches).hasSize((int) tournamentService.countEntries(after) / 2);
    // The entrant/wrestler chains on repository-loaded matches are lazy proxies — take the
    // booked match from the initialized graph instead.
    TournamentMatch booked =
        after.getRounds().stream()
            .filter(r -> r.getRoundNumber() == 1)
            .findFirst()
            .orElseThrow()
            .getMatches()
            .stream()
            .filter(m -> m.getSegment() != null)
            .min(Comparator.comparing(TournamentMatch::getId))
            .orElseThrow();
    assertThat(fedSegment.getWrestlers())
        .extracting(w -> w.getId())
        .containsExactlyInAnyOrder(
            booked.getEntrant1().getWrestler().getId(), booked.getEntrant2().getWrestler().getId());
    // The top fixture wrestler is seed 1 — it must be in the first-booked match.
    assertThat(fedSegment.getWrestlers())
        .extracting(w -> w.getId())
        .contains(roster.get(0).getId());
    // Winner mirrored into the bracket.
    assertThat(booked.getWinner()).isNotNull();
    assertThat(fedSegment.getWinners())
        .extracting(w -> w.getId())
        .containsExactly(booked.getWinner().getWrestler().getId());
  }

  @Test
  @DisplayName("A second approval advances the bracket — next open match is fed")
  void secondApproval_feedsNextOpenMatch() {
    ProposedSegment first = singles(null, null, null);
    first.setType(eventType.getName());
    approveCard(show, List.of(first));

    // Second show on the same template feeds the next open match of the same tournament.
    Show secondShow = cloneShow();
    ProposedSegment second = singles(null, null, null);
    second.setType(eventType.getName());
    approveCard(secondShow, List.of(second));

    List<TournamentMatch> round1 = matchRepository.findByRoundId(firstRoundId());
    List<TournamentMatch> booked = round1.stream().filter(m -> m.getSegment() != null).toList();
    assertThat(booked).as("Two approvals must book two distinct round-1 matches").hasSize(2);
    assertThat(booked.get(0).getSegment().getId()).isNotEqualTo(booked.get(1).getSegment().getId());
    // Every booked match has a winner recorded and its loser eliminated in the bracket.
    // Entrant proxies are detached here — resolve statuses through the graph re-read.
    Tournament after = tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow();
    List<TournamentMatch> reloaded =
        after.getRounds().stream()
            .filter(r -> r.getRoundNumber() == 1)
            .findFirst()
            .orElseThrow()
            .getMatches()
            .stream()
            .filter(m -> m.getSegment() != null)
            .toList();
    assertThat(reloaded).hasSize(2);
    reloaded.forEach(
        match -> {
          assertThat(match.getWinner()).isNotNull();
          TournamentEntry loser =
              match.getWinner().equals(match.getEntrant1())
                  ? match.getEntrant2()
                  : match.getEntrant1();
          assertThat(loser.getStatus()).isEqualTo(TournamentEntryStatus.ELIMINATED);
        });
  }

  @Test
  @DisplayName("Approving without the paired type does not touch the tournament")
  void approval_withoutPairedType_leavesTournamentScheduled() {
    approveCard(
        show,
        List.of(
            singles(roster.get(0).getName(), roster.get(1).getName(), roster.get(0).getName())));

    assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.SCHEDULED);
    assertThat(tournamentService.countEntries(tournament)).isZero();
  }

  @Test
  @DisplayName("A card failing validation is rejected before any booking")
  void approval_validationStillApplies() {
    // Deterministic validation failure: a heat>=30 rivalry must be booked with a stipulation
    // when it appears on the card (STIPULATION_REQUIRED). The paired tournament type satisfies
    // the MUST_BOOK coverage, so the error fires and the whole approval rolls back.
    Rivalry rivalry =
        rivalryService
            .createRivalry(
                roster.get(4).getId(),
                roster.get(5).getId(),
                "IT fixture rivalry",
                universe.getId())
            .orElseThrow();
    rivalryService.addHeat(rivalry.getId(), 30, "IT fixture heat");
    ProposedSegment rumble =
        singles(roster.get(4).getName(), roster.get(5).getName(), roster.get(4).getName());
    rumble.setType(eventType.getName());
    rumble.setRivalryId(rivalry.getId());
    // No rules → STIPULATION_REQUIRED error.

    assertThatThrownBy(() -> approveCard(show, List.of(rumble)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("STIPULATION_REQUIRED");
    // Nothing leaked into the tournament.
    assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.SCHEDULED);
    assertThat(tournamentService.countEntries(tournament)).isZero();
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private Long firstRoundId() {
    return tournamentService
        .findByIdWithDetails(tournament.getId())
        .orElseThrow()
        .getRounds()
        .stream()
        .filter(r -> r.getRoundNumber() == 1)
        .findFirst()
        .orElseThrow()
        .getId();
  }

  private ProposedSegment singles(String w1, String w2, String winner) {
    ProposedSegment segment = new ProposedSegment();
    segment.setType("One on One");
    if (w1 != null) {
      segment.setTeams(List.of(List.of(w1), List.of(w2)));
      if (winner != null) {
        segment.setWinners(List.of(winner));
      }
    }
    return segment;
  }

  private Show cloneShow() {
    return showService.createShow(
        "Fed PLE II " + System.nanoTime(),
        "Second tournament-fed PLE",
        show.getType().getId(),
        LocalDate.now().plusDays(14),
        null,
        show.getTemplate().getId(),
        universe.getId(),
        null,
        null,
        null);
  }
}
