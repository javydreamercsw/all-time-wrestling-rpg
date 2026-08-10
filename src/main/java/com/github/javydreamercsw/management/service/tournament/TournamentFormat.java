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
package com.github.javydreamercsw.management.service.tournament;

import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import java.util.List;

/**
 * Strategy interface for tournament bracket formats. Implement and annotate with {@code @Component}
 * to auto-register the format — no other wiring required.
 */
public interface TournamentFormat {

  /** Unique identifier stored in the database, e.g. "SINGLE_ELIMINATION". */
  String getFormatId();

  /** Display name shown in the tournament creation wizard. */
  String getDisplayName();

  /** Minimum number of entrants this format supports. */
  int getMinEntrants();

  /**
   * Maximum number of entrants this format supports, or {@code Integer.MAX_VALUE} for unlimited.
   */
  int getMaxEntrants();

  /**
   * Generate all rounds and match stubs for the given tournament. The entrants in {@code
   * tournament.getEntries()} are already seeded and ordered. Implementations must persist rounds
   * and matches via the provided repositories.
   */
  List<TournamentRound> generateBracket(Tournament tournament, TournamentFormatContext ctx);

  /**
   * Given the completed rounds so far, generate the next round's matches. Returns an empty list
   * when the tournament is complete.
   */
  List<TournamentMatch> advanceRound(Tournament tournament, TournamentFormatContext ctx);

  /** Returns true when all matches have a winner and the tournament champion is determined. */
  boolean isComplete(Tournament tournament);
}
