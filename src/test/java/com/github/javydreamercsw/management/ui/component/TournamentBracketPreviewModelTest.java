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
package com.github.javydreamercsw.management.ui.component;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.tournament.TournamentFormat.RenderMode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for the not-yet-started bracket preview model. */
class TournamentBracketPreviewModelTest {

  @Test
  void singleElimination_pairsTopSeedVsBottomSeed() {
    Tournament tournament = tournament(8);
    TournamentBracketPreviewModel model =
        new TournamentBracketPreviewModel(tournament, RenderMode.TREE);

    assertThat(model.getTotalRounds()).isEqualTo(3);
    assertThat(model.getMatches()).hasSize(4);
    // 1 vs 8, 2 vs 7, 3 vs 6, 4 vs 5.
    assertThat(model.getMatches().get(0).getWrestler1Name()).isEqualTo("W1");
    assertThat(model.getMatches().get(0).getWrestler2Name()).isEqualTo("W8");
    assertThat(model.getMatches().get(3).getWrestler1Name()).isEqualTo("W4");
    assertThat(model.getMatches().get(3).getWrestler2Name()).isEqualTo("W5");
    assertThat(model.getMatches().get(0).getWinnerId()).isNull();
  }

  @Test
  void singleElimination_sixEntrants_spanThreeRounds() {
    TournamentBracketPreviewModel model =
        new TournamentBracketPreviewModel(tournament(6), RenderMode.TREE);

    // Next power of two ≥ 6 is 8 → 3 rounds; round 1 has 3 matches (2 byes).
    assertThat(model.getTotalRounds()).isEqualTo(3);
    assertThat(model.getMatches()).hasSize(3);
    assertThat(model.getMatches().get(0).getWrestler1Name()).isEqualTo("W1");
    assertThat(model.getMatches().get(0).getWrestler2Name()).isEqualTo("W6");
  }

  @Test
  void roundRobin_schedulesEveryPairAcrossRounds() {
    TournamentBracketPreviewModel model =
        new TournamentBracketPreviewModel(tournament(4), RenderMode.ROUND_ROBIN_GRID);

    // 4 entrants → 3 rounds × 2 matches = 6 pairings (every pair exactly once).
    assertThat(model.getTotalRounds()).isEqualTo(3);
    assertThat(model.getMatches()).hasSize(6);
    List<String> pairs =
        model.getMatches().stream()
            .map(m -> m.getWrestler1Name() + "-" + m.getWrestler2Name())
            .toList();
    assertThat(pairs).doesNotHaveDuplicates();
    // Every entrant appears exactly 3 times (once per round).
    List<String> names =
        model.getMatches().stream()
            .flatMap(m -> java.util.stream.Stream.of(m.getWrestler1Name(), m.getWrestler2Name()))
            .toList();
    assertThat(java.util.Collections.frequency(names, "W1")).isEqualTo(3);
  }

  @Test
  void fewerThanTwoEntrants_producesEmptyBracket() {
    TournamentBracketPreviewModel model =
        new TournamentBracketPreviewModel(tournament(1), RenderMode.TREE);

    assertThat(model.getMatches()).isEmpty();
    assertThat(model.getTotalRounds()).isEqualTo(0);
  }

  private static Tournament tournament(int entrantCount) {
    Tournament tournament = new Tournament();
    tournament.setId(1L);
    tournament.setName("Preview Cup");
    tournament.setEntries(new ArrayList<>());
    for (int i = 0; i < entrantCount; i++) {
      Wrestler w = new Wrestler();
      w.setId((long) (i + 1));
      w.setName("W" + (i + 1));
      w.setActive(true);
      w.setGender(Gender.MALE);
      TournamentEntry entry =
          TournamentEntry.builder().tournament(tournament).wrestler(w).seed(i + 1).build();
      tournament.getEntries().add(entry);
    }
    return tournament;
  }
}
