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
package com.github.javydreamercsw.base.image;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Categories of images used in the system, each with a default fallback filename. */
@Getter
@RequiredArgsConstructor
public enum ImageCategory {
  WRESTLER("generic-wrestler.png", "wrestlers", false),
  NPC("generic-npc.png", "npcs", false),
  SHOW("generic-show.png", "shows", true),
  VENUE("generic-venue.png", "venues", true),
  LOCATION("generic-location.png", "locations", true),
  TITLE("generic-title.png", "championships", true),
  TEAM("generic-team.png", "teams", true),
  FACTION("generic-faction.png", "factions", true);

  private final String defaultFilename;
  private final String directoryName;
  private final boolean useKebabCase;

  /**
   * Formats the entity name according to the category's naming strategy.
   *
   * @param name The entity name.
   * @return The formatted name.
   */
  public String formatName(String name) {
    if (name == null) {
      return "";
    }
    return useKebabCase ? name.toLowerCase().replaceAll("\\s+", "-") : name;
  }
}
