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
package com.github.javydreamercsw.base.domain.account;

import lombok.Getter;

@Getter
public enum AchievementCategory {
  COLLECTION("Collection"),
  FANS("Fans"),
  CHAMPIONSHIP("Championship"),
  MATCH_TYPE("Match Type"),
  BOOKING("Booking"),
  SPECIAL_EVENT("Special Event");

  private final String displayName;

  AchievementCategory(String displayName) {
    this.displayName = displayName;
  }
}
