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
package com.github.javydreamercsw.management.service.show.planning;

import java.util.List;
import lombok.Getter;
import lombok.NonNull;

/**
 * Result of show card validation, separating hard errors from advisory warnings.
 *
 * <p>Errors (e.g. STIPULATION_REQUIRED) block approval. Warnings (e.g. MUST_BOOK rivalries not on
 * card) are advisory — the booker may acknowledge and proceed.
 */
public final class CardValidationResult {

  @Getter private final List<String> errors;
  @Getter private final List<String> warnings;

  public CardValidationResult(
      @NonNull final List<String> errors, @NonNull final List<String> warnings) {
    this.errors = List.copyOf(errors);
    this.warnings = List.copyOf(warnings);
  }

  /** Returns {@code true} when there are no hard errors (warnings are allowed). */
  public boolean isValid() {
    return errors.isEmpty();
  }

  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }
}
