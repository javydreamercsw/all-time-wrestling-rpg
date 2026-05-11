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
package com.github.javydreamercsw.base.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility class for sanitizing log messages to prevent log injection vulnerabilities. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LogSanitizer {

  /**
   * Sanitizes a string by replacing newline and carriage return characters.
   *
   * @param input The string to sanitize.
   * @return The sanitized string, or an empty string if input is null.
   */
  public static String sanitize(final Object input) {
    if (input == null) {
      return "";
    }
    return input.toString().replace('\n', '_').replace('\r', '_');
  }
}
