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
package com.github.javydreamercsw.management.domain.title;

/** Lifecycle of a held title opportunity (Money in the Bank-style briefcase, ATW-8p72). */
public enum TitleOpportunityStatus {
  /** Won and unspent — the holder may cash it in for a title match at any time. */
  HELD,
  /** Cash-in booked (win or lose, the case is spent — the WWE rule). */
  CASHED_IN,
  /** The expiry window passed without a cash-in. */
  EXPIRED,
  /** Administratively cancelled (never cashable). */
  VOIDED
}
