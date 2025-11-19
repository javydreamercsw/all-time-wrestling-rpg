package com.github.javydreamercsw.base.ai.notion;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents an injury page from the Notion Injuries database. Contains injury-specific properties
 * for wrestler injury tracking.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class InjuryPage extends NotionPage {
  private NotionProperties properties;

  @Data
  @EqualsAndHashCode(callSuper = false)
  public static class NotionProperties extends NotionPage.BaseNotionProperties {
    // Injury-specific properties based on REAL Notion structure

    /** Name of the injury type (e.g., "Head injury", "Back injury") */
    private Property InjuryName;

    /** Health effect penalty (numeric, e.g., -3, -1, -2) */
    private Property HealthEffect;

    /** Stamina effect penalty (numeric, e.g., 0, -3, -2) */
    private Property StaminaEffect;

    /** Card effect penalty (numeric, e.g., -2, 0, -1) */
    private Property CardEffect;

    /** Special game effects description (text, e.g., "No reversal ability") */
    private Property SpecialEffects;
  }
}
