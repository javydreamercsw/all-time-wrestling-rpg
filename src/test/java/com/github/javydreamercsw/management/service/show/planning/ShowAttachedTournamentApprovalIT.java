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

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowCategory;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.tournament.TournamentService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end approval IT for one-time show-attached tournaments (ATW-xbn4), on the real service
 * graph with a live database: a tournament bound to a particular show books its payoff there once —
 * auto-seeding and auto-starting at the host show when still SCHEDULED — while its non-final rounds
 * pace onto earlier weekly shows without any template row.
 */
class ShowAttachedTournamentApprovalIT extends ManagementIntegrationTest {

  @Autowired private TournamentService tournamentService;
  @Autowired private ShowService showService;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private TournamentRepository tournamentRepository;
  @Autowired private UniverseRepository universeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private ShowPlanningService showPlanningService;

  private Universe universe;
  private List<Wrestler> roster;
  private Show payoffShow;
  private Show weeklyShow;
  private Tournament tournament;
  private SegmentType singlesType;

  @BeforeEach
  void setUpFixture() {
    universe =
        universeRepository
            .findByName("Default Universe")
            .orElseThrow(() -> new IllegalStateException("Default Universe missing from test DB"));
    roster = seedRoster(6);
    tournament = seedTournament();
    singlesType = seedSinglesType();
    ShowType weeklyType = seedWeeklyType();
    payoffShow = seedShow(weeklyType, "Crown Cup Final", LocalDate.now().plusDays(21));
    weeklyShow = seedShow(weeklyType, "Crown Cup Week", LocalDate.now().plusDays(7));
  }

