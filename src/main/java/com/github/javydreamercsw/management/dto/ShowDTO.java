package com.github.javydreamercsw.management.dto;

import lombok.Data;

/**
 * Data Transfer Object for Show entity. Used for JSON serialization/deserialization in export
 * operations.
 */
@Data
public class ShowDTO {
  private String name;
  private String showType;
  private String description;
  private String showDate; // ISO date format (YYYY-MM-DD)
  private String seasonName; // Reference to season by name
  private String templateName; // Reference to show template by name
  private String externalId; // External system ID (e.g., Notion page ID)
}
