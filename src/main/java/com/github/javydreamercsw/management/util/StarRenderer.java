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
package com.github.javydreamercsw.management.util;

/** Converts a 1.0–5.0 show quality score into a filled/half/empty star string. */
public final class StarRenderer {

  private StarRenderer() {}

  /**
   * Renders a score in the range [1.0, 5.0] as a five-position star string using ★, ½, and ☆.
   *
   * <p>Examples: 3.0 → "★★★☆☆", 3.5 → "★★★½☆", 5.0 → "★★★★★"
   */
  public static String renderStars(final double score) {
    int full = (int) score;
    boolean half = (score - full) >= 0.5;
    int empty = 5 - full - (half ? 1 : 0);
    return "★".repeat(full) + (half ? "½" : "") + "☆".repeat(empty);
  }
}
