package com.github.javydreamercsw.management.dto;

import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import java.time.Instant;
import lombok.Data;
import lombok.NonNull;

/**
 * Data Transfer Object for injury sync operations between Notion and the database. Contains all
 * necessary information to create or update injury records.
 */
@Data
public class InjuryDTO {

  // ==================== CORE PROPERTIES ====================

  /** External ID from Notion for duplicate detection */
  @NonNull private String externalId;

  /** Name/title of the injury */
  @NonNull private String name;

  /** Detailed description of the injury */
  private String description;

  // ==================== INJURY DETAILS ====================

  /** Name of the wrestler who sustained the injury */
  @NonNull private String wrestlerName;

  /** Severity level of the injury */
  @NonNull private InjurySeverity severity;

  /** Health penalty caused by this injury */
  private Integer healthPenalty;

  /** Whether the injury is currently active */
  private Boolean isActive;

  // ==================== DATES ====================

  /** Date when the injury occurred */
  private Instant injuryDate;

  /** Date when the injury was healed (if applicable) */
  private Instant healedDate;

  // ==================== ADDITIONAL INFO ====================

  /** Cost in fans to attempt healing this injury */
  private Integer healingCost;

  /** Additional notes about the injury */
  private String injuryNotes;

  /** How the injury was sustained (match, training, etc.) */
  private String injurySource;

  /** Name of the match where injury occurred (if applicable) */
  private String injuryMatchName;

  /** Recovery progress or status */
  private String recoveryStatus;

  /** Expected recovery time in days */
  private Integer expectedRecoveryDays;

  // ==================== METADATA ====================

  /** Notion creation timestamp */
  private Instant createdTime;

  /** Notion last edit timestamp */
  private Instant lastEditedTime;

  /** User who created the record in Notion */
  private String createdBy;

  /** User who last edited the record in Notion */
  private String lastEditedBy;

  // ==================== VALIDATION METHODS ====================

  /**
   * Validates that this DTO contains the minimum required information for sync.
   *
   * @return true if the DTO is valid for sync operations
   */
  public boolean isValid() {
    return externalId != null
        && !externalId.trim().isEmpty()
        && name != null
        && !name.trim().isEmpty()
        && wrestlerName != null
        && !wrestlerName.trim().isEmpty()
        && severity != null;
  }

  /**
   * Gets a summary string for logging and debugging.
   *
   * @return Summary of the injury DTO
   */
  public String getSummary() {
    return String.format(
        "InjuryDTO[id=%s, name='%s', wrestler='%s', severity=%s, active=%s]",
        externalId, name, wrestlerName, severity, isActive);
  }

  /**
   * Checks if this injury is currently active.
   *
   * @return true if the injury is active, false if healed or inactive
   */
  public boolean isCurrentlyActive() {
    return Boolean.TRUE.equals(isActive) && healedDate == null;
  }

  /**
   * Gets the effective health penalty, defaulting to severity-based values if not specified.
   *
   * @return Health penalty value
   */
  public int getEffectiveHealthPenalty() {
    if (healthPenalty != null && healthPenalty > 0) {
      return healthPenalty;
    }

    // Default health penalties based on severity
    return switch (severity) {
      case MINOR -> 5;
      case MODERATE -> 10;
      case SEVERE -> 20;
      case CRITICAL -> 35;
    };
  }

  /**
   * Gets the effective healing cost, defaulting to severity-based values if not specified.
   *
   * @return Healing cost in fans
   */
  public int getEffectiveHealingCost() {
    if (healingCost != null && healingCost > 0) {
      return healingCost;
    }

    // Default healing costs based on severity
    return switch (severity) {
      case MINOR -> 100;
      case MODERATE -> 250;
      case SEVERE -> 500;
      case CRITICAL -> 1000;
    };
  }
}
