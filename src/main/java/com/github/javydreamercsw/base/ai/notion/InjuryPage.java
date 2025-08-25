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
    // Injury-specific properties only (common properties inherited from base)

    /** The wrestler who sustained the injury (relation to Wrestlers database) */
    private Property Wrestler;

    /** Description of the injury */
    private Property Description;

    /** Severity level of the injury (MINOR, MODERATE, SEVERE, CRITICAL) */
    private Property Severity;

    /** Health penalty caused by this injury */
    private Property HealthPenalty;

    /** Whether the injury is currently active */
    private Property IsActive;

    /** Date when the injury occurred */
    private Property InjuryDate;

    /** Date when the injury was healed (if applicable) */
    private Property HealedDate;

    /** Cost in fans to attempt healing this injury */
    private Property HealingCost;

    /** Additional notes about the injury */
    private Property InjuryNotes;

    /** How the injury was sustained (match, training, etc.) */
    private Property InjurySource;

    /** Match where the injury occurred (relation to Matches database) */
    private Property InjuryMatch;

    /** Recovery progress or status */
    private Property RecoveryStatus;

    /** Expected recovery time */
    private Property ExpectedRecoveryTime;
  }
}
