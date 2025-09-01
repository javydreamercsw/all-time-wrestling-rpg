package com.github.javydreamercsw.management.dto;

import java.time.Instant;
import lombok.Data;
import lombok.NonNull;

/**
 * Data Transfer Object for injury type sync operations between Notion and the database. Based on
 * REAL Notion Injuries database structure - represents injury TYPES for the card game, not
 * individual wrestler injuries.
 */
@Data
public class InjuryTypeDTO {

  // ==================== CORE PROPERTIES ====================

  /** External ID from Notion for duplicate detection */
  @NonNull private String externalId;

  /** Name of the injury type (e.g., "Head injury", "Back injury") */
  @NonNull private String injuryName;

  // ==================== GAME EFFECT PROPERTIES ====================

  /** Health effect penalty (numeric, e.g., -3, -1, -2) */
  private Integer healthEffect;

  /** Stamina effect penalty (numeric, e.g., 0, -3, -2) */
  private Integer staminaEffect;

  /** Card effect penalty (numeric, e.g., -2, 0, -1) */
  private Integer cardEffect;

  /** Special game effects description (e.g., "No reversal ability") */
  private String specialEffects;

  // ==================== METADATA ====================

  /** Notion creation timestamp */
  private Instant createdTime;

  /** Notion last edit timestamp */
  private Instant lastEditedTime;

  /** User who created the record in Notion */
  private String createdBy;

  /** User who last edited the record in Notion */
  private String lastEditedBy;

  // ==================== CONSTRUCTORS ====================

  public InjuryTypeDTO() {}

  public InjuryTypeDTO(@NonNull String externalId, @NonNull String injuryName) {
    this.externalId = externalId;
    this.injuryName = injuryName;
  }

  // ==================== VALIDATION METHODS ====================

  /**
   * Validates that this DTO contains the minimum required information for sync.
   *
   * @return true if the DTO is valid for sync operations
   */
  public boolean isValid() {
    return externalId != null
        && !externalId.trim().isEmpty()
        && injuryName != null
        && !injuryName.trim().isEmpty();
  }

  /**
   * Gets a summary string for logging and debugging.
   *
   * @return Summary of the injury type DTO
   */
  public String getSummary() {
    return String.format(
        "InjuryTypeDTO[id=%s, name='%s', health=%d, stamina=%d, card=%d]",
        externalId,
        injuryName,
        healthEffect != null ? healthEffect : 0,
        staminaEffect != null ? staminaEffect : 0,
        cardEffect != null ? cardEffect : 0);
  }

  /**
   * Gets the effective health effect, defaulting to 0 if not specified.
   *
   * @return Health effect value
   */
  public int getEffectiveHealthEffect() {
    return healthEffect != null ? healthEffect : 0;
  }

  /**
   * Gets the effective stamina effect, defaulting to 0 if not specified.
   *
   * @return Stamina effect value
   */
  public int getEffectiveStaminaEffect() {
    return staminaEffect != null ? staminaEffect : 0;
  }

  /**
   * Gets the effective card effect, defaulting to 0 if not specified.
   *
   * @return Card effect value
   */
  public int getEffectiveCardEffect() {
    return cardEffect != null ? cardEffect : 0;
  }

  /**
   * Checks if this injury has any special effects.
   *
   * @return true if special effects are defined and not "N/A"
   */
  public boolean hasSpecialEffects() {
    return specialEffects != null
        && !specialEffects.trim().isEmpty()
        && !"N/A".equals(specialEffects.trim());
  }

  /** Calculates the total penalty severity of this injury type. */
  public int getTotalPenalty() {
    return Math.abs(getEffectiveHealthEffect())
        + Math.abs(getEffectiveStaminaEffect())
        + Math.abs(getEffectiveCardEffect());
  }
}
