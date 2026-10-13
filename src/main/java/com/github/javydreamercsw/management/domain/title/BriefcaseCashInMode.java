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

/**
 * How an NPC briefcase holder cashes in (ATW-2b9u).
 *
 * <ul>
 *   <li>{@link #PLANNED} — a scheduled title match announced on the card.
 *   <li>{@link #AMBUSH} — the holder strikes right after the champion's match on the same show; the
 *       champion's wear carries over at resolution.
 *   <li>{@link #RUN_IN} — a mid-match run-in. Recorded on proposals but inert in this issue:
 *       ATW-qsm9 adds the half-starting-HP trigger that activates booking.
 * </ul>
 */
public enum BriefcaseCashInMode {
  PLANNED,
  AMBUSH,
  RUN_IN,
}
