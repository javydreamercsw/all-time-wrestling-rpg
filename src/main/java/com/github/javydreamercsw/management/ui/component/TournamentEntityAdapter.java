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
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundStatus;
import com.github.javydreamercsw.management.service.tournament.TournamentFormat;
import com.github.javydreamercsw.management.service.tournament.TournamentFormat.RenderMode;
import java.util.List;
import java.util.Optional;
import lombok.Getter;

/**
 * Bridges the domain {@link Tournament} entity (already graph-initialized via {@code
 * TournamentService.findByIdWithDetails}) to {@link TournamentBracketModel}.
 */
public class TournamentEntityAdapter implements TournamentBracketModel {

  private final Tournament tournament;
  @Getter private final RenderMode renderMode;
  private final TournamentFormat format;

  public TournamentEntityAdapter(Tournament tournament, List<TournamentFormat> formats) {
    this.tournament = tournament;
    this.format =
        formats.stream()
            .filter(f -> f.getFormatId().equals(tournament.getFormatId()))
            .findFirst()
            .orElse(null);
    this.renderMode = format != null ? format.renderMode() : RenderMode.TREE;
  }

  @Override
  public int getTotalRounds() {
    return tournament.getRounds().size();
  }

  @Override
  public boolean isComplete() {
    // The format knows whether the bracket played through its final — lazy round generation
    // means "a decided match exists" is not the same as "the tournament is finished".
    return format != null && format.isComplete(tournament);
  }

  @Override
  public String getRoundName(int round) {
    return tournament.getRounds().stream()
        .filter(r -> r.getRoundNumber() == round)
        .findFirst()
        .map(r -> r.getRoundName())
        .orElse(null);
  }

  @Override
  public int getCurrentRound() {
    Optional<Integer> inProgress =
        tournament.getRounds().stream()
            .filter(r -> r.getStatus() == TournamentRoundStatus.IN_PROGRESS)
            .map(r -> r.getRoundNumber())
            .findFirst();
    return inProgress.orElseGet(
        () ->
            tournament.getRounds().stream()
                .filter(r -> r.getStatus() == TournamentRoundStatus.COMPLETE)
                .mapToInt(r -> r.getRoundNumber())
                .max()
                .orElse(1));
  }

  @Override
  public List<MatchModel> getMatches() {
    return tournament.getRounds().stream()
        .flatMap(
            round ->
                round.getMatches().stream()
                    .map(m -> new EntityMatchModel(round.getRoundNumber(), m)))
        .map(m -> (MatchModel) m)
        .toList();
  }

  private record EntityMatchModel(int roundNumber, TournamentMatch match) implements MatchModel {

    @Override
    public int getRound() {
      return roundNumber;
    }

    @Override
    public Long getWrestler1Id() {
      return match.getEntrant1().getWrestler().getId();
    }

    @Override
    public String getWrestler1Name() {
      return match.getEntrant1().getWrestler().getName();
    }

    @Override
    public Long getWrestler2Id() {
      return match.getEntrant2().getWrestler().getId();
    }

    @Override
    public String getWrestler2Name() {
      return match.getEntrant2().getWrestler().getName();
    }

    @Override
    public Long getWinnerId() {
      return match.getWinner() != null ? match.getWinner().getWrestler().getId() : null;
    }

    @Override
    public List<String> getExtraEntrantNames() {
      if (!match.isMultiEntrant()) {
        return List.of();
      }
      // Slots 0 and 1 render through the classic two lines; slot 2+ land here.
      return match.entrants().stream().skip(2).map(e -> e.getWrestler().getName()).toList();
    }

    @Override
    public boolean isPlayerMatch() {
      return false;
    }
  }
}
