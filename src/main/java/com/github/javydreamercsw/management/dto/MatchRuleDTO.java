package com.github.javydreamercsw.management.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for loading MatchRule data from JSON files. Used for deserializing match
 * rule configuration from external files.
 */
@Data
@NoArgsConstructor
public class MatchRuleDTO {

  /** Name of the match rule */
  @JsonProperty("name")
  private String name;

  /** Description of the match rule */
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
   * Check if this is a high-intensity rule for extreme matches.
   *
   * @return true if this rule is for extreme/dangerous matches
   */
  public boolean isExtremeRule() {
    return requiresHighHeat
        || name.toLowerCase().contains("extreme")
        || name.toLowerCase().contains("deathmatch")
        || name.toLowerCase().contains("exploding");
  }
}
