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
package com.github.javydreamercsw.management.domain.title;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** How often a title must be defended. */
@Getter
@RequiredArgsConstructor
public enum DefenseFrequencyType {
  WEEKLY("Weekly", 7),
  BI_WEEKLY("Bi-Weekly", 14),
  PLE("PLE (Monthly)", 28);

  private final String displayName;
  private final int days;

  /**
   * Returns true when the title is considered overdue for a defense.
   *
   * @param daysSinceLastDefense days elapsed since the last defense
   */
  public boolean isOverdue(final long daysSinceLastDefense) {
    return daysSinceLastDefense >= days;
  }
}
