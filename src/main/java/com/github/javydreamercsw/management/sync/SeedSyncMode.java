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
package com.github.javydreamercsw.management.sync;

/** Seed sync execution mode derived from {@code data.initializer.skip-if-not-empty}. */
public enum SeedSyncMode {
  /** Full sync: update existing rows to match the seed. */
  FULL,
  /** Protect mode: top up missing entries, never re-save existing rows. */
  PROTECT,
  /** Verify mode: top up missing entries, reconcile seed-owned fields of fingerprint-clean rows. */
  VERIFY
}
