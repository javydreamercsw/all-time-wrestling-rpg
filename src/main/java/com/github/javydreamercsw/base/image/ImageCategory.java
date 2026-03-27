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
  WRESTLER("generic-wrestler.png"),
  NPC("generic-npc.png"),
  SHOW("generic-show.png"),
  VENUE("generic-venue.png"),
  TITLE("generic-title.png"),
  TEAM("generic-team.png"),
  FACTION("generic-faction.png");

  private final String defaultFilename;
}
