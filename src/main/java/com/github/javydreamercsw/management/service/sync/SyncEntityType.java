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
package com.github.javydreamercsw.management.service.sync;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumeration of all entity types that can be synchronized with Notion. This provides a type-safe
 * way to reference entity types throughout the sync system.
 */
@Getter
@RequiredArgsConstructor
public enum SyncEntityType {
  SHOWS("Shows", "Show"),
  WRESTLERS("Wrestlers", "Wrestler"),
  FACTIONS("Factions", "Faction"),
  TEAMS("Teams", "Team"),
  TEMPLATES("Templates", "ShowTemplate"),
  SEASONS("Seasons", "Season"),
  SHOW_TYPES("Show-types", "ShowType"),
  INJURIES("Injuries", "InjuryType"),
  NPCS("Npcs", "Npc"),
  TITLES("Titles", "Title"),
  RIVALRIES("Rivalries", "Rivalry"),
  FACTION_RIVALRIES("Faction-rivalries", "FactionRivalry"),
  SEGMENTS("Segments", "Segment"),
  TITLE_REIGN("Title-reigns", "TitleReign");

  /** The string key used for configuration and logging */
  private final String key;

  /** The entity class simple name */
  private final String entityClassName;

  /**
   * Finds a SyncEntityType by its key (case-insensitive).
   *
   * @param key The key to search for
   * @return Optional containing the matching SyncEntityType, or empty if not found
   */
  public static Optional<SyncEntityType> fromKey(String key) {
    if (key == null || key.isBlank()) {
      return Optional.empty();
    }
    return Arrays.stream(values()).filter(type -> type.key.equalsIgnoreCase(key)).findFirst();
  }

  /**
   * Finds a SyncEntityType by its entity class name (case-insensitive).
   *
   * @param entityClassName The entity class name to search for
   * @return Optional containing the matching SyncEntityType, or empty if not found
   */
  public static Optional<SyncEntityType> fromEntityClassName(String entityClassName) {
    if (entityClassName == null || entityClassName.isBlank()) {
      return Optional.empty();
    }
    return Arrays.stream(values())
        .filter(type -> type.entityClassName.equalsIgnoreCase(entityClassName))
        .findFirst();
  }

  @Override
  public String toString() {
    return key;
  }
}
