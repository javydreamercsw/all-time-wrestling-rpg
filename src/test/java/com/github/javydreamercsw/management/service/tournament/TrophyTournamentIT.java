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
import com.github.javydreamercsw.management.domain.team.Team;
import com.github.javydreamercsw.management.domain.team.TeamRepository;
import com.github.javydreamercsw.management.domain.team.TeamSize;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.domain.tournament.TrophyReign;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.show.ShowBookingService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for named-trophy tournaments on a live (Flyway-migrated) database: completing a
 * trophy tournament stamps one {@code trophy_reign} row through {@code markWinner} (the single
 * completion choke point), for singles and team brackets alike, and the read side ({@link
 * TrophyReignService#trophyHonorsOf}) expands team wins into member honor lines for AI narration.
 * Schema (V165/V134) and service logic both under test.
 */
class TrophyTournamentIT extends ManagementIntegrationTest {

  @Autowired private TournamentService tournamentService;
  @Autowired private TrophyReignService trophyReignService;
  @Autowired private UniverseRepository universeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private TeamRepository teamRepository;
  @Autowired private ShowBookingService showBookingService;

  private Universe anyUniverse() {
    return universeRepository.findAll().stream()
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No universe in test database"));
  }

  @Test
  @DisplayName("A completed singles trophy tournament stamps its reign and honors its winner")
  void singlesTrophyTournament_stampsReignAndHonors() {
    Universe universe = anyUniverse();
    String trophyName = "IT Singles Trophy " + System.nanoTime();
    Wrestler ace =
        wrestlerRepository.save(
            TestUtils.createWrestler("IT TAce " + System.nanoTime(), 0L, universe));
    Wrestler bea =
        wrestlerRepository.save(
            TestUtils.createWrestler("IT TBea " + System.nanoTime(), 0L, universe));

    Tournament tournament =
        tournamentService.createTournament(
            "IT Singles Trophy Cup " + System.nanoTime(),
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
            trophyName);
    assertThat(tournament.getTrophyName()).isEqualTo(trophyName);

    tournamentService.addEntry(tournament, ace, 1);
    tournamentService.addEntry(tournament, bea, 2);
    // addEntry persists rows without mutating this detached instance's collection — re-fetch
    // so startTournament's entrant guard sees them.
    Long tournamentId = tournament.getId();
    tournament = tournamentService.findByIdWithDetails(tournamentId).orElseThrow();
    tournamentService.startTournament(tournament);
    playOutBracket(tournament, ace.getId()); // Ace takes every match

    Tournament completed = tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow();
    assertThat(completed.getStatus()).isEqualTo(TournamentStatus.COMPLETE);

    // The reign row: one per edition, winner = the bracket winner.
    Optional<TrophyReign> reign = tournamentService.trophyReignOf(tournamentId);
    assertThat(reign).isPresent();
    assertThat(reign.get().getTrophyName()).isEqualTo(trophyName);
    assertThat(reign.get().getYearWon()).isEqualTo(LocalDate.now().getYear());
    assertThat(reign.get().getWinnerWrestler().getId()).isEqualTo(ace.getId());

    // The read side renders the narrator honor line.
    assertThat(trophyReignService.trophyHonorsOf(ace.getId()))
        .anyMatch(
            line -> line.startsWith(LocalDate.now().getYear() + " " + trophyName + " winner"));

    // Lineage: the trophy's editions, newest first.
    assertThat(trophyReignService.lineageOf(trophyName))
        .anySatisfy(r -> assertThat(r.getTournament().getId()).isEqualTo(tournamentId));
  }

  @Test
  @DisplayName("A completed team trophy tournament stamps the team and honors its members")
  void teamTrophyTournament_stampsReignAndHonorsMembers() {
    Universe universe = anyUniverse();
    String trophyName = "IT Team Trophy " + System.nanoTime();
    Wrestler ace =
        wrestlerRepository.save(
            TestUtils.createWrestler("IT TTAce " + System.nanoTime(), 0L, universe));
    Wrestler bea =
        wrestlerRepository.save(
            TestUtils.createWrestler("IT TTBea " + System.nanoTime(), 0L, universe));
    Wrestler cy =
        wrestlerRepository.save(
            TestUtils.createWrestler("IT TTCy " + System.nanoTime(), 0L, universe));
    Wrestler di =
        wrestlerRepository.save(
            TestUtils.createWrestler("IT TTDi " + System.nanoTime(), 0L, universe));
    Team winners = duo("IT TT Winners " + System.nanoTime(), ace, bea);
    Team runnersUp = duo("IT TT Runners " + System.nanoTime(), cy, di);

    Tournament tournament =
        tournamentService.createTournament(
            "IT Team Trophy Cup " + System.nanoTime(),
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
            TeamSize.DUO,
            trophyName);
    tournamentService.addEntry(tournament, winners, 1);
    tournamentService.addEntry(tournament, runnersUp, 2);
    // addEntry persists rows without mutating this detached instance's collection — re-fetch
    // so startTournament's entrant guard sees them.
    Long tournamentId = tournament.getId();
    tournament = tournamentService.findByIdWithDetails(tournamentId).orElseThrow();
    tournamentService.startTournament(tournament);
    playOutBracket(tournament, winners.getId()); // Winners take every match

    Tournament completed = tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow();
    assertThat(completed.getStatus()).isEqualTo(TournamentStatus.COMPLETE);

    Optional<TrophyReign> reign = tournamentService.trophyReignOf(tournamentId);
    assertThat(reign).isPresent();
    assertThat(reign.get().getWinnerTeam().getId()).isEqualTo(winners.getId());
    assertThat(reign.get().getWinnerWrestler()).isNull();

    // Team wins expand to member-level honors: both members are trophy winners (naming
    // teammates), while a non-member is not.
    String yearPrefix = LocalDate.now().getYear() + " " + trophyName + " winner";
    assertThat(trophyReignService.trophyHonorsOf(ace.getId()))
        .anySatisfy(
            line -> {
              assertThat(line).startsWith(yearPrefix);
              assertThat(line).contains("(with " + bea.getName() + ")");
            });
    assertThat(trophyReignService.trophyHonorsOf(bea.getId()))
        .anySatisfy(
            line -> {
              assertThat(line).startsWith(yearPrefix);
              assertThat(line).contains("(with " + ace.getName() + ")");
            });
    assertThat(trophyReignService.trophyHonorsOf(cy.getId()))
        .noneMatch(line -> line.startsWith(yearPrefix));
  }

  /**
   * Records results for every open match until the tournament completes, with {@code winnerId}
   * (wrestler id or team id per entry shape) taking each match. A single-elimination bracket
   * finishes through repeated {@code advanceToNextRound} calls — the manual-completion path the
   * detail view drives.
   */
  private void playOutBracket(Tournament tournament, Long winnerId) {
    for (int guard = 0; guard < 10; guard++) {
      List<TournamentMatch> open =
          tournamentService
              .findByIdWithDetails(tournament.getId())
              .orElseThrow()
              .getRounds()
              .stream()
              .flatMap(r -> r.getMatches().stream())
              .filter(m -> m.getWinner() == null)
              .toList();
      for (TournamentMatch match : open) {
        TournamentEntry winner =
            match.entrants().stream()
                .filter(
                    e ->
                        (e.getWrestler() != null && e.getWrestler().getId().equals(winnerId))
                            || (e.getTeam() != null && e.getTeam().getId().equals(winnerId)))
                .findFirst()
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "Winner id " + winnerId + " is not an entrant of the open match"));
        tournamentService.recordMatchResult(match, winner);
      }
      tournamentService.advanceToNextRound(
          tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow());
      if (tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow().getStatus()
          == TournamentStatus.COMPLETE) {
        return;
      }
    }
    throw new IllegalStateException(
        "Bracket did not complete within the guard — format: " + tournament.getFormatId());
  }

  private Team duo(String name, Wrestler first, Wrestler second) {
    Team team = new Team();
    team.setName(name);
    team.setMembers(new ArrayList<>(List.of(first, second)));
    team.setTeamSize(TeamSize.DUO);
    return teamRepository.save(team);
  }
}
