package com.github.javydreamercsw.management.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for loading SegmentRule data from JSON files. Used for deserializing segment
 * rule configuration from external files.
 */
@Data
@NoArgsConstructor
public class SegmentRuleDTO {

  /** Name of the segment rule */
  @JsonProperty("name")
  private String name;

  /** Description of the segment rule */
  @JsonProperty("description")
  private String description;

  /**
   * Whether this rule requires high heat between wrestlers. High heat rules are used for intense
   * rivalries.
   */
  @JsonProperty("requiresHighHeat")
  private boolean requiresHighHeat;

  /**
   * Check if this is a promo rule (non-wrestling segment).
   *
   * @return true if this is a promo rule
   */
  public boolean isPromoRule() {
    return "Promo".equals(name) || name.toLowerCase().contains("promo");
  }

  /**
   * Check if this is a high-intensity rule for extreme segments.
   *
   * @return true if this rule is for extreme/dangerous segments
   */
  public boolean isExtremeRule() {
    return requiresHighHeat
        || name.toLowerCase().contains("extreme")
        || name.toLowerCase().contains("deathmatch")
        || name.toLowerCase().contains("exploding");
  }
}
