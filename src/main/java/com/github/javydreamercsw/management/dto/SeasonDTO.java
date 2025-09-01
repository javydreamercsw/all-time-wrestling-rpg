package com.github.javydreamercsw.management.dto;

import lombok.Data;

/**
 * Data Transfer Object for Season entity. Used for JSON serialization/deserialization in sync and
 * export operations.
 */
@Data
public class SeasonDTO {
  private String name;
  private String description;
  private String startDate; // ISO date format (YYYY-MM-DD)
  private String endDate; // ISO date format (YYYY-MM-DD)
  private Boolean isActive;
  private Integer showsPerPpv;
  private String notionId; // External system ID (e.g., Notion page ID)
}
