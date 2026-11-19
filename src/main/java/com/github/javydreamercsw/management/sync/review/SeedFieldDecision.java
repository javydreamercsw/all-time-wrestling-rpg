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
package com.github.javydreamercsw.management.sync.review;

/**
 * The four outcomes an administrator can choose for a seed-data collision detected by the
 * seed-update review feature.
 */
public enum SeedFieldDecision {
  /** Keep the database's current value; the incoming seed value is discarded. */
  KEEP_MINE,
  /** Adopt the incoming seed value; the database's current value is overwritten. */
  USE_NEW,
  /** Keep the database's current value and mark it as custom (no longer seed-owned). */
  KEEP_AS_CUSTOM,
  /** Remove the database row; the seed no longer carries it. */
  REMOVE
}
