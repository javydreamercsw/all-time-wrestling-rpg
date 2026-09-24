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

  /**
   * Whether the tournament has played through its final. Governs the champion box: a decided match
   * in the last *rendered* round is not a champion when later rounds generate lazily. Defaults
   * false — a model that cannot answer completeness never shows a champion.
   */
  default boolean isComplete() {
    return false;
  }

  /**
   * The persisted display name for a round ("Qualifiers", "Final"), or null when the model does not
   * carry real round names and the component should label positionally (Finals, Semi-Finals…).
   */
  default String getRoundName(int round) {
    return null;
  }

  /** Which visual layout the bracket component should use. */
  RenderMode getRenderMode();

  List<MatchModel> getMatches();

  interface MatchModel {
    int getRound();

    Long getWrestler1Id();

    String getWrestler1Name();

    Long getWrestler2Id();

    String getWrestler2Name();

    /**
     * Full entrant list for multi-entrant matches (Free-for-All qualifiers, multi-man finals —
     * ATW-oloa): {@code name@id} beyond the first two slots renders as extra lines. Empty for the
     * classic two-entrant shape.
     */
    default List<String> getExtraEntrantNames() {
      return List.of();
    }

    Long getWinnerId();

    /** True when this match involves the player's wrestler (campaign context only). */
    boolean isPlayerMatch();
  }
}
