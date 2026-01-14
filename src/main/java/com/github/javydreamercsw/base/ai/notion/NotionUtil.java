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

import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import java.util.Collections;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.NotionClient;
import notion.api.v1.exception.NotionAPIError;
import notion.api.v1.model.databases.DatabaseProperty;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageProperty;
import notion.api.v1.model.users.User;

@Slf4j
@UtilityClass
public class NotionUtil {
  public String getValue(@NonNull NotionClient client, @NonNull PageProperty value) {
    return getValue(client, value, true);
  }

  public String getValue(
      @NonNull NotionClient client, @NonNull PageProperty value, boolean resolveRelationships) {
    try {
      // Handle cases where type is null but we can infer the type from populated fields
      String propertyType;

      if (value.getType() != null) {
        propertyType = value.getType().getValue();
      } else {
        // Infer property type from populated fields when type is null
        propertyType = inferPropertyType(value);
        if (propertyType == null) {
          log.debug("Property type is null and cannot be inferred for property: {}", value);
          return "N/A";
        }
        log.debug("Inferred property type '{}' for property with null type", propertyType);
      }

      return switch (propertyType) {
        case "formula" -> value.getFormula() != null ? getFormulaValue(value.getFormula()) : "N/A";
        case "people" ->
            value.getPeople() != null && !value.getPeople().isEmpty()
                ? value.getPeople().stream().findFirst().map(User::getName).orElse("N/A")
                : "N/A";
        case "created_by" -> value.getCreatedBy() != null ? value.getCreatedBy().getName() : "N/A";
        case "last_edited_by" ->
            value.getLastEditedBy() != null ? value.getLastEditedBy().getName() : "N/A";
        case "created_time" -> value.getCreatedTime() != null ? value.getCreatedTime() : "N/A";
        case "number" -> value.getNumber() != null ? value.getNumber().toString() : "N/A";
        case "last_edited_time" ->
            value.getLastEditedTime() != null ? value.getLastEditedTime() : "N/A";
        case "unique_id" ->
            value.getUniqueId() != null
                ? value.getUniqueId().getPrefix() + "-" + value.getUniqueId().getNumber()
                : "N/A";
        case "title" ->
            value.getTitle() != null && !value.getTitle().isEmpty()
                ? value.getTitle().get(0).getPlainText()
                : "N/A";
        case "rich_text" -> {
          // Handle rich_text properties (commonly used for text fields in Notion)
          if (value.getRichText() != null && !value.getRichText().isEmpty()) {
            yield value.getRichText().stream()
                .map(PageProperty.RichText::getPlainText)
                .filter(text -> text != null && !text.trim().isEmpty())
                .reduce((a, b) -> a + " " + b)
                .orElse("N/A");
          } else {
            yield "N/A";
          }
        }
        case "relation" -> {
          if (value.getRelation() == null || value.getRelation().isEmpty()) {
            yield "N/A";
          } else if (!resolveRelationships) {
            // Fast mode: return comma-separated IDs without resolving names
            yield value.getRelation().stream()
                .map(PageProperty.PageReference::getId)
                .collect(Collectors.joining(", "));
          } else {
            // Full mode: resolve relationship names (expensive)
            yield value.getRelation().stream()
                .map(
                    relation -> {
                      Page relatedPage =
                          executeWithRetry(
                              () -> client.retrievePage(relation.getId(), Collections.emptyList()));

                      // Try multiple common title property names
                      String[] titlePropertyNames = {
                        "Name", "Title", "Title Name", "Championship", "name", "title"
                      };
                      for (String propertyName : titlePropertyNames) {
                        PageProperty titleProperty = relatedPage.getProperties().get(propertyName);
                        if (titleProperty != null
                            && titleProperty.getTitle() != null
                            && !titleProperty.getTitle().isEmpty()) {
                          return titleProperty.getTitle().get(0).getPlainText();
                        }
                      }

                      // If no title property found, log available properties for debugging
                      log.debug(
                          "No title property found for relation {}. Available properties: {}",
                          relation.getId(),
                          relatedPage.getProperties().keySet());
                      return relation.getId();
                    })
                .reduce((a, b) -> a + ", " + b)
                .orElse("N/A");
          }
        }
        case "select" -> {
          // Handle select properties (dropdown with single selection)
          if (value.getSelect() != null) {
            yield value.getSelect().getName();
          } else {
            yield "N/A";
          }
        }
        case "status" -> {
          // Handle status properties (workflow status)
          if (value.getStatus() != null) {
            yield value.getStatus().getName();
          } else {
            yield "N/A";
          }
        }
        case "multi_select" -> {
          // Handle multi_select properties (dropdown with multiple selections)
          if (value.getMultiSelect() != null && !value.getMultiSelect().isEmpty()) {
            yield value.getMultiSelect().stream()
                .map(DatabaseProperty.MultiSelect.Option::getName)
                .filter(name -> name != null && !name.trim().isEmpty())
                .reduce((a, b) -> a + ", " + b)
                .orElse("N/A");
          } else {
            yield "N/A";
          }
        }
        case "date" -> {
          // Handle date properties
          if (value.getDate() != null && value.getDate().getStart() != null) {
            String dateStr = value.getDate().getStart();
            // The Notion API sometimes prefixes dates with @, so we remove it.
            if (dateStr.startsWith("@")) {
              yield dateStr.substring(1);
            }
            yield dateStr;
          } else {
            yield "N/A";
          }
        }
        case "checkbox" -> {
          // Handle checkbox properties
          if (value.getCheckbox() != null) {
            yield value.getCheckbox().toString();
          } else {
            yield "false";
          }
        }
        case "url" -> {
          // Handle URL properties
          if (value.getUrl() != null && !value.getUrl().trim().isEmpty()) {
            yield value.getUrl();
          } else {
            yield "N/A";
          }
        }
        case "email" -> {
          // Handle email properties
          if (value.getEmail() != null && !value.getEmail().trim().isEmpty()) {
            yield value.getEmail();
          } else {
            yield "N/A";
          }
        }
        case "phone_number" -> {
          // Handle phone number properties
          if (value.getPhoneNumber() != null && !value.getPhoneNumber().trim().isEmpty()) {
            yield value.getPhoneNumber();
          } else {
            yield "N/A";
          }
        }
        default -> {
          // Log unhandled property types for debugging
          log.warn("Unhandled property type '{}' for property: {}", propertyType, value);
          yield "N/A";
        }
      };
    } catch (Exception e) {
      log.debug("Exception in getValue method: {}", e.getMessage());
      return "N/A";
    }
  }

