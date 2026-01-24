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
package com.github.javydreamercsw.base.util;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for retrieving configuration values from system properties and environment
 * variables. This class provides a consistent way to access configuration values with fallback
 * mechanisms.
 *
 * <p>The retrieval order is:
 *
 * <ol>
 *   <li>System property (e.g., -DNOTION_TOKEN=value)
 *   <li>Environment variable (e.g., NOTION_TOKEN=value)
 *   <li>Default value (if provided)
 * </ol>
 */
@Slf4j
public final class EnvironmentVariableUtil {

  private EnvironmentVariableUtil() {
    // Utility class - prevent instantiation
  }

  /**
   * Retrieves a configuration value with fallback from system property to environment variable.
   *
   * @param key the configuration key (e.g., "NOTION_TOKEN")
   * @return the configuration value, or null if not found
   */
  public static String getValue(@NonNull String key) {
    return getValue(key, null);
  }

  /**
   * Retrieves a configuration value with fallback from system property to environment variable to
   * default.
   *
   * @param key the configuration key (e.g., "NOTION_TOKEN")
   * @param defaultValue the default value to return if neither property nor environment variable is
   *     set
   * @return the configuration value, or the default value if not found
   */
  public static String getValue(@NonNull String key, String defaultValue) {
    // First try system property
    String value = System.getProperty(key);
    if (value != null && !value.trim().isEmpty()) {
      log.debug("Found configuration value from system property: {}", key);
      return value.trim();
    }

    // Then try environment variable
    value = System.getenv(key);
    if (value != null && !value.trim().isEmpty()) {
      log.debug("Found configuration value from environment variable: {}", key);
      return value.trim();
    }

    // Return default value
    if (defaultValue != null) {
      log.debug("Using default value for {}", key);
    } else {
      log.debug("No value found for {}", key);
    }
    return defaultValue;
  }

  /**
   * Retrieves the Notion token from system property or environment variable. This is a convenience
   * method for the most common use case.
   *
   * @return the Notion token, or null if not found
   */
  public static String getNotionToken() {
    return getValue("NOTION_TOKEN");
  }

  /**
   * Checks if a configuration value is available (not null and not empty).
   *
   * @param key the configuration key
   * @return true if a value is available, false otherwise
   */
  public static boolean isAvailable(String key) {
    String value = getValue(key);
    return value != null && !value.trim().isEmpty();
  }

  /**
   * Checks if the Notion token is available.
   *
   * @return true if the Notion token is available, false otherwise
   */
  public static boolean isNotionTokenAvailable() {
    return isAvailable("NOTION_TOKEN");
  }
}
