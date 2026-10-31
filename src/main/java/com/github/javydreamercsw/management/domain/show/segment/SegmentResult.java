/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.domain.show.segment;

/**
 * Declared result of a segment. The default {@link #WINNERS} means the segment is decided by its
 * winner flags. {@link #DRAW} and {@link #NO_CONTEST} are explicit booked results with no winner —
 * they make "nobody won" an intentional call rather than an omission.
 */
public enum SegmentResult {
  WINNERS,
  DRAW,
  NO_CONTEST
}
