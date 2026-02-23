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
package com.github.javydreamercsw.management.service.interference;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InterferenceType {
  REF_DISTRACTION(
      "Referee Distraction",
      10,
      20,
      "Distract the official to allow for recovery or illegal tactics. Provides a small win"
          + " probability boost."),
  WEAPON_SLIDE(
      "Weapon Slide",
      25,
      40,
      "Introduce a foreign object into the ring. Provides a large boost to attack damage and win"
          + " probability."),
  TRIP_ANKLE_PULL(
      "Trip/Ankle Pull",
      15,
      30,
      "Interrupt an opponent's move or grounding them. Provides a moderate momentum boost."),
  CHEAP_SHOT(
      "Cheap Shot",
      30,
      50,
      "A direct illegal strike when the ref isn't looking. Provides a massive boost to win"
          + " probability but carries extreme risk.");

  private final String displayName;
  private final int baseImpact; // Momentum/Weight boost
  private final int baseRisk; // Detection increase
  private final String description;
}
