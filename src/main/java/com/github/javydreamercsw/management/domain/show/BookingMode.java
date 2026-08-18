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
package com.github.javydreamercsw.management.domain.show;

/**
 * Determines the segment-generation strategy used when auto-booking a show. The show type (PLE,
 * Weekly, …) describes what the event is; the booking mode describes how its card is built.
 */
public enum BookingMode {

  /** Standard rivalry- and availability-driven booking (default). */
  STANDARD,

  /** All segments are generated from active faction-vs-faction rivalries and feuds. */
  FACTION_WAR,
}