  /**
   * Infers the property type from populated fields when the type field is null. This is a fallback
   * mechanism for cases where the Notion API doesn't properly set the type.
   */
  public String inferPropertyType(@NonNull PageProperty value) {
    // Check each possible property type by looking at which field is populated
    if (value.getTitle() != null && !value.getTitle().isEmpty()) {
      return "title";
    }
    if (value.getRichText() != null && !value.getRichText().isEmpty()) {
      return "rich_text";
    }
    if (value.getSelect() != null) {
      return "select";
    }
    if (value.getStatus() != null) {
      return "status";
    }
    if (value.getMultiSelect() != null && !value.getMultiSelect().isEmpty()) {
      return "multi_select";
    }
    if (value.getDate() != null) {
      return "date";
    }
    if (value.getCheckbox() != null) {
      return "checkbox";
    }
    if (value.getNumber() != null) {
      return "number";
    }
    if (value.getUrl() != null) {
      return "url";
    }
    if (value.getEmail() != null) {
      return "email";
    }
    if (value.getPhoneNumber() != null) {
      return "phone_number";
    }
    if (value.getPeople() != null && !value.getPeople().isEmpty()) {
      return "people";
    }
    if (value.getRelation() != null && !value.getRelation().isEmpty()) {
      return "relation";
    }
    if (value.getFormula() != null) {
      return "formula";
    }
    if (value.getCreatedBy() != null) {
      return "created_by";
    }
    if (value.getLastEditedBy() != null) {
      return "last_edited_by";
    }
    if (value.getCreatedTime() != null) {
      return "created_time";
    }
    if (value.getLastEditedTime() != null) {
      return "last_edited_time";
    }
    if (value.getUniqueId() != null) {
      return "unique_id";
    }

    // Could not infer type
    return null;
  }

  /** Helper method to extract values from formula properties based on their result type. */
  public String getFormulaValue(@NonNull PageProperty.Formula formula) {

    // Check the formula result type and extract accordingly
    if (formula.getString() != null) {
      return formula.getString();
    } else if (formula.getNumber() != null) {
      return formula.getNumber().toString();
    } else if (formula.getBoolean() != null) {
      return formula.getBoolean().toString();
    } else if (formula.getDate() != null) {
      // Handle formula date - extract the start date and format it properly
      if (formula.getDate().getStart() != null) {
        String dateStr = formula.getDate().getStart();
        // The Notion API sometimes prefixes dates with @, so we remove it.
        if (dateStr.startsWith("@")) {
          return dateStr.substring(1);
        }
        return dateStr;
      } else {
        return "N/A";
      }
    } else {
      return "null";
    }
  }

  public <T> T executeWithRetry(Supplier<T> action) {
    RetryPolicy<T> rateLimitPolicy =
        RetryPolicy.<T>builder()
            .handleIf(
                e -> {
                  if (e instanceof NotionAPIError notionError) {
                    String message = notionError.getMessage().toLowerCase();
                    if (message.contains("429")
                        || message.contains("rate limit")
                        || message.contains("too many requests")
                        || message.contains("rate_limited")) {
                      return true;
                    }
                    return notionError.getError().getStatus() == 429
                        || "rate_limited".equals(notionError.getError().getCode());
                  }
                  return false;
                })
            .withBackoff(1, 5, java.time.temporal.ChronoUnit.SECONDS)
            .withMaxRetries(3)
            .onRetry(e -> log.warn("Rate limited by Notion API. Retrying...", e.getLastException()))
            .build();

    RetryPolicy<T> serverRetryPolicy =
        RetryPolicy.<T>builder()
            .handleIf(
                e ->
                    e instanceof NotionAPIError
                        && ((NotionAPIError) e).getError().getStatus() >= 500)
            .withBackoff(1, 5, java.time.temporal.ChronoUnit.SECONDS)
            .withMaxRetries(3)
            .onRetry(e -> log.warn("Server side error. Retrying...", e.getLastException()))
            .build();

    return Failsafe.with(rateLimitPolicy, serverRetryPolicy).get(action::get);
  }
}
