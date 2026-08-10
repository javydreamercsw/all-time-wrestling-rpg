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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SingleEliminationFormatTest {

  @Mock private TournamentRoundRepository roundRepo;
  @Mock private TournamentMatchRepository matchRepo;

  private SingleEliminationFormat format;
  private TournamentFormatContext ctx;

  @BeforeEach
  void setUp() {
    format = new SingleEliminationFormat();
    ctx = new TournamentFormatContext(roundRepo, matchRepo);
  }

  @Test
  void formatId_isCorrect() {
    assertThat(format.getFormatId()).isEqualTo("SINGLE_ELIMINATION");
  }

  @Test
  void renderMode_isTree() {
    assertThat(format.renderMode()).isEqualTo(RenderMode.TREE);
  }

  @Test
  void minMaxEntrants() {
    assertThat(format.getMinEntrants()).isEqualTo(4);
    assertThat(format.getMaxEntrants()).isEqualTo(64);
  }

  @Test
  void generateBracket_createsRound1WithCorrectPairings() {
    Tournament t = tournamentWith8Entries();

    TournamentRound round1 =
        TournamentRound.builder()
            .roundNumber(1)
            .roundName("Round 1")
            .status(TournamentRoundStatus.PENDING)
            .tournament(t)
            .build();
    when(roundRepo.save(any())).thenReturn(round1);
    when(matchRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

    List<TournamentRound> rounds = format.generateBracket(t, ctx);

    assertThat(rounds).hasSize(1);
    // 8 entrants → 4 matches in round 1
    assertThat(rounds.get(0).getMatches()).hasSize(4);
  }

  @Test
  void isComplete_falseWhenNoRounds() {
    Tournament t = new Tournament();
    t.setRounds(new ArrayList<>());
    assertThat(format.isComplete(t)).isFalse();
  }

  @Test
  void isComplete_trueWhenFinalMatchHasWinner() {
    TournamentEntry winner = entry(1);
    TournamentMatch finalMatch =
        TournamentMatch.builder().entrant1(winner).entrant2(entry(2)).winner(winner).build();

    TournamentRound finalRound =
        TournamentRound.builder()
            .roundNumber(1)
            .roundName("Final")
            .status(TournamentRoundStatus.COMPLETE)
            .matches(new ArrayList<>(List.of(finalMatch)))
            .build();

    Tournament t = new Tournament();
    t.setRounds(new ArrayList<>(List.of(finalRound)));

    assertThat(format.isComplete(t)).isTrue();
  }

  @Test
  void advanceRound_returnsEmptyWhenNoCompleteRound() {
    Tournament t = new Tournament();
    t.setRounds(new ArrayList<>());
    when(roundRepo.findByTournamentIdOrderByRoundNumberAsc(any())).thenReturn(List.of());

    assertThat(format.advanceRound(t, ctx)).isEmpty();
  }

  @Test
  void advanceRound_generatesNextRoundFromWinners() {
    Tournament t = tournamentWith8Entries();

    TournamentEntry e1 = entry(1);
    TournamentEntry e2 = entry(2);
    TournamentMatch m1 =
        TournamentMatch.builder().entrant1(e1).entrant2(entry(8)).winner(e1).build();
    TournamentMatch m2 =
        TournamentMatch.builder().entrant1(e2).entrant2(entry(7)).winner(e2).build();

    TournamentRound complete =
        TournamentRound.builder()
            .roundNumber(1)
            .roundName("Round 1")
            .status(TournamentRoundStatus.COMPLETE)
            .matches(new ArrayList<>(List.of(m1, m2)))
            .build();

    when(roundRepo.findByTournamentIdOrderByRoundNumberAsc(any())).thenReturn(List.of(complete));

    TournamentRound round2 =
        TournamentRound.builder()
            .roundNumber(2)
            .roundName("Final")
            .status(TournamentRoundStatus.PENDING)
            .tournament(t)
            .build();
    when(roundRepo.save(any())).thenReturn(round2);
    when(matchRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

    List<TournamentMatch> matches = format.advanceRound(t, ctx);

    assertThat(matches).hasSize(1);
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private static Tournament tournamentWith8Entries() {
    Tournament t = new Tournament();
    t.setFormatId("SINGLE_ELIMINATION");
    List<TournamentEntry> entries = new ArrayList<>();
    for (int i = 1; i <= 8; i++) {
      entries.add(entry(i));
    }
    t.setEntries(entries);
    t.setRounds(new ArrayList<>());
    return t;
  }

  private static TournamentEntry entry(int seed) {
    Wrestler w = new Wrestler();
    w.setName("Wrestler " + seed);
    return TournamentEntry.builder().wrestler(w).seed(seed).build();
  }
}
