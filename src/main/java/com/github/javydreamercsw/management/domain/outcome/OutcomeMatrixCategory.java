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
package com.github.javydreamercsw.management.domain.outcome;

/** Categories of outcome charts, indicating when during gameplay they apply. */
public enum OutcomeMatrixCategory {
  MATCH_FLOW("Match Flow", "Controls the ebb and flow of match momentum"),
  FINISHER("Finisher / Momentum", "Determines outcomes when a wrestler goes for a finishing move"),
  POST_MATCH("Post-Match Scenario", "Events that occur after a match concludes"),
  FEUD_ANGLE("Feud / Angle", "Drives ongoing rivalries and generates dramatic storyline twists"),
  HIGHLIGHT_REEL("Highlight Reel", "TV segment narrative outcomes affecting Grudge and TV Grades"),
  PROMO("Promo", "Outcomes from promo segments affecting crowd reaction and grades");

  private final String displayName;
  private final String description;

  OutcomeMatrixCategory(final String displayName, final String description) {
    this.displayName = displayName;
    this.description = description;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
