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

import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.campaign.TournamentDTO;
import com.github.javydreamercsw.management.service.tournament.RoundRobinFormat;
import com.github.javydreamercsw.management.service.tournament.SingleEliminationFormat;
import com.github.javydreamercsw.management.service.tournament.TournamentFormat;
import com.github.javydreamercsw.management.service.tournament.TournamentFormat.RenderMode;
import com.github.javydreamercsw.management.ui.component.TournamentBracketModel.MatchModel;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TournamentBracketAdapterTest {

  // ── TournamentDTOAdapter ──────────────────────────────────────────────────

  @Test
  void dtoAdapter_renderModeIsAlwaysTree() {
    TournamentDTOAdapter adapter = new TournamentDTOAdapter(dtoWith1Match());
    assertThat(adapter.getRenderMode()).isEqualTo(RenderMode.TREE);
  }

  @Test
  void dtoAdapter_forwardsRoundAndEntrantInfo() {
    TournamentDTO dto = dtoWith1Match();
    TournamentDTOAdapter adapter = new TournamentDTOAdapter(dto);

    assertThat(adapter.getTotalRounds()).isEqualTo(2);
    assertThat(adapter.getCurrentRound()).isEqualTo(1);

    List<MatchModel> matches = adapter.getMatches();
    assertThat(matches).hasSize(1);

    MatchModel m = matches.get(0);
    assertThat(m.getRound()).isEqualTo(1);
    assertThat(m.getWrestler1Id()).isEqualTo(10L);
    assertThat(m.getWrestler1Name()).isEqualTo("Alpha");
    assertThat(m.getWrestler2Id()).isEqualTo(20L);
    assertThat(m.getWrestler2Name()).isEqualTo("Beta");
    assertThat(m.getWinnerId()).isEqualTo(10L);
    assertThat(m.isPlayerMatch()).isTrue();
  }

  // ── TournamentEntityAdapter ───────────────────────────────────────────────

  @Test
  void entityAdapter_usesFormatRenderMode_singleElimination() {
    Tournament t = tournamentEntity("SINGLE_ELIMINATION");
    List<TournamentFormat> formats = List.of(new SingleEliminationFormat(), new RoundRobinFormat());

    TournamentEntityAdapter adapter = new TournamentEntityAdapter(t, formats);
    assertThat(adapter.getRenderMode()).isEqualTo(RenderMode.TREE);
  }

  @Test
  void entityAdapter_usesFormatRenderMode_roundRobin() {
    Tournament t = tournamentEntity("ROUND_ROBIN");
    List<TournamentFormat> formats = List.of(new SingleEliminationFormat(), new RoundRobinFormat());

    TournamentEntityAdapter adapter = new TournamentEntityAdapter(t, formats);
    assertThat(adapter.getRenderMode()).isEqualTo(RenderMode.ROUND_ROBIN_GRID);
  }

  @Test
  void entityAdapter_defaultsToTreeForUnknownFormat() {
    Tournament t = tournamentEntity("UNKNOWN_FORMAT");
    TournamentEntityAdapter adapter = new TournamentEntityAdapter(t, List.of());
    assertThat(adapter.getRenderMode()).isEqualTo(RenderMode.TREE);
  }

  @Test
  void entityAdapter_mapsMatchesCorrectly() {
    Tournament t = tournamentEntity("SINGLE_ELIMINATION");

    Wrestler w1 = wrestler(1L, "Rocky");
    Wrestler w2 = wrestler(2L, "Austin");
    TournamentEntry e1 = entry(w1);
    TournamentEntry e2 = entry(w2);

    TournamentMatch match = TournamentMatch.builder().entrant1(e1).entrant2(e2).winner(e1).build();

    TournamentRound round =
        TournamentRound.builder()
            .roundNumber(1)
            .roundName("Final")
            .status(TournamentRoundStatus.COMPLETE)
            .matches(new ArrayList<>(List.of(match)))
            .build();

    t.setRounds(new ArrayList<>(List.of(round)));
    t.setEntries(List.of(e1, e2));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new SingleEliminationFormat()));

    assertThat(adapter.getTotalRounds()).isEqualTo(1);

    List<MatchModel> matches = adapter.getMatches();
    assertThat(matches).hasSize(1);

    MatchModel m = matches.get(0);
    assertThat(m.getRound()).isEqualTo(1);
    assertThat(m.getWrestler1Id()).isEqualTo(1L);
    assertThat(m.getWrestler1Name()).isEqualTo("Rocky");
    assertThat(m.getWrestler2Id()).isEqualTo(2L);
    assertThat(m.getWrestler2Name()).isEqualTo("Austin");
    assertThat(m.getWinnerId()).isEqualTo(1L);
    assertThat(m.isPlayerMatch()).isFalse();
  }

  @Test
  void entityAdapter_getCurrentRound_inProgressRound() {
    Tournament t = tournamentEntity("SINGLE_ELIMINATION");

    TournamentRound r1 = roundWithStatus(1, TournamentRoundStatus.COMPLETE);
    TournamentRound r2 = roundWithStatus(2, TournamentRoundStatus.IN_PROGRESS);
    t.setRounds(new ArrayList<>(List.of(r1, r2)));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new SingleEliminationFormat()));
    assertThat(adapter.getCurrentRound()).isEqualTo(2);
  }

  @Test
  void entityAdapter_getCurrentRound_fallsBackToLastComplete() {
    Tournament t = tournamentEntity("SINGLE_ELIMINATION");

    TournamentRound r1 = roundWithStatus(1, TournamentRoundStatus.COMPLETE);
    TournamentRound r2 = roundWithStatus(2, TournamentRoundStatus.COMPLETE);
    t.setRounds(new ArrayList<>(List.of(r1, r2)));

    TournamentEntityAdapter adapter =
        new TournamentEntityAdapter(t, List.of(new SingleEliminationFormat()));
    assertThat(adapter.getCurrentRound()).isEqualTo(2);
  }

  // ── TournamentBracketComponent round-robin rendering ─────────────────────

  @Test
  void component_roundRobinGridRendersWithoutError() {
    TournamentDTO dto = new TournamentDTO();
    dto.setTotalRounds(2);
    dto.setCurrentRound(1);

    TournamentDTO.TournamentMatch m1 = new TournamentDTO.TournamentMatch();
    m1.setRound(1);
    m1.setWrestler1Id(1L);
    m1.setWrestler1Name("A");
    m1.setWrestler2Id(2L);
    m1.setWrestler2Name("B");

    dto.setMatches(List.of(m1));

    // Use a model that returns ROUND_ROBIN_GRID
    TournamentBracketModel model =
        new TournamentBracketModel() {
          @Override
          public int getTotalRounds() {
            return 1;
          }

          @Override
          public int getCurrentRound() {
            return 1;
          }

          @Override
          public RenderMode getRenderMode() {
            return RenderMode.ROUND_ROBIN_GRID;
          }

          @Override
          public List<MatchModel> getMatches() {
            return new TournamentDTOAdapter(dto).getMatches();
          }
        };

    TournamentBracketComponent component = new TournamentBracketComponent(model);
    assertThat(component.getChildren()).isNotEmpty();
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private static TournamentDTO dtoWith1Match() {
    TournamentDTO dto = new TournamentDTO();
    dto.setTotalRounds(2);
    dto.setCurrentRound(1);

    TournamentDTO.TournamentMatch m = new TournamentDTO.TournamentMatch();
    m.setRound(1);
    m.setWrestler1Id(10L);
    m.setWrestler1Name("Alpha");
    m.setWrestler2Id(20L);
    m.setWrestler2Name("Beta");
    m.setWinnerId(10L);
    m.setPlayerMatch(true);
    dto.setMatches(List.of(m));
    return dto;
  }

  private static Tournament tournamentEntity(String formatId) {
    Tournament t = new Tournament();
    t.setFormatId(formatId);
    t.setRounds(new ArrayList<>());
    t.setEntries(new ArrayList<>());
    return t;
  }

  private static Wrestler wrestler(Long id, String name) {
    Wrestler w = new Wrestler();
    w.setId(id);
    w.setName(name);
    return w;
  }

  private static TournamentEntry entry(Wrestler w) {
    return TournamentEntry.builder().wrestler(w).seed(1).build();
  }

  private static TournamentRound roundWithStatus(int number, TournamentRoundStatus status) {
    TournamentRound r = new TournamentRound();
    r.setRoundNumber(number);
    r.setStatus(status);
    r.setMatches(new ArrayList<>());
    return r;
  }
}
