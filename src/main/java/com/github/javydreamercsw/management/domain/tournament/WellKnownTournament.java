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
package com.github.javydreamercsw.management.domain.tournament;

import java.util.Optional;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * Stable machine-readable identifiers for tournaments defined in tournaments.json (ATW-vg16).
 *
 * <p>The companion test {@code WellKnownTournamentTest} enforces that every constant here has a
 * matching {@code code} entry in {@code tournaments.json} and vice versa, so the two never drift
 * apart.
 */
public enum WellKnownTournament {
  /** The high-intensity tournament from OMZ's backstory — rules pool per GAME_MECHANICS.md. */
  DEADLY_COMBAT("deadly_combat");

  @Getter private final String code;

  WellKnownTournament(final String code) {
    this.code = code;
  }

  /** Null-safe match against a {@link Tournament} entity's code field. */
  public boolean matches(@Nullable Tournament tournament) {
    return tournament != null && code.equals(tournament.getCode());
  }

  /** Reverse lookup by code string; empty when the code is not a well-known tournament. */
  public static Optional<WellKnownTournament> fromCode(@Nullable String code) {
    if (code == null) {
      return Optional.empty();
    }
    for (WellKnownTournament v : values()) {
      if (v.code.equals(code)) {
        return Optional.of(v);
      }
    }
    return Optional.empty();
  }
}
