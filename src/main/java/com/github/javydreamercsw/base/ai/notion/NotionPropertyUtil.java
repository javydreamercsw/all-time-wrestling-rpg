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
package com.github.javydreamercsw.base.ai.notion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/** Static utilities for extracting typed values from raw Notion property objects. */
@Slf4j
public final class NotionPropertyUtil {

  private static final String UUID_PATTERN =
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

  private NotionPropertyUtil() {}

  public static String extractRelationId(final Object property) {
    List<String> ids = extractRelationIds(property);
    return ids.isEmpty() ? null : ids.get(0);
  }

  public static List<String> extractRelationIds(final Object property) {
    List<String> ids = new ArrayList<>();
    if (property == null) {
      return ids;
    }
    if (property instanceof String str) {
      if (str.matches(UUID_PATTERN)) {
        ids.add(str);
      }
    } else if (property instanceof List<?> list) {
      for (Object item : list) {
        if (item instanceof String str && str.matches(UUID_PATTERN)) {
          ids.add(str);
        } else if (item instanceof Map<?, ?> map) {
          Object id = map.get("id");
          if (id instanceof String str) {
            ids.add(str);
          }
        }
      }
    } else if (property instanceof Map<?, ?> map) {
      Object id = map.get("id");
      if (id instanceof String str) {
        ids.add(str);
      }
    }
    return ids;
  }

  public static String extractStringProperty(
      @NonNull final NotionPage page, @NonNull final String propertyName) {
    if (page.getRawProperties() == null) {
      return null;
    }
    Object property = page.getRawProperties().get(propertyName);
    if (property == null) {
      return null;
    }
    String propertyStr = property.toString().trim();
    if (propertyStr.matches("\\d+ (items?|relations?)")) {
      log.debug(
          "Property '{}' shows as relationship count ({}), cannot resolve in sync mode",
          propertyName,
          propertyStr);
      return null;
    }
    if (propertyStr.contains(",")) {
      String firstPart = propertyStr.split(",")[0].trim();
      if (!firstPart.isEmpty()) {
        return firstPart;
      }
    }
    if (!propertyStr.isEmpty() && !propertyStr.matches(UUID_PATTERN)) {
      return propertyStr;
    }
    return null;
  }
}
