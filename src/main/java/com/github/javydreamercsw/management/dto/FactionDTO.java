package com.github.javydreamercsw.management.dto;

import java.util.List;
import lombok.Data;

/**
 * Data Transfer Object for Faction entity. Used for JSON serialization/deserialization in sync and
 * export operations.
 */
@Data
public class FactionDTO {
  private String name;
  private String description;
  private String leader; // Leader wrestler name (will be resolved to Wrestler entity)
  private List<String>
      members; // List of member wrestler names (will be resolved to Wrestler entities)
  private List<String> teams; // List of team names associated with this faction

  private Boolean isActive;
  private String formedDate; // ISO date format (YYYY-MM-DD)
  private String disbandedDate; // ISO date format (YYYY-MM-DD)
  private String externalId; // External system ID (e.g., Notion page ID)
}
