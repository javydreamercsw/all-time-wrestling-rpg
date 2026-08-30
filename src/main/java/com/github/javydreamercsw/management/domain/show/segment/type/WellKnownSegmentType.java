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
package com.github.javydreamercsw.management.domain.show.segment.type;

import java.util.Optional;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * Stable machine-readable identifiers for segment types defined in segment_types.json.
 *
 * <p>The companion test {@code WellKnownSegmentTypeTest} enforces that every constant here has a
 * matching {@code code} entry in {@code segment_types.json} and vice versa, so the two never drift
 * apart.
 */
public enum WellKnownSegmentType {
  ONE_ON_ONE("one_on_one"),
  TAG_TEAM("tag_team"),
  PROMO("promo"),
  ABU_DHABI_RUMBLE("abu_dhabi_rumble"),
  FREE_FOR_ALL("free_for_all"),
  HANDICAP_MATCH("handicap_match");

  @Getter private final String code;

  WellKnownSegmentType(final String code) {
    this.code = code;
  }

  /** Null-safe match against a {@link SegmentType} entity's code field. */
  public boolean matches(@Nullable final SegmentType type) {
    return type != null && code.equals(type.getCode());
  }

  /** Reverse lookup by code string; empty when the code is not a well-known type. */
  public static Optional<WellKnownSegmentType> fromCode(@Nullable final String code) {
    if (code == null) {
      return Optional.empty();
    }
    for (WellKnownSegmentType v : values()) {
      if (v.code.equals(code)) {
        return Optional.of(v);
      }
    }
    return Optional.empty();
  }
}