  private List<Wrestler> seedRoster(int count) {
    List<Wrestler> roster = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Wrestler w = new Wrestler();
      w.setName("Show-Attached Roster " + i + " " + System.nanoTime());
      w.setActive(true);
      w.setIsPlayer(false);
      w.setGender(Gender.MALE);
      w = wrestlerRepository.saveAndFlush(w);
      WrestlerState state = new WrestlerState();
      state.setWrestler(w);
      // Fans well above the sync roster's (which default to 0) so the fixture wrestlers top
      // the fan ranking and auto-seeding picks exactly them.
      state.setFans(1_000_000L * (count - i));
      state.setUniverse(universe);
      w.getWrestlerStates().add(state);
      roster.add(wrestlerRepository.saveAndFlush(w));
    }
    return roster;
  }

  private Tournament seedTournament() {
    Tournament t = new Tournament();
    t.setName("Show-Attached Cup IT " + System.nanoTime());
    t.setFormatId("SINGLE_ELIMINATION");
    t.setStatus(TournamentStatus.SCHEDULED);
    t.setUniverse(universe);
    t.setEntries(new ArrayList<>());
    t.setRounds(new ArrayList<>());
    return tournamentRepository.saveAndFlush(t);
  }

  /** Persist exactly {@code count} entries on the tournament — no auto-seed roster creep. */
  private void seedEntries(int count) {
    for (int i = 0; i < count; i++) {
      TournamentEntry entry =
          TournamentEntry.builder()
              .wrestler(roster.get(i))
              .seed(i + 1)
              .status(TournamentEntryStatus.ACTIVE)
              .tournament(tournament)
              .build();
      tournament.getEntries().add(entry);
    }
    tournament = tournamentRepository.saveAndFlush(tournament);
  }

  private SegmentType seedSinglesType() {
    return segmentTypeRepository
        .findByCode(WellKnownSegmentType.ONE_ON_ONE.getCode())
        .orElseGet(
            () -> {
              SegmentType type = new SegmentType();
              type.setName("One on One");
              type.setDescription("IT fixture singles type");
              return segmentTypeRepository.saveAndFlush(type);
            });
  }

  private ShowType seedWeeklyType() {
    return showTypeRepository.findAll().stream()
        .filter(t -> t.getCategory() == ShowCategory.WEEKLY)
        .findFirst()
        .orElseGet(
            () -> {
              ShowType type = new ShowType();
              type.setName("Weekly IT " + System.nanoTime());
              type.setCategory(ShowCategory.WEEKLY);
              return showTypeRepository.saveAndFlush(type);
            });
  }

  private Show seedShow(ShowType weeklyType, String name, LocalDate date) {
    // Weekly category type and NO template — the one-time tournament needs no template row.
    return showService.createShow(
        name + " " + System.nanoTime(),
        "Show-attached tournament IT fixture",
        weeklyType.getId(),
        date,
        null,
        null,
        universe.getId(),
        null,
        null,
        null);
  }

  @Test
  @DisplayName("Approving the host show auto-starts the tournament and books the payoff")
  void hostShowApproval_booksPayoff() {
    // Exactly two entries: the bracket's sole match IS the final, so the host-show approval
    // must book it as the payoff (no linked title → not flagged as a title segment).
    seedEntries(2);
    tournament.setPayoffShow(payoffShow);
    tournament = tournamentRepository.saveAndFlush(tournament);

    approveCard(payoffShow, List.of(cardForPayoff()));

    Tournament after = tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow();
    assertThat(after.getStatus()).isEqualTo(TournamentStatus.COMPLETE);
    // The host-show link is consumed.
    assertThat(after.getPayoffShow()).isNull();
    assertThat(tournamentRepository.findByPayoffShowId(payoffShow.getId()))
        .noneMatch(t -> t.getId().equals(tournament.getId()));

    List<Segment> saved = segmentRepository.findByShowOrderBySegmentOrderAsc(payoffShow);
    // The AI-proposed slot AND the payoff: the tournament booking is an extra segment.
    assertThat(saved).hasSize(2);
    Segment payoff = saved.get(1);
    assertThat(payoff.getIsTitleSegment()).isFalse(); // no linked title on this fixture
    // The payoff segment's participants are bracket-driven (the two entrants).
    assertThat(payoff.getWrestlers())
        .extracting(Wrestler::getId)
        .containsExactlyInAnyOrder(roster.get(0).getId(), roster.get(1).getId());
    // The winner entry exists — the bracket completed at the host show.
    assertThat(
            after.getEntries().stream()
                .anyMatch(e -> e.getStatus() == TournamentEntryStatus.WINNER))
        .isTrue();
  }

  @Test
  @DisplayName("Re-approving the host show does not book a second payoff")
  void hostShowReapproval_noDuplicatePayoff() {
    seedEntries(2);
    tournament.setPayoffShow(payoffShow);
    tournament = tournamentRepository.saveAndFlush(tournament);
    approveCard(payoffShow, List.of(cardForPayoff()));
    int segmentsAfterFirst = segmentRepository.findByShowOrderBySegmentOrderAsc(payoffShow).size();
    assertThat(segmentsAfterFirst).isPositive();

    approveCard(payoffShow, List.of(cardForPayoff()));

    // The second card's AI-proposed segment books, but no second tournament segment appears —
    // the consumed link stays consumed and the bracket never re-books.
    List<Segment> saved = segmentRepository.findByShowOrderBySegmentOrderAsc(payoffShow);
    assertThat(saved).hasSize(segmentsAfterFirst + 1);
    Tournament after = tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow();
    assertThat(
            after.getRounds().stream()
                .flatMap(r -> r.getMatches().stream())
                .filter(m -> m.getSegment() != null)
                .count())
        .isEqualTo(1L);
    assertThat(tournamentRepository.findByPayoffShowId(payoffShow.getId()))
        .noneMatch(t -> t.getId().equals(tournament.getId()));
  }

  @Test
  @DisplayName("Weekly shows before the host show pace the non-final rounds")
  void weeklyShowApproval_pacesRounds() {
    // 4-entrant bracket: 3 matches — round 1 holds 2, the final is the payoff. The weekly
    // approval books exactly one paced round; the final stays for the host show.
    seedEntries(4);
    tournament.setPayoffShow(payoffShow);
    tournament = tournamentRepository.saveAndFlush(tournament);

    approveCard(
        weeklyShow,
        List.of(
            singles(roster.get(4).getName(), roster.get(5).getName(), roster.get(4).getName())));

    Tournament after = tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow();
    assertThat(after.getStatus()).isEqualTo(TournamentStatus.IN_PROGRESS);
    // Pacing: 2 non-final matches over 1 weekly slot → both round-1 matches book here; the
    // final stays for the host show.
    assertThat(
            after.getRounds().stream()
                .flatMap(r -> r.getMatches().stream())
                .filter(m -> m.getSegment() != null)
                .count())
        .isEqualTo(2L);
    // The AI-proposed segment also saved.
    assertThat(segmentRepository.findByShowOrderBySegmentOrderAsc(weeklyShow)).hasSize(3);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  /** Run the real approval path over the given card. */
  private void approveCard(Show targetShow, List<ProposedSegment> card) {
    showPlanningService.approveSegments(targetShow, card);
  }

  private ProposedSegment singles(String w1, String w2, String winner) {
    ProposedSegment segment = new ProposedSegment();
    segment.setType(singlesType.getName());
    if (w1 != null) {
      segment.setTeams(List.of(List.of(w1), List.of(w2)));
      if (winner != null) {
        segment.setWinners(List.of(winner));
      }
    }
    return segment;
  }

  private ProposedSegment cardForPayoff() {
    // Participants are irrelevant — the tournament owns the payoff slot. Keep names resolvable
    // so the AI-proposed fallback path could save them if the payoff did not fire.
    return singles(roster.get(0).getName(), roster.get(1).getName(), roster.get(0).getName());
  }
}
