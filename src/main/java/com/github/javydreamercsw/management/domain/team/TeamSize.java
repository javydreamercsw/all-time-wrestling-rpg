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
package com.github.javydreamercsw.management.domain.team;

/**
 * The declared size of a team. Drives which match types a team can enter (a DUO teams up for tag
 * matches, a TRIO for trios matches) and pairs with {@code Tournament.teamSize} / {@code
 * Title.teamSize} to gate entrants and payoffs. Extensible with more sizes as the game grows.
 */
public enum TeamSize {
  /** Two-wrestler team — the classic tag team. */
  DUO(2),
  /** Three-wrestler team. */
  TRIO(3);

  private final int memberCount;

  TeamSize(final int memberCount) {
    this.memberCount = memberCount;
  }

  /** How many wrestlers belong to a team of this size. */
  public int getMemberCount() {
    return memberCount;
  }
}
