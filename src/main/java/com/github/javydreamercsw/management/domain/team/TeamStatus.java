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
package com.github.javydreamercsw.management.domain.team;

/** Represents the status of a team in the ATW RPG system. */
public enum TeamStatus {
  /** Team is currently active and can compete in matches */
  ACTIVE("Active"),

  /** Team has been disbanded and is no longer active */
  DISBANDED("Disbanded"),

  /** Team is temporarily inactive (e.g., due to injuries, storyline) */
  INACTIVE("Inactive");

  private final String displayName;

  TeamStatus(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
