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
package com.github.javydreamercsw.management.domain.tournament;

/**
 * Edition cadence of a tournament (ATW-o4ad). {@code NONE} preserves the one-shot lifecycle — the
 * payoff books exactly once and the PLE template pairing is consumed after it (ATW-xbn4). {@code
 * ANNUAL} treats the tournament as one edition of a chain: when its payoff books, the booking path
 * creates the next edition (ordinal + 1, SCHEDULED) and re-points the template pairing to it, so
 * each future PLE instance from the pairing's template hosts the next cycle.
 */
public enum TournamentRecurrence {
  /** One-shot tournament (default; every pre-existing tournament). Pairing is consumed. */
  NONE,
  /** Recurring edition: next edition auto-creates at payoff booking; pairing re-points. */
  ANNUAL
}
