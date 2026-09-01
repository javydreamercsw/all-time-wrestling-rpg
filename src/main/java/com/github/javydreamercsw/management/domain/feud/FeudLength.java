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
import lombok.RequiredArgsConstructor;

/** Named length buckets for a feud story arc, mapped to PLE appearance counts. */
@Getter
@RequiredArgsConstructor
public enum FeudLength {
  SHORT("Short", 1),
  MEDIUM("Medium", 2),
  LONG("Long", 3);

  private final String label;
  private final int pleCount;

  public static FeudLength fromPleCount(int count) {
    return switch (count) {
      case 1 -> SHORT;
      case 2 -> MEDIUM;
      default -> LONG;
    };
  }

  @Override
  public String toString() {
    return label + " (" + pleCount + " PLE" + (pleCount > 1 ? "s" : "") + ")";
  }
}
