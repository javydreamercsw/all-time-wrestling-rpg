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
package com.github.javydreamercsw.management.domain.outcome;

/**
 * Non-persisted result of resolving a dice roll against an OutcomeMatrix. Contains the raw entry,
 * the rendered text (variables substituted), and any redirect target.
 */
public record OutcomeMatrixResult(
    OutcomeMatrixEntry entry, String renderedText, OutcomeMatrix redirectMatrix) {

  public boolean isRedirect() {
    return redirectMatrix != null;
  }
}
