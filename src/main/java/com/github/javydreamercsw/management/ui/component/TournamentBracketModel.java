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

import com.github.javydreamercsw.management.service.tournament.TournamentFormat;
import com.github.javydreamercsw.management.service.tournament.TournamentFormat.RenderMode;
import java.util.List;
import java.util.Optional;

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

  /**
   * The projected COMPLETE bracket — every round including lazily-generated future ones, with
   * "Winner of Match N" placeholder slots. Empty when the model cannot project (campaign DTO path).
   */
  default Optional<TournamentFormat.BracketProjection> getProjection() {
    return Optional.empty();
  }

  /**
   * The "type · rule" context line for a round's matches ("Free-for-All · No DQ"), derived from the
   * tournament's own rule data — the round's fixed rule, else the tournament's allowed-rules pool,
   * else the format's default — or null when the model can't derive it (campaign DTO path).
   */
  default String getRoundTypeRuleLabel(int round) {
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
     * ATW-oloa): entrants beyond the first two slots render as extra lines, styled by the same
     * winner/loser rule as the classic slots. Empty for the classic two-entrant shape.
     */
    default List<ExtraEntrant> getExtraEntrants() {
      return List.of();
    }

    /** One extra-entrant line: the wrestler's display name and id (id drives loser styling). */
    record ExtraEntrant(String name, Long wrestlerId) {}

    Long getWinnerId();

    /**
     * Segment type + rule context for the match ("Free-for-All · No DQ"), or null when the model
     * doesn't carry it (campaign DTO path). Rendered as an inline line on the match card.
     */
    default String getTypeRuleLabel() {
      return null;
    }

    /** True when this match involves the player's wrestler (campaign context only). */
    boolean isPlayerMatch();
  }
}
