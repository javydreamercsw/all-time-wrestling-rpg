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

  // ── helpers ──────────────────────────────────────────────────────────────

  private static Tournament tournamentWithEntries(int count) {
    Tournament t = new Tournament();
    t.setFormatId("ROUND_ROBIN");
    List<TournamentEntry> entries = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
      Wrestler w = new Wrestler();
      w.setName("W" + i);
      entries.add(TournamentEntry.builder().wrestler(w).seed(i).build());
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
}
