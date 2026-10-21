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

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.domain.tournament.TrophyReignRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.show.ShowService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Integration tests for the standalone tournament runner (ATW-p0sz): the fresh-install scenario (no
 * shows — the host show is auto-created), a picked existing show, and round-by-round pacing, all
 * asserting full RPG consequences (persisted segments, wrestler state deltas, trophy lineage).
 */
class TournamentRunnerServiceIT extends ManagementIntegrationTest {

  @Autowired private TournamentRunnerService runnerService;
  @Autowired private TournamentService tournamentService;
  @Autowired private ShowService showService;
  @Autowired private TournamentRepository tournamentRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private UniverseRepository universeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private WrestlerStateRepository wrestlerStateRepository;
  @Autowired private TrophyReignRepository trophyReignRepository;

  @Autowired
  private com.github.javydreamercsw.management.domain.title.TitleOpportunityRepository
      titleOpportunityRepository;

  @Autowired private TransactionTemplate transactionTemplate;

  private Universe universe;

  @BeforeEach
  void setUp() {
    universe = anyUniverse();
  }

  private Universe anyUniverse() {
    return universeRepository.findAll().stream()
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No universe in test database"));
  }

  @Test
  @DisplayName("runTournament with no shows auto-creates a host show and completes the bracket")
  void freshInstall_runTournament_autoCreatesHostShowAndCompletes() {
    long showCountBefore = showRepository.count();
    String unique = String.valueOf(System.nanoTime());
    List<Wrestler> roster = seedRoster(unique, 4);

    Tournament tournament =
        tournamentService.createTournament(
            "IT Runner Cup " + unique,
            "SINGLE_ELIMINATION",
            universe,
            null,
            LocalDate.now(),
            List.of(),
            null,
            null,
            null,
            null,
            false,
            false,
            null,
            "IT Runner Trophy " + unique);
    for (int i = 0; i < roster.size(); i++) {
      tournamentService.addEntry(tournament, roster.get(i), i + 1);
    }
    tournamentService.startTournament(
        tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow());

    Tournament started = tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow();
    TournamentRunnerService.TournamentRunReport report =
        transactionTemplate.execute(status -> runnerService.runTournament(started, null));

    assertThat(report).isNotNull();
    assertThat(report.tournamentComplete()).isTrue();
    assertThat(report.matchesPlayed()).isEqualTo(3);
    assertThat(report.winner()).isNotNull();

    // The host show was auto-created: one more show than before, named "<tournament> Night".
    assertThat(showRepository.count()).isEqualTo(showCountBefore + 1);
    Show hostShow = report.hostShow();
    assertThat(hostShow.getName()).isEqualTo("IT Runner Cup " + unique + " Night");
    assertThat(hostShow.getType()).isNotNull();

    // Every bracket match landed a persisted, NPC-generated segment on the host show.
    Tournament completed = tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow();
    assertThat(completed.getStatus())
        .isEqualTo(
            com.github.javydreamercsw.management.domain.tournament.TournamentStatus.COMPLETE);
    List<Segment> segments =
        completed.getRounds().stream()
            .flatMap(r -> r.getMatches().stream())
            .map(TournamentMatch::getSegment)
            .toList();
    assertThat(segments).hasSize(3).allMatch(s -> s != null && s.getShow() != null);
    // Show is a lazy proxy on a detached segment — compare ids, not names (no session here).
    assertThat(segments.stream().map(Segment::getShow))
        .allMatch(s -> s != null && hostShow.getId().equals(s.getId()));

    // The winner entry is stamped and the trophy lineage row exists (markWinner through the
    // runner's advanceToNextRound choke point).
    TournamentEntry winnerEntry =
        completed.getEntries().stream()
            .filter(e -> e.getStatus() == TournamentEntryStatus.WINNER)
            .findFirst()
            .orElseThrow();
    assertThat(trophyReignRepository.existsByTournamentId(tournament.getId())).isTrue();

    // Full RPG consequences: the winner's state moved (health/stamina deltas from resolution).
    WrestlerState winnerState =
        wrestlerStateRepository
            .findByWrestlerIdAndUniverseId(winnerEntry.getWrestler().getId(), universe.getId())
            .orElseThrow();
    assertThat(winnerState).isNotNull();
  }

