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

import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotionPageDataExtractor {

  private final NotionHandler notionHandler;

  public NotionPageDataExtractor(NotionHandler notionHandler) {
    this.notionHandler = notionHandler;
  }

  /** Extracts name from any NotionPage type using raw properties. */
  public String extractNameFromNotionPage(@NonNull NotionPage page) {
    if (page.getRawProperties() != null) {
      // Try different possible property names for name
      Object name = page.getRawProperties().get("Name");
      if (name == null) {
        name = page.getRawProperties().get("Title"); // Alternative name property
      }

      if (name != null) {
        String nameStr = extractTextFromProperty(name);
        if (nameStr != null && !nameStr.trim().isEmpty() && !"N/A".equals(nameStr)) {
          return nameStr.trim();
        }
      }

      log.debug("Name property not found or empty for page: {}", page.getId());
    }
    return "Unknown";
  }

  /** Extracts description from any NotionPage type using raw properties. */
  public String extractDescriptionFromNotionPage(@NonNull NotionPage page) {
    if (page.getRawProperties() != null) {
      Object description = page.getRawProperties().get("Description");
      return description != null ? description.toString() : "";
    }
    return "";
  }

  /** Extracts text content from a Notion property object. */
  public String extractTextFromProperty(Object property) {
    if (property == null) {
      return null;
    }

    // Handle Map objects that mimic Notion's structure
    if (property instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) property;
      if (map.containsKey("title")) {
        Object titleObj = map.get("title");
        if (titleObj instanceof List) {
          @SuppressWarnings("unchecked")
          List<Object> titleList = (List<Object>) titleObj;
          if (!titleList.isEmpty() && titleList.get(0) instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> titleMap = (Map<String, Object>) titleList.get(0);
            if (titleMap.containsKey("text") && titleMap.get("text") instanceof Map) {
              @SuppressWarnings("unchecked")
              Map<String, Object> textMap = (Map<String, Object>) titleMap.get("text");
              if (textMap.containsKey("content")) {
                return textMap.get("content").toString();
              }
            }
          }
        }
      }
    }

    // Handle PageProperty objects (from Notion API)
    if (property instanceof notion.api.v1.model.pages.PageProperty pageProperty) {

      // Handle title properties
      if (pageProperty.getTitle() != null && !pageProperty.getTitle().isEmpty()) {
        return pageProperty.getTitle().get(0).getPlainText();
      }

      // Handle rich text properties
      if (pageProperty.getRichText() != null && !pageProperty.getRichText().isEmpty()) {
        return pageProperty.getRichText().get(0).getPlainText();
      }

      // Handle select properties
      if (pageProperty.getSelect() != null) {
        return pageProperty.getSelect().getName();
      }

      // Handle other property types as needed
      log.debug("Unhandled PageProperty type: {}", pageProperty.getType());
      return null;
    }

    // Handle simple string values
    String str = property.toString().trim();

    // Avoid returning the entire PageProperty string representation
    if (str.startsWith("PageProperty(")) {
      log.warn("Property extraction returned PageProperty object string - this indicates a bug");
      return null;
    }

    return str.isEmpty() ? null : str;
  }

  /** Extracts a property value as a string from raw properties map. */
  public String extractPropertyAsString(
      @NonNull java.util.Map<String, Object> rawProperties, @NonNull String propertyName) {

    Object property = rawProperties.get(propertyName);
    return extractTextFromProperty(property);
  }

  /** Extracts faction from a team page. */
  public String extractFactionFromNotionPage(
      @NonNull com.github.javydreamercsw.base.ai.notion.NotionPage page) {
    if (page.getRawProperties() != null) {
      // Try different possible property names for faction
      Object faction = page.getRawProperties().get("Faction");
      if (faction == null) {
        faction = page.getRawProperties().get("faction");
      }

      if (faction != null && !faction.toString().trim().isEmpty()) {
        String factionStr = faction.toString().trim();

        // If it shows as "X relations", preserve existing faction data
        if (factionStr.matches("\\d+ relations?")) {
          log.debug(
              "Faction shows as relationship count ({}), preserving existing faction", factionStr);
          return null;
        }

        // If it looks like a relationship ID, don't use it
        if (factionStr.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
          log.debug(
              "Faction appears to be a relationship ID, preserving existing faction: {}",
              factionStr);
          return null;
        }

        // If it's a readable name, use it
        log.debug("Found faction name: {}", factionStr);
        return factionStr;
      }
    }
    return null;
  }
}
