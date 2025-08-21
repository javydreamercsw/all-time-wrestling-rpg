package com.github.javydreamercsw.management.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for loading ShowTemplate data from JSON files. Used for deserializing show
 * template configuration from external files.
 */
@Data
@NoArgsConstructor
public class ShowTemplateDTO {

  /** Name of the show template */
  @JsonProperty("name")
  private String name;

  /** Description of the show template */
  @JsonProperty("description")
  private String description;

  /** Name of the show type this template belongs to */
  @JsonProperty("showTypeName")
  private String showTypeName;

  /** URL to the Notion page containing template details */
  @JsonProperty("notionUrl")
  private String notionUrl;

  /**
   * Check if this is a Premium Live Event template.
   *
   * @return true if this template is for a PLE
   */
  public boolean isPremiumLiveEvent() {
    return "Premium Live Event (PLE)".equals(showTypeName);
  }

  /**
   * Check if this is a Weekly show template.
   *
   * @return true if this template is for a weekly show
   */
  public boolean isWeeklyShow() {
    return "Weekly".equals(showTypeName);
  }

  /**
   * Get a clean show type name without parentheses.
   *
   * @return simplified show type name
   */
  public String getSimpleShowTypeName() {
    if (showTypeName == null) {
      return null;
    }

    if (showTypeName.contains("(")) {
      return showTypeName.substring(0, showTypeName.indexOf('(')).trim();
    }

    return showTypeName;
  }
}