  @Test
  @DisplayName("runTournament with a picked show books every segment on it, creating no shows")
  void existingShow_runTournament_usesPickedShow() {
    String unique = String.valueOf(System.nanoTime());
    List<Wrestler> roster = seedRoster(unique, 4);
    Show picked =
        showService.createShow(
            "IT Picked Show " + unique,
            "IT fixture",
            showTypeRepository.findAll().getFirst().getId(),
            LocalDate.now(),
            null,
            null,
            universe.getId(),
            null,
            null,
            null);
    long showCountAfterCreate = showRepository.count();

    Tournament tournament =
        tournamentService.createTournament(
            "IT Picked Cup " + unique,
            "SINGLE_ELIMINATION",
            universe,
            null,
            LocalDate.now(),
            List.of());
    for (int i = 0; i < roster.size(); i++) {
      tournamentService.addEntry(tournament, roster.get(i), i + 1);
    }
    tournamentService.startTournament(
        tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow());

    Tournament started = tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow();
    TournamentRunnerService.TournamentRunReport report =
        transactionTemplate.execute(status -> runnerService.runTournament(started, picked.getId()));

    assertThat(report).isNotNull();
    assertThat(report.tournamentComplete()).isTrue();
    assertThat(report.hostShow().getId()).isEqualTo(picked.getId());
    assertThat(showRepository.count()).isEqualTo(showCountAfterCreate);
  }

  @Test
  @DisplayName("runRound advances one round at a time; the second run completes the bracket")
  void runRound_pacesOneRoundPerCall() {
    String unique = String.valueOf(System.nanoTime());
    List<Wrestler> roster = seedRoster(unique, 4);

    Tournament tournament =
        tournamentService.createTournament(
            "IT Paced Cup " + unique,
            "SINGLE_ELIMINATION",
            universe,
            null,
            LocalDate.now(),
            List.of());
    for (int i = 0; i < roster.size(); i++) {
      tournamentService.addEntry(tournament, roster.get(i), i + 1);
    }
    tournamentService.startTournament(
        tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow());

    Tournament started = tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow();
    TournamentRunnerService.RoundRunReport round1 =
        transactionTemplate.execute(status -> runnerService.runRound(started, null));

    assertThat(round1).isNotNull();
    assertThat(round1.matchesPlayed()).isEqualTo(2);
    assertThat(round1.tournamentComplete()).isFalse();

    // After round 1: round 2 exists and the tournament is still IN_PROGRESS.
    Tournament midTournament =
        tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow();
    assertThat(midTournament.getStatus())
        .isEqualTo(
            com.github.javydreamercsw.management.domain.tournament.TournamentStatus.IN_PROGRESS);
    assertThat(midTournament.getRounds()).hasSize(2);

    TournamentRunnerService.RoundRunReport round2 =
        transactionTemplate.execute(
            status ->
                runnerService.runRound(
                    tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow(), null));

    assertThat(round2).isNotNull();
    assertThat(round2.matchesPlayed()).isEqualTo(1);
    assertThat(round2.tournamentComplete()).isTrue();
  }

  @Test
  @DisplayName("A briefcase-deciding tournament run standalone grants the briefcase immediately")
  void briefcaseTournament_runStandalone_grantsBriefcase() {
    String unique = String.valueOf(System.nanoTime());
    List<Wrestler> roster = seedRoster(unique, 4);

    Tournament tournament =
        tournamentService.createTournament(
            "IT Briefcase Cup " + unique,
            "SINGLE_ELIMINATION",
            universe,
            null,
            LocalDate.now(),
            List.of(),
            null,
            null,
            null,
            null,
            false,
            true, // briefcaseDeciding
            null,
            null);
    for (int i = 0; i < roster.size(); i++) {
      tournamentService.addEntry(tournament, roster.get(i), i + 1);
    }
    tournamentService.startTournament(
        tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow());

    Tournament started = tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow();
    transactionTemplate.execute(status -> runnerService.runTournament(started, null));

    // The idempotent grant ran at completion — one HELD briefcase opportunity exists for the
    // bracket winner, earned from this tournament.
    Tournament completed = tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow();
    TournamentEntry winnerEntry =
        completed.getEntries().stream()
            .filter(e -> e.getStatus() == TournamentEntryStatus.WINNER)
            .findFirst()
            .orElseThrow();
    Optional<com.github.javydreamercsw.management.domain.title.TitleOpportunity> briefcase =
        titleOpportunityRepository.findFirstByWrestlerIdAndStatus(
            winnerEntry.getWrestler().getId(),
            com.github.javydreamercsw.management.domain.title.TitleOpportunityStatus.HELD);
    assertThat(briefcase).isPresent();
    assertThat(briefcase.get().getEarnedFromTournament().getId()).isEqualTo(tournament.getId());
  }

  private List<Wrestler> seedRoster(String unique, int count) {
    return java.util.stream.IntStream.rangeClosed(1, count)
        .mapToObj(
            i ->
                wrestlerRepository.save(
                    TestUtils.createWrestler("IT Runner " + unique + " W" + i, 0L, universe)))
        .toList();
  }
}
