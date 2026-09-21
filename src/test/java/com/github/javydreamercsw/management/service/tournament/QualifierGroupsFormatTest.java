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
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatchRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Bracket generation for qualifier groups feeding a multi-entrant final (ATW-oloa). */
@ExtendWith(MockitoExtension.class)
class QualifierGroupsFormatTest {

  @Mock private TournamentRoundRepository roundRepo;
  @Mock private TournamentMatchRepository matchRepo;

  private QualifierGroupsFormat format;
  private TournamentFormatContext ctx;

  @BeforeEach
  void setUp() {
    format = new QualifierGroupsFormat();
    ctx = new TournamentFormatContext(roundRepo, matchRepo);
  }

  @Test
  void formatId_isCorrect() {
    assertThat(format.getFormatId()).isEqualTo("QUALIFIER_GROUPS");
  }

  @Test
  void generateBracket_12Entrants_four3ManQualifiers() {
    Tournament t = tournamentWith(12);
    when(roundRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(matchRepo.save(any()))
        .thenAnswer(
            inv -> {
              TournamentMatch m = inv.getArgument(0);
              // Simulate the DB identity assignment the round's cascade relies on.
              if (m.getParticipants() != null) {
                m.getParticipants().forEach(p -> p.setMatch(m));
              }
              return m;
            });

    List<TournamentRound> rounds = format.generateBracket(t, ctx);

    assertThat(rounds).hasSize(1);
    TournamentRound qualifiers = rounds.get(0);
    assertThat(qualifiers.getRoundName()).isEqualTo("Qualifiers");
    assertThat(qualifiers.getMatches()).hasSize(4);
    // Every match is multi-entrant with all group members in order.
    assertThat(qualifiers.getMatches()).allMatch(TournamentMatch::isMultiEntrant);
    int totalEntrants = qualifiers.getMatches().stream().mapToInt(m -> m.entrants().size()).sum();
    assertThat(totalEntrants).isEqualTo(12);
    // Each entrant plays exactly one qualifier.
    assertThat(qualifiers.getMatches().stream().flatMap(m -> m.entrants().stream()).distinct())
        .hasSize(12);
  }

  @Test
  void advanceRound_generatesMultiEntrantFinalFromQualifierWinners() {
    Tournament t = tournamentWith(6);

    TournamentRound qualifierRound =
        TournamentRound.builder()
            .tournament(t)
            .roundNumber(1)
            .roundName("Qualifiers")
            .status(TournamentRoundStatus.COMPLETE)
            .build();
    List<TournamentEntry> groupA = List.of(entry(1), entry(2));
    List<TournamentEntry> groupB = List.of(entry(3), entry(4));
    TournamentMatch q1 =
        TournamentMatch.builder()
            .round(qualifierRound)
            .entrant1(groupA.get(0))
            .entrant2(groupA.get(1))
            .winner(groupA.get(0))
            .build();
    TournamentMatch q2 =
        TournamentMatch.builder()
            .round(qualifierRound)
            .entrant1(groupB.get(0))
            .entrant2(groupB.get(1))
            .winner(groupB.get(0))
            .build();
    qualifierRound.setMatches(new ArrayList<>(List.of(q1, q2)));
    t.setRounds(new ArrayList<>(List.of(qualifierRound)));

    when(roundRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(matchRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    // The tournament has no DB id in this unit fixture — the repository query would scan by id
    // and find nothing. Serve the in-memory rounds instead (production passes a persisted id).
    when(roundRepo.findByTournamentIdOrderByRoundNumberAsc(any())).thenAnswer(inv -> t.getRounds());

    List<TournamentMatch> finals = format.advanceRound(t, ctx);

    assertThat(finals).hasSize(1);
    TournamentMatch finalMatch = finals.get(0);
    assertThat(finalMatch.getRound().getRoundName()).isEqualTo("Final");
    // The final carries exactly the qualifier winners. Two qualifiers → a 2-entrant final,
    // which uses the classic entrant1/entrant2 shape (isMultiEntrant false — correct).
    assertThat(finalMatch.entrants()).containsExactly(groupA.get(0), groupB.get(0));
    assertThat(finalMatch.isMultiEntrant()).isFalse();
  }

  @Test
  void advanceRound_threeQualifiers_finalIsMultiEntrant() {
    Tournament t = tournamentWith(9);

    TournamentRound qualifierRound =
        TournamentRound.builder()
            .tournament(t)
            .roundNumber(1)
            .roundName("Qualifiers")
            .status(TournamentRoundStatus.COMPLETE)
            .build();
    List<TournamentEntry> winners = new ArrayList<>();
    for (int g = 0; g < 3; g++) {
      TournamentEntry winner = entry(g * 3 + 1);
      winners.add(winner);
      qualifierRound
          .getMatches()
          .add(
              TournamentMatch.builder()
                  .round(qualifierRound)
                  .entrant1(winner)
                  .entrant2(entry(g * 3 + 2))
                  .winner(winner)
                  .build());
    }
    t.setRounds(new ArrayList<>(List.of(qualifierRound)));

    when(roundRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(matchRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(roundRepo.findByTournamentIdOrderByRoundNumberAsc(any())).thenAnswer(inv -> t.getRounds());

    List<TournamentMatch> finals = format.advanceRound(t, ctx);

    assertThat(finals).hasSize(1);
    // Three qualifiers → a 3-entrant final: the multi-entrant shape kicks in.
    assertThat(finals.get(0).isMultiEntrant()).isTrue();
    assertThat(finals.get(0).entrants()).containsExactlyElementsOf(winners);
  }

  @Test
  void advanceRound_afterFinal_returnsEmpty() {
    Tournament t = tournamentWith(6);
    TournamentRound finalRound =
        TournamentRound.builder()
            .tournament(t)
            .roundNumber(2)
            .roundName("Final")
            .status(TournamentRoundStatus.COMPLETE)
            .build();
    t.setRounds(new ArrayList<>(List.of(finalRound)));
    assertThat(format.advanceRound(t, ctx)).isEmpty();
  }

  @Test
  void isComplete_falseUntilFinalHasWinner() {
    Tournament t = tournamentWith(6);
    TournamentRound qualifierRound =
        TournamentRound.builder()
            .tournament(t)
            .roundNumber(1)
            .roundName("Qualifiers")
            .status(TournamentRoundStatus.COMPLETE)
            .build();
    qualifierRound.setMatches(
        new ArrayList<>(
            List.of(
                TournamentMatch.builder()
                    .round(qualifierRound)
                    .entrant1(entry(1))
                    .entrant2(entry(2))
                    .winner(entry(1))
                    .build(),
                TournamentMatch.builder()
                    .round(qualifierRound)
                    .entrant1(entry(3))
                    .entrant2(entry(4))
                    .winner(entry(3))
                    .build())));
    t.setRounds(new ArrayList<>(List.of(qualifierRound)));
    assertThat(format.isComplete(t)).isFalse();

    TournamentRound finalRound =
        TournamentRound.builder()
            .tournament(t)
            .roundNumber(2)
            .roundName("Final")
            .status(TournamentRoundStatus.COMPLETE)
            .build();
    finalRound.setMatches(
        new ArrayList<>(
            List.of(
                TournamentMatch.builder()
                    .round(finalRound)
                    .entrant1(entry(1))
                    .entrant2(entry(3))
                    .winner(entry(1))
                    .build())));
    t.getRounds().add(finalRound);
    assertThat(format.isComplete(t)).isTrue();
  }

  @Test
  void estimateTotalMatches_groupsPlusFinal() {
    // 12 entrants → 4 groups of 3 → 4 qualifiers + 1 final = 5.
    assertThat(format.estimateTotalMatches(tournamentWith(12))).isEqualTo(5);
    // 7 entrants → ceil(7/3)=3 groups (3/2/2) → 3 qualifiers + 1 final = 4.
    assertThat(format.estimateTotalMatches(tournamentWith(7))).isEqualTo(4);
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private static Tournament tournamentWith(int count) {
    Tournament t = new Tournament();
    t.setFormatId(QualifierGroupsFormat.FORMAT_ID);
    List<TournamentEntry> entries = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
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
