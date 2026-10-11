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
package com.github.javydreamercsw.management.ui.view.tournament;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Arrange Match-ups dialog's pairings→seed-order math (ATW-hw6m). The mapping
 * must reproduce {@code SingleEliminationFormat.generateBracket}'s round-1 contract: seeds i and
 * n-1-i (0-based) form match i+1; the middle seed of an odd-sized bracket gets the bye.
 */
class ArrangeMatchupsDialogTest {

  private static Wrestler wrestler(long id, String name) {
    Wrestler w = new Wrestler();
    w.setId(id);
    w.setName(name);
    return w;
  }

  private static TournamentEntry entry(long id, int seed, Wrestler w) {
    return TournamentEntry.builder().id(id).wrestler(w).seed(seed).build();
  }

  /** 8 entrants, seeds 1..8, ids 1..8 — bySeed.get(i) has seed i+1. */
  private static List<TournamentEntry> eightBySeed() {
    return java.util.stream.IntStream.rangeClosed(1, 8)
        .mapToObj(i -> entry(i, i, wrestler(i, "W" + i)))
        .toList();
  }

  private static List<TournamentEntry> fiveBySeed() {
    return java.util.stream.IntStream.rangeClosed(1, 5)
        .mapToObj(i -> entry(i, i, wrestler(i, "W" + i)))
        .toList();
  }

  /** Default pairings for n entrants: match j = (seed j, seed n+1-j) — the current template. */
  private static List<TournamentEntry[]> defaultPairings(List<TournamentEntry> bySeed) {
    int n = bySeed.size();
    return java.util.stream.IntStream.range(0, n / 2)
        .mapToObj(i -> new TournamentEntry[] {bySeed.get(i), bySeed.get(n - 1 - i)})
        .toList();
  }

  @Test
  @DisplayName("toSeedOrder: even count, default pairings preserves the current seed order")
  void toSeedOrder_evenCount_defaultPairings_preservesCurrentSeedOrder() {
    List<TournamentEntry> bySeed = eightBySeed();

    List<Long> order = ArrangeMatchupsDialog.toSeedOrder(bySeed, defaultPairings(bySeed));

    assertThat(order).containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
  }

  @Test
  @DisplayName("toSeedOrder: rearranged pairings map onto the outer seed positions")
  void toSeedOrder_rearrangedPairings_mapOntoOuterSeedPositions() {
    // bySeed: A(1) B(2) C(3) D(4). Desired: match1 = B vs C, match2 = A vs D.
    List<TournamentEntry> bySeed =
        List.of(
            entry(1, 1, wrestler(1, "A")),
            entry(2, 2, wrestler(2, "B")),
            entry(3, 3, wrestler(3, "C")),
            entry(4, 4, wrestler(4, "D")));
    List<TournamentEntry[]> pairings =
        List.of(
            new TournamentEntry[] {bySeed.get(1), bySeed.get(2)}, // B vs C
            new TournamentEntry[] {bySeed.get(0), bySeed.get(3)}); // A vs D

    List<Long> order = ArrangeMatchupsDialog.toSeedOrder(bySeed, pairings);

    // match j → positions j and n+1-j: B→1, C→4, A→2, D→3 → [B, A, D, C] ids.
    assertThat(order).containsExactly(2L, 1L, 4L, 3L);
  }

  @Test
  @DisplayName("toSeedOrder: odd count keeps the bye at the middle seed position")
  void toSeedOrder_oddCount_keepsByeAtMiddleSeedPosition() {
    List<TournamentEntry> bySeed = fiveBySeed();

    List<Long> order = ArrangeMatchupsDialog.toSeedOrder(bySeed, defaultPairings(bySeed));

    // Positions 1,2 and 4,5 pair off; seed 3 (middle) keeps the bye.
    assertThat(order).containsExactly(1L, 2L, 3L, 4L, 5L);
  }

  @Test
  @DisplayName("toSeedOrder: the same wrestler picked twice throws")
  void toSeedOrder_duplicateWrestler_throws() {
    List<TournamentEntry> bySeed = fiveBySeed();
    // Match 1 uses W1 twice.
    List<TournamentEntry[]> pairings =
        List.of(
            new TournamentEntry[] {bySeed.get(0), bySeed.get(0)},
            new TournamentEntry[] {bySeed.get(2), bySeed.get(3)});

    assertThatThrownBy(() -> ArrangeMatchupsDialog.toSeedOrder(bySeed, pairings))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("toSeedOrder: the bye entrant picked into a match throws")
  void toSeedOrder_byeWrestlerPickedIntoAMatch_throws() {
    List<TournamentEntry> bySeed = fiveBySeed();
    // Middle seed (W3, index 2) placed into match 2 instead of sitting out.
    List<TournamentEntry[]> pairings =
        List.of(
            new TournamentEntry[] {bySeed.get(0), bySeed.get(1)},
            new TournamentEntry[] {bySeed.get(4), bySeed.get(2)});

    assertThatThrownBy(() -> ArrangeMatchupsDialog.toSeedOrder(bySeed, pairings))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("toSeedOrder: incomplete pairings throw")
  void toSeedOrder_incompletePairings_throws() {
    List<TournamentEntry> bySeed = eightBySeed();

    assertThatThrownBy(() -> ArrangeMatchupsDialog.toSeedOrder(bySeed, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
