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
package com.github.javydreamercsw.management.domain.feud;

import lombok.Getter;

/**
 * Role of an external (non-feud) wrestler on a story arc beat. Team semantics: feud wrestlers are
 * always team 1; externals land on the opposing side (team 2). Mirrors the {@link FeudRole} display
 * style but carries team placement instead of allegiance.
 */
public enum FeudBeatParticipantRole {
  /** Faces the feud's wrestlers — the surprise opponent. */
  OPPONENT("Opponent", "Faces the feud's wrestlers on the opposing side"),

  /** Joins the opposing side alongside the opponent (run-in, second). */
  EXTRA("Extra", "Joins the opposing side alongside the opponent (run-in / second)");

  @Getter private final String displayName;
  @Getter private final String description;

  FeudBeatParticipantRole(final String displayName, final String description) {
    this.displayName = displayName;
    this.description = description;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
