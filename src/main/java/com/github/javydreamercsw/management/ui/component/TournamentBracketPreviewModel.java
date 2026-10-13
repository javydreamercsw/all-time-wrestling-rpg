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

import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.service.tournament.TournamentFormat.RenderMode;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/**
 * In-memory {@link TournamentBracketModel} for a tournament whose bracket has not been generated
 * yet (SCHEDULED) — previews what "Start Tournament" would create without persisting anything.
 *
 * <p>Mirrors the generation rules of the real formats: single elimination pairs top seed vs bottom
 * seed for round 1 ({@code SingleEliminationFormat.generateBracket}); round robin rotates a
 * circle-phantom schedule for every round ({@code RoundRobinFormat.generateBracket}). Byes are not
 * represented as matches, matching both formats.
 */
public class TournamentBracketPreviewModel implements TournamentBracketModel {

  @Getter private final RenderMode renderMode;
  private final List<PreviewMatch> matches = new ArrayList<>();
  @Getter private final int totalRounds;

  public TournamentBracketPreviewModel(Tournament tournament, RenderMode renderMode) {
    this.renderMode = renderMode;
    List<TournamentEntry> seeds = tournament.getEntries();

    if (renderMode == RenderMode.ROUND_ROBIN_GRID) {
      this.totalRounds = buildRoundRobin(seeds);
    } else {
      this.totalRounds = buildSingleElimination(seeds);
    }
  }

  private int buildSingleElimination(List<TournamentEntry> seeds) {
    if (seeds.size() < 2) {
      return 0;
    }
    int n = seeds.size();
    // Round-1 pairings mirror SingleEliminationFormat: top seed vs bottom seed.
    for (int i = 0; i < n / 2; i++) {
      matches.add(new PreviewMatch(1, seeds.get(i), seeds.get(n - 1 - i)));
    }
    // Total rounds the bracket will span once byes resolve (next power of two ≥ n).
    int bracketSize = Integer.highestOneBit(Math.max(1, n - 1)) * 2;
    return Math.max(1, log2(bracketSize));
  }

  private static int log2(int v) {
    return 31 - Integer.numberOfLeadingZeros(v);
  }

  private int buildRoundRobin(List<TournamentEntry> seeds) {
    List<TournamentEntry> entries = new ArrayList<>(seeds);
    if (entries.size() % 2 != 0) {
      entries.add(null); // bye
    }
    int n = entries.size();
    for (int r = 0; r < n - 1; r++) {
      for (int i = 0; i < n / 2; i++) {
        TournamentEntry e1 = entries.get(i);
        TournamentEntry e2 = entries.get(n - 1 - i);
        if (e1 != null && e2 != null) {
          matches.add(new PreviewMatch(r + 1, e1, e2));
        }
      }
      // Rotate: fix the first element, rotate the rest (mirrors RoundRobinFormat).
      TournamentEntry last = entries.remove(n - 1);
      entries.add(1, last);
    }
    return Math.max(1, n - 1);
  }

  @Override
  public int getCurrentRound() {
    return 1;
  }

  @Override
  public List<MatchModel> getMatches() {
    return List.copyOf(matches);
  }

  private record PreviewMatch(@Getter int round, TournamentEntry e1, TournamentEntry e2)
      implements MatchModel {

    @Override
    public Long getWrestler1Id() {
      return e1.getWrestler().getId();
    }

    @Override
    public String getWrestler1Name() {
      return e1.getWrestler().getName();
    }

    @Override
    public Long getWrestler2Id() {
      return e2.getWrestler().getId();
    }

    @Override
    public String getWrestler2Name() {
      return e2.getWrestler().getName();
    }

    @Override
    public Long getWinnerId() {
      return null;
    }

    @Override
    public boolean isPlayerMatch() {
      return false;
    }
  }
}
