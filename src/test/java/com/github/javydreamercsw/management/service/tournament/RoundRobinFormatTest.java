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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatchRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.tournament.TournamentFormat.RenderMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoundRobinFormatTest {

  @Mock private TournamentRoundRepository roundRepo;
  @Mock private TournamentMatchRepository matchRepo;

  private RoundRobinFormat format;
  private TournamentFormatContext ctx;

  @BeforeEach
  void setUp() {
    format = new RoundRobinFormat();
    ctx = new TournamentFormatContext(roundRepo, matchRepo);
  }

  @Test
  void formatId_isCorrect() {
    assertThat(format.getFormatId()).isEqualTo("ROUND_ROBIN");
  }

  @Test
  void renderMode_isRoundRobinGrid() {
    assertThat(format.renderMode()).isEqualTo(RenderMode.ROUND_ROBIN_GRID);
  }

  @Test
  void minMaxEntrants() {
    assertThat(format.getMinEntrants()).isEqualTo(3);
    assertThat(format.getMaxEntrants()).isEqualTo(16);
  }

  @Test
  void generateBracket_createsNMinusOneRoundsForEvenGroup() {
    Tournament t = tournamentWithEntries(4);

    when(roundRepo.save(any()))
        .thenAnswer(
            inv -> {
              TournamentRound r = inv.getArgument(0);
              r.setMatches(new ArrayList<>());
              return r;
            });
    when(matchRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

    List<TournamentRound> rounds = format.generateBracket(t, ctx);

    // 4 entrants → N-1 = 3 rounds
    assertThat(rounds).hasSize(3);
  }

  @Test
  void advanceRound_returnsEmpty() {
    Tournament t = new Tournament();
    t.setRounds(new ArrayList<>());
    assertThat(format.advanceRound(t, ctx)).isEmpty();
  }

  @Test
  void isComplete_trueWhenAllRoundsComplete() {
    TournamentRound r1 = roundWithStatus(TournamentRoundStatus.COMPLETE);
    TournamentRound r2 = roundWithStatus(TournamentRoundStatus.COMPLETE);

    Tournament t = new Tournament();
    t.setRounds(new ArrayList<>(List.of(r1, r2)));

    assertThat(format.isComplete(t)).isTrue();
  }

  @Test
  void isComplete_falseWhenAnyRoundNotComplete() {
    TournamentRound r1 = roundWithStatus(TournamentRoundStatus.COMPLETE);
    TournamentRound r2 = roundWithStatus(TournamentRoundStatus.PENDING);

    Tournament t = new Tournament();
    t.setRounds(new ArrayList<>(List.of(r1, r2)));

    assertThat(format.isComplete(t)).isFalse();
  }

  @Test
  void estimateTotalMatches_isNTimesNMinusOneOverTwo() {
    // 4 entrants → 4*3/2 = 6 matches.
    assertThat(format.estimateTotalMatches(tournamentWithEntries(4))).isEqualTo(6);
    assertThat(format.estimateTotalMatches(tournamentWithEntries(5))).isEqualTo(10);
  }

  @Test
  void estimateTotalMatches_zeroWhenTooFewEntrants() {
    Tournament tooSmall = new Tournament();
    tooSmall.setFormatId("ROUND_ROBIN");
    tooSmall.setEntries(new ArrayList<>());
    assertThat(format.estimateTotalMatches(tooSmall)).isZero();
  }

  // ── Winner & standings (ATW-2aq3) ─────────────────────────────────────────

  @Test
  void eliminatesLosers_isFalse() {
    // A round-robin loss costs points, not the tournament — entrants keep playing.
    assertThat(format.eliminatesLosers()).isFalse();
  }

  @Test
  void standings_rankByPoints() {
    // 4 entrants, 3 matches decided: A 2-0 (4 pts), B 1-1 (2), C 0-1 (0, 1 match left), D 0-1.
    Tournament t = tournamentWithEntries(4);
    List<TournamentEntry> entries = t.getEntries();
    decided(entries.get(0), entries.get(1)); // A beats B
    decided(entries.get(0), entries.get(2)); // A beats C
    decided(entries.get(1), entries.get(3)); // B beats D

    List<TournamentFormat.Standing> standings = format.standings(t);

    assertThat(standings).hasSize(4);
    assertThat(standings.get(0).entrantName()).isEqualTo("W1");
    assertThat(standings.get(0).points()).isEqualTo(4);
    assertThat(standings.get(0).wins()).isEqualTo(2);
    assertThat(standings.get(0).rank()).isEqualTo(1);
    assertThat(standings.get(1).entrantName()).isEqualTo("W2");
    assertThat(standings.get(1).points()).isEqualTo(2);
    // Undecided matches are neither wins nor losses.
    assertThat(standings.get(2).wins()).isZero();
    assertThat(standings.get(2).losses()).isEqualTo(1);
    assertThat(standings.get(3).points()).isZero();
  }

  @Test
  void standings_headToHeadBreaksPointsTie() {
    // A and B both 1-1 (2 pts); A beat B head-to-head → A ranks first.
    Tournament t = tournamentWithEntries(3);
    List<TournamentEntry> entries = t.getEntries();
    decided(entries.get(0), entries.get(1)); // A beats B
    decided(entries.get(1), entries.get(2)); // B beats C

    List<TournamentFormat.Standing> standings = format.standings(t);

    assertThat(standings.get(0).entrantName()).isEqualTo("W1");
    assertThat(standings.get(1).entrantName()).isEqualTo("W2");
    assertThat(standings.get(0).points()).isEqualTo(standings.get(1).points());
  }

  @Test
  void standings_seedBreaksHeadToHeadCycle() {
    // Three-way cycle (A beat B, B beat C, C beat A): every tied entrant is 1-1 within the
    // group, head-to-head cannot separate them, and the lower seed number wins — A (seed 1).
    Tournament t = tournamentWithEntries(3);
    List<TournamentEntry> entries = t.getEntries();
    decided(entries.get(0), entries.get(1)); // A beats B
    decided(entries.get(1), entries.get(2)); // B beats C
    decided(entries.get(2), entries.get(0)); // C beats A

    List<TournamentFormat.Standing> standings = format.standings(t);

    assertThat(standings.get(0).entrantName()).isEqualTo("W1");
    assertThat(standings.get(1).entrantName()).isEqualTo("W2");
    assertThat(standings.get(2).entrantName()).isEqualTo("W3");
    assertThat(standings.get(0).points()).isEqualTo(standings.get(1).points());
    assertThat(standings.get(1).points()).isEqualTo(standings.get(2).points());
  }

  @Test
  void selectWinner_crownsPointsLeaderOnSplitBracket() {
    // THE BUG (ATW-2aq3): a split round-robin crowns nobody today — markWinner promotes "the
    // first remaining ACTIVE entry" and a split leaves none. The format must answer the points
    // leader instead. Valid 6-match bracket, two-way tie at 2-1: B beat A head-to-head → B wins
    // (D beat C for 3rd; every entrant plays every other exactly once).
    Tournament t = tournamentWithEntries(4);
    List<TournamentEntry> entries = t.getEntries();
    decided(entries.get(0), entries.get(2)); // A beats C
    decided(entries.get(0), entries.get(3)); // A beats D
    decided(entries.get(1), entries.get(0)); // B beats A
    decided(entries.get(1), entries.get(3)); // B beats D
    decided(entries.get(2), entries.get(1)); // C beats B
    decided(entries.get(3), entries.get(2)); // D beats C
    // Standings: B 2-1, A 2-1 (B via h2h), D 1-2, C 1-2 (D via h2h).

    Optional<TournamentEntry> winner = format.selectWinner(t);

    assertThat(winner).isPresent();
    assertThat(winner.get()).isSameAs(entries.get(1));
  }

  @Test
  void selectWinner_emptyWhenNoMatchesDecided() {
    Tournament t = tournamentWithEntries(4);
    assertThat(format.selectWinner(t)).isEmpty();
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private static Tournament tournamentWithEntries(int count) {
    Tournament t = new Tournament();
    t.setFormatId("ROUND_ROBIN");
    List<TournamentEntry> entries = new ArrayList<>();
    for (int i = 1; i < count + 1; i++) {
      Wrestler w = new Wrestler();
      w.setName("W" + i);
      TournamentEntry entry = TournamentEntry.builder().wrestler(w).seed(i).build();
      entry.setTournament(t);
      entry.setId((long) i); // persisted entries always carry an ID (selectWinner resolves by it)
      entries.add(entry);
    }
    t.setEntries(entries);
    t.setRounds(new ArrayList<>());
    return t;
  }

  private static TournamentRound roundWithStatus(TournamentRoundStatus status) {
    TournamentRound r = new TournamentRound();
    r.setStatus(status);
    r.setMatches(new ArrayList<>());
    return r;
  }

  private static void decided(TournamentEntry winner, TournamentEntry loser) {
    Tournament tournament = winner.getTournament();
    TournamentRound round = roundWithStatus(TournamentRoundStatus.COMPLETE);
    round.setTournament(tournament);
    TournamentMatch match =
        TournamentMatch.builder().round(round).entrant1(winner).entrant2(loser).build();
    match.setWinner(winner);
    round.getMatches().add(match);
    tournament.getRounds().add(round);
  }
}
