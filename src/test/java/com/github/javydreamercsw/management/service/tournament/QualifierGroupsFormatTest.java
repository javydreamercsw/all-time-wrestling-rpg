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

import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatchParticipant;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatchRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

  @Test
  void groupSize_pinned_splitHonorsTournamentSetting() {
    // 18 entrants pinned at 6 per group → 3 groups of 6 → 3 qualifiers + 1 final = 4 matches.
    Tournament t = tournamentWith(18);
    t.setQualifierGroupSize(6);
    when(roundRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(matchRepo.save(any()))
        .thenAnswer(
            inv -> {
              TournamentMatch m = inv.getArgument(0);
              if (m.getParticipants() != null) {
                m.getParticipants().forEach(p -> p.setMatch(m));
              }
              return m;
            });

    List<TournamentRound> rounds = format.generateBracket(t, ctx);

    assertThat(rounds).hasSize(1);
    List<TournamentMatch> qualifiers = rounds.get(0).getMatches();
    assertThat(qualifiers).hasSize(3);
    assertThat(qualifiers).allSatisfy(m -> assertThat(m.entrants()).hasSize(6));
    assertThat(format.estimateTotalMatches(t)).isEqualTo(4);
  }

  @Test
  void groupSize_tooLargeForTwoGroups_fallsBackToAutoSize() {
    // A setting that cannot form two groups (12 entrants, size 10 → only 1 group of 12/10)
    // falls back to the balanced auto split rather than producing a single mega-group.
    Tournament t = tournamentWith(12);
    t.setQualifierGroupSize(10);
    assertThat(format.estimateTotalMatches(t)).isEqualTo(5); // ceil(12/3)=4 groups + final
  }

  @Test
  void groupSize_outOfRange_clampedToSupportedBounds() {
    // 18 entrants, configured 99 → clamped to 10 → 18/10 = 1 group is rejected (needs 2), so
    // the size clamp keeps the split sane: 2 groups of 9 (18/10 rounds down; guard requires
    // entrants ≥ 2×clamped size, so 18 ≥ 20 is false → auto split). The lower clamp: 18 at 1
    // → clamped to 2 → 9 groups of 2.
    Tournament low = tournamentWith(18);
    low.setQualifierGroupSize(1);
    assertThat(format.estimateTotalMatches(low)).isEqualTo(10); // 9 groups of 2 + final

    Tournament high = tournamentWith(18);
    high.setQualifierGroupSize(QualifierGroupsFormat.MAX_GROUP_SIZE + 50);
    // Clamped to 10; 18 < 2×10 → auto split (ceil(18/3)=6 groups).
    assertThat(format.estimateTotalMatches(high)).isEqualTo(7);
  }

  @Test
  void roundSegmentType_isFreeForAll() {
    assertThat(format.getRoundSegmentTypeCode())
        .isEqualTo(WellKnownSegmentType.FREE_FOR_ALL.getCode());
  }

  @Test
  void defaultRoundRule_isNoDQ() {
    // Qualifier scrambles are No-DQ by convention — "No DQ" matches the rule catalog's seeded
    // no-DQ rule (no_dq=1) and reads cleanly next to the Free-for-All segment type on cards.
    assertThat(format.getDefaultRoundRuleName()).isEqualTo("No DQ");
  }

  // ── Bracket projection (full-render request): 18 entrants → 6 qualifiers + 6-slot final ──

  @Test
  void projectBracket_18Entrants_groupSize6_threeQualifiersPlus6SlotFinal() {
    Tournament t = tournamentWith(18);
    t.setQualifierGroupSize(6);

    Optional<TournamentFormat.BracketProjection> projection = format.projectBracket(t);

    assertThat(projection).isPresent();
    TournamentFormat.BracketProjection bracket = projection.orElseThrow();
    assertThat(bracket.roundNames()).containsExactly("Qualifiers", "Final");
    // 18 entrants at 6 per group → 3 qualifiers + 1 final = 4 matches.
    assertThat(bracket.matches()).hasSize(4);

    // Round 1 slots carry the real seeded wrestlers, 6 per qualifier, sequential match numbers.
    List<TournamentFormat.ProjectedMatch> qualifiers = matchesInRound(bracket, 1);
    assertThat(qualifiers).hasSize(3);
    qualifiers.forEach(
        q ->
            assertThat(q.slots())
                .extracting(TournamentFormat.ProjectedSlot::entrantName)
                .hasSize(6)
                .doesNotContainNull());
    assertThat(qualifiers.get(0).slots().get(0).entrantName()).isEqualTo("Wrestler 1");
    assertThat(bracket.matches())
        .extracting(TournamentFormat.ProjectedMatch::matchNumber)
        .containsExactly(1, 2, 3, 4);

    // The final (match 4) has one "Winner of Match N" placeholder slot per qualifier — a 6-entrant
    // final shape rendered before the final exists.
    TournamentFormat.ProjectedMatch finalMatch = matchesInRound(bracket, 2).get(0);
    assertThat(finalMatch.matchNumber()).isEqualTo(4);
    assertThat(finalMatch.slots())
        .extracting(TournamentFormat.ProjectedSlot::sourceMatchNumber)
        .containsExactly(1, 2, 3);
    assertThat(finalMatch.slots())
        .extracting(TournamentFormat.ProjectedSlot::entrantName)
        .containsOnlyNulls();
  }

  @Test
  void projectBracket_persistedWinnerOverlaysQualifierSlots() {
    // A decided qualifier keeps its real winner (green) — the projection carries decidedWinner
    // so the UI strikes everyone who didn't win the Free-for-All. The winner also propagates
    // into the final: the advancing name replaces "Winner of Match N".
    Tournament t = tournamentWith(3);
    TournamentRound qualifiers =
        TournamentRound.builder()
            .tournament(t)
            .roundNumber(1)
            .roundName("Qualifiers")
            .status(TournamentRoundStatus.IN_PROGRESS)
            .build();
    List<TournamentEntry> seeds = t.getEntries();
    TournamentMatch played =
        TournamentMatch.builder()
            .round(qualifiers)
            .entrant1(seeds.get(0))
            .entrant2(seeds.get(1))
            .winner(seeds.get(0))
            .build();
    played.setParticipants(
        new ArrayList<>(
            List.of(
                participant(played, seeds.get(0), 0),
                participant(played, seeds.get(1), 1),
                participant(played, seeds.get(2), 2))));
    qualifiers.setMatches(new ArrayList<>(List.of(played)));
    t.setRounds(new ArrayList<>(List.of(qualifiers)));

    TournamentFormat.ProjectedMatch first =
        matchesInRound(format.projectBracket(t).orElseThrow(), 1).get(0);

    assertThat(first.decidedWinnerId()).isEqualTo(seeds.get(0).getWrestler().getId());
    assertThat(first.decidedWinnerName()).isEqualTo("Wrestler 1");
    assertThat(first.slots()).hasSize(3);

    // The decided winner propagates into the projected final: the slot shows "Wrestler 1"
    // (advancing) instead of "Winner of Match 1" — there is only one group in this fixture.
    TournamentFormat.ProjectedMatch finalMatch =
        matchesInRound(format.projectBracket(t).orElseThrow(), 2).get(0);
    assertThat(finalMatch.slots()).hasSize(1);
    assertThat(finalMatch.slots().get(0).entrantName()).isEqualTo("Wrestler 1");
    assertThat(finalMatch.slots().get(0).advancing()).isTrue();
  }

  @Test
  void projectBracket_finalWinnerOverlaysFinalSlots() {
    // A decided final's winner overlays the projected final match.
    Tournament t = tournamentWith(4);
    List<TournamentEntry> seeds = t.getEntries();
    TournamentRound finalRound =
        TournamentRound.builder()
            .tournament(t)
            .roundNumber(2)
            .roundName("Final")
            .status(TournamentRoundStatus.COMPLETE)
            .build();
    TournamentMatch finalMatch =
        TournamentMatch.builder()
            .round(finalRound)
            .entrant1(seeds.get(0))
            .entrant2(seeds.get(1))
            .winner(seeds.get(0))
            .build();
    finalRound.setMatches(new ArrayList<>(List.of(finalMatch)));
    t.setRounds(new ArrayList<>(List.of(finalRound)));

    TournamentFormat.ProjectedMatch projectedFinal =
        matchesInRound(format.projectBracket(t).orElseThrow(), 2).get(0);

    assertThat(projectedFinal.decidedWinnerId()).isEqualTo(seeds.get(0).getWrestler().getId());
  }

  @Test
  void projectBracket_noEntries_returnsEmpty() {
    Tournament t = new Tournament();
    t.setFormatId(QualifierGroupsFormat.FORMAT_ID);
    t.setEntries(new ArrayList<>());
    t.setRounds(new ArrayList<>());
    assertThat(format.projectBracket(t)).isEmpty();
  }

  private static List<TournamentFormat.ProjectedMatch> matchesInRound(
      TournamentFormat.BracketProjection bracket, int round) {
    return bracket.matches().stream().filter(m -> m.roundNumber() == round).toList();
  }

  private static TournamentMatchParticipant participant(
      TournamentMatch match, TournamentEntry entry, int slot) {
    return TournamentMatchParticipant.builder().match(match).entry(entry).slot(slot).build();
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private static Tournament tournamentWith(int count) {
    Tournament t = new Tournament();
    t.setFormatId(QualifierGroupsFormat.FORMAT_ID);
    List<TournamentEntry> entries = new ArrayList<>();
    for (int i = 1; i < count + 1; i++) {
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
