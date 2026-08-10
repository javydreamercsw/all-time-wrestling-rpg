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

/**
 * Bridges the domain {@link Tournament} entity (already graph-initialized via {@code
 * TournamentService.findByIdWithDetails}) to {@link TournamentBracketModel}.
 */
public class TournamentEntityAdapter implements TournamentBracketModel {

  private final Tournament tournament;
  private final RenderMode renderMode;

  public TournamentEntityAdapter(Tournament tournament, List<TournamentFormat> formats) {
    this.tournament = tournament;
    this.renderMode =
        formats.stream()
            .filter(f -> f.getFormatId().equals(tournament.getFormatId()))
            .findFirst()
            .map(TournamentFormat::renderMode)
            .orElse(RenderMode.TREE);
  }

  @Override
  public int getTotalRounds() {
    return tournament.getRounds().size();
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
  public RenderMode getRenderMode() {
    return renderMode;
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
    public boolean isPlayerMatch() {
      return false;
    }
  }
}
