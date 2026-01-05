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
package com.github.javydreamercsw.management.domain.npc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Npc Type. */
@RequiredArgsConstructor
@Getter
public enum NpcType {
  MANAGER("Manager"),
  ANNOUNCER("Announcer"),
  REFEREE("Referee"),
  OWNER("Owner"),
  BACKSTAGE_PERSONNEL("Backstage Personnel");

  private final String name;
}
