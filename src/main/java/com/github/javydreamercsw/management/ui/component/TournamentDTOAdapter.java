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

import com.github.javydreamercsw.management.dto.campaign.TournamentDTO;
import com.github.javydreamercsw.management.service.tournament.TournamentFormat.RenderMode;
import java.util.List;

/** Bridges the campaign {@link TournamentDTO} to {@link TournamentBracketModel}. */
public class TournamentDTOAdapter implements TournamentBracketModel {

  private final TournamentDTO dto;

  public TournamentDTOAdapter(TournamentDTO dto) {
    this.dto = dto;
  }

  @Override
  public int getTotalRounds() {
    return dto.getTotalRounds();
  }

  @Override
  public int getCurrentRound() {
    return dto.getCurrentRound();
  }

  @Override
  public RenderMode getRenderMode() {
    return RenderMode.TREE;
  }

  @Override
  public List<MatchModel> getMatches() {
    return dto.getMatches().stream().map(DTOMatchModel::new).map(m -> (MatchModel) m).toList();
  }

  private record DTOMatchModel(TournamentDTO.TournamentMatch inner) implements MatchModel {

    @Override
    public int getRound() {
      return inner.getRound();
    }

    @Override
    public Long getWrestler1Id() {
      return inner.getWrestler1Id();
    }

    @Override
    public String getWrestler1Name() {
      return inner.getWrestler1Name();
    }

    @Override
    public Long getWrestler2Id() {
      return inner.getWrestler2Id();
    }

    @Override
    public String getWrestler2Name() {
      return inner.getWrestler2Name();
    }

    @Override
    public Long getWinnerId() {
      return inner.getWinnerId();
    }

    @Override
    public boolean isPlayerMatch() {
      return inner.isPlayerMatch();
    }
  }
}
