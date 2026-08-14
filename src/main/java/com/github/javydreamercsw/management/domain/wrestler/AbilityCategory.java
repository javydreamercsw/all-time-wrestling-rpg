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
package com.github.javydreamercsw.management.domain.wrestler;

public enum AbilityCategory {
  /** Wrestler-specific abilities with cube/token tracking (e.g. Reversal, signature finishers). */
  SIGNATURE,
  /** Always-on rules or event-triggered resource generators (e.g. Scheme, Amigo). */
  PASSIVE,
  /** Standard wrestler actions (Recover, Taunt) that vary in effect per wrestler. */
  ACTION
}
