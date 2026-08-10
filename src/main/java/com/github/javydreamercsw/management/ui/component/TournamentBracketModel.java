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

import com.github.javydreamercsw.management.service.tournament.TournamentFormat.RenderMode;
import java.util.List;

/**
 * View-layer abstraction over tournament data. Implemented by adapters for the campaign {@code
 * TournamentDTO} and the booker domain {@code Tournament} entity.
 */
public interface TournamentBracketModel {

  int getTotalRounds();

  int getCurrentRound();

  /** Which visual layout the bracket component should use. */
  RenderMode getRenderMode();

  List<MatchModel> getMatches();

  interface MatchModel {
    int getRound();

    Long getWrestler1Id();

    String getWrestler1Name();

    Long getWrestler2Id();

    String getWrestler2Name();

    Long getWinnerId();

    /** True when this match involves the player's wrestler (campaign context only). */
    boolean isPlayerMatch();
  }
}
